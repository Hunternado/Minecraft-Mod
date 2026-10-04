package com.hunternado.frieren.client.gui;

import com.hunternado.frieren.client.ClientMagicCache;
import com.hunternado.frieren.data.ExamProgress;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.exam.ExamType;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.mana.ManaSensing;
import com.hunternado.frieren.quest.Quest;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Journey Journal: rank, mana progression, concealment, the current exam and the quest log. */
public class JourneyScreen extends Screen {
    public JourneyScreen() {
        super(Component.translatable("screen.frieren.journey"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        MagicData data = ClientMagicCache.data();
        graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        int left = this.width / 2 - 200;
        int y = 32;
        graphics.text(this.font, Component.translatable("screen.frieren.journey.rank", data.rank().displayName()), left, y, 0xFFFFFFFF, true);
        y += 14;
        graphics.text(this.font, Component.translatable("screen.frieren.journey.mana", Math.round(data.mana()), data.maxMana()), left, y, 0xFF9FD8FF, false);
        y += 11;
        int cap = data.rank().growthCap();
        graphics.text(this.font, Component.translatable("screen.frieren.journey.growth", data.manaGrowth(), cap, data.bonusGrowth()), left, y, 0xFF9FD8FF, false);
        y += 11;
        float needed = ManaHelper.xpForNextGrowth(data.manaGrowth());
        graphics.text(this.font, Component.translatable("screen.frieren.journey.training", Math.round(data.trainingXp()), Math.round(needed)), left, y, 0xFF8FB0D0, false);
        y += 11;
        graphics.text(this.font, Component.translatable("screen.frieren.journey.conceal", String.format("%.1f", data.concealMastery()),
            ManaSensing.playerConcealLevel(withConcealing(data))), left, y, 0xFFB7A8FF, false);
        y += 11;
        graphics.text(this.font, Component.translatable("screen.frieren.journey.detect", String.format("%.1f", data.detectionMastery()),
            ManaHelper.detectionPower(data)), left, y, 0xFFB8F0A0, false);
        y += 11;
        graphics.text(this.font, Component.translatable("screen.frieren.journey.spells", data.knownCount()), left, y, 0xFFE8C15A, false);
        y += 18;

        // ---- Exam ----
        ExamProgress exam = data.exam();
        ExamType activeType = exam != null ? ExamType.forRank(exam.target()) : null;
        if (exam != null && activeType != null) {
            long secondsLeft = Math.max(0L, (exam.deadline() - ClientMagicCache.gameTime()) / 20L);
            graphics.text(this.font, Component.translatable("screen.frieren.journey.exam_active", exam.target().displayName(),
                activeType.stage(exam.stage()).title(), String.format("%d:%02d", secondsLeft / 60L, secondsLeft % 60L)).withStyle(ChatFormatting.GOLD), left, y, 0xFFFFFFFF, false);
            y += 11;
            graphics.text(this.font, activeType.stage(exam.stage()).instructions(), left, y, 0xFFAAAAAA, false);
            y += 16;
        } else {
            MageRank next = data.rank().next();
            ExamType nextType = next != null ? ExamType.forRank(next) : null;
            if (nextType == null) {
                graphics.text(this.font, Component.translatable("exam.frieren.max_rank"), left, y, 0xFFE8C15A, false);
                y += 16;
            } else {
                graphics.text(this.font, Component.translatable("screen.frieren.journey.next_exam", next.displayName()), left, y, 0xFFFFFFFF, false);
                y += 11;
                List<Component> unmet = nextType.unmetRequirements(data, false);
                if (unmet.isEmpty()) {
                    graphics.text(this.font, Component.translatable("screen.frieren.journey.eligible"), left + 8, y, 0xFF7FE07F, false);
                    y += 11;
                }
                for (Component line : unmet) {
                    graphics.text(this.font, Component.literal("- ").append(line), left + 8, y, 0xFFE07F7F, false);
                    y += 11;
                }
                y += 6;
            }
        }

        // ---- Quests (right column) ----
        int questLeft = this.width / 2 + 10;
        int qy = 32;
        graphics.text(this.font, Component.translatable("screen.frieren.journey.quests"), questLeft, qy, 0xFFFFFFFF, true);
        qy += 14;
        for (Quest quest : Quest.values()) {
            boolean done = data.questCompleted(quest.id());
            int progress = Math.min(quest.target(), data.stat(quest.stat()));
            int color = done ? 0xFF7FE07F : 0xFFFFFFFF;
            graphics.text(this.font, Component.literal(done ? "✔ " : "• ").append(quest.title()), questLeft, qy, color, false);
            qy += 10;
            graphics.text(this.font, done ? quest.rewardText() : quest.description().copy().append(" (" + progress + "/" + quest.target() + ")"),
                questLeft + 8, qy, 0xFF999999, false);
            qy += 13;
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
