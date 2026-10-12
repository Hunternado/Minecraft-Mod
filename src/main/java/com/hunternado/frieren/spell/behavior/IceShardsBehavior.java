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
 * Frost Lance: a fan of ice shards that chill what they hit. Params: {@code damage}, {@code count},
 * {@code spread} (degrees between shards), {@code speed}, {@code slow_ticks}, {@code range}.
 */
public class IceShardsBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        int count = Math.max(1, (int) ctx.param("count", 3.0D));
        double spread = ctx.param("spread", 7.0D);
        float damage = ctx.scaled("damage", 4.0D);
        float speed = (float) ctx.param("speed", 1.4D);
        int slow = (int) ctx.param("slow_ticks", 60.0D);
        Vec3 aim = SpellTargeting.aimPoint(ctx.caster(), ctx.param("range", 32.0D));
        Vec3 eye = ctx.caster().getEyePosition();
        Vec3 direction = aim.subtract(eye).normalize();
        for (int i = 0; i < count; i++) {
            double offset = (i - (count - 1) / 2.0D) * spread;
            Vec3 shardDirection = direction.yRot((float) Math.toRadians(offset));
            ManaBoltEntity bolt = ManaBoltEntity.shoot(ctx.level(), ctx.caster(), eye.add(shardDirection.scale(10.0D)), damage, speed,
                ctx.color(), ModDamageTypes.SPELL, 1.0F);
            if (bolt != null) {
                bolt.withFrost(slow);
            }
        }
        SpellFx.frost(ctx.level(), eye.add(direction.scale(1.2D)), ctx.color(), 0.6F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.WIND.get(), 0.9F, 1.6F);
        return true;
    }
}
