package com.hunternado.frieren.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Idle wandering to a random dry, standable spot within eight blocks. Water is avoided implicitly: the block
 * under a destination must be solid. Built on the base {@link Goal} API only.
 */
public class WanderGoal extends Goal {
    private static final int HORIZONTAL_RANGE = 8;
    private static final int VERTICAL_RANGE = 3;
    private static final int MAX_WALK_TICKS = 120;

    private final PathfinderMob mob;
    private final double speed;
    private BlockPos destination;
    private int ticksLeft;

    public WanderGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null || mob.isPassenger() || mob.getRandom().nextInt(120) != 0) {
            return false;
        }
        destination = findDestination();
        return destination != null;
    }

    @Override
    public boolean canContinueToUse() {
        return ticksLeft > 0 && mob.getTarget() == null && destination != null
            && mob.distanceToSqr(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D) > 2.25D;
    }

    @Override
    public void start() {
        ticksLeft = MAX_WALK_TICKS;
        mob.getNavigation().moveTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D, speed);
    }

    @Override
    public void tick() {
        ticksLeft--;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        destination = null;
    }

    private BlockPos findDestination() {
        Level level = mob.level();
        BlockPos origin = mob.blockPosition();
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = mob.getRandom().nextInt(HORIZONTAL_RANGE * 2 + 1) - HORIZONTAL_RANGE;
            int dz = mob.getRandom().nextInt(HORIZONTAL_RANGE * 2 + 1) - HORIZONTAL_RANGE;
            for (int dy = VERTICAL_RANGE; dy >= -VERTICAL_RANGE; dy--) {
                BlockPos candidate = origin.offset(dx, dy, dz);
                if (level.getBlockState(candidate.below()).isSolid()
                    && !level.getBlockState(candidate).isSolid()
                    && !level.getBlockState(candidate.above()).isSolid()) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
