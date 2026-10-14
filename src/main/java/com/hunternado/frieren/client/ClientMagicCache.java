package com.hunternado.frieren.client;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.network.packet.CastStatePacket;
import com.hunternado.frieren.network.packet.ManaSensePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client mirror of the local player's magic data, independent of the player entity (which is recreated on
 * respawn and dimension change). Only ever written by clientbound packet handlers.
 */
public final class ClientMagicCache {
    /** How long detection results stay visible. */
    public static final long SENSE_DURATION_MS = 8000L;

    private static MagicData data = new MagicData();
    private static final List<ManaSensePacket.Entry> sensed = new ArrayList<>();
    private static long sensedAt;
    private static boolean sawThrough;
    private static float senseSelfMana = 1.0F;
    private static boolean castKeyHeld;
    private static byte castState = CastStatePacket.IDLE;
    private static @Nullable Identifier castSpell;
    private static long castStart;
    private static int castTotal;
    private static long brokenAtMs = -10000L;

    private ClientMagicCache() {}

    public static MagicData data() {
        return data;
    }

    public static void reset() {
        data = new MagicData();
        sensed.clear();
        castState = CastStatePacket.IDLE;
        castSpell = null;
    }

    public static void setCastState(CastStatePacket packet) {
        if (packet.state() == CastStatePacket.BROKEN) {
            brokenAtMs = System.currentTimeMillis();
            castState = CastStatePacket.IDLE;
            castSpell = null;
            return;
        }
        castState = packet.state();
        castSpell = packet.spell().isEmpty() ? null : Identifier.tryParse(packet.spell());
        castStart = packet.startTime();
        castTotal = Math.max(1, packet.totalTicks());
    }

    /** INCANTING, CHARGING, CHANNELING or IDLE (see {@link CastStatePacket}). */
    public static byte castState() {
        return castState;
    }

    public static @Nullable Identifier castSpell() {
        return castSpell;
    }

    /** Ticks since the current incantation, charge or channel began. */
    public static float castElapsed(float partialTick) {
        return Math.max(0.0F, gameTime() - castStart + partialTick);
    }

    public static int castTotal() {
        return castTotal;
    }

    /** 0..1 while the "concentration broken" flash is showing, fading out over 0.8 s. */
    public static float brokenFlash() {
        long age = System.currentTimeMillis() - brokenAtMs;
        return age >= 800L ? 0.0F : 1.0F - age / 800.0F;
    }

    public static long gameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null ? minecraft.level.getGameTime() : 0L;
    }

    public static void setSensed(ManaSensePacket packet) {
        sensed.clear();
        sensed.addAll(packet.entries());
        sawThrough = packet.sawThroughSomething();
        senseSelfMana = Math.max(1.0F, packet.selfMana());
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

    /** The detector's own capacity at the time of the pulse (for "weaker"/"overwhelming"). */
    public static float senseSelfMana() {
        return senseSelfMana;
    }

    /** 0..1: how much of the sense display time is left (for fading). */
    public static float senseRemaining() {
        long age = System.currentTimeMillis() - sensedAt;
        return age >= SENSE_DURATION_MS ? 0.0F : 1.0F - age / (float) SENSE_DURATION_MS;
    }

    public static boolean castKeyHeld() {
        return castKeyHeld;
    }

    public static void setCastKeyHeld(boolean held) {
        castKeyHeld = held;
    }
}
