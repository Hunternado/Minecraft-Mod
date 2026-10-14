package com.hunternado.frieren.client.gui;

import com.hunternado.frieren.client.ClientMagicCache;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.data.SpellProgress;
import com.hunternado.frieren.magic.SpellSchool;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.BindSpellPacket;
import com.hunternado.frieren.spell.SpellCaster;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The grimoire: an open book with the spells you know on the left page (filtered by the school ribbons along the top)
 * and the chosen spell on the right, with its icon, lore, costs and mastery. The five quick slots sit under the book:
 * pick a spell, then click a slot to bind it; right-click a slot to clear it.
 */
public class SpellbookScreen extends Screen {
    private static final int ROW_HEIGHT = 18;
    private static final SpellSchool[] TABS = {null, SpellSchool.OFFENSIVE, SpellSchool.DEFENSIVE, SpellSchool.UTILITY, SpellSchool.SPECIAL,
        SpellSchool.ORIGINAL};

    private final List<Identifier> spells = new ArrayList<>();
    private @Nullable Identifier selected;
    private @Nullable SpellSchool filter;
    private int page;

    // layout, recomputed in init()
    private int bookX;
    private int bookY;
    private int bookW;
    private int bookH;
    private int rowsPerPage;

    public SpellbookScreen() {
        this(null);
    }

    /** Opens with a spell already chosen (the showcase uses this). */
    public SpellbookScreen(@Nullable Identifier preselected) {
        super(Component.translatable("screen.frieren.spellbook"));
        this.selected = preselected;
    }

    @Override
    protected void init() {
        bookW = Math.min(this.width - 16, 372);
        bookH = Math.min(this.height - 46, 204);
        bookX = (this.width - bookW) / 2;
        bookY = Math.max(14, (this.height - bookH - 30) / 2 + 4);
        rowsPerPage = Math.max(1, (bookH - 10 - 24 - 20) / ROW_HEIGHT);
        refreshList();
    }

    private void refreshList() {
        spells.clear();
        for (Identifier id : ClientMagicCache.data().knownSpells().keySet()) {
            SpellDefinition definition = SpellManager.client(id);
            if (filter == null || (definition != null && definition.school() == filter)) {
                spells.add(id);
            }
        }
        // rarity first (common to forbidden), then name, so the book reads like a bestiary
        spells.sort(Comparator.<Identifier, Integer>comparing(id -> {
            SpellDefinition d = SpellManager.client(id);
            return d != null ? d.rarity().ordinal() : 99;
        }).thenComparing(id -> {
            SpellDefinition d = SpellManager.client(id);
            return d != null ? d.displayName().getString() : id.toString();
        }));
        page = Math.min(page, pages() - 1);
    }

    private int pages() {
        return Math.max(1, (spells.size() + rowsPerPage - 1) / rowsPerPage);
    }

