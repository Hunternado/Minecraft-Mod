package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Curse Purification: removes harmful effects (and Aura's enthrallment) from you and nearby allies. */
public class PurifyBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        double radius = ctx.param("radius", 6.0D);
        int cleansed = 0;
        List<LivingEntity> targets = SpellTargeting.livingInRadius(ctx.level(), null, ctx.caster().position(), radius, HealBehavior::isFriendly);
        for (LivingEntity target : targets) {
            List<MobEffectInstance> harmful = new ArrayList<>();
            for (MobEffectInstance instance : target.getActiveEffects()) {
                if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    harmful.add(instance);
                }
            }
            for (MobEffectInstance instance : harmful) {
                target.removeEffect(instance.getEffect());
                cleansed++;
            }
            if (target instanceof Player player) {
                MagicData data = MagicData.get(player);
                if (data != null && data.enthralledUntil() > 0L) {
                    data.setEnthralledUntil(0L);
                    cleansed++;
                }
            }
        }
        if (cleansed == 0) {
            return false;
        }
        ctx.markHit();
        SpellFx.ring(ctx.level(), ctx.caster().position().add(0, 0.2D, 0), ctx.color(), (float) radius);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.HEAL.get(), 1.0F, 0.8F);
        return true;
    }
}
