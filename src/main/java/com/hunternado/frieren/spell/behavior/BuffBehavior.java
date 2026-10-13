package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Small blessings for everyday travel: grants the caster, and players standing close by, one helpful effect.
 * Params: {@code effect} (0 night vision, 1 water breathing, 2 haste, 3 slow falling, 4 fire resistance),
 * {@code duration} in ticks, {@code amplifier}, {@code radius} for sharing (0 = caster only).
 */
public class BuffBehavior implements SpellBehavior {
    private static final List<Holder<MobEffect>> EFFECTS = List.of(MobEffects.NIGHT_VISION, MobEffects.WATER_BREATHING,
        MobEffects.HASTE, MobEffects.SLOW_FALLING, MobEffects.FIRE_RESISTANCE);

    @Override
    public boolean cast(SpellContext ctx) {
        int index = (int) ctx.param("effect", 0.0D);
        if (index < 0 || index >= EFFECTS.size()) {
            return false;
        }
        Holder<MobEffect> effect = EFFECTS.get(index);
        int duration = (int) ctx.param("duration", 3600.0D);
        int amplifier = (int) ctx.param("amplifier", 0.0D);
        double radius = ctx.param("radius", 0.0D);
        apply(ctx.caster(), effect, duration, amplifier);
        if (radius > 0.0D) {
            for (ServerPlayer ally : ctx.level().getEntitiesOfClass(ServerPlayer.class, ctx.caster().getBoundingBox().inflate(radius),
                    p -> p != ctx.caster() && p.isAlive() && !p.isSpectator())) {
                apply(ally, effect, duration, amplifier);
                SpellFx.wisps(ctx.level(), ally.position().add(0.0D, 1.0D, 0.0D), ctx.color(), 0.6F);
            }
        }
        SpellFx.spiral(ctx.level(), ctx.caster().position(), ctx.color(), 2.0F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.UTILITY.get(), 0.7F, 1.3F);
        return true;
    }

    private static void apply(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
    }
}