    // ------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawRibbons(graphics, mouseX, mouseY);   // under the cover, so only their tails show above it
        GrimoireUi.book(graphics, bookX, bookY, bookW, bookH);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawLeftPage(graphics, mouseX, mouseY);
        drawRightPage(graphics, mouseX, mouseY);
        drawSlots(graphics, mouseX, mouseY);
    }

    private int tabX(int i) {
        return bookX + 16 + i * 15;
    }

    private void drawRibbons(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (int i = 0; i < TABS.length; i++) {
            boolean active = TABS[i] == filter;
            int x = tabX(i);
            boolean hovered = GrimoireUi.inside(mouseX, mouseY, x, bookY - 12, 10, 14);
            int stick = active ? 13 : hovered ? 10 : 7;
            GrimoireUi.ribbon(graphics, x, bookY - stick, 22 + stick, GrimoireUi.schoolColor(TABS[i]));
            if (hovered) {
                graphics.setTooltipForNextFrame(this.font, tabName(TABS[i]), mouseX, mouseY);
            }
        }
    }

    private Component tabName(@Nullable SpellSchool school) {
        return school == null ? Component.translatable("screen.frieren.spellbook.all") : school.displayName();
    }

    private void drawLeftPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = bookX + 14;
        int width = bookW / 2 - 22;
        int cx = left + width / 2;
        int y = bookY + 13;
        Component header = Component.literal(tabName(filter).getString() + "  (" + spells.size() + ")");
        GrimoireUi.centered(graphics, this.font, header, cx, y, GrimoireUi.INK);
        GrimoireUi.divider(graphics, left + 8, y + 12, width - 16);

        int top = listTop();
        if (spells.isEmpty()) {
            Component none = Component.translatable(ClientMagicCache.data().knownCount() == 0
                ? "screen.frieren.spellbook.none" : "screen.frieren.spellbook.none_here");
            int ly = top + 8;
            for (FormattedCharSequence line : this.font.split(none, width - 8)) {
                graphics.centeredText(this.font, line, cx, ly, GrimoireUi.INK_SOFT);
                ly += 10;
            }
        }
        for (int i = 0; i < rowsPerPage; i++) {
            int index = page * rowsPerPage + i;
            if (index >= spells.size()) {
                break;
            }
            Identifier id = spells.get(index);
            SpellDefinition definition = SpellManager.client(id);
            int ry = top + i * ROW_HEIGHT;
            boolean hovered = GrimoireUi.inside(mouseX, mouseY, left, ry, width, ROW_HEIGHT);
            GrimoireUi.row(graphics, left, ry, width, ROW_HEIGHT, id.equals(selected), hovered);
            GrimoireUi.spellIcon(graphics, id, definition, left + 3, ry + 1, 16);
            Component name = definition != null ? definition.displayName() : Component.literal(id.toString());
            String text = GrimoireUi.fit(this.font, name.getString(), width - 34);
            graphics.text(this.font, text, left + 23, ry + 5, GrimoireUi.INK, false);
            if (definition != null) {
                GrimoireUi.sprite(graphics, GrimoireUi.gem(definition.rarity()), left + width - 10, ry + 5, 7, 7);
            }
        }

        int footer = bookY + bookH - 21;
        boolean many = pages() > 1;
        GrimoireUi.button(graphics, this.font, Component.literal("<"), left + 4, footer, 16, 12, mouseX, mouseY, many && page > 0);
        GrimoireUi.button(graphics, this.font, Component.literal(">"), left + width - 20, footer, 16, 12, mouseX, mouseY,
            many && page < pages() - 1);
        GrimoireUi.centered(graphics, this.font, Component.literal((page + 1) + " / " + pages()), cx, footer + 2, GrimoireUi.INK_SOFT);
    }

    private int listTop() {
        return bookY + 31;
    }

    private void drawRightPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = bookX + bookW / 2 + 10;
        int width = bookW - bookW / 2 - 24;
        int cx = left + width / 2;
        int y = bookY + 12;
        MagicData data = ClientMagicCache.data();
        SpellDefinition definition = selected != null ? SpellManager.client(selected) : null;
        SpellProgress progress = selected != null ? data.progress(selected) : null;
        if (definition == null || progress == null) {
            GrimoireUi.circle(graphics, cx, bookY + bookH / 2, Math.min(width, bookH - 30));
            Component hint = Component.translatable(spells.isEmpty() ? "screen.frieren.spellbook.hint_empty" : "screen.frieren.spellbook.hint");
            int ly = bookY + bookH / 2 - 8;
            for (FormattedCharSequence line : this.font.split(hint, width - 16)) {
                graphics.centeredText(this.font, line, cx, ly, GrimoireUi.INK_SOFT);
                ly += 10;
            }
            return;
        }

        // the icon sits in the middle of a small circle; the name starts below the circle, never across it
        GrimoireUi.circle(graphics, cx, y + 20, 44);
        GrimoireUi.spellIcon(graphics, selected, definition, cx - 16, y + 4, 32);
        y += 49;
        Component name = definition.displayName();
        for (FormattedCharSequence line : this.font.split(name, width)) {
            graphics.centeredText(this.font, line, cx, y, GrimoireUi.INK);
            y += 10;
        }
        Component kind = Component.literal(definition.rarity().displayName().getString() + "  ·  " + definition.school().displayName().getString());
        int kindW = this.font.width(kind) + 10;
        GrimoireUi.sprite(graphics, GrimoireUi.gem(definition.rarity()), cx - kindW / 2, y + 1, 7, 7);
        graphics.text(this.font, kind, cx - kindW / 2 + 10, y, GrimoireUi.rarityInk(definition.rarity()), false);
        y += 13;
        GrimoireUi.divider(graphics, left + 10, y, width - 20);
        y += 6;

        int statsTop = bookY + bookH - 52;
        for (FormattedCharSequence line : this.font.split(definition.description(), width - 4)) {
            if (y > statsTop - 10) {
                break;
            }
            graphics.text(this.font, line, left + 2, y, GrimoireUi.INK_SOFT, false);
            y += 10;
        }

        y = statsTop;
        int col2 = left + width / 2 + 4;
        stat(graphics, "mana", Component.translatable("screen.frieren.spellbook.mana", Math.round(definition.manaCost())), left + 2, y);
        stat(graphics, "cooldown", Component.literal(String.format("%.1fs", definition.cooldown() / 20.0F)), col2, y);
        y += 12;
        if (definition.isCharged()) {
            stat(graphics, "cast", Component.translatable("screen.frieren.spellbook.charged", String.format("%.1fs", definition.chargeTicks() / 20.0F)), left + 2, y);
        } else if (definition.castTime() > 0) {
            stat(graphics, "cast", Component.translatable("screen.frieren.spellbook.incant", String.format("%.1fs", definition.castTime() / 20.0F)), left + 2, y);
        } else if (definition.isChannelled()) {
            stat(graphics, "cast", Component.translatable("screen.frieren.spellbook.channelled"), left + 2, y);
        } else {
            stat(graphics, "cast", Component.translatable("screen.frieren.spellbook.instant"), left + 2, y);
        }
        stat(graphics, "mastery", Component.translatable("screen.frieren.spellbook.casts", progress.casts()), col2, y);
        y += 13;
        Component mastery = Component.translatable("screen.frieren.spellbook.mastery_level", progress.mastery());
        graphics.text(this.font, mastery, left + 2, y, GrimoireUi.INK_GOLD, false);
        int barX = left + 6 + this.font.width(mastery);
        // What the practice is worth: mastery's multiplier on damage, healing and duration.
        String power = String.format("\u00D7%.2f", SpellCaster.masteryPower(progress.masteryFraction()));
        int powerX = left + width - 2 - this.font.width(power);
        graphics.text(this.font, power, powerX, y, GrimoireUi.INK_GOLD, false);
        if (GrimoireUi.inside(mouseX, mouseY, barX, y - 1, left + width - 2 - barX, 10)) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("screen.frieren.spellbook.power_tooltip", power,
                String.format("\u00D7%.2f", SpellCaster.masteryPower(1.0F))), mouseX, mouseY);
        }
        int barW = powerX - 4 - barX;
        graphics.fill(barX, y + 2, barX + barW, y + 7, 0xFF745A3A);
        graphics.fill(barX + 1, y + 3, barX + barW - 1, y + 6, 0xFFE9DDBA);
        graphics.fill(barX + 1, y + 3, barX + 1 + Math.round((barW - 2) * progress.masteryFraction()), y + 6, 0xFFD9A030);
    }

    private void stat(GuiGraphicsExtractor graphics, String icon, Component text, int x, int y) {
        GrimoireUi.sprite(graphics, GrimoireUi.stat(icon), x, y, 9, 9);
        graphics.text(this.font, text, x + 12, y + 1, GrimoireUi.INK, false);
    }

    private int slotsLeft() {
        return (this.width - (MagicData.QUICK_SLOTS * 26 - 4)) / 2;
    }

    private int slotsTop() {
        return bookY + bookH + 5;
    }

    private void drawSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MagicData data = ClientMagicCache.data();
        int top = slotsTop();
        for (int i = 0; i < MagicData.QUICK_SLOTS; i++) {
            int x = slotsLeft() + i * 26;
            boolean hovered = GrimoireUi.inside(mouseX, mouseY, x, top, 22, 22);
            if (hovered) {
                GrimoireUi.sprite(graphics, GrimoireUi.SLOT_SELECTED, x - 1, top - 1, 24, 24);
            } else {
                GrimoireUi.sprite(graphics, GrimoireUi.SLOT, x, top, 22, 22);
            }
            Identifier bound = data.slot(i);
            if (bound != null) {
                GrimoireUi.spellIcon(graphics, bound, SpellManager.client(bound), x + 3, top + 3, 16);
            }
            graphics.text(this.font, String.valueOf(i + 1), x + 2, top + 1, 0xFFE8E0C8, true);
            if (hovered) {
                SpellDefinition definition = bound != null ? SpellManager.client(bound) : null;
                Component tip = definition != null ? definition.displayName()
                    : Component.translatable(selected != null ? "screen.frieren.spellbook.bind_hint" : "screen.frieren.spellbook.empty");
                graphics.setTooltipForNextFrame(this.font, tip, mouseX, mouseY);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        for (int i = 0; i < TABS.length; i++) {
            if (GrimoireUi.inside(mx, my, tabX(i), bookY - 14, 10, 16)) {
                if (filter != TABS[i]) {
                    filter = TABS[i];
                    page = 0;
                    refreshList();
                    playPage();
                }
                return true;
            }
        }
        int left = bookX + 14;
        int width = bookW / 2 - 22;
        int top = listTop();
        for (int i = 0; i < rowsPerPage; i++) {
            int index = page * rowsPerPage + i;
            if (index < spells.size() && GrimoireUi.inside(mx, my, left, top + i * ROW_HEIGHT, width, ROW_HEIGHT)) {
                selected = spells.get(index);
                playClick();
                return true;
            }
        }
        int footer = bookY + bookH - 21;
        if (GrimoireUi.inside(mx, my, left + 4, footer, 16, 12) && page > 0) {
            page--;
            playPage();
            return true;
        }
        if (GrimoireUi.inside(mx, my, left + width - 20, footer, 16, 12) && page < pages() - 1) {
            page++;
            playPage();
            return true;
        }
        for (int slot = 0; slot < MagicData.QUICK_SLOTS; slot++) {
            if (GrimoireUi.inside(mx, my, slotsLeft() + slot * 26, slotsTop(), 22, 22)) {
                bind(slot, event.button() == 1 ? null : selected);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int next = Math.max(0, Math.min(pages() - 1, page - (int) Math.signum(scrollY)));
        if (next != page) {
            page = next;
            playPage();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void bind(int slot, @Nullable Identifier spell) {
        ClientMagicCache.data().setSlot(slot, spell);
        FrierenNetwork.sendToServer(new BindSpellPacket(slot, spell != null ? spell.toString() : ""));
        playClick();
    }

    private static void playPage() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.7F, 1.0F);
        }
    }

    private static void playClick() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.35F, 1.6F);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
