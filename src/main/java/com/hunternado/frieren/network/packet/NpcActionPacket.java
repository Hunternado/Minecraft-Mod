package com.hunternado.frieren.network.packet;

import com.hunternado.frieren.entity.npc.NpcInteractions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client → server: a button pressed in an NPC screen (start exam, buy offer, ...). The server re-checks
 * distance, entity type and all requirements.
 */
public record NpcActionPacket(int entityId, int action, int argument) {
    public static final int EXAM_START = 0;
    public static final int EXAM_ABANDON = 1;
    public static final int CLAIM_PRIVILEGE = 2;
    public static final int BUY_OFFER = 3;
    public static final int SUBMIT_EXAM_ITEM = 4;

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcActionPacket> STREAM_CODEC =
        StreamCodec.ofMember(NpcActionPacket::encode, NpcActionPacket::decode);

    private static void encode(NpcActionPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.action);
        buf.writeVarInt(msg.argument);
    }

    private static NpcActionPacket decode(RegistryFriendlyByteBuf buf) {
        return new NpcActionPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(NpcActionPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            NpcInteractions.handleAction(player, msg.entityId, msg.action, msg.argument);
        }
    }
}
