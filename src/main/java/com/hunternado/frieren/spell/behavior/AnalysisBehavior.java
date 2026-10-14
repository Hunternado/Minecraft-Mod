package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.entity.demon.DemonEntity;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Frieren's Analysis. The creature looked at loses its demon barrier and every beneficial effect, and becomes
 * {@link ModEffects#EXPOSED} for {@code exposed} ticks (half for bosses): spell damage against it is multiplied by
 * {@link SpellDamage#EXPOSED_BONUS}. Params: {@code range}, {@code exposed}.
 */
public class AnalysisBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        SpellTargeting.EntityHit hit = SpellTargeting.firstEntity(ctx.level(), ctx.caster(), ctx.param("range", 32.0D),
            target -> SpellDamage.canHarm(ctx.level(), ctx.caster(), target));
        if (hit == null) {
            return false;
        }
        LivingEntity target = hit.entity();
        if (target instanceof DemonEntity demon) {
            demon.shatterBarrier();
        }
        List<MobEffectInstance> helpful = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                helpful.add(effect);
            }
        }
        for (MobEffectInstance effect : helpful) {
            target.removeEffect(effect.getEffect());
        }
        int ticks = Math.round(ctx.scaled("exposed", 200.0D));
        target.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.EXPOSED), target instanceof FrierenBoss ? ticks / 2 : ticks, 0));
        ctx.markHit();
        SpellFx.beam(ctx.level(), ctx.caster().getEyePosition(), hit.location(), ctx.color(), 0.12F, 14,
            SpellBeamEntity.STYLE_LIGHT);
        SpellFx.hexagon(ctx.level(), target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D), ctx.color(), 1.3F);
        SpellFx.sound(ctx.level(), target.position(), ModSounds.BARRIER_BREAK.get(), 1.0F, 1.3F);
        return true;
    }
}
