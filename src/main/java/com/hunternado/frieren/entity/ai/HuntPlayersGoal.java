package com.hunternado.frieren.entity.ai;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.mana.ManaHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Demon target selector that respects mana concealment: a concealing player is only noticed within
 * {@code followRange * (1 - concealFraction)}. Creative and spectator players are ignored.
 */
public class HuntPlayersGoal extends Goal {
    private final Mob mob;
    private int scanDelay;

    public HuntPlayersGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    public static double senseRange(Mob mob, Player player) {
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        MagicData data = MagicData.get(player);
        if (data != null) {
            range *= 1.0D - ManaHelper.concealFraction(data);
        }
        return Math.max(2.0D, range);
    }

    public static boolean canSense(Mob mob, Player player) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
            return false;
        }
        double range = senseRange(mob, player);
        return mob.distanceToSqr(player) <= range * range;
    }

    @Override
    public boolean canUse() {
        if (--scanDelay > 0) {
            return false;
        }
        scanDelay = 10;
        if (mob.getTarget() != null && mob.getTarget().isAlive()) {
            return false;
        }
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : mob.level().players()) {
            if (!canSense(mob, player)) {
                continue;
            }
            double distance = mob.distanceToSqr(player);
            if (distance < bestDistance && mob.hasLineOfSight(player)) {
                bestDistance = distance;
                best = player;
            }
        }
        if (best != null) {
            mob.setTarget(best);
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() instanceof Player player) {
            // Concealing beyond the reduced range makes the demon lose track.
            return canSense(mob, player) || mob.distanceToSqr(player) < 16.0D;
        }
        return false;
    }

    @Override
    public void stop() {
        if (mob.getTarget() instanceof Player) {
            mob.setTarget(null);
        }
    }
}
