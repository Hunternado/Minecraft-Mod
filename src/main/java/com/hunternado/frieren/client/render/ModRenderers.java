package com.hunternado.frieren.client.render;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.EntityRenderersEvent;

/**
 * Entity renderer registration. Kept out of {@code ClientSetup}: EventBus 7 treats every static method of an
 * {@code @EventBusSubscriber} class that takes an event as a listener, so helpers with an event parameter
 * cannot live there.
 */
public final class ModRenderers {
    private ModRenderers() {}

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
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

    private static <T extends LivingEntity> void humanoid(EntityRenderersEvent.RegisterRenderers event, EntityType<T> type,
                                                          String texture, float shadow) {
        event.registerEntityRenderer(type, context -> new FrierenHumanoidRenderer<T>(context,
            FrierenIds.id("textures/entity/" + texture + ".png"), shadow));
    }
}
