package com.hunternado.frieren.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Server → client: what the local player is casting right now, sent whenever it changes, so the HUD can draw the
 * incantation and charge bars. The client advances progress itself from {@code startTime}; nothing here decides an
 * outcome. {@code spell} is the spell id ("" when idle).
 */
public record CastStatePacket(byte state, String spell, long startTime, int totalTicks) {
    public static final byte IDLE = 0;
    public static final byte INCANTING = 1;
    public static final byte CHARGING = 2;
    public static final byte CHANNELING = 3;
    /** Concentration broke: the HUD flashes the bar red for a moment. */
    public static final byte BROKEN = 4;

    public static final StreamCodec<RegistryFriendlyByteBuf, CastStatePacket> STREAM_CODEC =
        StreamCodec.ofMember(CastStatePacket::encode, CastStatePacket::decode);

    private static void encode(CastStatePacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeByte(msg.state);
        buf.writeUtf(msg.spell, 256);
        buf.writeLong(msg.startTime);
        buf.writeVarInt(msg.totalTicks);
    }

    private static CastStatePacket decode(RegistryFriendlyByteBuf buf) {
        return new CastStatePacket(buf.readByte(), buf.readUtf(256), buf.readLong(), buf.readVarInt());
    }
}
