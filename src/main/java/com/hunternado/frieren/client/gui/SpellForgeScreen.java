package com.hunternado.frieren.client.gui;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.client.ClientMagicCache;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.CreateSpellPacket;
import com.hunternado.frieren.spell.OriginalSpells;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellForge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Locale;

/**
 * Serie's spell forge: ask for a spell and see at once what it will cost. The left page is the request (form,
 * element, potency up to Annihilation, reach, how it is cast, modifiers, colour); the right page names it, picks
 * which of your three original-magic slots it goes into, and shows the spell exactly as the server will build it:
 * mana, cooldown, incantation or charge time, damage, reach, whether a hit can break it, and Serie's price. The
 * numbers come from {@link OriginalSpells#compute}, the same code the server runs when it builds the spell.
 */
public class SpellForgeScreen extends Screen {
    private static final int ROW = 15;

    private final int serieId;
    private int form;
    private int element;
    private int potency = 2;
    private int reach = 1;
    private int casting = OriginalSpells.CAST_INSTANT;
    private int modifiers;
    private int color;
    private int slot;
    private String name = "";

    private int bookX;
    private int bookY;
    private int bookW;
    private int bookH;
    private EditBox nameBox;

    public SpellForgeScreen(int serieId) {
        super(Component.translatable("screen.frieren.forge"));
        this.serieId = serieId;
        // start on the first empty slot, or the first slot with what was asked for last time
        MagicData data = ClientMagicCache.data();
        slot = 0;
        for (int i = 0; i < OriginalSpells.MAX_SPELLS; i++) {
            if (!data.originals().containsKey(i)) {
                slot = i;
                break;
            }
        }
        loadSlot();
    }

    private void loadSlot() {
        MagicData.OriginalEntry entry = ClientMagicCache.data().originals().get(slot);
        if (entry != null) {
            OriginalSpells.Recipe r = entry.recipe();
            form = r.form();
            element = r.element();
            potency = r.potency();
            reach = r.reach();
            casting = r.casting();
            modifiers = r.modifiers();
            color = r.color();
            name = r.name();
        }
        if (nameBox != null) {
            nameBox.setValue(name);
        }
    }

    private OriginalSpells.Recipe recipe() {
        return new OriginalSpells.Recipe(form, element, potency, reach, casting, modifiers, color, name).sanitized();
    }

    // ------------------------------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        bookW = Math.min(this.width - 16, 372);
        bookH = Math.min(this.height - 20, 214);
        bookX = (this.width - bookW) / 2;
        bookY = (this.height - bookH) / 2;
        int left = bookX + 14;
        int pageW = bookW / 2 - 24;
        int y = bookY + 30;
        String[] rows = {"form", "element", "potency", "reach", "casting"};
        for (int i = 0; i < rows.length; i++) {
            final int row = i;
            int ry = y + i * ROW;
            this.addRenderableWidget(new InkButton(left + 52, ry, 11, 12, Component.literal("<"), () -> cycle(row, -1)));
            this.addRenderableWidget(new InkButton(left + pageW - 11, ry, 11, 12, Component.literal(">"), () -> cycle(row, 1)));
        }
        int my = y + rows.length * ROW + 12;
        int bw = (pageW - 4) / 3;
        for (int i = 0; i < OriginalSpells.MODIFIERS.length; i++) {
            final int bit = 1 << i;
            int bx = left + (i % 3) * (bw + 2);
            int by = my + (i / 3) * 14;
            this.addRenderableWidget(new InkButton(bx, by, bw, 12,
                Component.translatable("original.frieren.modifier." + OriginalSpells.MODIFIERS[i]), () -> modifiers ^= bit));
        }

