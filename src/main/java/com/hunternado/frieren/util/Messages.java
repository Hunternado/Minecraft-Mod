package com.hunternado.frieren.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Messaging helpers so the (version-sensitive) vanilla calls live in one place. */
public final class Messages {
    private Messages() {}

    /** Short status text above the hotbar. */
    public static void actionBar(Player player, Component message) {
        player.sendOverlayMessage(message);
    }

    public static void chat(Player player, Component message) {
        player.sendSystemMessage(message);
    }

    /** Sends a chat line to every player within {@code radius} of {@code center}. */
    public static void nearby(ServerLevel level, Vec3 center, double radius, Component message) {
        double radiusSqr = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(center) <= radiusSqr) {
                player.sendSystemMessage(message);
            }
        }
    }
}
