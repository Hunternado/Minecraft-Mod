package com.hunternado.frieren.client.gui;

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
 * Compact HUD in the bottom-left corner: mana bar, the five quick slots with cooldowns, and status tags
 * (concealment, barrier, flight, casting). Hidden until the first spell is learned (configurable).
 */
public final class ManaHudLayer implements ForgeLayer {
    private static final int BAR_WIDTH = 104;
    private static final int SLOT_SIZE = 20;

    @Override
    public void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !FrierenClientConfig.showHud()) {
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

        // ---- Quick slots ----
        int slotsTop = bottom - SLOT_SIZE;
        for (int i = 0; i < MagicData.QUICK_SLOTS; i++) {
            int x = left + i * (SLOT_SIZE + 2);
            boolean selected = i == data.selectedSlot();
            graphics.fill(x - 1, slotsTop - 1, x + SLOT_SIZE + 1, slotsTop + SLOT_SIZE + 1, selected ? 0xFFE8C15A : 0xFF2A2A3A);
            graphics.fill(x, slotsTop, x + SLOT_SIZE, slotsTop + SLOT_SIZE, 0xCC10101C);
            Identifier spellId = data.slot(i);
            if (spellId == null) {
                graphics.text(font, String.valueOf(i + 1), x + 7, slotsTop + 6, 0xFF555566, false);
                continue;
            }
            SpellDefinition definition = SpellManager.client(spellId);
            String label = abbreviation(definition != null ? definition.displayName().getString() : spellId.getPath());
            int color = definition != null ? 0xFF000000 | definition.color() : 0xFFFFFFFF;
            graphics.text(font, label, x + (SLOT_SIZE - font.width(label)) / 2, slotsTop + 6, color, true);
            int remaining = data.cooldownRemaining(spellId, time);
            if (remaining > 0 && definition != null && definition.cooldown() > 0) {
                float fraction = Math.min(1.0F, remaining / (float) definition.cooldown());
                int height = Math.round(SLOT_SIZE * fraction);
                graphics.fill(x, slotsTop + SLOT_SIZE - height, x + SLOT_SIZE, slotsTop + SLOT_SIZE, 0xAA000000);
                String seconds = String.valueOf((remaining + 19) / 20);
                graphics.text(font, seconds, x + SLOT_SIZE - font.width(seconds) - 1, slotsTop + SLOT_SIZE - 9, 0xFFFFFFFF, true);
            }
        }

        // ---- Mana bar ----
        int barTop = slotsTop - 9;
        float fraction = data.maxMana() > 0 ? data.mana() / data.maxMana() : 0.0F;
        graphics.fill(left - 1, barTop - 1, left + BAR_WIDTH + 1, barTop + 5, 0xFF1A1A2A);
        graphics.fill(left, barTop, left + Math.round(BAR_WIDTH * fraction), barTop + 4, data.concealing() ? 0xFF8A7FE8 : 0xFF4FA8FF);
        String manaText = Math.round(data.mana()) + " / " + data.maxMana();
        graphics.text(font, manaText, left, barTop - 10, 0xFFCFE8FF, true);

        // ---- Status tags ----
        int tagX = left + font.width(manaText) + 6;
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

    /** Two-letter label for a slot, e.g. "Zoltraak" -> "Zo", "Flight Magic" -> "FM". */
    static String abbreviation(String name) {
        String[] words = name.trim().split("[\\s\\-']+");
        if (words.length >= 2 && !words[1].isEmpty()) {
            return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase(java.util.Locale.ROOT);
        }
        return name.length() >= 2 ? name.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + name.substring(1, 2) : name;
    }
}
