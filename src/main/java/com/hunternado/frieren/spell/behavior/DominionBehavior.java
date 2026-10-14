package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ObedienceTracker;
import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Hostility;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * Dominion: the Demon King's command. Up to {@code max_puppets} hostile creatures within {@code radius} (never bosses,
 * never anything already bound) become the caster's puppets for {@code duration} ticks, with no weighing of mana. They
 * take orders from the Command Puppets key like Azeryuze's.
 */
public class DominionBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        double radius = ctx.param("radius", 24.0D);
        List<Mob> subjects = ctx.level().getEntitiesOfClass(Mob.class, new AABB(ctx.caster().blockPosition()).inflate(radius),
            mob -> mob.isAlive() && !(mob instanceof FrierenBoss) && Hostility.isHostile(mob) && !ObedienceTracker.isObedient(mob)
                && mob.distanceToSqr(ctx.caster()) <= radius * radius);
        if (subjects.isEmpty()) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.dominion_none"));
            return false;
        }
        subjects.sort(Comparator.comparingDouble((LivingEntity mob) -> mob.distanceToSqr(ctx.caster())));
        int max = Math.max(1, (int) ctx.param("max_puppets", 12.0D));
        int duration = Math.round(ctx.scaled("duration", 1200.0D));
        int bound = 0;
        for (Mob mob : subjects) {
            if (bound >= max) {
                break;
            }
            ObedienceTracker.enthrall(ctx.level(), mob, ctx.caster(), duration);
            SpellFx.scales(ctx.level(), mob.position().add(0.0D, mob.getBbHeight() + 0.6D, 0.0D), ctx.color());
            bound++;
            ctx.markHit();
        }
        SpellFx.ring(ctx.level(), ctx.caster().position().add(0.0D, 0.1D, 0.0D), ctx.color(), (float) radius);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.BOSS_ROAR.get(), 1.0F, 0.5F);
        Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.dominion", bound));
        return true;
    }
}