        int right = bookX + bookW / 2 + 10;
        int rightW = bookW - bookW / 2 - 24;
        nameBox = new EditBox(this.font, right, bookY + 26, rightW, 13, Component.translatable("screen.frieren.forge.name"));
        nameBox.setMaxLength(OriginalSpells.MAX_NAME);
        nameBox.setHint(Component.literal(OriginalSpells.defaultName(recipe())));
        nameBox.setValue(name);
        nameBox.setResponder(value -> name = value);
        this.addRenderableWidget(nameBox);
        int sw = (rightW - 4) / 3;
        for (int i = 0; i < OriginalSpells.MAX_SPELLS; i++) {
            final int index = i;
            this.addRenderableWidget(new InkButton(right + i * (sw + 2), bookY + 44, sw, 12, slotLabel(i), () -> {
                slot = index;
                loadSlot();
            }));
        }
        this.addRenderableWidget(new InkButton(right + rightW / 2 - 40, bookY + bookH - 24, 80, 15,
            Component.translatable("screen.frieren.forge.ask"), this::ask));
    }

    private Component slotLabel(int index) {
        MagicData.OriginalEntry entry = ClientMagicCache.data().originals().get(index);
        String text = entry == null ? Component.translatable("screen.frieren.forge.slot_empty", index + 1).getString()
            : (index + 1) + ": " + (entry.recipe().name().isEmpty() ? OriginalSpells.defaultName(entry.recipe()) : entry.recipe().name());
        return Component.literal(GrimoireUi.fit(this.font, text, (bookW - bookW / 2 - 28) / 3 - 4));
    }

    private void cycle(int row, int step) {
        switch (row) {
            case 0 -> form = Math.floorMod(form + step, OriginalSpells.FORMS.length);
            case 1 -> element = Math.floorMod(element + step, OriginalSpells.ELEMENTS.length);
            case 2 -> potency = 1 + Math.floorMod(potency - 1 + step, OriginalSpells.ANNIHILATION);
            case 3 -> reach = 1 + Math.floorMod(reach - 1 + step, 3);
            default -> casting = Math.floorMod(casting + step, OriginalSpells.CASTINGS.length);
        }
        // keep the shown values honest: what cannot be asked for snaps to what Serie would actually make
        OriginalSpells.Recipe r = recipe();
        potency = r.potency();
        casting = r.casting();
        if (nameBox != null) {
            nameBox.setHint(Component.literal(OriginalSpells.defaultName(r)));
        }
    }

    private void ask() {
        FrierenNetwork.sendToServer(new CreateSpellPacket(serieId, slot, recipe().save()));
        this.onClose();
    }

    // ------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        GrimoireUi.book(graphics, bookX, bookY, bookW, bookH);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        OriginalSpells.Recipe r = recipe();
        OriginalSpells.Stats stats = OriginalSpells.compute(r);
        drawRequest(graphics, r, mouseX, mouseY);
        drawPreview(graphics, r, stats);
    }

    private void drawRequest(GuiGraphicsExtractor graphics, OriginalSpells.Recipe r, int mouseX, int mouseY) {
        int left = bookX + 14;
        int pageW = bookW / 2 - 24;
        int cx = left + pageW / 2;
        GrimoireUi.centered(graphics, this.font, Component.translatable("screen.frieren.forge"), cx, bookY + 11, GrimoireUi.INK);
        GrimoireUi.divider(graphics, left + 10, bookY + 23, pageW - 20);
        int y = bookY + 30;
        String[] rows = {"form", "element", "potency", "reach", "casting"};
        Component[] values = {
            Component.translatable("original.frieren.form." + OriginalSpells.FORMS[r.form()]),
            Component.translatable("original.frieren.element." + OriginalSpells.ELEMENTS[r.element()]),
            r.potency() >= OriginalSpells.ANNIHILATION ? Component.translatable("screen.frieren.forge.annihilation")
                : Component.literal(String.valueOf(r.potency())),
            Component.literal(String.valueOf(r.reach())),
            Component.translatable("original.frieren.casting." + OriginalSpells.CASTINGS[r.casting()]),
        };
        for (int i = 0; i < rows.length; i++) {
            int ry = y + i * ROW;
            graphics.text(this.font, Component.translatable("screen.frieren.forge." + rows[i]), left, ry + 2, GrimoireUi.INK_SOFT, false);
            int valueCx = left + 52 + 11 + (pageW - 52 - 22) / 2;
            int ink = i == 2 && r.potency() >= OriginalSpells.ANNIHILATION ? GrimoireUi.INK_RED : GrimoireUi.INK;
            GrimoireUi.centered(graphics, this.font, values[i], valueCx, ry + 2, ink);
        }
        int my = y + rows.length * ROW;
        graphics.text(this.font, Component.translatable("screen.frieren.forge.modifiers"), left, my + 1, GrimoireUi.INK_SOFT, false);
        int bw = (pageW - 4) / 3;
        for (int i = 0; i < OriginalSpells.MODIFIERS.length; i++) {
            int bx = left + (i % 3) * (bw + 2);
            int by = my + 12 + (i / 3) * 14;
            boolean on = r.has(1 << i);
            boolean barred = (1 << i) == OriginalSpells.MOD_UNWAVERING && r.potency() >= OriginalSpells.ANNIHILATION;
            if (barred) {
                graphics.fill(bx + 3, by + 6, bx + bw - 3, by + 7, GrimoireUi.INK_RED);
            }
            if (on) {
                // a ribbon of the spell's colour under the chosen modifiers
                graphics.fill(bx, by + 12, bx + bw, by + 13, 0xFF000000 | OriginalSpells.COLORS[r.color()]);
                graphics.fill(bx - 1, by - 1, bx + bw + 1, by, GrimoireUi.INK_GOLD);
            }
            if (GrimoireUi.inside(mouseX, mouseY, bx, by, bw, 12)) {
                graphics.setTooltipForNextFrame(this.font, Component.translatable(barred ? "original.frieren.modifier.unwavering.barred"
                    : "original.frieren.modifier." + OriginalSpells.MODIFIERS[i] + ".desc"), mouseX, mouseY);
            }
        }
        int sy = my + 12 + 2 * 14 + 4;
        graphics.text(this.font, Component.translatable("screen.frieren.forge.color"), left, sy + 1, GrimoireUi.INK_SOFT, false);
        int swatch = Math.min(11, (pageW - 40) / OriginalSpells.COLORS.length);
        for (int i = 0; i < OriginalSpells.COLORS.length; i++) {
            int x = left + 38 + i * swatch;
            graphics.fill(x, sy, x + swatch - 2, sy + 9, 0xFF3A2A1C);
            graphics.fill(x + 1, sy + 1, x + swatch - 3, sy + 8, 0xFF000000 | OriginalSpells.COLORS[i]);
            if (i == r.color()) {
                graphics.fill(x - 1, sy + 10, x + swatch - 1, sy + 11, GrimoireUi.INK);
            }
        }
    }

    private void drawPreview(GuiGraphicsExtractor graphics, OriginalSpells.Recipe r, OriginalSpells.Stats s) {
        int right = bookX + bookW / 2 + 10;
        int width = bookW - bookW / 2 - 24;
        graphics.text(this.font, Component.translatable("screen.frieren.forge.name"), right, bookY + 15, GrimoireUi.INK_SOFT, false);
        // the chosen slot is underlined in gold
        int sw = (width - 4) / 3;
        graphics.fill(right + slot * (sw + 2), bookY + 57, right + slot * (sw + 2) + sw, bookY + 58, GrimoireUi.INK_GOLD);

        int y = bookY + 62;
        SpellDefinition spell = OriginalSpells.build(FrierenIds.id("original/preview"), r);
        GrimoireUi.spellIcon(graphics, spell.id(), spell, right, y, 16);
        graphics.text(this.font, GrimoireUi.fit(this.font, spell.displayName().getString(), width - 22), right + 20, y + 4,
            GrimoireUi.rarityInk(spell.rarity()), false);
        y += 20;
        y = line(graphics, right, y, Component.translatable("screen.frieren.forge.stat.mana", Math.round(s.manaCost())));
        y = line(graphics, right, y, Component.translatable("screen.frieren.forge.stat.cooldown", seconds(s.cooldown())));
        if (s.chargeTicks() > 0) {
            y = line(graphics, right, y, Component.translatable("screen.frieren.forge.stat.charge", seconds(s.chargeTicks())));
        } else if (s.castTime() > 0) {
            y = line(graphics, right, y, Component.translatable("screen.frieren.forge.stat.incant", seconds(s.castTime())));
        } else {
            y = line(graphics, right, y, Component.translatable("screen.frieren.forge.stat.instant"));
        }
        if (s.interruptible()) {
            y = line(graphics, right, y, Component.translatable("screen.frieren.forge.stat.fragile").withColor(GrimoireUi.INK_RED & 0xFFFFFF));
        }
        for (FormattedCharSequence seq : this.font.split(spell.description(), width)) {
            if (y > bookY + bookH - 44) {
                break;
            }
            graphics.text(this.font, seq, right, y, GrimoireUi.INK_SOFT, false);
            y += 9;
        }
        int crystals = SpellForge.crystalCost(r);
        int cores = SpellForge.coreCost(r);
        Component price = cores > 0 ? Component.translatable("screen.frieren.forge.price_cores", crystals, cores)
            : Component.translatable("screen.frieren.forge.price", crystals);
        GrimoireUi.centered(graphics, this.font, price, right + width / 2, bookY + bookH - 36, GrimoireUi.INK_GOLD);
    }

    private int line(GuiGraphicsExtractor graphics, int x, int y, Component text) {
        graphics.text(this.font, text, x, y, GrimoireUi.INK, false);
        return y + 10;
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1fs", ticks / 20.0F);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // colour swatches are drawn, not widgets
        int left = bookX + 14;
        int pageW = bookW / 2 - 24;
        int sy = bookY + 30 + 5 * ROW + 12 + 2 * 14 + 4;
        int swatch = Math.min(11, (pageW - 40) / OriginalSpells.COLORS.length);
        for (int i = 0; i < OriginalSpells.COLORS.length; i++) {
            int x = left + 38 + i * swatch;
            if (GrimoireUi.inside(event.x(), event.y(), x, sy, swatch - 2, 9)) {
                color = i;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
