package com.hunternado.frieren.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Server → client: full snapshot of the local player's magic data (sent only on structural changes). */
public record SyncMagicDataPacket(CompoundTag data, long gameTime) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMagicDataPacket> STREAM_CODEC =
        StreamCodec.ofMember(SyncMagicDataPacket::encode, SyncMagicDataPacket::decode);

    private static void encode(SyncMagicDataPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
        buf.writeLong(msg.gameTime);
    }

    private static SyncMagicDataPacket decode(RegistryFriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncMagicDataPacket(tag != null ? tag : new CompoundTag(), buf.readLong());
    }
}
