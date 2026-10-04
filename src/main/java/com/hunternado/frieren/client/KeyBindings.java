package com.hunternado.frieren.client;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.client.gui.JourneyScreen;
import com.hunternado.frieren.client.gui.SpellbookScreen;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.CastSpellPacket;
import com.hunternado.frieren.network.packet.DetectManaPacket;
import com.hunternado.frieren.network.packet.ReleaseChannelPacket;
import com.hunternado.frieren.network.packet.SelectSlotPacket;
import com.hunternado.frieren.network.packet.ToggleConcealmentPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Magic key mappings. All keys are rebindable in Controls; gameplay never reads raw key codes. */
public final class KeyBindings {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(FrierenIds.id("magic"));

    public static final KeyMapping CAST = key("cast", GLFW.GLFW_KEY_R);
    public static final KeyMapping CYCLE = key("cycle_spell", GLFW.GLFW_KEY_V);
    public static final KeyMapping SPELLBOOK = key("spellbook", GLFW.GLFW_KEY_B);
    public static final KeyMapping CONCEAL = key("conceal", GLFW.GLFW_KEY_G);
    public static final KeyMapping DETECT = key("detect", GLFW.GLFW_KEY_H);
    public static final KeyMapping JOURNAL = key("journal", GLFW.GLFW_KEY_J);

    private KeyBindings() {}

    private static KeyMapping key(String name, int defaultKey) {
        KeyMapping mapping = new KeyMapping("key.frieren." + name, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
        mapping.setKeyConflictContext(KeyConflictContext.IN_GAME);
        return mapping;
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(CAST);
        event.register(CYCLE);
        event.register(SPELLBOOK);
        event.register(CONCEAL);
        event.register(DETECT);
        event.register(JOURNAL);
    }

    /** Called every client tick. Sends intents only; the server decides everything. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) {
            if (ClientMagicCache.castKeyHeld()) {
                ClientMagicCache.setCastKeyHeld(false);
                FrierenNetwork.sendToServer(ReleaseChannelPacket.INSTANCE);
            }
            return;
        }
        MagicData data = ClientMagicCache.data();

        boolean castDown = CAST.isDown();
        if (castDown && !ClientMagicCache.castKeyHeld()) {
            FrierenNetwork.sendToServer(new CastSpellPacket(data.selectedSlot()));
        } else if (!castDown && ClientMagicCache.castKeyHeld()) {
            FrierenNetwork.sendToServer(ReleaseChannelPacket.INSTANCE);
        }
        ClientMagicCache.setCastKeyHeld(castDown);
        while (CAST.consumeClick()) {
            // Edge handled above; drain queued clicks so they don't fire later.
        }

        while (CYCLE.consumeClick()) {
            int next = (data.selectedSlot() + 1) % MagicData.QUICK_SLOTS;
            for (int i = 0; i < MagicData.QUICK_SLOTS && data.slot(next) == null; i++) {
                next = (next + 1) % MagicData.QUICK_SLOTS;
            }
            data.setSelectedSlot(next);
            FrierenNetwork.sendToServer(new SelectSlotPacket(next));
        }
        while (SPELLBOOK.consumeClick()) {
            minecraft.gui.setScreen(new SpellbookScreen());
        }
        while (JOURNAL.consumeClick()) {
            minecraft.gui.setScreen(new JourneyScreen());
        }
        while (CONCEAL.consumeClick()) {
            FrierenNetwork.sendToServer(ToggleConcealmentPacket.INSTANCE);
        }
        while (DETECT.consumeClick()) {
            FrierenNetwork.sendToServer(DetectManaPacket.INSTANCE);
        }
    }
}
