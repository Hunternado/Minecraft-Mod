package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.fx.BarrierShellEntity;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Reflection barrier: for a few seconds, projectiles and spells that strike the caster are turned back at whoever
 * cast or fired them (handled in CombatEvents). Melee goes through. Params: {@code duration}, {@code reflect}
 * (share of the damage sent back).
 */
public class ReflectBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        int duration = (int) (ctx.param("duration", 60.0D) * (1.0D + 0.5D * ctx.masteryFraction()));
        ctx.caster().addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.REFLECTING), duration, 0));
        BarrierShellEntity.flash(ctx.level(), ctx.caster(), ctx.color(), duration);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.BARRIER_UP.get(), 1.0F, 1.5F);
        return true;
    }
}
