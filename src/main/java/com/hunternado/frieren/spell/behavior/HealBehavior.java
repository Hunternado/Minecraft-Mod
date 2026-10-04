package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/**
 * Goddess Healing (priest magic): heals the friendly creature you look at, otherwise yourself.
 * Params: {@code heal}, {@code range}.
 */
public class HealBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        SpellTargeting.EntityHit hit = SpellTargeting.firstEntity(ctx.level(), ctx.caster(), ctx.param("range", 12.0D), HealBehavior::isFriendly);
        LivingEntity target = hit != null ? hit.entity() : ctx.caster();
        if (target.getHealth() >= target.getMaxHealth()) {
            return false;
        }
        target.heal(ctx.scaled("heal", 6.0D));
        ctx.markHit();
        SpellFx.burst(ctx.level(), target.position().add(0, target.getBbHeight() * 0.6D, 0), ctx.color(), 0.8F);
        SpellFx.sound(ctx.level(), target.position(), ModSounds.HEAL.get(), 0.8F, 1.2F);
        return true;
    }

    static boolean isFriendly(LivingEntity entity) {
        return entity instanceof Player || entity instanceof TamableAnimal || entity instanceof Animal || entity instanceof AbstractVillager
            || entity instanceof com.hunternado.frieren.entity.npc.FrierenNpcBase;
    }
}
