package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Demon King's Annihilation, a charged spell: a colossal beam that pierces everything along {@code range}. With a
 * charge of at least {@code execute_charge} it kills whatever is not a boss outright (execution); a boss (or a vanilla
 * boss) loses {@code boss_fraction} of its maximum health, scaled by the charge. Params: {@code damage}, {@code range},
 * {@code width}, {@code boss_fraction}, {@code barrier_cost}.
 */
public class AnnihilationBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        Vec3 from = ctx.caster().getEyePosition();
        Vec3 direction = ctx.caster().getLookAngle();
        double range = ctx.param("range", 80.0D);
        Vec3 wanted = from.add(direction.scale(range));
        BlockHitResult blockHit = ctx.level().clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx.caster()));
        Vec3 to = blockHit.getType() == HitResult.Type.MISS ? wanted : blockHit.getLocation();
        float width = (float) ctx.param("width", 2.4D) * (0.4F + 0.6F * ctx.charge());
        float damage = ctx.scaled("damage", 120.0D);
        float bossFraction = (float) ctx.param("boss_fraction", 0.25D) * ctx.charge();
        for (SpellTargeting.EntityHit hit : SpellTargeting.entitiesOnLine(ctx.level(), ctx.caster(), from, to, 0.4D + width * 0.4D,
                target -> SpellDamage.canHarm(ctx.level(), ctx.caster(), target))) {
            LivingEntity target = hit.entity();
            float amount = damage;
            if (target instanceof FrierenBoss || target.is(SpellDamage.EXECUTE_IMMUNE)) {
                amount = Math.max(amount, target.getMaxHealth() * bossFraction);
            }
            SpellDamage.hurt(ctx, target, amount, ModDamageTypes.ANNIHILATION, (float) ctx.param("barrier_cost", 10.0D));
        }
        SpellFx.beam(ctx.level(), from.add(direction.scale(0.8D)), to, ctx.color(), width, 26, SpellBeamEntity.STYLE_DEMON);
        SpellFx.beam(ctx.level(), from.add(direction.scale(0.8D)), to, 0xFFE0E0, width * 0.35F, 20, SpellBeamEntity.STYLE_ZOLTRAAK);
        double length = from.distanceTo(to);
        for (double d = 6.0D; d < length; d += 8.0D) {
            SpellFx.burst(ctx.level(), from.add(direction.scale(d)), ctx.color(), width);
        }
        SpellFx.burst(ctx.level(), to, ctx.color(), width * 2.0F);
        SpellFx.ring(ctx.level(), to, ctx.color(), width * 2.5F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.ANNIHILATION.get(), 2.0F, 0.6F);
        return true;
    }
}
