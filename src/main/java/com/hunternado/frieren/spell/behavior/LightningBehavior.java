package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Judradjim, the lightning summoning spell. A visual-only bolt (no fire, no creeper charging) plus
 * controlled area damage. Params: {@code damage}, {@code range}, {@code radius}.
 */
public class LightningBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        double range = ctx.param("range", 40.0D);
        double radius = ctx.param("radius", 3.0D);
        float damage = ctx.scaled("damage", 10.0D);
        Vec3 target = SpellTargeting.aimPoint(ctx.caster(), range);

        LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, ctx.level());
        bolt.setPos(target);
        bolt.setVisualOnly(true);
        ctx.level().addFreshEntity(bolt);

        for (LivingEntity victim : SpellTargeting.livingInRadius(ctx.level(), ctx.caster(), target, radius,
            entity -> SpellDamage.canHarm(ctx.level(), ctx.caster(), entity))) {
            float falloff = (float) (1.0D - 0.5D * victim.position().distanceTo(target) / radius);
            SpellDamage.hurt(ctx, victim, damage * Math.max(0.5F, falloff), ModDamageTypes.SPELL, 1.2F);
        }
        SpellFx.burst(ctx.level(), target, ctx.color(), (float) radius);
        return true;
    }
}
