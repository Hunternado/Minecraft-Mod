package com.hunternado.frieren.entity.ai;

import com.hunternado.frieren.util.Hostility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Target selector for friendly fighters: picks the nearest hostile mob within range every half second.
 * Implemented on the plain {@link Goal} API so it does not depend on vanilla targeting-condition internals.
 */
public class HuntHostilesGoal extends Goal {
    private final Mob mob;
    private final double range;
    private int scanDelay;

    public HuntHostilesGoal(Mob mob, double range) {
        this.mob = mob;
        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (--scanDelay > 0) {
            return false;
        }
        scanDelay = 10;
        LivingEntity current = mob.getTarget();
        if (current != null && current.isAlive()) {
            return false;
        }
        List<LivingEntity> candidates = mob.level().getEntitiesOfClass(LivingEntity.class, new AABB(mob.blockPosition()).inflate(range),
            entity -> entity.isAlive() && Hostility.isHostile(entity));
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distance = candidate.distanceToSqr(mob);
            if (distance < best && mob.hasLineOfSight(candidate)) {
                best = distance;
                nearest = candidate;
            }
        }
        if (nearest != null) {
            mob.setTarget(nearest);
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && target.distanceToSqr(mob) < range * range * 4.0D;
    }

    @Override
    public void stop() {
        mob.setTarget(null);
    }
}
