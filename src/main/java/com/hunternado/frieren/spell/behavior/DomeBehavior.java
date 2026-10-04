package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.magic.SpellAreaEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;

/** Barrier Dome: a large defensive barrier centred on the caster that deflects hostile projectiles. */
public class DomeBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        SpellAreaEntity.spawn(ctx.level(), ctx.caster(), ctx.caster().position(), SpellAreaEntity.Mode.DOME,
            (float) ctx.param("radius", 5.0D), (int) (ctx.param("duration", 160.0D) * (0.75D + 0.25D * ctx.power())), 0.0F, ctx.color());
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.BARRIER_UP.get(), 1.2F, 0.7F);
        return true;
    }
}
