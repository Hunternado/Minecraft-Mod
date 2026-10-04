package com.hunternado.frieren.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Idle head movement: now and then the mob turns to watch the nearest player in range for a couple of seconds.
 * Built on the base {@link Goal} API only, so it does not depend on vanilla's look-goal classes.
 */
public class IdleLookGoal extends Goal {
    private final Mob mob;
    private final float range;
    private Player watched;
    private int ticksLeft;

    public IdleLookGoal(Mob mob, float range) {
        this.mob = mob;
        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null || mob.getRandom().nextFloat() >= 0.02F) {
            return false;
        }
        watched = nearestPlayer();
        return watched != null;
    }

    @Override
    public boolean canContinueToUse() {
        return ticksLeft > 0 && mob.getTarget() == null && watched != null && watched.isAlive()
            && mob.distanceToSqr(watched) <= (double) range * range;
    }

    @Override
    public void start() {
        ticksLeft = 40 + mob.getRandom().nextInt(40);
    }

    @Override
    public void stop() {
        watched = null;
    }

    @Override
    public void tick() {
        ticksLeft--;
        if (watched != null) {
            mob.getLookControl().setLookAt(watched, 10.0F, 30.0F);
        }
    }

    private Player nearestPlayer() {
        Player best = null;
        double bestDistance = (double) range * range;
        for (Player player : mob.level().players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            double distance = mob.distanceToSqr(player);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }
}
