package com.hunternado.frieren.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Server → client: compact mana update, sent at most every 10 ticks and only when it changed. */
public record SyncManaPacket(float mana, int maxMana) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaPacket> STREAM_CODEC =
        StreamCodec.ofMember(SyncManaPacket::encode, SyncManaPacket::decode);

    private static void encode(SyncManaPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeFloat(msg.mana);
        buf.writeVarInt(msg.maxMana);
    }

    private static SyncManaPacket decode(RegistryFriendlyByteBuf buf) {
        return new SyncManaPacket(buf.readFloat(), buf.readVarInt());
    }
}
