package com.hunternado.frieren.client.gui;

import com.hunternado.frieren.client.ClientMagicCache;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.data.SpellProgress;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.BindSpellPacket;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists known spells with their stats and mastery, and binds them to the five quick slots.
 * Click a spell, then a slot button. Clicking a slot with no spell selected clears it.
 */
public class SpellbookScreen extends Screen {
    private static final int PAGE_SIZE = 10;

    private final List<Identifier> spells = new ArrayList<>();
    private @Nullable Identifier selected;
    private int page;

    public SpellbookScreen() {
        super(Component.translatable("screen.frieren.spellbook"));
    }

    @Override
    protected void init() {
        spells.clear();
        spells.addAll(ClientMagicCache.data().knownSpells().keySet());
        int pages = Math.max(1, (spells.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pages - 1);

        int listLeft = this.width / 2 - 200;
        int top = 40;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            if (index >= spells.size()) {
                break;
            }
            Identifier id = spells.get(index);
            SpellDefinition definition = SpellManager.client(id);
            Component label = definition != null ? definition.displayName() : Component.literal(id.toString());
            this.addRenderableWidget(Button.builder(label, button -> {
                selected = id;
                this.rebuildWidgets();
            }).pos(listLeft, top + i * 22).size(150, 20).build());
        }
        if (pages > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                page = Math.max(0, page - 1);
                this.rebuildWidgets();
            }).pos(listLeft, top + PAGE_SIZE * 22 + 2).size(20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                page = Math.min(pages - 1, page + 1);
                this.rebuildWidgets();
            }).pos(listLeft + 130, top + PAGE_SIZE * 22 + 2).size(20, 20).build());
        }

        int slotTop = this.height - 30;
        int slotLeft = this.width / 2 - (MagicData.QUICK_SLOTS * 82) / 2;
        MagicData data = ClientMagicCache.data();
        for (int slot = 0; slot < MagicData.QUICK_SLOTS; slot++) {
            final int index = slot;
            Identifier bound = data.slot(slot);
            SpellDefinition boundDefinition = bound != null ? SpellManager.client(bound) : null;
            Component label = Component.literal((slot + 1) + ": ").append(boundDefinition != null ? boundDefinition.displayName()
                : Component.translatable("screen.frieren.spellbook.empty"));
            this.addRenderableWidget(Button.builder(label, button -> bind(index)).pos(slotLeft + slot * 82, slotTop).size(80, 20).build());
        }
    }

    private void bind(int slot) {
        MagicData data = ClientMagicCache.data();
        String spell = selected != null ? selected.toString() : "";
        data.setSlot(slot, selected);
        FrierenNetwork.sendToServer(new BindSpellPacket(slot, spell));
        this.rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
        MagicData data = ClientMagicCache.data();
        int detailsLeft = this.width / 2 - 30;
        int y = 42;
        if (spells.isEmpty()) {
            graphics.text(this.font, Component.translatable("screen.frieren.spellbook.none"), detailsLeft, y, 0xFFAAAAAA, false);
            return;
        }
        if (selected == null) {
            graphics.text(this.font, Component.translatable("screen.frieren.spellbook.hint"), detailsLeft, y, 0xFFAAAAAA, false);
            return;
        }
        SpellDefinition definition = SpellManager.client(selected);
        SpellProgress progress = data.progress(selected);
        if (definition == null || progress == null) {
            return;
        }
        graphics.text(this.font, definition.displayName(), detailsLeft, y, 0xFFFFFFFF, true);
        y += 12;
        graphics.text(this.font, definition.rarity().displayName().copy().append("  ").append(definition.school().displayName()), detailsLeft, y, 0xFFFFFFFF, false);
        y += 14;
        for (FormattedCharSequence line : this.font.split(definition.description(), 220)) {
            graphics.text(this.font, line, detailsLeft, y, 0xFFCCCCCC, false);
            y += 10;
        }
        y += 6;
        graphics.text(this.font, Component.translatable("screen.frieren.spellbook.cost", Math.round(definition.manaCost()),
            String.format("%.1f", definition.cooldown() / 20.0F)), detailsLeft, y, 0xFF9FD8FF, false);
        y += 12;
        if (definition.castTime() > 0) {
            graphics.text(this.font, Component.translatable("screen.frieren.spellbook.cast_time", String.format("%.1f", definition.castTime() / 20.0F)),
                detailsLeft, y, 0xFF9FD8FF, false);
            y += 12;
        }
        graphics.text(this.font, Component.translatable("screen.frieren.spellbook.mastery", progress.mastery(), progress.casts()),
            detailsLeft, y, 0xFFE8C15A, false);
        y += 12;
        int barWidth = 150;
        graphics.fill(detailsLeft, y, detailsLeft + barWidth, y + 4, 0xFF2A2A3A);
        graphics.fill(detailsLeft, y, detailsLeft + Math.round(barWidth * progress.masteryFraction()), y + 4, 0xFFE8C15A);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
