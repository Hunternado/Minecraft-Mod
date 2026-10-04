package com.hunternado.frieren.network.packet;

import com.hunternado.frieren.mana.ManaSensing;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client → server: perform a mana detection pulse (rate limited server-side). */
public record DetectManaPacket() {
    public static final DetectManaPacket INSTANCE = new DetectManaPacket();
    public static final StreamCodec<RegistryFriendlyByteBuf, DetectManaPacket> STREAM_CODEC =
        StreamCodec.ofMember((msg, buf) -> {}, buf -> INSTANCE);

    public static void handle(DetectManaPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            ManaSensing.detect(player);
        }
    }
}
