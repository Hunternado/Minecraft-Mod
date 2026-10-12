package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.magic.SpellAreaEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;

/**
 * Goddess's Sanctuary: a circle of holy light on the ground that heals everyone friendly standing in it every
 * second and puts out flames. Params: {@code radius}, {@code duration}, {@code heal} (per second).
 */
public class SanctuaryBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        float radius = (float) ctx.param("radius", 5.0D);
        int duration = (int) ctx.param("duration", 200.0D);
        float heal = ctx.scaled("heal", 1.5D);
        SpellAreaEntity.spawn(ctx.level(), ctx.caster(), ctx.caster().position(), SpellAreaEntity.Mode.SANCTUARY, radius, duration,
            heal, ctx.color());
        MagicCircleEntity.onGround(ctx.level(), ctx.caster().position(), MagicCircleEntity.STYLE_HOLY, ctx.color(), radius, duration);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.HEAL.get(), 1.0F, 0.8F);
        return true;
    }
}
