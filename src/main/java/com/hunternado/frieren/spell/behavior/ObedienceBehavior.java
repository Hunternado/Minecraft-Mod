package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ObedienceTracker;
import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Azeryuze, Aura's Scales of Obedience, as analysed by humans. Weighs the caster's mana against the target's
 * vitality: success makes the target fight for the caster for {@code duration} ticks, after which the
 * puppet crumbles; failure enthralls the caster briefly. Bosses and players are immune.
 * Params: {@code range}, {@code duration}, {@code ratio} (mana needed per point of target max health).
 */
public class ObedienceBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        SpellTargeting.EntityHit hit = SpellTargeting.firstEntity(ctx.level(), ctx.caster(), ctx.param("range", 16.0D),
            target -> target instanceof Mob && !(target instanceof FrierenBoss) && !(target instanceof Player));
        if (hit == null || !(hit.entity() instanceof Mob mob)) {
            return false;
        }
        SpellFx.scales(ctx.level(), mob.position().add(0, mob.getBbHeight() + 0.6D, 0), ctx.color());
        double needed = mob.getMaxHealth() * ctx.param("ratio", 6.0D);
        double weight = ctx.data().maxMana() * ctx.power();
        if (weight >= needed) {
            ObedienceTracker.enthrall(ctx.level(), mob, ctx.caster(), (int) ctx.param("duration", 1200.0D));
            ctx.markHit();
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.obedience_success", mob.getDisplayName()));
            SpellFx.sound(ctx.level(), mob.position(), ModSounds.AURA_SCALES.get(), 1.0F, 1.2F);
        } else {
            ctx.caster().addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.ENTHRALLED), 60, 0));
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.obedience_fail", mob.getDisplayName()));
            SpellFx.sound(ctx.level(), mob.position(), ModSounds.AURA_SCALES.get(), 1.0F, 0.6F);
        }
        return true;
    }
}
