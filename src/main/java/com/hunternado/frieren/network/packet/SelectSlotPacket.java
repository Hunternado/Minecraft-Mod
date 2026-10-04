package com.hunternado.frieren.network.packet;

import com.hunternado.frieren.data.MagicData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client → server: change the highlighted quick slot. */
public record SelectSlotPacket(int slot) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectSlotPacket> STREAM_CODEC =
        StreamCodec.ofMember(SelectSlotPacket::encode, SelectSlotPacket::decode);

    private static void encode(SelectSlotPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot);
    }

    private static SelectSlotPacket decode(RegistryFriendlyByteBuf buf) {
        return new SelectSlotPacket(buf.readVarInt());
    }

    public static void handle(SelectSlotPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null || msg.slot < 0 || msg.slot >= MagicData.QUICK_SLOTS) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data != null) {
            data.setSelectedSlot(msg.slot);
        }
    }
}
