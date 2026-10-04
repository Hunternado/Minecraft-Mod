package com.hunternado.frieren;

import com.hunternado.frieren.combat.CombatEvents;
import com.hunternado.frieren.config.FrierenClientConfig;
import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.data.MagicDataEvents;
import com.hunternado.frieren.entity.npc.NpcInteractions;
import com.hunternado.frieren.event.ServerEvents;
import com.hunternado.frieren.exam.ExamEvents;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.quest.QuestEvents;
import com.hunternado.frieren.registry.ModBlocks;
import com.hunternado.frieren.registry.ModCreativeTabs;
import com.hunternado.frieren.registry.ModDataComponents;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.registry.ModTriggers;
import com.hunternado.frieren.spell.SpellBehaviors;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point for Frieren: Mage's Journey.
 * <p>
 * Only wiring happens here: registries, configs, networking and event listeners. Gameplay lives in the
 * dedicated packages. Client-only setup is in {@code client.ClientSetup}, which Forge only loads on the
 * physical client through its {@code @Mod.EventBusSubscriber(value = Dist.CLIENT)} annotation.
 */
@Mod(FrierenMod.MODID)
public final class FrierenMod {
    public static final String MODID = "frieren";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FrierenMod(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        ModBlocks.BLOCKS.register(modBusGroup);
        ModItems.ITEMS.register(modBusGroup);
        ModEntities.ENTITY_TYPES.register(modBusGroup);
        ModSounds.SOUND_EVENTS.register(modBusGroup);
        ModEffects.MOB_EFFECTS.register(modBusGroup);
        ModDataComponents.DATA_COMPONENTS.register(modBusGroup);
        ModCreativeTabs.TABS.register(modBusGroup);
        ModTriggers.TRIGGERS.register(modBusGroup);

        context.registerConfig(ModConfig.Type.SERVER, FrierenConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, FrierenClientConfig.SPEC);

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(FrierenMod::commonSetup);
        EntityAttributeCreationEvent.BUS.addListener(ModEntities::registerAttributes);
        SpawnPlacementRegisterEvent.BUS.addListener(ModEntities::registerSpawnPlacements);

        SpellBehaviors.bootstrap();
        MagicDataEvents.register();
        CombatEvents.register();
        ServerEvents.register();
        ExamEvents.register();
        QuestEvents.register();
        NpcInteractions.register();
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        FrierenNetwork.register();
        LOGGER.info("Frieren: Mage's Journey initialised with {} spell behaviors", SpellBehaviors.count());
    }
}
