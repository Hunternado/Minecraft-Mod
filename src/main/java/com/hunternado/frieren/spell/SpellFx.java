package com.hunternado.frieren.spell;

import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.SpellFxPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side visual/audio helpers. Each call sends one compact {@link SpellFxPacket} to players within
 * {@link #FX_RANGE} blocks; the client expands it into particles.
 */
public final class SpellFx {
    public static final double FX_RANGE = 64.0D;

    private SpellFx() {}

    public static void beam(ServerLevel level, Vec3 from, Vec3 to, int color) {
        send(level, from, SpellFxPacket.BEAM, from, to, color, 0.0F);
    }

    public static void burst(ServerLevel level, Vec3 at, int color, float radius) {
        send(level, at, SpellFxPacket.BURST, at, at, color, radius);
    }

    public static void ring(ServerLevel level, Vec3 center, int color, float radius) {
        send(level, center, SpellFxPacket.RING, center, center, color, radius);
    }

    /** {@code style}: 0 = hellfire, 1 = cutting arc. */
    public static void cone(ServerLevel level, Vec3 origin, Vec3 direction, int color, float range, int style) {
        Vec3 end = origin.add(direction.normalize().scale(range));
        send(level, origin, SpellFxPacket.CONE, origin, end, color, style);
    }

    public static void hexagon(ServerLevel level, Vec3 center, int color, float radius) {
        send(level, center, SpellFxPacket.HEXAGON, center, center, color, radius);
    }

    public static void spiral(ServerLevel level, Vec3 base, int color, float height) {
        send(level, base, SpellFxPacket.SPIRAL, base, base, color, height);
    }

    public static void scales(ServerLevel level, Vec3 at, int color) {
        send(level, at, SpellFxPacket.SCALES, at, at, color, 0.0F);
    }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void send(ServerLevel level, Vec3 near, byte type, Vec3 a, Vec3 b, int color, float param) {
        FrierenNetwork.sendNear(level, near, FX_RANGE, new SpellFxPacket(type, a.x, a.y, a.z, b.x, b.y, b.z, color, param));
    }
}
