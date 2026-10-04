package com.hunternado.frieren.client;

import com.hunternado.frieren.client.gui.JourneyScreen;
import net.minecraft.client.Minecraft;

/** Client-only entry points that common code may call from client-side branches. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openJourney() {
        Minecraft.getInstance().gui.setScreen(new JourneyScreen());
    }
}
