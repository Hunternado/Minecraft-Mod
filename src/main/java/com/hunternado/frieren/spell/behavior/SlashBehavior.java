package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.registry.ModSounds;
import net.minecraft.world.entity.LivingEntity;

/**
 * Reelseiden, Übel's "magic that cuts anything": a short arc that is very expensive for barriers to block.
 * Params: {@code damage}, {@code range}, {@code angle}, {@code barrier_cost}.
 */
public class SlashBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        double range = ctx.param("range", 5.0D);
        double angle = ctx.param("angle", 45.0D);
        float damage = ctx.scaled("damage", 9.0D);
        float barrierCost = (float) ctx.param("barrier_cost", 3.0D);
        for (LivingEntity target : SpellTargeting.livingInRadius(ctx.level(), ctx.caster(), ctx.caster().getEyePosition(), range,
            entity -> SpellDamage.canHarm(ctx.level(), ctx.caster(), entity) && SpellTargeting.inCone(ctx.caster(), entity, angle))) {
            SpellDamage.hurt(ctx, target, damage, ModDamageTypes.SPELL, barrierCost);
        }
        SpellFx.cone(ctx.level(), ctx.caster().getEyePosition(), ctx.caster().getLookAngle(), ctx.color(), (float) range, 1);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.SLASH.get(), 1.0F, 1.4F);
        return true;
    }
}
