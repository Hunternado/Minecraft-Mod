package com.hunternado.frieren.client;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.client.gui.ManaHudLayer;
import com.hunternado.frieren.client.render.FrierenHumanoidModel;
import com.hunternado.frieren.client.render.FrierenHumanoidRenderer;
import com.hunternado.frieren.client.render.InvisibleRenderer;
import com.hunternado.frieren.client.render.MimicModel;
import com.hunternado.frieren.client.render.MimicRenderer;
import com.hunternado.frieren.client.render.ModModelLayers;
import com.hunternado.frieren.client.render.StilleModel;
import com.hunternado.frieren.client.render.StilleRenderer;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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
        event.registerLayerDefinition(ModModelLayers.HUMANOID, FrierenHumanoidModel::createLayer);
        event.registerLayerDefinition(ModModelLayers.MIMIC, MimicModel::createLayer);
        event.registerLayerDefinition(ModModelLayers.STILLE, StilleModel::createLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        humanoid(event, ModEntities.DEMON_SOLDIER.get(), "demon_soldier", 0.5F);
        humanoid(event, ModEntities.DEMON_MAGE.get(), "demon_mage", 0.5F);
        humanoid(event, ModEntities.AURA_THRALL.get(), "aura_thrall", 0.55F);
        humanoid(event, ModEntities.AURA.get(), "aura", 0.6F);
        humanoid(event, ModEntities.QUAL.get(), "qual", 0.6F);
        humanoid(event, ModEntities.SPIEGEL.get(), "spiegel", 0.7F);
        humanoid(event, ModEntities.MIRROR_REPLICA.get(), "mirror_replica", 0.5F);
        humanoid(event, ModEntities.WANDERING_MAGE.get(), "wandering_mage", 0.5F);
        humanoid(event, ModEntities.EXAMINER.get(), "examiner", 0.5F);
        humanoid(event, ModEntities.PRIEST.get(), "priest", 0.5F);
        humanoid(event, ModEntities.FRIEREN.get(), "frieren", 0.45F);
        humanoid(event, ModEntities.FERN.get(), "fern", 0.5F);
        humanoid(event, ModEntities.STARK.get(), "stark", 0.5F);
        humanoid(event, ModEntities.SERIE.get(), "serie", 0.45F);
        humanoid(event, ModEntities.MANA_DECOY.get(), "mana_decoy", 0.0F);
        event.registerEntityRenderer(ModEntities.MIMIC.get(), MimicRenderer::new);
        event.registerEntityRenderer(ModEntities.STILLE.get(), StilleRenderer::new);
        event.registerEntityRenderer(ModEntities.MANA_BOLT.get(), InvisibleRenderer::new);
        event.registerEntityRenderer(ModEntities.SPELL_AREA.get(), InvisibleRenderer::new);
    }

    private static <T extends LivingEntity> void humanoid(EntityRenderersEvent.RegisterRenderers event, EntityType<T> type, String texture, float shadow) {
        event.registerEntityRenderer(type, context -> new FrierenHumanoidRenderer<T>(context, FrierenIds.id("textures/entity/" + texture + ".png"), shadow));
    }

    @SubscribeEvent
    public static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(FrierenIds.id("mana_hud"), new ManaHudLayer());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        KeyBindings.tick();
        ClientPacketHandlers.tickSenseMarks();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMagicCache.reset();
        SpellManager.setClientDefinitions(List.of());
    }
}
