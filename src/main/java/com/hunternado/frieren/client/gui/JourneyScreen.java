package com.hunternado.frieren.client.gui;

import com.hunternado.frieren.client.ClientMagicCache;
import com.hunternado.frieren.data.ExamProgress;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.exam.ExamType;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.mana.ManaSensing;
import com.hunternado.frieren.quest.Quest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * The Journey Journal, kept like a travel diary: the left page holds your rank, mana and senses and the next exam;
 * the right page is the quest log, a page at a time.
 */
public class JourneyScreen extends Screen {
    private int bookX;
    private int bookY;
    private int bookW;
    private int bookH;
    private int questPage;
    private final List<Integer> pageStarts = new ArrayList<>();

    public JourneyScreen() {
        super(Component.translatable("screen.frieren.journey"));
    }

    @Override
    protected void init() {
        bookW = Math.min(this.width - 16, 372);
        bookH = Math.min(this.height - 20, 214);
        bookX = (this.width - bookW) / 2;
        bookY = (this.height - bookH) / 2;
        paginateQuests();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        GrimoireUi.book(graphics, bookX, bookY, bookW, bookH);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawJourney(graphics);
        drawQuests(graphics, mouseX, mouseY);
    }

    // ------------------------------------------------------------------------------------------
    // Left page: the mage
    // ------------------------------------------------------------------------------------------
    private void drawJourney(GuiGraphicsExtractor graphics) {
        MagicData data = ClientMagicCache.data();
        int left = bookX + 16;
        int width = bookW / 2 - 26;
        int cx = left + width / 2;
        int y = bookY + 13;
        GrimoireUi.centered(graphics, this.font, this.title, cx, y, GrimoireUi.INK);
        y += 12;
        GrimoireUi.divider(graphics, left + 8, y, width - 16);
        y += 6;
        GrimoireUi.centered(graphics, this.font, GrimoireUi.plain(data.rank().displayName()), cx, y, GrimoireUi.INK_GOLD);
        y += 14;

        float needed = ManaHelper.xpForNextGrowth(data.manaGrowth());
        y = stat(graphics, "mana", Component.translatable("screen.frieren.journey.mana", Math.round(data.mana()), data.maxMana()), left, y);
        y = stat(graphics, "mastery", Component.translatable("screen.frieren.journey.growth", data.manaGrowth(), data.rank().growthCap(),
            data.bonusGrowth()), left, y);
        y = stat(graphics, "cooldown", Component.translatable("screen.frieren.journey.training", Math.round(data.trainingXp()), Math.round(needed)),
            left, y);
        y = stat(graphics, "cast", Component.translatable("screen.frieren.journey.conceal", String.format("%.1f", data.concealMastery()),
            ManaSensing.playerConcealLevel(withConcealing(data))), left, y);
        y = stat(graphics, "cast", Component.translatable("screen.frieren.journey.detect", String.format("%.1f", data.detectionMastery()),
            ManaHelper.detectionPower(data)), left, y);
        y = stat(graphics, "mastery", Component.translatable("screen.frieren.journey.spells", data.knownCount()), left, y);
        y += 4;
        GrimoireUi.divider(graphics, left + 8, y, width - 16);
        y += 7;

        int bottom = bookY + bookH - 12;
        ExamProgress exam = data.exam();
        ExamType activeType = exam != null ? ExamType.forRank(exam.target()) : null;
        if (exam != null && activeType != null) {
            long secondsLeft = Math.max(0L, (exam.deadline() - ClientMagicCache.gameTime()) / 20L);
            y = wrap(graphics, Component.translatable("screen.frieren.journey.exam_active", exam.target().displayName(),
                activeType.stage(exam.stage()).title(), String.format("%d:%02d", secondsLeft / 60L, secondsLeft % 60L)), left, y, width, GrimoireUi.INK_GOLD, bottom);
            wrap(graphics, activeType.stage(exam.stage()).instructions(), left, y + 2, width, GrimoireUi.INK_SOFT, bottom);
            return;
        }
        MageRank next = data.rank().next();
        ExamType nextType = next != null ? ExamType.forRank(next) : null;
        if (nextType == null) {
            wrap(graphics, Component.translatable("exam.frieren.max_rank"), left, y, width, GrimoireUi.INK_GOLD, bottom);
            return;
        }
        y = wrap(graphics, Component.translatable("screen.frieren.journey.next_exam", next.displayName()), left, y, width, GrimoireUi.INK, bottom);
        List<Component> unmet = nextType.unmetRequirements(data, false);
        if (unmet.isEmpty()) {
            wrap(graphics, Component.translatable("screen.frieren.journey.eligible"), left + 6, y, width - 6, GrimoireUi.INK_GREEN, bottom);
        }
        for (Component line : unmet) {
            y = wrap(graphics, Component.literal("- " + line.getString()), left + 6, y, width - 6, GrimoireUi.INK_RED, bottom);
        }
    }

    private int stat(GuiGraphicsExtractor graphics, String icon, Component text, int x, int y) {
        GrimoireUi.sprite(graphics, GrimoireUi.stat(icon), x, y, 9, 9);
        graphics.text(this.font, GrimoireUi.plain(text), x + 13, y + 1, GrimoireUi.INK_SOFT, false);
        return y + 12;
    }

