package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Fern's rapid-fire Zoltraak. Channelled: one beam on cast, then another every {@code interval} ticks while
 * the key is held, each costing {@code pulse_cost} mana and scattering by up to {@code spread} degrees.
 */
public class BarrageBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        BeamBehavior.fire(ctx, ctx.caster().getLookAngle());
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.ZOLTRAAK.get(), 0.8F, 1.3F);
        return true;
    }

    @Override
    public boolean tickChannel(SpellContext ctx, int channelTick) {
        int interval = Math.max(1, (int) ctx.param("interval", 4.0D));
        if (channelTick % interval != 0) {
            return true;
        }
        if (!ctx.spendExtraMana((float) ctx.param("pulse_cost", 4.0D))) {
            return false;
        }
        double spread = ctx.param("spread", 4.0D) * (1.0D - 0.5D * ctx.masteryFraction());
        BeamBehavior.fire(ctx, scatter(ctx.caster().getLookAngle(), spread, ctx.level().getRandom()));
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.ZOLTRAAK.get(), 0.6F, 1.4F + ctx.level().getRandom().nextFloat() * 0.2F);
        return true;
    }

    private static Vec3 scatter(Vec3 direction, double degrees, RandomSource random) {
        double yaw = Math.toRadians((random.nextDouble() - 0.5D) * 2.0D * degrees);
        double pitch = Math.toRadians((random.nextDouble() - 0.5D) * 2.0D * degrees);
        return direction.yRot((float) yaw).xRot((float) pitch).normalize();
    }
}
