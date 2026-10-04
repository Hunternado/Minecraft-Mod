package com.hunternado.frieren.network.packet;

import com.hunternado.frieren.spell.SpellDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/** Server → client: all spell definitions (login and /reload), so UI shows the server's balance. */
public record SyncSpellsPacket(List<SpellDefinition> spells) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSpellsPacket> STREAM_CODEC =
        StreamCodec.ofMember(SyncSpellsPacket::encode, SyncSpellsPacket::decode);

    private static void encode(SyncSpellsPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.spells.size());
        for (SpellDefinition spell : msg.spells) {
            spell.write(buf);
        }
    }

    private static SyncSpellsPacket decode(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<SpellDefinition> spells = new ArrayList<>(Math.min(count, 1024));
        for (int i = 0; i < count; i++) {
            spells.add(SpellDefinition.read(buf));
        }
        return new SyncSpellsPacket(List.copyOf(spells));
    }
}
