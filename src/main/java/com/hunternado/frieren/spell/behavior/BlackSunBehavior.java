package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.magic.SpellAreaEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.world.phys.Vec3;

/**
 * The Black Sun: a dark sun rises where the caster aims and three rings of blasts roll outward from it, each ring
 * telegraphed by circles on the ground first (see SpellAreaEntity's BLACK_SUN mode). Params: {@code range},
 * {@code radius} (of the outer ring), {@code damage} (per blast), {@code execute_max_health}.
 */
public class BlackSunBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        Vec3 at = SpellTargeting.aimPoint(ctx.caster(), ctx.param("range", 40.0D));
        SpellAreaEntity area = SpellAreaEntity.spawn(ctx.level(), ctx.caster(), at, SpellAreaEntity.Mode.BLACK_SUN,
            (float) ctx.param("radius", 10.0D), 46, ctx.scaled("damage", 24.0D), ctx.color());
        if (area == null) {
            return false;
        }
        area.withExecute(ctx.executeCap());
        SpellFx.wisps(ctx.level(), at.add(0.0D, 3.0D, 0.0D), ctx.color(), 2.5F);
        SpellFx.sound(ctx.level(), at, ModSounds.CURSE.get(), 1.4F, 0.5F);
        return true;
    }
}
