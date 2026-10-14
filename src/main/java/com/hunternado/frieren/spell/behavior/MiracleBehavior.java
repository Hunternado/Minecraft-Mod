package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Heiter's Miracle. Everyone friendly within {@code radius} (the caster included) is healed to full, cleansed of every
 * harmful effect and fire, and receives {@link ModEffects#BLESSED} for {@code blessing} ticks: the next blow that
 * would kill them leaves them at half health instead (CombatEvents).
 */
public class MiracleBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        double radius = ctx.param("radius", 8.0D);
        int blessing = Math.round(ctx.scaled("blessing", 1200.0D));
        List<LivingEntity> allies = SpellTargeting.livingInRadius(ctx.level(), null, ctx.caster().position(), radius,
            target -> target == ctx.caster() || HealBehavior.isFriendly(target));
        for (LivingEntity ally : allies) {
            ally.setHealth(ally.getMaxHealth());
            List<MobEffectInstance> harmful = new ArrayList<>();
            for (MobEffectInstance effect : ally.getActiveEffects()) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    harmful.add(effect);
                }
            }
            for (MobEffectInstance effect : harmful) {
                ally.removeEffect(effect.getEffect());
            }
            ally.clearFire();
            ally.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.BLESSED), blessing, 0));
            SpellFx.burst(ctx.level(), ally.position().add(0.0D, ally.getBbHeight() * 0.7D, 0.0D), ctx.color(), 0.9F);
            ctx.markHit();
        }
        SpellFx.groundCircle(ctx.level(), ctx.caster().position().add(0.0D, 0.05D, 0.0D), ctx.spell(), (float) radius, 40);
        SpellFx.petals(ctx.level(), ctx.caster().position(), 0xFFFFFF, (float) radius);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.HEAL.get(), 1.2F, 0.8F);
        return true;
    }
}
