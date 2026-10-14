package com.hunternado.frieren.spell;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.combat.SpellDamageSource;
import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.entity.demon.DemonEntity;
import com.hunternado.frieren.entity.magic.ManaDecoyEntity;
import com.hunternado.frieren.entity.npc.FrierenNpcBase;
import com.hunternado.frieren.util.WorldRules;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** Damage application for spells, including friendly-fire rules, demon bonuses and executions. */
public final class SpellDamage {
    /** Creatures no execution can kill outright, whatever their health (the vanilla bosses). */
    public static final TagKey<EntityType<?>> EXECUTE_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, FrierenIds.id("execute_immune"));

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

    /** Full form, also used by mob casters ({@code ctx == null}). The execution threshold comes from the spell. */
    public static boolean hurt(ServerLevel level, @Nullable Entity direct, Entity caster, LivingEntity target, float amount,
                               ResourceKey<DamageType> type, float barrierCost, boolean demonBane, @Nullable SpellContext ctx) {
        float executeCap = ctx != null ? ctx.executeCap() : 0.0F;
        return hurt(level, direct, caster, target, amount, type, barrierCost, demonBane, ctx, executeCap);
    }

    /**
     * As above with an explicit execution threshold, for projectiles and areas that outlive their cast. A target whose
     * maximum health is at most {@code executeCap} dies to the hit: forbidden and legendary magic does not bargain with
     * zombies or demon soldiers. Players, Frieren bosses and {@link #EXECUTE_IMMUNE} creatures only take the plain damage.
     */
    public static boolean hurt(ServerLevel level, @Nullable Entity direct, Entity caster, LivingEntity target, float amount,
                               ResourceKey<DamageType> type, float barrierCost, boolean demonBane, @Nullable SpellContext ctx,
                               float executeCap) {
        if (amount <= 0.0F || !canHarm(level, caster, target)) {
            return false;
        }
        float finalAmount = amount;
        if (demonBane && target instanceof DemonEntity) {
            finalAmount *= 1.2F;
        }
        if (executes(target, executeCap)) {
            // Enough to go through any armour, Resistance V aside: five times what is left plus a margin.
            finalAmount = Math.max(finalAmount, (target.getHealth() + target.getAbsorptionAmount()) * 5.0F + 20.0F);
        }
        SpellDamageSource source = new SpellDamageSource(ModDamageTypes.holder(level, type), direct, caster, barrierCost, demonBane);
        boolean damaged = target.hurtServer(level, source, finalAmount);
        if (damaged && ctx != null) {
            ctx.markHit();
        }
        return damaged;
    }

    public static boolean executes(LivingEntity target, float executeCap) {
        return executeCap > 0.0F && !(target instanceof Player) && !(target instanceof FrierenBoss)
            && !target.is(EXECUTE_IMMUNE) && target.getMaxHealth() <= executeCap;
    }
}
