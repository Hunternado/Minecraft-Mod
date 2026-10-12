package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Wind Step: a gust carries the caster forward and softens the landing. Params: {@code strength}, {@code lift},
 * {@code speed_ticks}.
 */
public class DashBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        Vec3 look = ctx.caster().getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            return false;
        }
        double strength = ctx.param("strength", 1.6D) * (1.0D + 0.25D * ctx.masteryFraction());
        Vec3 push = flat.normalize().scale(strength).add(0.0D, ctx.param("lift", 0.35D), 0.0D);
        ctx.caster().setDeltaMovement(push);
        ctx.caster().needsSync = true;
        ctx.caster().fallDistance = 0.0F;
        ctx.caster().addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false, true));
        ctx.caster().addEffect(new MobEffectInstance(MobEffects.SPEED, (int) ctx.param("speed_ticks", 60.0D), 1, false, false, true));
        SpellFx.streaks(ctx.level(), ctx.caster().position().add(0.0D, 1.0D, 0.0D), ctx.color(), 1.2F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.WIND.get(), 1.0F, 1.4F);
        return true;
    }
}
