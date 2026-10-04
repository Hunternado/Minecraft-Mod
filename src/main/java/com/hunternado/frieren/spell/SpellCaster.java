package com.hunternado.frieren.spell;

import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.data.SpellProgress;
import com.hunternado.frieren.item.StaffItem;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.quest.QuestManager;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The single server-side entry point for casting. Clients only ever request "cast slot N"; everything
 * else (knowledge, cooldown, mana, effects) is decided here.
 */
public final class SpellCaster {
    private static final float BASE_MASTERY_GAIN = 0.8F;
    private static final float HIT_MASTERY_BONUS = 0.6F;

    private SpellCaster() {}

    // ---------------------------------------------------------------------------------------
    // Scaling helpers (shared with UI through the same formulas)
    // ---------------------------------------------------------------------------------------

    public static StaffItem.Tier heldStaff(ServerPlayer player) {
        return heldStaffOf(player.getMainHandItem(), player.getOffhandItem());
    }

    public static StaffItem.Tier heldStaffOf(ItemStack main, ItemStack off) {
        if (main.getItem() instanceof StaffItem staff) {
            return staff.tier();
        }
        if (off.getItem() instanceof StaffItem staff) {
            return staff.tier();
        }
        return StaffItem.Tier.NONE;
    }

    public static float effectiveCost(SpellDefinition spell, float masteryFraction, StaffItem.Tier staff) {
        return (float) (spell.manaCost() * (1.0F - 0.3F * masteryFraction) * staff.costMultiplier() * FrierenConfig.spellCostMultiplier());
    }

    public static int effectiveCooldown(SpellDefinition spell, float masteryFraction, StaffItem.Tier staff) {
        return (int) Math.ceil(spell.cooldown() * (1.0F - 0.25F * masteryFraction) * staff.cooldownMultiplier() * FrierenConfig.spellCooldownMultiplier());
    }

    public static float effectivePower(float masteryFraction, StaffItem.Tier staff) {
        return (float) ((1.0F + 0.5F * masteryFraction) * staff.powerMultiplier() * FrierenConfig.spellDamageMultiplier());
    }

    public static int effectiveCastTime(SpellDefinition spell, float masteryFraction) {
        return Math.round(spell.castTime() * (1.0F - 0.3F * masteryFraction));
    }

    static float scaleCost(float raw, SpellContext ctx) {
        return (float) (raw * (1.0F - 0.3F * ctx.masteryFraction()) * ctx.staff().costMultiplier() * FrierenConfig.spellCostMultiplier());
    }

    // ---------------------------------------------------------------------------------------
    // Requests from the client
    // ---------------------------------------------------------------------------------------

    public static void requestCast(ServerPlayer player, int slot) {
        MagicData data = MagicData.get(player);
        if (data == null || player.isSpectator() || !player.isAlive()) {
            return;
        }
        Identifier spellId = data.slot(slot);
        if (spellId == null) {
            Messages.actionBar(player, Component.translatable("message.frieren.empty_slot"));
            return;
        }
        if (data.selectedSlot() != slot) {
            data.setSelectedSlot(slot);
        }
        tryCast(player, data, spellId);
    }

    public static void releaseChannel(ServerPlayer player) {
        MagicData data = MagicData.get(player);
        if (data != null && data.channelingSpell() != null) {
            data.stopChannel();
            data.markDirty();
        }
    }

    /** Validation shared by key casting and commands. */
    public static void tryCast(ServerPlayer player, MagicData data, Identifier spellId) {
        ServerLevel level = player.level();
        long time = level.getGameTime();

        if (!data.knows(spellId)) {
            fail(player, Component.translatable("message.frieren.unknown_spell"));
            return;
        }
        SpellDefinition spell = SpellManager.server(spellId);
        if (spell == null) {
            fail(player, Component.translatable("message.frieren.missing_definition", spellId.toString()));
            return;
        }
        SpellBehavior behavior = SpellBehaviors.get(spell.behavior());
        if (behavior == null) {
            fail(player, Component.translatable("message.frieren.missing_definition", spellId.toString()));
            return;
        }

        // Toggles switch off for free and ignore every other restriction.
        if (behavior.isToggle() && behavior.isActive(data)) {
            behavior.deactivate(player, data);
            return;
        }

        if (isSilenced(player, data, time)) {
            fail(player, Component.translatable("message.frieren.enthralled"));
            return;
        }
        if (data.channelingSpell() != null || data.pendingCast() != null) {
            return;
        }
        int cooldown = data.cooldownRemaining(spellId, time);
        if (cooldown > 0) {
            Messages.actionBar(player, Component.translatable("message.frieren.cooldown", spell.displayName(), String.format("%.1f", cooldown / 20.0F)));
            return;
        }

        SpellProgress progress = data.progress(spellId);
        float mastery = progress != null ? progress.masteryFraction() : 0.0F;
        StaffItem.Tier staff = heldStaff(player);
        float cost = effectiveCost(spell, mastery, staff);
        if (data.mana() < cost) {
            fail(player, Component.translatable("message.frieren.no_mana", String.format("%.0f", cost)));
            return;
        }
        if (spell.healthCost() > 0.0F && player.getHealth() <= spell.healthCost() + 1.0F) {
            fail(player, Component.translatable("message.frieren.no_health"));
            return;
        }

        int castTime = effectiveCastTime(spell, mastery);
        if (castTime > 0) {
            data.beginCast(spellId, castTime);
            Messages.actionBar(player, Component.translatable("message.frieren.casting", spell.displayName()));
            return;
        }
        execute(player, data, spell, behavior);
    }

