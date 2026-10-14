package com.hunternado.frieren.entity.ai;

import com.hunternado.frieren.util.Ground;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Supplier;

/** Companions stay close to their party leader, and catch up by teleport when they fall more than 24 blocks behind. */
public class FollowLeaderGoal extends Goal {
    private final PathfinderMob mob;
    private final Supplier<LivingEntity> leader;
    private final double speed;
    private final double startDistance;
    private final double stopDistance;
    private int repathDelay;

    public FollowLeaderGoal(PathfinderMob mob, Supplier<LivingEntity> leader, double speed, double startDistance, double stopDistance) {
        this.mob = mob;
        this.leader = leader;
        this.speed = speed;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = leader.get();
        return target != null && target.isAlive() && mob.getTarget() == null && mob.distanceToSqr(target) > startDistance * startDistance;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = leader.get();
        return target != null && target.isAlive() && mob.getTarget() == null && mob.distanceToSqr(target) > stopDistance * stopDistance;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = leader.get();
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 10.0F, mob.getMaxHeadXRot());
        if (mob.distanceToSqr(target) > 24.0D * 24.0D && mob.level() instanceof ServerLevel level) {
            Vec3 spot = Ground.standable(level, target.position().subtract(target.getLookAngle().multiply(2.0D, 0.0D, 2.0D)));
            if (spot != null) {
                mob.teleportTo(spot.x, spot.y, spot.z);
                mob.getNavigation().stop();
                return;
            }
        }
        if (--repathDelay <= 0) {
            repathDelay = 10;
            mob.getNavigation().moveTo(target, speed);
        }
    }
}
