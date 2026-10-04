package com.hunternado.frieren.network.packet;

import com.hunternado.frieren.data.MagicData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client → server: bind a known spell to a quick slot (empty id clears the slot). */
public record BindSpellPacket(int slot, String spell) {
    public static final StreamCodec<RegistryFriendlyByteBuf, BindSpellPacket> STREAM_CODEC =
        StreamCodec.ofMember(BindSpellPacket::encode, BindSpellPacket::decode);

    private static void encode(BindSpellPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot);
        buf.writeUtf(msg.spell, 256);
    }

    private static BindSpellPacket decode(RegistryFriendlyByteBuf buf) {
        return new BindSpellPacket(buf.readVarInt(), buf.readUtf(256));
    }

    public static void handle(BindSpellPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null || msg.slot < 0 || msg.slot >= MagicData.QUICK_SLOTS) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        if (msg.spell.isEmpty()) {
            data.setSlot(msg.slot, null);
            return;
        }
        Identifier id = Identifier.tryParse(msg.spell);
        // Only spells the player actually knows may be bound.
        if (id != null && data.knows(id)) {
            data.setSlot(msg.slot, id);
        }
    }
}
