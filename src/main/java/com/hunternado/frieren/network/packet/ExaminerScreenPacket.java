package com.hunternado.frieren.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Server → client: open the Association examiner dialog with the player's exam status. */
public record ExaminerScreenPacket(int entityId, CompoundTag info) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ExaminerScreenPacket> STREAM_CODEC =
        StreamCodec.ofMember(ExaminerScreenPacket::encode, ExaminerScreenPacket::decode);

    private static void encode(ExaminerScreenPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeNbt(msg.info);
    }

    private static ExaminerScreenPacket decode(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        CompoundTag tag = buf.readNbt();
        return new ExaminerScreenPacket(id, tag != null ? tag : new CompoundTag());
    }
}
