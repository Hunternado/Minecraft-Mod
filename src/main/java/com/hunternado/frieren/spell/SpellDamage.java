package com.hunternado.frieren.spell;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.combat.SpellDamageSource;
import com.hunternado.frieren.entity.demon.DemonEntity;
import com.hunternado.frieren.entity.magic.ManaDecoyEntity;
import com.hunternado.frieren.entity.npc.FrierenNpcBase;
import com.hunternado.frieren.util.WorldRules;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** Damage application for spells, including friendly-fire rules and demon bonuses. */
public final class SpellDamage {
    private SpellDamage() {}

    /**
     * Players never hurt themselves, their pets or decoys, friendly NPCs, or other players unless the
     * {@code pvp} game rule allows it.
     */
    public static boolean canHarm(ServerLevel level, Entity caster, Entity target) {
        if (target == caster || !target.isAlive() || !(target instanceof LivingEntity)) {
            return false;
        }
        if (target instanceof TamableAnimal pet && pet.getOwner() == caster) {
            return false;
        }
        if (target instanceof ManaDecoyEntity decoy && decoy.isOwnedBy(caster)) {
            return false;
        }
        if (caster instanceof Player && target instanceof FrierenNpcBase) {
            return false;
        }
        if (caster instanceof Player && target instanceof Player) {
            return WorldRules.pvp(level);
        }
        return true;
    }

    public static boolean hurt(SpellContext ctx, LivingEntity target, float amount, ResourceKey<DamageType> type, float barrierCost) {
        return hurt(ctx.level(), ctx.caster(), ctx.caster(), target, amount, type, barrierCost, ctx.data().hasDemonHunterInsignia(), ctx);
    }

    /** Full form, also used by mob casters ({@code ctx == null}). */
    public static boolean hurt(ServerLevel level, @Nullable Entity direct, Entity caster, LivingEntity target, float amount,
                               ResourceKey<DamageType> type, float barrierCost, boolean demonBane, @Nullable SpellContext ctx) {
        if (amount <= 0.0F || !canHarm(level, caster, target)) {
            return false;
        }
        float finalAmount = amount;
        if (demonBane && target instanceof DemonEntity) {
            finalAmount *= 1.2F;
        }
        SpellDamageSource source = new SpellDamageSource(ModDamageTypes.holder(level, type), direct, caster, barrierCost, demonBane);
        boolean damaged = target.hurtServer(level, source, finalAmount);
        if (damaged && ctx != null) {
            ctx.markHit();
        }
        return damaged;
    }
}
