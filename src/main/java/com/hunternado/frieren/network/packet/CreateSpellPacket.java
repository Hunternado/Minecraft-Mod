package com.hunternado.frieren.network.packet;

import com.hunternado.frieren.spell.OriginalSpells;
import com.hunternado.frieren.spell.SpellForge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client → server: "Serie, make me this". Carries only the recipe and the slot; the server re-checks Serie, the
 * requirements and the price, and recomputes every number itself ({@link OriginalSpells#compute}).
 */
public record CreateSpellPacket(int entityId, int slot, CompoundTag recipe) {
    public static final StreamCodec<RegistryFriendlyByteBuf, CreateSpellPacket> STREAM_CODEC =
        StreamCodec.ofMember(CreateSpellPacket::encode, CreateSpellPacket::decode);

    private static void encode(CreateSpellPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.slot);
        buf.writeNbt(msg.recipe);
    }

    private static CreateSpellPacket decode(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int slot = buf.readVarInt();
        CompoundTag tag = buf.readNbt();
        return new CreateSpellPacket(entityId, slot, tag != null ? tag : new CompoundTag());
    }

    public static void handle(CreateSpellPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            SpellForge.create(player, msg.entityId, msg.slot, OriginalSpells.Recipe.load(msg.recipe));
        }
    }
}
