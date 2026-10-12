package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Draht's wire magic, from a demon grimoire: invisible threads lash out from a point and bind every enemy around
 * it. Bosses are only held for a moment. Params: {@code range}, {@code radius}, {@code duration}, {@code damage},
 * {@code max_targets}.
 */
public class WireSnareBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        Vec3 point = SpellTargeting.aimPoint(ctx.caster(), ctx.param("range", 20.0D));
        double radius = ctx.param("radius", 6.0D);
        int duration = (int) ctx.param("duration", 60.0D);
        float damage = ctx.scaled("damage", 2.0D);
        int max = (int) ctx.param("max_targets", 6.0D);
        List<LivingEntity> targets = SpellTargeting.livingInRadius(ctx.level(), ctx.caster(), point, radius,
            candidate -> SpellDamage.canHarm(ctx.level(), ctx.caster(), candidate));
        if (targets.isEmpty()) {
            return false;
        }
        Vec3 hand = ctx.caster().getEyePosition().subtract(0.0D, 0.3D, 0.0D);
        int bound = 0;
        for (LivingEntity target : targets) {
            if (bound++ >= max) {
                break;
            }
            int ticks = target instanceof FrierenBoss ? Math.min(20, duration) : duration;
            target.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.BOUND), ticks, 0));
            SpellDamage.hurt(ctx, target, damage, ModDamageTypes.DEMON_MAGIC, 1.0F);
            Vec3 at = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
            SpellFx.beam(ctx.level(), hand, at, ctx.color(), 0.05F, Math.min(ticks, 40), SpellBeamEntity.STYLE_LIGHT);
        }
        SpellFx.streaks(ctx.level(), point, ctx.color(), (float) radius);
        SpellFx.sound(ctx.level(), point, ModSounds.CURSE.get(), 1.0F, 1.8F);
        return true;
    }
}
