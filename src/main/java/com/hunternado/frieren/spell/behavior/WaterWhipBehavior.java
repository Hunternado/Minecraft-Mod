package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Water Whip: a lash of water sweeping a cone in front of the caster. Knocks enemies back, puts out burning
 * creatures and nearby fire. Params: {@code damage}, {@code range}, {@code angle}, {@code knockback}.
 */
public class WaterWhipBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        double range = ctx.param("range", 7.0D);
        double angle = ctx.param("angle", 45.0D);
        float damage = ctx.scaled("damage", 3.0D);
        double knockback = ctx.param("knockback", 1.3D);
        Vec3 eye = ctx.caster().getEyePosition();
        for (LivingEntity target : SpellTargeting.livingInRadius(ctx.level(), ctx.caster(), eye, range,
            candidate -> SpellTargeting.inCone(ctx.caster(), candidate, angle))) {
            target.clearFire();
            if (!SpellDamage.canHarm(ctx.level(), ctx.caster(), target)) {
                continue;
            }
            SpellDamage.hurt(ctx, target, damage, ModDamageTypes.SPELL, 1.0F);
            Vec3 push = target.position().subtract(ctx.caster().position()).multiply(1.0D, 0.0D, 1.0D).normalize().scale(knockback);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.35D, push.z));
            target.needsSync = true;
        }
        ctx.caster().clearFire();
        // Douse fire in the cone (always allowed: putting fires out is never griefing).
        BlockPos center = BlockPos.containing(eye);
        int r = (int) Math.ceil(range);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 2, r))) {
            if (ctx.level().getBlockState(pos).getBlock() instanceof BaseFireBlock
                    && Vec3.atCenterOf(pos).distanceTo(eye) <= range) {
                ctx.level().setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        Vec3 look = ctx.caster().getLookAngle();
        for (int i = 1; i <= (int) range; i++) {
            Vec3 p = eye.add(look.scale(i));
            ctx.level().sendParticles(ParticleTypes.SPLASH, p.x, p.y, p.z, 12, 0.4D * i / range + 0.2D, 0.3D, 0.4D * i / range + 0.2D, 0.2D);
            ctx.level().sendParticles(ParticleTypes.BUBBLE_POP, p.x, p.y, p.z, 4, 0.3D, 0.3D, 0.3D, 0.05D);
        }
        SpellFx.cone(ctx.level(), eye, look, ctx.color(), (float) range, 1);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.WIND.get(), 1.0F, 0.8F);
        return true;
    }
}
