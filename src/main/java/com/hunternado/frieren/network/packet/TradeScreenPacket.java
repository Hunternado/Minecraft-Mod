package com.hunternado.frieren.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Server → client: open an NPC's trade list. */
public record TradeScreenPacket(int entityId, String titleKey, List<Offer> offers) {
    public record Offer(ItemStack costA, ItemStack costB, ItemStack result, int stock) {}

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeScreenPacket> STREAM_CODEC =
        StreamCodec.ofMember(TradeScreenPacket::encode, TradeScreenPacket::decode);

    public Component title() {
        return Component.translatable(titleKey);
    }

    private static void encode(TradeScreenPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeUtf(msg.titleKey, 256);
        buf.writeVarInt(msg.offers.size());
        for (Offer offer : msg.offers) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, offer.costA);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, offer.costB);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, offer.result);
            buf.writeVarInt(offer.stock);
        }
    }

    private static TradeScreenPacket decode(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        String title = buf.readUtf(256);
        int count = Math.min(buf.readVarInt(), 64);
        List<Offer> offers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            offers.add(new Offer(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarInt()));
        }
        return new TradeScreenPacket(id, title, List.copyOf(offers));
    }
}
