package com.hunternado.frieren.client;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.network.packet.ManaSensePacket;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Client mirror of the local player's magic data, independent of the player entity (which is recreated on
 * respawn and dimension change). Only ever written by clientbound packet handlers.
 */
public final class ClientMagicCache {
    /** How long detection results stay visible. */
    private static final long SENSE_DURATION_MS = 4000L;

    private static MagicData data = new MagicData();
    private static final List<ManaSensePacket.Entry> sensed = new ArrayList<>();
    private static long sensedAt;
    private static boolean sawThrough;
    private static boolean castKeyHeld;

    private ClientMagicCache() {}

    public static MagicData data() {
        return data;
    }

    public static void reset() {
        data = new MagicData();
        sensed.clear();
    }

    public static long gameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null ? minecraft.level.getGameTime() : 0L;
    }

    public static void setSensed(ManaSensePacket packet) {
        sensed.clear();
        sensed.addAll(packet.entries());
        sawThrough = packet.sawThroughSomething();
        sensedAt = System.currentTimeMillis();
    }

    public static List<ManaSensePacket.Entry> activeSense() {
        if (System.currentTimeMillis() - sensedAt > SENSE_DURATION_MS) {
            sensed.clear();
        }
        return sensed;
    }

    public static boolean sawThrough() {
        return sawThrough;
    }

    public static boolean castKeyHeld() {
        return castKeyHeld;
    }

    public static void setCastKeyHeld(boolean held) {
        castKeyHeld = held;
    }
}
