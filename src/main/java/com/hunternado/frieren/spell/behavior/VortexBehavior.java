package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.magic.SpellAreaEntity;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.registry.ModSounds;
import net.minecraft.world.phys.Vec3;

/** Waldgose, Denken's tornado. Spawns a tornado construct at the aimed point. */
public class VortexBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        Vec3 at = SpellTargeting.aimPoint(ctx.caster(), ctx.param("range", 24.0D));
        SpellAreaEntity.spawn(ctx.level(), ctx.caster(), at, SpellAreaEntity.Mode.TORNADO,
            (float) ctx.param("radius", 4.5D), (int) ctx.param("duration", 80.0D), ctx.scaled("damage", 2.0D), ctx.color());
        SpellFx.sound(ctx.level(), at, ModSounds.WIND.get(), 1.2F, 0.6F);
        return true;
    }
}
