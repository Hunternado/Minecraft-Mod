package com.hunternado.frieren.client.gui;

import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.NpcActionPacket;
import com.hunternado.frieren.network.packet.TradeScreenPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/** Simple merchant list: each offer shows its costs and result with a Buy button. */
public class TradeScreen extends Screen {
    private final TradeScreenPacket packet;

    public TradeScreen(TradeScreenPacket packet) {
        super(packet.title());
        this.packet = packet;
    }

    @Override
    protected void init() {
        int top = 40;
        int right = this.width / 2 + 150;
        for (int i = 0; i < packet.offers().size(); i++) {
            final int index = i;
            this.addRenderableWidget(Button.builder(Component.translatable("screen.frieren.trades.buy"),
                b -> FrierenNetwork.sendToServer(new NpcActionPacket(packet.entityId(), NpcActionPacket.BUY_OFFER, index)))
                .pos(right - 50, top + i * 24).size(50, 20).build());
        }
    }

    private static MutableComponent describe(ItemStack stack) {
        return Component.literal(stack.getCount() + "× ").append(stack.getHoverName());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
        int left = this.width / 2 - 150;
        int top = 40;
        for (int i = 0; i < packet.offers().size(); i++) {
            TradeScreenPacket.Offer offer = packet.offers().get(i);
            MutableComponent line = describe(offer.costA());
            if (!offer.costB().isEmpty()) {
                line.append(" + ").append(describe(offer.costB()));
            }
            line.append("  →  ").append(describe(offer.result()));
            graphics.text(this.font, line, left, top + i * 24 + 6, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
