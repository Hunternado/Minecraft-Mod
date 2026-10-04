package com.hunternado.frieren.network.packet;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.spell.SpellCaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client → server: "I pressed cast for quick slot N". Everything else is decided by the server. */
public record CastSpellPacket(int slot) {
    public static final StreamCodec<RegistryFriendlyByteBuf, CastSpellPacket> STREAM_CODEC =
        StreamCodec.ofMember(CastSpellPacket::encode, CastSpellPacket::decode);

    private static void encode(CastSpellPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot);
    }

    private static CastSpellPacket decode(RegistryFriendlyByteBuf buf) {
        return new CastSpellPacket(buf.readVarInt());
    }

    public static void handle(CastSpellPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null || msg.slot < 0 || msg.slot >= MagicData.QUICK_SLOTS) {
            return;
        }
        SpellCaster.requestCast(player, msg.slot);
    }
}
