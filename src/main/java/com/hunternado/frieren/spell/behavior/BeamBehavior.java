package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Zoltraak: an instantaneous killing beam. Params: {@code damage}, {@code range}, {@code pierce} (0/1),
 * {@code barrier_cost} (how expensive it is for barriers to stop), {@code demon_magic} (0/1: armour-piercing
 * demon formula, e.g. Qual's original Zoltraak).
 */
public class BeamBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        Vec3 direction = ctx.caster().getLookAngle();
        fire(ctx, direction);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.ZOLTRAAK.get(), 1.0F, 1.0F + ctx.level().getRandom().nextFloat() * 0.2F);
        return true;
    }

    /** Fires one beam along {@code direction}; shared with the barrage behavior. */
    public static void fire(SpellContext ctx, Vec3 direction) {
        double range = ctx.param("range", 32.0D);
        boolean pierce = ctx.param("pierce", 0.0D) > 0.5D;
        float barrierCost = (float) ctx.param("barrier_cost", 1.0D);
        ResourceKey<DamageType> type = ctx.param("demon_magic", 0.0D) > 0.5D ? ModDamageTypes.DEMON_MAGIC : ModDamageTypes.ZOLTRAAK;
        float damage = ctx.scaled("damage", 6.0D);

        Vec3 from = ctx.caster().getEyePosition();
        Vec3 wanted = from.add(direction.normalize().scale(range));
        BlockHitResult blockHit = ctx.level().clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx.caster()));
        Vec3 to = blockHit.getType() == HitResult.Type.MISS ? wanted : blockHit.getLocation();

        List<SpellTargeting.EntityHit> hits = SpellTargeting.entitiesOnLine(ctx.level(), ctx.caster(), from, to, 0.25D,
            target -> SpellDamage.canHarm(ctx.level(), ctx.caster(), target));
        Vec3 visualEnd = to;
        float current = damage;
        for (SpellTargeting.EntityHit hit : hits) {
            SpellDamage.hurt(ctx, hit.entity(), current, type, barrierCost);
            if (!pierce) {
                visualEnd = hit.location();
                break;
            }
            current *= 0.85F;
        }
        SpellFx.beam(ctx.level(), from.add(direction.normalize().scale(0.6D)), visualEnd, ctx.color());
    }
}
