package com.hunternado.frieren.client.gui;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.client.ClientMagicCache;
import com.hunternado.frieren.config.FrierenClientConfig;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.network.packet.CastStatePacket;
import com.hunternado.frieren.network.packet.ManaSensePacket;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraftforge.client.gui.overlay.ForgeLayer;

/**
 * Compact HUD in the bottom-left corner: a gem-capped mana bar, the five quick slots (spell icons on dark glass,
 * shaded while cooling down), and status tags (concealment, barrier, flight). Hidden until the first spell is learned
 * (configurable). Under the crosshair, a cast bar shows an incantation filling, a charge growing (with its percentage
 * and a pulse once full) or a channel draining, and flashes red when concentration breaks.
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

        drawCastBar(graphics, font, deltaTracker.getGameTimeDeltaPartialTick(false));
        drawSense(graphics, font, minecraft);
    }

    /**
     * Detection results as a compass around the crosshair: one marker per signature in its direction, coloured by kind
     * (blue mages, red demons, green NPCs), larger for stronger mana; concealment you noticed but could not see through
     * flickers as a "?". The four strongest are labelled with how they compare to you and how far away they are.
     */
    private static void drawSense(GuiGraphicsExtractor graphics, Font font, Minecraft minecraft) {
        List<ManaSensePacket.Entry> entries = ClientMagicCache.activeSense();
        float remaining = ClientMagicCache.senseRemaining();
        Player self = minecraft.player;
        if (entries.isEmpty() || remaining <= 0.0F || self == null || minecraft.level == null) {
            return;
        }
        int alpha = Math.max(40, Math.round(255.0F * Math.min(1.0F, remaining * 3.0F))) << 24;
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        int radius = Math.min(70, graphics.guiHeight() / 4);
        float selfMana = ClientMagicCache.senseSelfMana();
        List<ManaSensePacket.Entry> labelled = new ArrayList<>(entries);
        labelled.sort(Comparator.comparingDouble((ManaSensePacket.Entry entry) -> -entry.apparentMana()));
        List<ManaSensePacket.Entry> top = labelled.subList(0, Math.min(4, labelled.size()));
        boolean flicker = (ClientMagicCache.gameTime() / 5L) % 2L == 0L;
        List<int[]> placed = new ArrayList<>();                  // labels drawn so far, as {x, y, width}
        for (ManaSensePacket.Entry entry : entries) {
            Entity entity = minecraft.level.getEntity(entry.entityId());
            if (entity == null) {
                continue;
            }
            double dx = entity.getX() - self.getX();
            double dz = entity.getZ() - self.getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            float relative = Mth.wrapDegrees(yaw - self.getYRot()) * Mth.DEG_TO_RAD;
            int mx = cx + Math.round(Mth.sin(relative) * radius);
            int my = cy - Math.round(Mth.cos(relative) * radius);
            int color = switch (entry.kind()) {
                case 0 -> 0x7FD4FF;
                case 1 -> 0xE0405A;
                case 2 -> 0xB8F0A0;
                default -> 0xC9A0FF;
            };
            if (entry.has(ManaSensePacket.FLAG_NOTICED)) {
                if (flicker) {
                    graphics.text(font, "?", mx - font.width("?") / 2, my - 4, alpha | 0xFFE08A, true);
                }
                continue;
            }
            int size = 1 + Math.min(4, (int) (Math.log10(Math.max(1.0F, entry.apparentMana())) * 1.4F));
            graphics.fill(mx - size - 1, my - size - 1, mx + size + 1, my + size + 1, alpha | 0x101018);
            graphics.fill(mx - size, my - size, mx + size, my + size, alpha | color);
            if (entry.has(ManaSensePacket.FLAG_BOSS)) {
                graphics.fill(mx - size - 2, my - size - 2, mx + size + 2, my - size - 1, alpha | 0xD4AF37);
            }
            if (top.contains(entry)) {
                float ratio = entry.apparentMana() / selfMana;
                String word = ratio < 0.25F ? "faint" : ratio < 0.75F ? "weaker" : ratio < 1.5F ? "comparable" : ratio < 3.0F ? "stronger" : "overwhelming";
                Component label = Component.translatable("hud.frieren.sense." + word, Math.round(Math.sqrt(dx * dx + dz * dz)));
                int labelColor = ratio >= 3.0F ? 0xFF6060 : ratio >= 1.5F ? 0xFFC060 : 0xE8E8F0;
                int lw = font.width(label);
                int lx = mx - lw / 2;
                int ly = my + size + 2;
                // signatures at nearly the same bearing would print over each other: each clash steps down a line
                for (int tries = 0; tries < 4 && overlaps(placed, lx, ly, lw); tries++) {
                    ly += 10;
                }
                placed.add(new int[] {lx, ly, lw});
                graphics.text(font, label, lx, ly, alpha | labelColor, true);
            }
        }
    }

    private static boolean overlaps(List<int[]> placed, int x, int y, int width) {
        for (int[] other : placed) {
            if (x < other[0] + other[2] + 2 && other[0] < x + width + 2 && Math.abs(y - other[1]) < 10) {
                return true;
            }
        }
        return false;
    }

    /** The bar under the crosshair: name above, progress below. Nothing is drawn while idle. */
    private static void drawCastBar(GuiGraphicsExtractor graphics, Font font, float partialTick) {
        int width = 82;
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() / 2 + 14;
        float broken = ClientMagicCache.brokenFlash();
        byte state = ClientMagicCache.castState();
        if (state == CastStatePacket.IDLE) {
            if (broken > 0.0F) {
                int alpha = (int) (broken * 255.0F) << 24;
                graphics.fill(x - 1, y - 1, x + width + 1, y + 5, alpha | 0x200000);
                graphics.fill(x, y, x + width, y + 4, alpha | 0xD83A30);
                Component text = Component.translatable("hud.frieren.concentration_broken");
                graphics.text(font, text, (graphics.guiWidth() - font.width(text)) / 2, y + 7, alpha | 0xFF8070, true);
            }
            return;
        }
        Identifier spellId = ClientMagicCache.castSpell();
        SpellDefinition definition = spellId != null ? SpellManager.client(spellId) : null;
        int color = definition != null ? definition.color() : 0xFFE08A;
        float elapsed = ClientMagicCache.castElapsed(partialTick);
        float total = ClientMagicCache.castTotal();
        float fraction;
        Component label;
        if (state == CastStatePacket.CHARGING) {
            fraction = Math.min(1.0F, elapsed / total);
            int percent = Math.round(fraction * 100.0F);
            label = Component.translatable(fraction >= 1.0F ? "hud.frieren.charged" : "hud.frieren.charging", percent);
            if (fraction >= 1.0F && (ClientMagicCache.gameTime() / 4L) % 2L == 0L) {
                color = 0xFFFFFF;                                 // full: the bar pulses white
            }
        } else if (state == CastStatePacket.CHANNELING) {
            fraction = Math.max(0.0F, 1.0F - elapsed / total);
            label = Component.translatable("hud.frieren.channeling");
        } else {
            fraction = Math.min(1.0F, elapsed / total);
            label = Component.translatable("hud.frieren.casting");
        }
        graphics.fill(x - 1, y - 1, x + width + 1, y + 5, 0xC0101018);
        graphics.fill(x, y, x + width, y + 4, 0xFF2A2A38);
        int filled = Math.round(width * fraction);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 4, 0xFF000000 | color);
            graphics.fill(x, y, x + filled, y + 1, 0x60FFFFFF);
        }
        if (state == CastStatePacket.CHARGING) {
            // Ticks at the minimum charge and at the execution threshold, where the spell turns lethal.
            mark(graphics, x, y, width, definition != null ? (float) definition.param("min_charge", 0.25D) : 0.25F, 0xFFB0B0C0);
            if (definition != null && definition.param("execute_max_health", 0.0D) > 0.0D) {
                mark(graphics, x, y, width, (float) definition.param("execute_charge", 0.95D), 0xFFFF6040);
            }
        }
        Component name = definition != null ? definition.displayName() : Component.empty();
        Component line = Component.empty().append(name).append(" ").append(label);
        graphics.text(font, line, (graphics.guiWidth() - font.width(line)) / 2, y + 7, 0xFFFFFFFF, true);
    }

    private static void mark(GuiGraphicsExtractor graphics, int x, int y, int width, float at, int color) {
        int mx = x + Math.round(width * Math.max(0.0F, Math.min(1.0F, at)));
        graphics.fill(mx, y - 2, mx + 1, y + 6, color);
    }

    private static int tag(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, true);
        return x + font.width(text) + 6;
    }
}
