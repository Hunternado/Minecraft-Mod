package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.magic.SpellAreaEntity;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.registry.ModSounds;
import net.minecraft.world.phys.Vec3;

/** Catastravia: rains arrows of light over the aimed area for a few seconds. */
public class ArrowRainBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        Vec3 at = SpellTargeting.aimPoint(ctx.caster(), ctx.param("range", 40.0D));
        SpellAreaEntity.spawn(ctx.level(), ctx.caster(), at, SpellAreaEntity.Mode.LIGHT_RAIN,
            (float) ctx.param("radius", 6.0D), (int) ctx.param("duration", 60.0D), ctx.scaled("damage", 4.0D), ctx.color());
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.LIGHT_RAIN.get(), 1.0F, 1.6F);
        return true;
    }
}
