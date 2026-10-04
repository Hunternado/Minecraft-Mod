package com.hunternado.frieren.network.packet;

import com.hunternado.frieren.spell.SpellCaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client → server: the cast key was released; ends a channelled spell early. */
public record ReleaseChannelPacket() {
    public static final ReleaseChannelPacket INSTANCE = new ReleaseChannelPacket();
    public static final StreamCodec<RegistryFriendlyByteBuf, ReleaseChannelPacket> STREAM_CODEC =
        StreamCodec.ofMember((msg, buf) -> {}, buf -> INSTANCE);

    public static void handle(ReleaseChannelPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            SpellCaster.releaseChannel(player);
        }
    }
}
