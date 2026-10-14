package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Sovereign Ward: {@link ModEffects#SOVEREIGN} for {@code duration} ticks (longer with mastery). CombatEvents cuts all
 * damage to the bearer to a fifth and returns half of every melee blow; the effect itself grants full knockback
 * resistance.
 */
public class SovereignWardBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        int duration = Math.round(ctx.scaled("duration", 300.0D));
        ctx.caster().addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.SOVEREIGN), duration, 0));
        SpellFx.shell(ctx.level(), ctx.caster(), ctx.color());
        SpellFx.ring(ctx.level(), ctx.caster().position().add(0.0D, 0.1D, 0.0D), ctx.color(), 2.5F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.BARRIER_UP.get(), 1.2F, 0.5F);
        return true;
    }
}
