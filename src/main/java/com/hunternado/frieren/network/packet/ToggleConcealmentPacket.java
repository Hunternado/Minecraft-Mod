package com.hunternado.frieren.network.packet;

import com.hunternado.frieren.mana.ManaSensing;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client → server: toggle mana concealment. */
public record ToggleConcealmentPacket() {
    public static final ToggleConcealmentPacket INSTANCE = new ToggleConcealmentPacket();
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleConcealmentPacket> STREAM_CODEC =
        StreamCodec.ofMember((msg, buf) -> {}, buf -> INSTANCE);

    public static void handle(ToggleConcealmentPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            ManaSensing.toggleConcealment(player);
        }
    }
}
