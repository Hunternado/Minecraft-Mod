package com.hunternado.frieren.client.gui;

import com.hunternado.frieren.client.ClientMagicCache;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.npc.NpcInteractions;
import com.hunternado.frieren.exam.ExamType;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.NpcActionPacket;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Association examiner dialog on an official parchment: rank, the next exam and what it still needs, starting or
 * abandoning exams, submitting items, and Serie's privilege when it is earned.
 */
public class ExaminerScreen extends Screen {
    private static final int SHEET_W = 260;

    private final int entityId;
    private int sheetX;
    private int sheetY;
    private int sheetH;

    public ExaminerScreen(int entityId) {
        super(Component.translatable("screen.frieren.examiner"));
        this.entityId = entityId;
    }

    private void send(int action, int argument) {
        FrierenNetwork.sendToServer(new NpcActionPacket(entityId, action, argument));
        this.onClose();
    }

    @Override
    protected void init() {
        MagicData data = ClientMagicCache.data();
        MageRank next = data.rank().next();
        ExamType type = next != null ? ExamType.forRank(next) : null;
        List<SpellDefinition> choices = data.seriePrivilegeAvailable()
            ? NpcInteractions.privilegeChoices(SpellManager.clientDefinitions()) : List.of();
        int buttons = (data.exam() == null ? 1 : 2) + Math.min(6, choices.size());
        sheetH = Math.min(this.height - 16, 112 + buttons * 18);
        sheetX = (this.width - SHEET_W) / 2;
        sheetY = (this.height - sheetH) / 2;

        int bx = sheetX + 30;
        int bw = SHEET_W - 60;
        int y = sheetY + sheetH - 14 - buttons * 18;
        if (data.exam() == null) {
            InkButton begin = new InkButton(bx, y, bw, 15, Component.translatable("screen.frieren.examiner.begin"),
                () -> send(NpcActionPacket.EXAM_START, 0));
            begin.active = type != null && type.unmetRequirements(data, false).isEmpty();
            this.addRenderableWidget(begin);
            y += 18;
        } else {
            this.addRenderableWidget(new InkButton(bx, y, bw, 15, Component.translatable("screen.frieren.examiner.submit"),
                () -> send(NpcActionPacket.SUBMIT_EXAM_ITEM, 0)));
            y += 18;
            this.addRenderableWidget(new InkButton(bx, y, bw, 15, Component.translatable("screen.frieren.examiner.abandon"),
                () -> send(NpcActionPacket.EXAM_ABANDON, 0)));
            y += 18;
        }
        for (int i = 0; i < choices.size() && i < 6; i++) {
            final int index = i;
            this.addRenderableWidget(new InkButton(bx, y, bw, 15,
                GrimoireUi.plain(Component.translatable("screen.frieren.examiner.privilege", choices.get(i).displayName())),
                () -> send(NpcActionPacket.CLAIM_PRIVILEGE, index)));
            y += 18;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        // the sheet belongs to the background so the ink buttons (widgets) land on top of it
        GrimoireUi.sheet(graphics, sheetX, sheetY, SHEET_W, sheetH);
        GrimoireUi.circle(graphics, sheetX + SHEET_W / 2, sheetY + 52, 84);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        MagicData data = ClientMagicCache.data();
        int cx = sheetX + SHEET_W / 2;
        int y = sheetY + 13;
        GrimoireUi.centered(graphics, this.font, this.title, cx, y, GrimoireUi.INK);
        y += 12;
        GrimoireUi.divider(graphics, sheetX + 40, y, SHEET_W - 80);
        y += 7;
        GrimoireUi.centered(graphics, this.font, GrimoireUi.plain(Component.translatable("screen.frieren.journey.rank", data.rank().displayName())),
            cx, y, GrimoireUi.INK_GOLD);
        y += 14;
        int wrap = SHEET_W - 36;
        MageRank next = data.rank().next();
        ExamType type = next != null ? ExamType.forRank(next) : null;
        if (data.exam() != null) {
            ExamType active = ExamType.forRank(data.exam().target());
            if (active != null) {
                GrimoireUi.centered(graphics, this.font, GrimoireUi.plain(active.stage(data.exam().stage()).title()), cx, y, GrimoireUi.INK);
                y += 12;
                lines(graphics, active.stage(data.exam().stage()).instructions(), cx, y, wrap, GrimoireUi.INK_SOFT);
            }
            return;
        }
        if (type == null) {
            GrimoireUi.centered(graphics, this.font, GrimoireUi.plain(Component.translatable("exam.frieren.max_rank")), cx, y, GrimoireUi.INK_GOLD);
            return;
        }
        GrimoireUi.centered(graphics, this.font, GrimoireUi.plain(Component.translatable("screen.frieren.journey.next_exam", next.displayName())),
            cx, y, GrimoireUi.INK);
        y += 12;
        List<Component> unmet = type.unmetRequirements(data, false);
        if (unmet.isEmpty()) {
            GrimoireUi.centered(graphics, this.font, GrimoireUi.plain(Component.translatable("screen.frieren.journey.eligible")), cx, y, GrimoireUi.INK_GREEN);
        }
        for (Component line : unmet) {
            y = lines(graphics, Component.literal("- " + line.getString()), cx, y, wrap, GrimoireUi.INK_RED);
        }
    }

    private int lines(GuiGraphicsExtractor graphics, Component text, int cx, int y, int wrap, int color) {
        for (FormattedCharSequence line : this.font.split(GrimoireUi.plain(text), wrap)) {
            graphics.centeredText(this.font, line, cx, y, color);
            y += 10;
        }
        return y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
