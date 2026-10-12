package com.hunternado.frieren.client;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.client.dev.ShowcaseDirector;
import com.hunternado.frieren.client.fx.MagicFx;
import com.hunternado.frieren.client.gui.ManaHudLayer;
import com.hunternado.frieren.client.render.MimicModel;
import com.hunternado.frieren.client.render.ModModelLayers;
import com.hunternado.frieren.client.render.ModRenderers;
import com.hunternado.frieren.client.render.StilleModel;
import com.hunternado.frieren.client.render.character.CharacterKind;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client-only wiring. Forge never loads this class on a dedicated server because of {@code value = Dist.CLIENT}.
 */
@Mod.EventBusSubscriber(modid = FrierenMod.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        KeyBindings.register(event);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        CharacterKind.registerLayers(event);
        event.registerLayerDefinition(ModModelLayers.MIMIC, MimicModel::createLayer);
        event.registerLayerDefinition(ModModelLayers.STILLE, StilleModel::createLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModRenderers.register(event);
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        MagicFx.registerProviders(event);
    }

    /** Added inside vanilla's pre-sleep HUD stack, so it hides with F1 like the hotbar does. */
    @SubscribeEvent
    public static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK, FrierenIds.id("mana_hud"), new ManaHudLayer());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        KeyBindings.tick();
        ClientPacketHandlers.tickSenseMarks();
        ShowcaseDirector.tick();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMagicCache.reset();
        SpellManager.setClientDefinitions(List.of());
    }
}
