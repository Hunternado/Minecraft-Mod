package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.SummonTracker;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.phys.Vec3;

/**
 * Serie's bottled golem (the guardians handed to First-Class examinees): a stone guardian steps out of a magic
 * circle in front of the caster, fights hostile creatures, and crumbles when its time is up. Params:
 * {@code duration} (ticks), {@code distance}.
 */
public class GolemBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        IronGolem golem = EntityType.IRON_GOLEM.create(ctx.level(), EntitySpawnReason.MOB_SUMMONED);
        if (golem == null) {
            return false;
        }
        Vec3 look = ctx.caster().getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z).normalize();
        Vec3 at = ctx.caster().position().add(flat.scale(ctx.param("distance", 2.5D)));
        golem.snapTo(at.x, ctx.caster().getY(), at.z, ctx.caster().getYRot(), 0.0F);
        golem.setPlayerCreated(true);
        if (!ctx.level().noCollision(golem)) {
            golem.snapTo(ctx.caster().getX(), ctx.caster().getY(), ctx.caster().getZ(), ctx.caster().getYRot(), 0.0F);
        }
        int duration = (int) (ctx.param("duration", 1200.0D) * (1.0D + ctx.masteryFraction()));
        SummonTracker.track(ctx.level(), golem, duration);
        ctx.level().addFreshEntity(golem);
        MagicCircleEntity.onGround(ctx.level(), golem.position(), MagicCircleEntity.STYLE_HOLY, ctx.color(), 1.6F, 40);
        SpellFx.burst(ctx.level(), golem.position().add(0.0D, 1.2D, 0.0D), ctx.color(), 1.4F);
        SpellFx.sound(ctx.level(), golem.position(), ModSounds.TELEPORT.get(), 1.0F, 0.7F);
        return true;
    }
}
