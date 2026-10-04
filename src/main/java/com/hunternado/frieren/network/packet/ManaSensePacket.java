package com.hunternado.frieren.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: results of a mana detection pulse. Only apparent (possibly concealed) values are sent,
 * never true mana, so a modified client cannot see through concealment.
 */
public record ManaSensePacket(List<Entry> entries, boolean sawThroughSomething) {
    /** kind: 0 = player, 1 = demon, 2 = mage NPC, 3 = mimic/other. */
    public record Entry(int entityId, float apparentMana, byte kind) {}

    public static final StreamCodec<RegistryFriendlyByteBuf, ManaSensePacket> STREAM_CODEC =
        StreamCodec.ofMember(ManaSensePacket::encode, ManaSensePacket::decode);

    private static void encode(ManaSensePacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.entries.size());
        for (Entry entry : msg.entries) {
            buf.writeVarInt(entry.entityId);
            buf.writeFloat(entry.apparentMana);
            buf.writeByte(entry.kind);
        }
        buf.writeBoolean(msg.sawThroughSomething);
    }

    private static ManaSensePacket decode(RegistryFriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), 256);
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readVarInt(), buf.readFloat(), buf.readByte()));
        }
        return new ManaSensePacket(List.copyOf(entries), buf.readBoolean());
    }
}
