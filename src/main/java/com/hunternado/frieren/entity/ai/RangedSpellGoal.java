package com.hunternado.frieren.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Keeps a spellcaster between {@code minRange} and {@code maxRange} of its target and casts every
 * {@code interval} ticks while it has line of sight.
 */
public class RangedSpellGoal extends Goal {
    /** Implemented by mobs that can cast at a target. */
    public interface Caster {
        void castAt(LivingEntity target);

        default boolean canCastNow() {
            return true;
        }
    }

    private final PathfinderMob mob;
    private final Caster caster;
    private final double speed;
    private final double minRange;
    private final double maxRange;
    private final int interval;
    private int cooldown;
    private int repathDelay;

    public RangedSpellGoal(PathfinderMob mob, Caster caster, double speed, double minRange, double maxRange, int interval) {
        this.mob = mob;
        this.caster = caster;
        this.speed = speed;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.interval = interval;
        this.cooldown = interval / 2;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    /** The aggressive flag is synced to clients, where the humanoid renderer uses it for the casting pose. */
    @Override
    public void start() {
        mob.setAggressive(true);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        mob.setAggressive(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = mob.distanceTo(target);
        boolean canSee = mob.hasLineOfSight(target);

        if (--repathDelay <= 0) {
            repathDelay = 10;
            if (distance > maxRange || !canSee) {
                mob.getNavigation().moveTo(target, speed);
            } else if (distance < minRange) {
                Vec3 away = mob.position().subtract(target.position()).normalize().scale(6.0D).add(mob.position());
                mob.getNavigation().moveTo(away.x, away.y, away.z, speed * 1.1D);
            } else {
                mob.getNavigation().stop();
            }
        }

        if (--cooldown <= 0 && canSee && distance <= maxRange + 2.0D && caster.canCastNow()) {
            caster.castAt(target);
            cooldown = interval;
        }
    }
}
