package com.hunternado.frieren.network.packet;

import com.hunternado.frieren.combat.ObedienceTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client → server: the Command Puppets key. Carries nothing: the server reads where the player is looking and
 * whether they sneak, so the client cannot pick targets it should not reach.
 */
public record CommandPuppetsPacket() {
    public static final CommandPuppetsPacket INSTANCE = new CommandPuppetsPacket();
    public static final StreamCodec<RegistryFriendlyByteBuf, CommandPuppetsPacket> STREAM_CODEC =
        StreamCodec.ofMember((msg, buf) -> {}, buf -> INSTANCE);

    public static void handle(CommandPuppetsPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            ObedienceTracker.command(player);
        }
    }
}
