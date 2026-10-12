package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.magic.ManaBoltEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.world.phys.Vec3;

/**
 * Lügner's blood magic, learned from a demon grimoire: a fan of crimson blades, each healing the caster for part of
 * the damage it deals. Paid partly in health (see the spell's {@code health_cost}). Params: {@code damage},
 * {@code count}, {@code spread}, {@code speed}, {@code lifesteal}, {@code range}.
 */
public class BloodBladesBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        int count = Math.max(1, (int) ctx.param("count", 5.0D));
        double spread = ctx.param("spread", 9.0D);
        float damage = ctx.scaled("damage", 4.0D);
        float speed = (float) ctx.param("speed", 1.6D);
        float lifesteal = (float) ctx.param("lifesteal", 0.3D);
        Vec3 eye = ctx.caster().getEyePosition();
        Vec3 direction = SpellTargeting.aimPoint(ctx.caster(), ctx.param("range", 28.0D)).subtract(eye).normalize();
        for (int i = 0; i < count; i++) {
            double offset = (i - (count - 1) / 2.0D) * spread;
            Vec3 bladeDirection = direction.yRot((float) Math.toRadians(offset));
            ManaBoltEntity bolt = ManaBoltEntity.shoot(ctx.level(), ctx.caster(), eye.add(bladeDirection.scale(10.0D)), damage, speed,
                ctx.color(), ModDamageTypes.DEMON_MAGIC, 1.5F);
            if (bolt != null) {
                bolt.withLifesteal(lifesteal);
            }
        }
        SpellFx.wisps(ctx.level(), eye.add(direction.scale(1.0D)), ctx.color(), 0.8F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.DEMON_BOLT.get(), 1.0F, 0.8F);
        return true;
    }
}
