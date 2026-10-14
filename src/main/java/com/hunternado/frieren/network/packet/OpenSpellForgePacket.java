package com.hunternado.frieren.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Server → client: Serie agreed to create a spell; open her forge (the client already has its own originals). */
public record OpenSpellForgePacket(int entityId) {
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSpellForgePacket> STREAM_CODEC =
        StreamCodec.ofMember(OpenSpellForgePacket::encode, OpenSpellForgePacket::decode);

    private static void encode(OpenSpellForgePacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
    }

    private static OpenSpellForgePacket decode(RegistryFriendlyByteBuf buf) {
        return new OpenSpellForgePacket(buf.readVarInt());
    }
}
