package com.hunternado.frieren.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** A button stamped in ink on parchment, for the grimoire-styled screens (vanilla's stone buttons clash with paper). */
public class InkButton extends AbstractButton {
    private final Runnable action;

    public InkButton(int x, int y, int width, int height, Component label, Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GrimoireUi.button(graphics, Minecraft.getInstance().font, getMessage(), getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, active);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
