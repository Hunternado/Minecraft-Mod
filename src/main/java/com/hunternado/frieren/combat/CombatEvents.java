package com.hunternado.frieren.combat;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.data.SpellProgress;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellManager;
import com.hunternado.frieren.spell.behavior.BarrierBehavior;
import com.hunternado.frieren.util.Messages;
import com.hunternado.frieren.util.WorldRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

/**
 * Combat rules shared by every entity: defensive-magic absorption, cast interruption, gilded and
 * obedient attackers.
 */
public final class CombatEvents {
    private static final float INTERRUPT_THRESHOLD = 4.0F;

    private CombatEvents() {}

    public static void register() {
        LivingHurtEvent.BUS.addListener(CombatEvents::onLivingHurt);
        TickEvent.ServerTickEvent.Post.BUS.addListener(CombatEvents::onServerTick);
    }

    private static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (event.server().getTickCount() % 10 == 0) {
            ObedienceTracker.tick(event.server());
        }
    }

    private static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        // Gilded statues cannot hurt anything.
        if (attacker instanceof LivingEntity livingAttacker && livingAttacker.hasEffect(ModEffects.holder(ModEffects.GILDED))) {
            event.setAmount(0.0F);
            return;
        }
        // Azeryuze puppets never hurt their master (or other players on non-PvP servers).
        if (attacker != null && ObedienceTracker.isObedient(attacker) && victim instanceof Player) {
            UUID owner = ObedienceTracker.ownerOf(attacker);
            boolean pvp = victim.level() instanceof net.minecraft.server.level.ServerLevel serverLevel && WorldRules.pvp(serverLevel);
            if (victim.getUUID().equals(owner) || !pvp) {
                event.setAmount(0.0F);
                return;
            }
        }

        if (victim instanceof ServerPlayer player) {
            MagicData data = MagicData.get(player);
            if (data == null) {
                return;
            }
            if (data.barrierActive() && event.getAmount() > 0.0F && isBarrierBlockable(source)) {
                event.setAmount(absorbWithBarrier(player, data, source, event.getAmount()));
            }
            if (data.pendingCast() != null && event.getAmount() >= INTERRUPT_THRESHOLD) {
                data.clearPendingCast();
                Messages.actionBar(player, Component.translatable("message.frieren.cast_interrupted"));
            }
        }
    }

    /** Barriers stop attacks and explosions, not hunger, falling, drowning or the void. */
    private static boolean isBarrierBlockable(DamageSource source) {
        return source.getEntity() != null || source.getDirectEntity() != null || source.is(DamageTypeTags.IS_EXPLOSION);
    }

    private static float absorbWithBarrier(ServerPlayer player, MagicData data, DamageSource source, float amount) {
        SpellDefinition definition = data.barrierSpell() != null ? SpellManager.server(data.barrierSpell()) : null;
        if (definition == null) {
            data.setBarrierSpell(null);
            return amount;
        }
        SpellProgress progress = data.progress(definition.id());
        float mastery = progress != null ? progress.masteryFraction() : 0.0F;
        boolean ranged = source.is(DamageTypeTags.IS_PROJECTILE) || source instanceof SpellDamageSource || source.is(DamageTypeTags.IS_EXPLOSION);
        float blockable = ranged ? amount : amount * (float) definition.param("melee_block", 0.7D);
        float costPerPoint = (float) definition.param("efficiency", 1.5D) * (1.0F - 0.3F * mastery);
        if (source instanceof SpellDamageSource spellSource) {
            costPerPoint *= spellSource.barrierCost();
        }
        costPerPoint = Math.max(0.05F, costPerPoint);

        float absorbed;
        float needed = blockable * costPerPoint;
        if (data.mana() >= needed) {
            data.trySpendMana(needed);
            absorbed = blockable;
            ManaHelper.onManaSpent(player, data, needed);
            SpellFx.hexagon(player.level(), player.position().add(0, 1.0D, 0), definition.color(), 1.2F);
            SpellFx.sound(player.level(), player.position(), ModSounds.BARRIER_HIT.get(), 0.7F, 1.0F + player.getRandom().nextFloat() * 0.3F);
        } else {
            absorbed = data.mana() / costPerPoint;
            ManaHelper.onManaSpent(player, data, data.mana());
            data.setMana(0.0F);
            BarrierBehavior.shatter(player, data);
        }
        if (progress != null) {
            progress.addMastery(0.15F);
        }
        data.pauseRegenUntil(player.level().getGameTime() + 40L);
        return Math.max(0.0F, amount - absorbed);
    }
}
