package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Fern's Zoltraak: not one beam but a storm of them. Channelled; magic circles open in a ring around the caster and
 * each pulse fires {@code per_pulse} beams from them at the aimed point. The pulses come faster the longer it is held,
 * from every {@code interval_start} ticks down to every {@code interval_min} over {@code ramp} ticks; each pulse costs
 * {@code pulse_cost} mana. Every beam deals {@code damage} (it is the sheer number that kills). Mastery tightens the
 * {@code spread}. Params: {@code damage}, {@code range}, {@code per_pulse}, {@code interval_start}, {@code interval_min},
 * {@code ramp}, {@code pulse_cost}, {@code spread}, {@code circles}.
 */
public class VolleyBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        int circles = Math.max(2, (int) ctx.param("circles", 6.0D));
        for (int i = 0; i < circles; i++) {
            Vec3 at = origin(ctx, i, circles);
            float yaw = ctx.caster().getYRot();
            MagicCircleEntity.standing(ctx.level(), at.add(ctx.caster().getLookAngle().scale(0.2D)), yaw, ctx.caster().getXRot(),
                MagicCircleEntity.STYLE_ARCANE, ctx.color(), 0.45F, 24);
        }
        pulse(ctx, 0);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.ZOLTRAAK.get(), 0.9F, 1.4F);
        return true;
    }

    @Override
    public boolean tickChannel(SpellContext ctx, int channelTick) {
        double start = ctx.param("interval_start", 6.0D);
        double min = ctx.param("interval_min", 1.0D);
        double ramp = Math.max(1.0D, ctx.param("ramp", 40.0D));
        int interval = (int) Math.max(min, Math.round(start - (start - min) * Math.min(1.0D, channelTick / ramp)));
        if (channelTick % Math.max(1, interval) != 0) {
            return true;
        }
        if (!ctx.spendExtraMana((float) ctx.param("pulse_cost", 3.0D))) {
            return false;
        }
        pulse(ctx, channelTick);
        if (channelTick % 20 == 0) {
            // The circles are renewed while the storm lasts.
            int circles = Math.max(2, (int) ctx.param("circles", 6.0D));
            for (int i = 0; i < circles; i++) {
                MagicCircleEntity.standing(ctx.level(), origin(ctx, i, circles), ctx.caster().getYRot(), ctx.caster().getXRot(),
                    MagicCircleEntity.STYLE_ARCANE, ctx.color(), 0.45F, 22);
            }
        }
        return true;
    }

    private static void pulse(SpellContext ctx, int channelTick) {
        int perPulse = Math.max(1, (int) ctx.param("per_pulse", 3.0D));
        int circles = Math.max(2, (int) ctx.param("circles", 6.0D));
        Vec3 target = SpellTargeting.aimPoint(ctx.caster(), ctx.param("range", 32.0D));
        double spread = ctx.param("spread", 3.0D) * (1.0D - 0.6D * ctx.masteryFraction());
        RandomSource random = ctx.level().getRandom();
        for (int i = 0; i < perPulse; i++) {
            Vec3 from = origin(ctx, (channelTick + i * 2) % circles, circles);
            Vec3 direction = scatter(target.subtract(from).normalize(), spread, random);
            BeamBehavior.fire(ctx, from, direction, 1.0F, 0.22F);
        }
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.ZOLTRAAK.get(), 0.45F, 1.5F + random.nextFloat() * 0.3F);
    }

    /** The i-th launch point: a ring around the caster's upper body, behind the shoulders, facing the target. */
    private static Vec3 origin(SpellContext ctx, int index, int count) {
        Vec3 look = ctx.caster().getLookAngle();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();
        Vec3 up = side.cross(look).normalize().scale(-1.0D);
        double angle = index * Math.PI * 2.0D / count;
        return ctx.caster().getEyePosition().subtract(0.0D, 0.15D, 0.0D)
            .add(side.scale(Math.cos(angle) * 1.1D)).add(up.scale(Math.sin(angle) * 0.8D + 0.2D)).subtract(look.scale(0.2D));
    }

    private static Vec3 scatter(Vec3 direction, double degrees, RandomSource random) {
        double yaw = Math.toRadians((random.nextDouble() - 0.5D) * 2.0D * degrees);
        double pitch = Math.toRadians((random.nextDouble() - 0.5D) * 2.0D * degrees);
        return direction.yRot((float) yaw).xRot((float) pitch).normalize();
    }
}
