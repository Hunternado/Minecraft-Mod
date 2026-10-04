package com.hunternado.frieren.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Server → nearby clients: one compact description of a spell visual (beam, burst, ring...), drawn with
 * client-side particles. One packet per effect instead of one vanilla particle packet per point.
 */
public record SpellFxPacket(byte type, double x1, double y1, double z1, double x2, double y2, double z2, int color, float param) {
    public static final byte BEAM = 0;
    public static final byte BURST = 1;
    public static final byte RING = 2;
    public static final byte CONE = 3;
    public static final byte HEXAGON = 4;
    public static final byte SPIRAL = 5;
    public static final byte SCALES = 6;

    public static final StreamCodec<RegistryFriendlyByteBuf, SpellFxPacket> STREAM_CODEC =
        StreamCodec.ofMember(SpellFxPacket::encode, SpellFxPacket::decode);

    private static void encode(SpellFxPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeByte(msg.type);
        buf.writeDouble(msg.x1);
        buf.writeDouble(msg.y1);
        buf.writeDouble(msg.z1);
        buf.writeDouble(msg.x2);
        buf.writeDouble(msg.y2);
        buf.writeDouble(msg.z2);
        buf.writeInt(msg.color);
        buf.writeFloat(msg.param);
    }

    private static SpellFxPacket decode(RegistryFriendlyByteBuf buf) {
        return new SpellFxPacket(buf.readByte(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readFloat());
    }
}
