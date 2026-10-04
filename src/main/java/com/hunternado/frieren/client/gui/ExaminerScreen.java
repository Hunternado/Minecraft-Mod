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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Association examiner dialog: requirements, starting/abandoning exams, submitting items, Serie's privilege. */
public class ExaminerScreen extends Screen {
    private final int entityId;

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
        int center = this.width / 2;
        int y = this.height / 2 + 10;
        MageRank next = data.rank().next();
        ExamType type = next != null ? ExamType.forRank(next) : null;

        if (data.exam() == null) {
            Button begin = Button.builder(Component.translatable("screen.frieren.examiner.begin"), b -> send(NpcActionPacket.EXAM_START, 0))
                .pos(center - 100, y).size(200, 20).build();
            begin.active = type != null && type.unmetRequirements(data, false).isEmpty();
            this.addRenderableWidget(begin);
        } else {
            this.addRenderableWidget(Button.builder(Component.translatable("screen.frieren.examiner.submit"), b -> send(NpcActionPacket.SUBMIT_EXAM_ITEM, 0))
                .pos(center - 100, y).size(200, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("screen.frieren.examiner.abandon"), b -> send(NpcActionPacket.EXAM_ABANDON, 0))
                .pos(center - 100, y + 24).size(200, 20).build());
        }

        if (data.seriePrivilegeAvailable()) {
            List<SpellDefinition> choices = NpcInteractions.privilegeChoices(SpellManager.clientDefinitions());
            int py = y + 52;
            for (int i = 0; i < choices.size() && i < 6; i++) {
                final int index = i;
                this.addRenderableWidget(Button.builder(Component.translatable("screen.frieren.examiner.privilege", choices.get(i).displayName()),
                    b -> send(NpcActionPacket.CLAIM_PRIVILEGE, index)).pos(center - 100, py + i * 22).size(200, 20).build());
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        MagicData data = ClientMagicCache.data();
        int center = this.width / 2;
        int y = this.height / 2 - 80;
        graphics.centeredText(this.font, this.title, center, y, 0xFFFFFFFF);
        y += 16;
        graphics.centeredText(this.font, Component.translatable("screen.frieren.journey.rank", data.rank().displayName()), center, y, 0xFFFFFFFF);
        y += 14;
        MageRank next = data.rank().next();
        ExamType type = next != null ? ExamType.forRank(next) : null;
        if (data.exam() != null) {
            ExamType active = ExamType.forRank(data.exam().target());
            if (active != null) {
                graphics.centeredText(this.font, active.stage(data.exam().stage()).title(), center, y, 0xFFE8C15A);
                y += 12;
                graphics.centeredText(this.font, active.stage(data.exam().stage()).instructions(), center, y, 0xFFAAAAAA);
            }
            return;
        }
        if (type == null) {
            graphics.centeredText(this.font, Component.translatable("exam.frieren.max_rank"), center, y, 0xFFE8C15A);
            return;
        }
        graphics.centeredText(this.font, Component.translatable("screen.frieren.journey.next_exam", next.displayName()), center, y, 0xFFFFFFFF);
        y += 12;
        List<Component> unmet = type.unmetRequirements(data, false);
        if (unmet.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("screen.frieren.journey.eligible"), center, y, 0xFF7FE07F);
        }
        for (Component line : unmet) {
            graphics.centeredText(this.font, line, center, y, 0xFFE07F7F);
            y += 11;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
