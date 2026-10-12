package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Earth Spikes: the ground erupts in a line in front of the caster, damaging and launching everything on it.
 * Purely visual on the terrain (no blocks change). Params: {@code damage}, {@code range}, {@code launch}.
 */
public class EarthSpikesBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        double range = ctx.param("range", 14.0D);
        float damage = ctx.scaled("damage", 7.0D);
        double launch = ctx.param("launch", 0.7D);
        Vec3 look = ctx.caster().getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            flat = Vec3.directionFromRotation(0.0F, ctx.caster().getYRot());
        }
        flat = flat.normalize();
        Set<Integer> struck = new HashSet<>();
        Vec3 origin = ctx.caster().position();
        for (double d = 1.5D; d <= range; d += 1.5D) {
            Vec3 point = ground(ctx, origin.add(flat.scale(d)));
            if (point == null) {
                break; // a wall or a drop ends the eruption
            }
            BlockState below = ctx.level().getBlockState(BlockPos.containing(point).below());
            ctx.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), point.x, point.y + 0.4D, point.z,
                14, 0.35D, 0.5D, 0.35D, 0.15D);
            if (((int) (d / 1.5D)) % 3 == 0) {
                MagicCircleEntity.onGround(ctx.level(), point, MagicCircleEntity.STYLE_ARCANE, ctx.color(), 0.9F, 16);
            }
            for (LivingEntity target : SpellTargeting.livingInRadius(ctx.level(), ctx.caster(), point.add(0.0D, 0.6D, 0.0D), 1.3D,
                candidate -> SpellDamage.canHarm(ctx.level(), ctx.caster(), candidate))) {
                if (struck.add(target.getId()) && SpellDamage.hurt(ctx, target, damage, ModDamageTypes.SPELL, 1.5F)) {
                    target.setDeltaMovement(target.getDeltaMovement().add(0.0D, launch, 0.0D));
                    target.needsSync = true;
                }
            }
        }
        SpellFx.sound(ctx.level(), origin, ModSounds.SLASH.get(), 1.0F, 0.5F);
        return true;
    }

    /** The standing surface at a point, searching a couple of blocks up and down; null if there is none. */
    private static Vec3 ground(SpellContext ctx, Vec3 at) {
        BlockPos base = BlockPos.containing(at);
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos feet = base.offset(0, dy, 0);
            if (ctx.level().getBlockState(feet).getCollisionShape(ctx.level(), feet).isEmpty()
                    && !ctx.level().getBlockState(feet.below()).getCollisionShape(ctx.level(), feet.below()).isEmpty()) {
                return new Vec3(at.x, feet.getY(), at.z);
            }
        }
        return null;
    }
}
