package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Gathering magic: pulls dropped items and experience within {@code radius} to the caster. */
public class GatherBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        double radius = ctx.param("radius", 10.0D) * (0.8D + 0.2D * ctx.power());
        List<Entity> loot = ctx.level().getEntities(ctx.caster(), new AABB(ctx.caster().blockPosition()).inflate(radius),
            entity -> entity instanceof ItemEntity || entity instanceof ExperienceOrb);
        if (loot.isEmpty()) {
            return false;
        }
        for (Entity entity : loot) {
            entity.setPos(ctx.caster().getX(), ctx.caster().getY() + 0.25D, ctx.caster().getZ());
            entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
            if (entity instanceof ItemEntity item) {
                item.setNoPickUpDelay();
            }
        }
        SpellFx.ring(ctx.level(), ctx.caster().position().add(0, 0.1D, 0), ctx.color(), (float) Math.min(radius, 6.0D));
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.UTILITY.get(), 0.7F, 1.6F);
        return true;
    }
}
