package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.entity.demon.DemonEntity;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.AABB;

/**
 * Flamme's Deception. The caster's mana vanishes: concealment switches on, every ordinary demon around loses track of
 * them and none can target them while {@link ModEffects#DECEIVING} lasts ({@code duration} ticks, longer with mastery).
 * Their magic hits demons {@link com.hunternado.frieren.spell.SpellDamage#DECEPTION_AMBUSH} times as hard; the first
 * such hit ends the deception a second later (see SpellDamage). Bosses are not fooled.
 */
public class DeceptionBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        int duration = Math.round(ctx.scaled("duration", 600.0D));
        ctx.caster().addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.DECEIVING), duration, 0, false, true, true));
        ctx.data().setConcealing(true);
        for (DemonEntity demon : ctx.level().getEntitiesOfClass(DemonEntity.class, new AABB(ctx.caster().blockPosition()).inflate(32.0D),
                demon -> !(demon instanceof FrierenBoss) && demon.getTarget() == ctx.caster())) {
            demon.setTarget(null);
            demon.getNavigation().stop();
        }
        SpellFx.petals(ctx.level(), ctx.caster().position(), ctx.color(), 2.0F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.MANA_CONCEAL.get(), 1.0F, 0.6F);
        return true;
    }
}
