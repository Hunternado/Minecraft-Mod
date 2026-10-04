package com.hunternado.frieren.entity.boss;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps a boss bar's audience equal to the players within range. Called once per second, so bosses do
 * not depend on entity tracking callbacks.
 */
public final class BossBarHelper {
    public static final double RANGE = 48.0D;

    private BossBarHelper() {}

    public static void update(ServerBossEvent bar, ServerLevel level, LivingEntity boss) {
        bar.setProgress(Math.max(0.0F, Math.min(1.0F, boss.getHealth() / boss.getMaxHealth())));
        if (boss.tickCount % 20 != 0) {
            return;
        }
        double rangeSqr = RANGE * RANGE;
        List<ServerPlayer> current = new ArrayList<>(bar.getPlayers());
        for (ServerPlayer player : current) {
            if (!player.isAlive() || player.level() != level || player.distanceToSqr(boss) > rangeSqr) {
                bar.removePlayer(player);
            }
        }
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && player.distanceToSqr(boss) <= rangeSqr && !bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        }
    }
}