    /** Wrapped ink text that stops at the bottom of the page; returns the next line's y. */
    private int wrap(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color, int bottom) {
        for (FormattedCharSequence line : this.font.split(GrimoireUi.plain(text), width)) {
            if (y > bottom - 9) {
                break;
            }
            graphics.text(this.font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    // ------------------------------------------------------------------------------------------
    // Right page: quests
    // ------------------------------------------------------------------------------------------
    private int questsTop() {
        return bookY + 31;
    }

    private int questsBottom() {
        return bookY + bookH - 26;
    }

    private int questWidth() {
        return bookW - bookW / 2 - 26;
    }

    private int questHeight(Quest quest, MagicData data) {
        if (data.questCompleted(quest.id())) {
            return 11;
        }
        return 10 + this.font.split(quest.description(), questWidth() - 10).size() * 10 + 6;
    }

    private void paginateQuests() {
        pageStarts.clear();
        MagicData data = ClientMagicCache.data();
        Quest[] quests = Quest.values();
        int room = questsBottom() - questsTop();
        int used = room;
        for (int i = 0; i < quests.length; i++) {
            int h = questHeight(quests[i], data);
            if (used + h > room) {
                pageStarts.add(i);
                used = 0;
            }
            used += h;
        }
        if (pageStarts.isEmpty()) {
            pageStarts.add(0);
        }
        questPage = Math.min(questPage, pageStarts.size() - 1);
    }

    private void drawQuests(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MagicData data = ClientMagicCache.data();
        int left = bookX + bookW / 2 + 12;
        int width = questWidth();
        int cx = left + width / 2;
        Quest[] quests = Quest.values();
        int done = 0;
        for (Quest quest : quests) {
            if (data.questCompleted(quest.id())) {
                done++;
            }
        }
        GrimoireUi.centered(graphics, this.font, Component.literal(Component.translatable("screen.frieren.journey.quests").getString()
            + "  (" + done + "/" + quests.length + ")"), cx, bookY + 13, GrimoireUi.INK);
        GrimoireUi.divider(graphics, left + 8, bookY + 25, width - 16);

        int start = pageStarts.get(questPage);
        int end = questPage + 1 < pageStarts.size() ? pageStarts.get(questPage + 1) : quests.length;
        int y = questsTop();
        for (int i = start; i < end; i++) {
            Quest quest = quests[i];
            boolean complete = data.questCompleted(quest.id());
            int progress = Math.min(quest.target(), data.stat(quest.stat()));
            graphics.text(this.font, complete ? "✔" : "•", left, y, complete ? GrimoireUi.INK_GREEN : GrimoireUi.INK_GOLD, false);
            String title = this.font.plainSubstrByWidth(quest.title().getString(), width - 10);
            graphics.text(this.font, title, left + 10, y, complete ? GrimoireUi.INK_GREEN : GrimoireUi.INK, false);
            y += 10;
            if (complete) {
                y += 1;
                continue;
            }
            for (FormattedCharSequence line : this.font.split(GrimoireUi.plain(quest.description()), width - 10)) {
                graphics.text(this.font, line, left + 10, y, GrimoireUi.INK_SOFT, false);
                y += 10;
            }
            int barW = width - 40;
            graphics.fill(left + 10, y + 1, left + 10 + barW, y + 4, 0xFF745A3A);
            graphics.fill(left + 11, y + 2, left + 9 + barW, y + 3, 0xFFE9DDBA);
            graphics.fill(left + 11, y + 2, left + 11 + Math.round((barW - 2) * progress / (float) Math.max(1, quest.target())), y + 3, 0xFFD9A030);
            graphics.text(this.font, progress + "/" + quest.target(), left + 14 + barW, y - 2, GrimoireUi.INK_FAINT, false);
            y += 6;
        }

        int footer = bookY + bookH - 21;
        boolean many = pageStarts.size() > 1;
        GrimoireUi.button(graphics, this.font, Component.literal("<"), left + 4, footer, 16, 12, mouseX, mouseY, many && questPage > 0);
        GrimoireUi.button(graphics, this.font, Component.literal(">"), left + width - 20, footer, 16, 12, mouseX, mouseY,
            many && questPage < pageStarts.size() - 1);
        GrimoireUi.centered(graphics, this.font, Component.literal((questPage + 1) + " / " + pageStarts.size()), cx, footer + 2, GrimoireUi.INK_SOFT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int left = bookX + bookW / 2 + 12;
        int footer = bookY + bookH - 21;
        if (GrimoireUi.inside(event.x(), event.y(), left + 4, footer, 16, 12) && questPage > 0) {
            questPage--;
            turnPage();
            return true;
        }
        if (GrimoireUi.inside(event.x(), event.y(), left + questWidth() - 20, footer, 16, 12) && questPage < pageStarts.size() - 1) {
            questPage++;
            turnPage();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int next = Math.max(0, Math.min(pageStarts.size() - 1, questPage - (int) Math.signum(scrollY)));
        if (next != questPage) {
            questPage = next;
            turnPage();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static void turnPage() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.7F, 1.0F);
        }
    }

    /** Concealment level as it would be while concealing (for display). */
    private static MagicData withConcealing(MagicData data) {
        if (data.concealing()) {
            return data;
        }
        MagicData copy = new MagicData();
        copy.load(data.save());
        copy.setConcealing(true);
        return copy;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
