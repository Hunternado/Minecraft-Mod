package com.hunternado.frieren.client.gui;

import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.NpcActionPacket;
import com.hunternado.frieren.network.packet.TradeScreenPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A merchant's ledger: one parchment row per offer with the price and the goods as items (hover for their names)
 * and an ink "Buy" stamp.
 */
public class TradeScreen extends Screen {
    private static final int SHEET_W = 236;
    private static final int ROW = 22;

    private final TradeScreenPacket packet;
    private int sheetX;
    private int sheetY;
    private int sheetH;
    private int visibleRows;

    public TradeScreen(TradeScreenPacket packet) {
        super(packet.title());
        this.packet = packet;
    }

    @Override
    protected void init() {
        visibleRows = Math.max(1, Math.min(packet.offers().size(), (this.height - 16 - 40) / ROW));
        sheetH = 40 + visibleRows * ROW;
        sheetX = (this.width - SHEET_W) / 2;
        sheetY = (this.height - sheetH) / 2;
        for (int i = 0; i < visibleRows; i++) {
            final int index = i;
            this.addRenderableWidget(new InkButton(sheetX + SHEET_W - 58, rowY(i) + 3, 40, 15, Component.translatable("screen.frieren.trades.buy"),
                () -> FrierenNetwork.sendToServer(new NpcActionPacket(packet.entityId(), NpcActionPacket.BUY_OFFER, index))));
        }
    }

    private int rowY(int i) {
        return sheetY + 30 + i * ROW;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        GrimoireUi.sheet(graphics, sheetX, sheetY, SHEET_W, sheetH);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = sheetX + SHEET_W / 2;
        GrimoireUi.centered(graphics, this.font, GrimoireUi.plain(this.title), cx, sheetY + 12, GrimoireUi.INK);
        GrimoireUi.divider(graphics, sheetX + 30, sheetY + 24, SHEET_W - 60);
        for (int i = 0; i < visibleRows; i++) {
            TradeScreenPacket.Offer offer = packet.offers().get(i);
            int y = rowY(i);
            int x = sheetX + 18;
            if (i > 0) {
                graphics.fill(sheetX + 16, y - 1, sheetX + SHEET_W - 16, y, GrimoireUi.RULE);
            }
            x = stack(graphics, offer.costA(), x, y + 3, mouseX, mouseY);
            if (!offer.costB().isEmpty()) {
                graphics.text(this.font, "+", x + 2, y + 7, GrimoireUi.INK_SOFT, false);
                x = stack(graphics, offer.costB(), x + 10, y + 3, mouseX, mouseY);
            }
            graphics.text(this.font, "→", x + 4, y + 7, GrimoireUi.INK, false);
            x = stack(graphics, offer.result(), x + 16, y + 3, mouseX, mouseY);
            String name = this.font.plainSubstrByWidth(offer.result().getHoverName().getString(), sheetX + SHEET_W - 64 - x);
            graphics.text(this.font, name, x + 2, y + 7, GrimoireUi.INK, false);
        }
    }

    /** Draws one item with its count; returns the x just past it. Hovering shows its tooltip. */
    private int stack(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        graphics.item(stack, x, y);
        graphics.itemDecorations(this.font, stack, x, y);
        if (GrimoireUi.inside(mouseX, mouseY, x, y, 16, 16)) {
            graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
        }
        return x + 18;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
