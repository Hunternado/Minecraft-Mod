package com.hunternado.frieren.client.gui;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.client.ClientMagicCache;
import com.hunternado.frieren.config.FrierenClientConfig;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.gui.overlay.ForgeLayer;

/**
 * Compact HUD in the bottom-left corner: a gem-capped mana bar, the five quick slots (spell icons on dark glass,
 * shaded while cooling down), and status tags (concealment, barrier, flight, casting). Hidden until the first spell is learned (configurable).
 */
public final class ManaHudLayer implements ForgeLayer {
    private static final int SLOT_SIZE = 22;
    /** 23px pitch keeps five slots clear of the hotbar at GUI scale 2 on an 854px-wide window. */
    private static final int SLOT_STEP = 23;
    private static final int[][] OUTLINE = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final Identifier MANA_FRAME = FrierenIds.id("hud/mana_frame");
    private static final Identifier MANA_FILL = FrierenIds.id("hud/mana_fill");
    private static final Identifier MANA_FILL_CONCEAL = FrierenIds.id("hud/mana_fill_conceal");

    @Override
    public void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !FrierenClientConfig.showHud()) {
            return;
        }
        MagicData data = ClientMagicCache.data();
        if (data.knownCount() == 0 && FrierenClientConfig.hideHudWithoutSpells()) {
            return;
        }
        Font font = minecraft.font;
        int left = FrierenClientConfig.hudOffsetX();
        int bottom = graphics.guiHeight() - FrierenClientConfig.hudOffsetY();
        long time = ClientMagicCache.gameTime();

        // ---- Quick slots: metal-framed glass with the spell's icon, a dial of shade while it cools down ----
        int slotsTop = bottom - SLOT_SIZE;
        for (int i = 0; i < MagicData.QUICK_SLOTS; i++) {
            int x = left + i * SLOT_STEP;
            boolean selected = i == data.selectedSlot();
            if (selected) {
                GrimoireUi.sprite(graphics, GrimoireUi.SLOT_SELECTED, x - 1, slotsTop - 1, SLOT_SIZE + 2, SLOT_SIZE + 2);
            } else {
                GrimoireUi.sprite(graphics, GrimoireUi.SLOT, x, slotsTop, SLOT_SIZE, SLOT_SIZE);
            }
            Identifier spellId = data.slot(i);
            if (spellId == null) {
                graphics.text(font, String.valueOf(i + 1), x + (SLOT_SIZE - font.width(String.valueOf(i + 1))) / 2, slotsTop + 7, 0xFF5A5E78, false);
                continue;
            }
            SpellDefinition definition = SpellManager.client(spellId);
            GrimoireUi.spellIcon(graphics, spellId, definition, x + 3, slotsTop + 3, 16);
            int remaining = data.cooldownRemaining(spellId, time);
            if (remaining > 0 && definition != null && definition.cooldown() > 0) {
                float fraction = Math.min(1.0F, remaining / (float) definition.cooldown());
                int height = Math.round(16 * fraction);
                graphics.fill(x + 3, slotsTop + 19 - height, x + 19, slotsTop + 19, 0xB0000000);
                String seconds = String.valueOf((remaining + 19) / 20);
                graphics.text(font, seconds, x + SLOT_SIZE - font.width(seconds) - 2, slotsTop + SLOT_SIZE - 10, 0xFFFFFFFF, true);
            }
        }

        // ---- Mana bar: a gem-capped frame, filled with flowing blue (violet while concealing). Bar and slots fit in
        // the 40px strip under vanilla's chat, so chat never runs through them; the number sits on the bar, outlined
        // like the vanilla experience level. ----
        int barTop = slotsTop - 10;
        int barWidth = MagicData.QUICK_SLOTS * SLOT_STEP - 1;
        float fraction = data.maxMana() > 0 ? Math.min(1.0F, data.mana() / data.maxMana()) : 0.0F;
        GrimoireUi.sprite(graphics, MANA_FRAME, left, barTop, barWidth, 9);
        int fillLeft = left + 7;
        int fillWidth = barWidth - 14;
        int filled = Math.round(fillWidth * fraction);
        if (filled > 0) {
            graphics.enableScissor(fillLeft, barTop + 3, fillLeft + filled, barTop + 6);
            GrimoireUi.sprite(graphics, data.concealing() ? MANA_FILL_CONCEAL : MANA_FILL, fillLeft, barTop + 3, fillWidth, 3);
            graphics.disableScissor();
        }
        String manaText = Math.round(data.mana()) + " / " + data.maxMana();
        int textX = left + (barWidth - font.width(manaText)) / 2;
        int textY = barTop + 1;
        for (int[] d : OUTLINE) {
            graphics.text(font, manaText, textX + d[0], textY + d[1], 0xFF0A0C18, false);
        }
        graphics.text(font, manaText, textX, textY, data.concealing() ? 0xFFD6CCFF : 0xFFCFEFFF, false);

        // ---- Status tags ----
        int tagX = left + 1;
        if (data.concealing()) {
            tagX = tag(graphics, font, Component.translatable("hud.frieren.concealed", Math.round(ManaHelper.concealFraction(data) * 100)), tagX, barTop - 10, 0xFFB7A8FF);
        }
        if (data.barrierActive()) {
            tagX = tag(graphics, font, Component.translatable("hud.frieren.barrier"), tagX, barTop - 10, 0xFF9FD8FF);
        }
        if (data.flightActive()) {
            tagX = tag(graphics, font, Component.translatable("hud.frieren.flight"), tagX, barTop - 10, 0xFFE8F6FF);
        }
        if (data.pendingCast() != null) {
            tag(graphics, font, Component.translatable("hud.frieren.casting"), tagX, barTop - 10, 0xFFFFE08A);
        } else if (data.channelingSpell() != null) {
            tag(graphics, font, Component.translatable("hud.frieren.channeling"), tagX, barTop - 10, 0xFFFFE08A);
        }
    }

    private static int tag(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, true);
        return x + font.width(text) + 6;
    }
}