    private static boolean isSilenced(ServerPlayer player, MagicData data, long time) {
        return data.enthralledUntil() > time || player.hasEffect(ModEffects.holder(ModEffects.ENTHRALLED));
    }

    private static void execute(ServerPlayer player, MagicData data, SpellDefinition spell, SpellBehavior behavior) {
        ServerLevel level = player.level();
        long time = level.getGameTime();
        SpellProgress progress = data.progress(spell.id());
        if (progress == null) {
            return;
        }
        StaffItem.Tier staff = heldStaff(player);
        float mastery = progress.masteryFraction();
        float cost = effectiveCost(spell, mastery, staff);
        if (!data.trySpendMana(cost)) {
            fail(player, Component.translatable("message.frieren.no_mana", String.format("%.0f", cost)));
            return;
        }
        if (spell.healthCost() > 0.0F) {
            player.hurtServer(level, level.damageSources().magic(), spell.healthCost());
        }

        SpellContext ctx = new SpellContext(player, level, data, spell, progress, staff, effectivePower(mastery, staff));
        boolean success = behavior.cast(ctx);
        if (!success) {
            data.setMana(data.mana() + cost);
            return;
        }

        ManaHelper.onManaSpent(player, data, cost);
        data.pauseRegenUntil(time + 40L);
        data.setCooldown(spell.id(), time + effectiveCooldown(spell, mastery, staff));
        progress.incrementCasts();
        progress.addMastery((BASE_MASTERY_GAIN + (ctx.hits() > 0 ? HIT_MASTERY_BONUS : 0.0F)) * (float) FrierenConfig.masteryGainMultiplier());
        if (spell.isChannelled()) {
            data.startChannel(spell.id());
        }
        data.markDirty();
        QuestManager.onSpellCast(player, data, spell, ctx.hits());
    }

    // ---------------------------------------------------------------------------------------
    // Per-tick processing (cast wind-up, channels, toggles)
    // ---------------------------------------------------------------------------------------

    public static void serverTick(ServerPlayer player, MagicData data) {
        Identifier pending = data.pendingCast();
        if (pending != null) {
            data.tickCast();
            if (data.castTicksRemaining() <= 0) {
                data.clearPendingCast();
                SpellDefinition spell = SpellManager.server(pending);
                SpellBehavior behavior = spell != null ? SpellBehaviors.get(spell.behavior()) : null;
                if (spell != null && behavior != null && data.knows(pending)) {
                    execute(player, data, spell, behavior);
                }
            }
        }

        Identifier channel = data.channelingSpell();
        if (channel != null) {
            SpellDefinition spell = SpellManager.server(channel);
            SpellBehavior behavior = spell != null ? SpellBehaviors.get(spell.behavior()) : null;
            SpellProgress progress = data.progress(channel);
            if (spell == null || behavior == null || progress == null || data.channelTicks() >= spell.maxChannelTicks()
                || isSilenced(player, data, player.level().getGameTime())) {
                data.stopChannel();
                data.markDirty();
            } else {
                data.tickChannel();
                StaffItem.Tier staff = heldStaff(player);
                SpellContext ctx = new SpellContext(player, player.level(), data, spell, progress, staff,
                    effectivePower(progress.masteryFraction(), staff));
                if (!behavior.tickChannel(ctx, data.channelTicks())) {
                    data.stopChannel();
                    data.markDirty();
                }
            }
        }

        tickToggle(player, data, data.flightSpell());
        tickToggle(player, data, data.barrierSpell());
    }

    private static void tickToggle(ServerPlayer player, MagicData data, @Nullable Identifier spellId) {
        if (spellId == null) {
            return;
        }
        SpellDefinition spell = SpellManager.server(spellId);
        SpellBehavior behavior = spell != null ? SpellBehaviors.get(spell.behavior()) : null;
        if (spell == null || behavior == null) {
            return;
        }
        behavior.tickActive(player, data, spell);
    }

    /** Turns every toggle off (death, dimension change, admin reset). */
    public static void deactivateAll(ServerPlayer player, MagicData data) {
        for (Identifier spellId : new Identifier[] {data.flightSpell(), data.barrierSpell()}) {
            if (spellId == null) {
                continue;
            }
            SpellDefinition spell = SpellManager.server(spellId);
            SpellBehavior behavior = spell != null ? SpellBehaviors.get(spell.behavior()) : null;
            if (behavior != null) {
                behavior.deactivate(player, data);
            }
        }
        data.setFlightSpell(null);
        data.setBarrierSpell(null);
        data.stopChannel();
        data.clearPendingCast();
    }

    private static void fail(ServerPlayer player, Component message) {
        Messages.actionBar(player, message);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SPELL_FAIL.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
    }
}
