package com.hunternado.frieren.spell;

import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.data.SpellProgress;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.item.StaffItem;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.CastStatePacket;
import com.hunternado.frieren.quest.QuestManager;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The single server-side entry point for casting. Clients only ever request "cast slot N" (key down) and "release"
 * (key up); everything else (knowledge, cooldown, mana, effects) is decided here.
 * <p>
 * Ways a spell is cast:
 * <ul>
 *   <li><b>instant</b>: paid and fired on the key press;</li>
 *   <li><b>incantation</b> ({@code cast_time}): a wind-up, then paid and fired; a hit may break it;</li>
 *   <li><b>charge</b> (param {@code charge}): mana pours in while the key is held (the full cost over the full
 *   charge), release fires with power {@code 0.2 + 0.8 * charge}; below {@code min_charge} it fizzles and half the
 *   poured mana returns. Held fully charged for more than ten seconds, it fires on its own. A hit may break it and
 *   the poured mana is lost;</li>
 *   <li><b>channel</b> ({@code channel}): keeps going while the key is held;</li>
 *   <li><b>toggle</b>: switched on and off (flight, defensive magic).</li>
 * </ul>
 * The client is told what is happening through {@link CastStatePacket} so the HUD can draw it.
 */
public final class SpellCaster {
    private static final float BASE_MASTERY_GAIN = 1.0F;
    private static final float HIT_MASTERY_BONUS = 1.0F;
    /** How long a fully charged spell can be held before it slips out on its own. */
    private static final int MAX_FULL_HOLD = 200;

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
        return (float) (masteryPower(masteryFraction) * staff.powerMultiplier() * FrierenConfig.spellDamageMultiplier());
    }

    /**
     * What mastery alone multiplies a spell's damage, healing and duration by: x1 for a fresh spell, x2.25 at full
     * mastery. Front-loaded (square root) so the first hours of practice are felt: x1.63 at 25, x1.88 at 50.
     */
    public static float masteryPower(float masteryFraction) {
        return 1.0F + 1.25F * (float) Math.sqrt(Math.max(0.0F, Math.min(1.0F, masteryFraction)));
    }

    public static int effectiveCastTime(SpellDefinition spell, float masteryFraction) {
        return Math.round(spell.castTime() * (1.0F - 0.3F * masteryFraction));
    }

    /** Ticks to a full charge: mastery shortens it by up to 30%, like an incantation. */
    public static int effectiveChargeTicks(SpellDefinition spell, float masteryFraction) {
        return Math.max(1, Math.round(spell.chargeTicks() * (1.0F - 0.3F * masteryFraction)));
    }

    /** What a charge of {@code fraction} multiplies the spell's full power by. */
    public static float chargeMultiplier(float fraction) {
        return 0.2F + 0.8F * Math.max(0.0F, Math.min(1.0F, fraction));
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

    /** The cast key was released: fires a held charge, or ends a channel. */
    public static void releaseChannel(ServerPlayer player) {
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        if (data.chargingSpell() != null) {
            releaseCharge(player, data);
        } else if (data.channelingSpell() != null) {
            endChannel(player, data);
        }
    }

    private static void endChannel(ServerPlayer player, MagicData data) {
        data.stopChannel();
        data.markDirty();
        FrierenNetwork.sendCastState(player, CastStatePacket.IDLE, null, 0);
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
        if (data.busyCasting()) {
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

        if (spell.isCharged()) {
            // Mana flows in while the key is held; enough for the minimum charge must be there to begin.
            float opening = cost * (float) spell.param("min_charge", 0.25D);
            if (data.mana() < opening) {
                fail(player, Component.translatable("message.frieren.no_mana", String.format("%.0f", opening)));
                return;
            }
            int full = effectiveChargeTicks(spell, mastery);
            data.beginCharge(spellId);
            if (!isSilent(spell)) {
                MagicCircleEntity.inFront(level, player, SpellFx.circleStyle(spell), spell.color(), 0.6F, 8, 1.2F);
            }
            SpellFx.sound(level, player.position(), ModSounds.CHARGE.get(), 0.7F, 0.8F);
            Messages.actionBar(player, Component.translatable("message.frieren.charging", spell.displayName()));
            FrierenNetwork.sendCastState(player, CastStatePacket.CHARGING, spellId, full);
            return;
        }

        int castTime = effectiveCastTime(spell, mastery);
        if (castTime > 0) {
            data.beginCast(spellId, castTime);
            if (!isSilent(spell)) {
                SpellFx.castCircle(level, player, spell, castTime + 6);
            }
            Messages.actionBar(player, Component.translatable("message.frieren.casting", spell.displayName()));
            FrierenNetwork.sendCastState(player, CastStatePacket.INCANTING, spellId, castTime);
            return;
        }
        execute(player, data, spell, behavior, 1.0F, -1.0F);
    }

    /**
     * Admin/testing (/frieren cast): casts a spell immediately as if the player had cast it, teaching it first if
     * needed and topping mana up. Skips cast time and cooldown checks; channels run to their full length.
     *
     * @return false if the spell or its behavior does not exist
     */
    public static boolean adminCast(ServerPlayer player, MagicData data, Identifier spellId) {
        SpellDefinition spell = SpellManager.server(spellId);
        SpellBehavior behavior = spell != null ? SpellBehaviors.get(spell.behavior()) : null;
        if (spell == null || behavior == null) {
            return false;
        }
        data.learn(spellId);
        data.setMana(data.maxMana());
        data.clearCooldowns();
        if (data.busyCasting()) {
            return false;
        }
        if (behavior.isToggle() && behavior.isActive(data)) {
            behavior.deactivate(player, data);
            return true;
        }
        execute(player, data, spell, behavior, 1.0F, -1.0F);    // charged spells fire as if fully charged
        return true;
    }

    private static boolean isSilenced(ServerPlayer player, MagicData data, long time) {
        return data.enthralledUntil() > time || player.hasEffect(ModEffects.holder(ModEffects.ENTHRALLED));
    }

    /**
     * Pays (unless {@code prepaid >= 0}: a charge already poured that much in) and fires the spell.
     *
     * @param charge 0..1 for charged spells, 1 otherwise
     */
    private static void execute(ServerPlayer player, MagicData data, SpellDefinition spell, SpellBehavior behavior, float charge,
                                float prepaid) {
        ServerLevel level = player.level();
        long time = level.getGameTime();
        SpellProgress progress = data.progress(spell.id());
        if (progress == null) {
            FrierenNetwork.sendCastState(player, CastStatePacket.IDLE, null, 0);
            return;
        }
        StaffItem.Tier staff = heldStaff(player);
        float mastery = progress.masteryFraction();
        float cost;
        if (prepaid >= 0.0F) {
            cost = prepaid;
        } else {
            cost = effectiveCost(spell, mastery, staff);
            if (!data.trySpendMana(cost)) {
                fail(player, Component.translatable("message.frieren.no_mana", String.format("%.0f", cost)));
                FrierenNetwork.sendCastState(player, CastStatePacket.IDLE, null, 0);
                return;
            }
        }
        if (spell.healthCost() > 0.0F) {
            player.hurtServer(level, level.damageSources().magic(), spell.healthCost());
        }

        float power = effectivePower(mastery, staff) * (spell.isCharged() ? chargeMultiplier(charge) : 1.0F);
        SpellContext ctx = new SpellContext(player, level, data, spell, progress, staff, power, spell.isCharged() ? charge : 1.0F);
        boolean success = behavior.cast(ctx);
        if (!success) {
            data.setMana(data.mana() + cost);
            FrierenNetwork.sendCastState(player, CastStatePacket.IDLE, null, 0);
            return;
        }

        if (spell.castTime() <= 0 && !spell.isCharged() && !isSilent(spell)) {
            SpellFx.castCircle(level, player, spell, spell.isChannelled() ? 20 : 12);
        }
        if (prepaid < 0.0F) {
            ManaHelper.onManaSpent(player, data, cost);          // a charge already trained as it was poured
        }
        data.pauseRegenUntil(time + 40L);
        data.setCooldown(spell.id(), time + effectiveCooldown(spell, mastery, staff));
        progress.incrementCasts();
        float gain = BASE_MASTERY_GAIN + (ctx.hits() > 0 ? HIT_MASTERY_BONUS : 0.0F);
        if (spell.isCharged()) {
            gain *= 0.5F + charge;                               // a full charge teaches more than a fizzle
        }
        progress.addMastery(gain * (float) FrierenConfig.masteryGainMultiplier());
        if (spell.isChannelled()) {
            data.startChannel(spell.id());
            FrierenNetwork.sendCastState(player, CastStatePacket.CHANNELING, spell.id(), spell.maxChannelTicks());
        } else {
            FrierenNetwork.sendCastState(player, CastStatePacket.IDLE, null, 0);
        }
        data.markDirty();
        QuestManager.onSpellCast(player, data, spell, ctx.hits());
    }

    // ---------------------------------------------------------------------------------------
    // Charging
    // ---------------------------------------------------------------------------------------

    private static void tickCharge(ServerPlayer player, MagicData data, Identifier spellId) {
        ServerLevel level = player.level();
        long time = level.getGameTime();
        SpellDefinition spell = SpellManager.server(spellId);
        SpellBehavior behavior = spell != null ? SpellBehaviors.get(spell.behavior()) : null;
        SpellProgress progress = data.progress(spellId);
        if (spell == null || behavior == null || progress == null || isSilenced(player, data, time)) {
            data.clearCharge();
            FrierenNetwork.sendCastState(player, CastStatePacket.IDLE, null, 0);
            return;
        }
        float mastery = progress.masteryFraction();
        StaffItem.Tier staff = heldStaff(player);
        int full = effectiveChargeTicks(spell, mastery);
        if (data.chargeTicks() < full) {
            float perTick = effectiveCost(spell, mastery, staff) / full;
            if (data.trySpendMana(perTick)) {
                data.advanceCharge(perTick);
                ManaHelper.onManaSpent(player, data, perTick);
                data.pauseRegenUntil(time + 40L);
                if (data.chargeTicks() == full) {
                    SpellFx.burst(level, chargePoint(player), spell.color(), 1.2F);
                    SpellFx.sound(level, player.position(), ModSounds.CHARGED.get(), 1.0F, 1.2F);
                    Messages.actionBar(player, Component.translatable("message.frieren.charge_full", spell.displayName())
                        .withStyle(ChatFormatting.GOLD));
                }
            } else if (!data.chargeStalled()) {
                data.setChargeStalled(true);
                Messages.actionBar(player, Component.translatable("message.frieren.charge_stalled", spell.displayName())
                    .withStyle(ChatFormatting.GRAY));
            }
        } else {
            data.advanceCharge(0.0F);                            // held at full: the clock runs, no more mana
            if (data.chargeTicks() - full > MAX_FULL_HOLD) {
                Messages.actionBar(player, Component.translatable("message.frieren.charge_slipped", spell.displayName()));
                releaseCharge(player, data);
                return;
            }
        }
        // The circle grows with the charge; mana gathers into it.
        float fraction = Math.min(1.0F, data.chargeTicks() / (float) full);
        if (data.chargeTicks() % 4 == 0 && !isSilent(spell)) {
            MagicCircleEntity.inFront(level, player, SpellFx.circleStyle(spell), spell.color(), 0.6F + 1.6F * fraction, 7, 1.2F);
        }
        if (data.chargeTicks() % 10 == 0 && fraction < 1.0F) {
            SpellFx.wisps(level, chargePoint(player), spell.color(), 1.5F - fraction);
            SpellFx.sound(level, player.position(), ModSounds.CHARGE.get(), 0.4F, 0.7F + fraction);
        }
    }

    private static void releaseCharge(ServerPlayer player, MagicData data) {
        Identifier spellId = data.chargingSpell();
        SpellDefinition spell = spellId != null ? SpellManager.server(spellId) : null;
        SpellBehavior behavior = spell != null ? SpellBehaviors.get(spell.behavior()) : null;
        SpellProgress progress = spellId != null ? data.progress(spellId) : null;
        float paid = data.chargeManaPaid();
        int ticks = data.chargeTicks();
        data.clearCharge();
        if (spell == null || behavior == null || progress == null) {
            FrierenNetwork.sendCastState(player, CastStatePacket.IDLE, null, 0);
            return;
        }
        float fraction = Math.min(1.0F, ticks / (float) effectiveChargeTicks(spell, progress.masteryFraction()));
        if (fraction < spell.param("min_charge", 0.25D)) {
            data.setMana(data.mana() + paid * 0.5F);
            SpellFx.sound(player.level(), player.position(), ModSounds.SPELL_FAIL.get(), 0.6F, 1.4F);
            Messages.actionBar(player, Component.translatable("message.frieren.charge_fizzled", spell.displayName()));
            FrierenNetwork.sendCastState(player, CastStatePacket.IDLE, null, 0);
            data.markDirty();
            return;
        }
        execute(player, data, spell, behavior, fraction, paid);
    }

    /** Silent spells (an Original-magic modifier) are cast without a visible circle and leave no mana flare. */
    public static boolean isSilent(SpellDefinition spell) {
        return spell.param("silent", 0.0D) > 0.5D;
    }

    private static Vec3 chargePoint(ServerPlayer player) {
        return player.getEyePosition().add(player.getLookAngle().scale(1.2D));
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
                    execute(player, data, spell, behavior, 1.0F, -1.0F);
                } else {
                    FrierenNetwork.sendCastState(player, CastStatePacket.IDLE, null, 0);
                }
            }
        }

        Identifier charging = data.chargingSpell();
        if (charging != null) {
            tickCharge(player, data, charging);
        }

        Identifier channel = data.channelingSpell();
        if (channel != null) {
            SpellDefinition spell = SpellManager.server(channel);
            SpellBehavior behavior = spell != null ? SpellBehaviors.get(spell.behavior()) : null;
            SpellProgress progress = data.progress(channel);
            if (spell == null || behavior == null || progress == null || data.channelTicks() >= spell.maxChannelTicks()
                || isSilenced(player, data, player.level().getGameTime())) {
                endChannel(player, data);
            } else {
                data.tickChannel();
                StaffItem.Tier staff = heldStaff(player);
                SpellContext ctx = new SpellContext(player, player.level(), data, spell, progress, staff,
                    effectivePower(progress.masteryFraction(), staff));
                if (!behavior.tickChannel(ctx, data.channelTicks())) {
                    endChannel(player, data);
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
        data.clearCharge();
        FrierenNetwork.sendCastState(player, CastStatePacket.IDLE, null, 0);
    }

    private static void fail(ServerPlayer player, Component message) {
        Messages.actionBar(player, message);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SPELL_FAIL.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
    }
}
