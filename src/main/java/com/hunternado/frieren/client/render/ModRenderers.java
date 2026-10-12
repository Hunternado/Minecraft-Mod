package com.hunternado.frieren.client.render;

import com.hunternado.frieren.client.render.character.CharacterKind;
import com.hunternado.frieren.client.render.character.CharacterRenderer;
import com.hunternado.frieren.client.render.character.HeroStatueRenderer;
import com.hunternado.frieren.client.render.character.HimmelEchoRenderer;
import com.hunternado.frieren.client.render.fx.BarrierShellRenderer;
import com.hunternado.frieren.client.render.fx.ExamConstructRenderer;
import com.hunternado.frieren.client.render.fx.MagicCircleRenderer;
import com.hunternado.frieren.client.render.fx.SpellBeamRenderer;
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
        character(event, ModEntities.DEMON_SOLDIER.get(), CharacterKind.DEMON_SOLDIER, 0.5F);
        character(event, ModEntities.DEMON_MAGE.get(), CharacterKind.DEMON_MAGE, 0.5F);
        character(event, ModEntities.AURA_THRALL.get(), CharacterKind.AURA_THRALL, 0.55F);
        character(event, ModEntities.AURA.get(), CharacterKind.AURA, 0.6F);
        character(event, ModEntities.QUAL.get(), CharacterKind.QUAL, 0.6F);
        character(event, ModEntities.SPIEGEL.get(), CharacterKind.SPIEGEL, 0.6F);
        character(event, ModEntities.MIRROR_REPLICA.get(), CharacterKind.MIRROR_REPLICA, 0.5F);
        character(event, ModEntities.MIRROR_FRIEREN.get(), CharacterKind.MIRROR_FRIEREN, 0.5F);
        character(event, ModEntities.DEMON_KING.get(), CharacterKind.DEMON_KING, 0.6F);
        character(event, ModEntities.MACHT.get(), CharacterKind.MACHT, 0.55F);
        character(event, ModEntities.LINIE.get(), CharacterKind.LINIE, 0.5F);
        character(event, ModEntities.DRAHT.get(), CharacterKind.DRAHT, 0.5F);
        character(event, ModEntities.LUGNER.get(), CharacterKind.LUGNER, 0.5F);
        event.registerEntityRenderer(ModEntities.EXAM_CONSTRUCT.get(), ExamConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.HERO_STATUE.get(), HeroStatueRenderer::new);
        event.registerEntityRenderer(ModEntities.HIMMEL_ECHO.get(), HimmelEchoRenderer::new);
        character(event, ModEntities.WANDERING_MAGE.get(), CharacterKind.WANDERING_MAGE, 0.5F);
        character(event, ModEntities.EXAMINER.get(), CharacterKind.EXAMINER, 0.5F);
        character(event, ModEntities.PRIEST.get(), CharacterKind.PRIEST, 0.5F);
        character(event, ModEntities.FRIEREN.get(), CharacterKind.FRIEREN, 0.5F);
        character(event, ModEntities.FERN.get(), CharacterKind.FERN, 0.5F);
        character(event, ModEntities.STARK.get(), CharacterKind.STARK, 0.5F);
        character(event, ModEntities.SERIE.get(), CharacterKind.SERIE, 0.5F);
        character(event, ModEntities.MANA_DECOY.get(), CharacterKind.MANA_DECOY, 0.0F);
        event.registerEntityRenderer(ModEntities.MIMIC.get(), MimicRenderer::new);
        event.registerEntityRenderer(ModEntities.STILLE.get(), StilleRenderer::new);
        event.registerEntityRenderer(ModEntities.MANA_BOLT.get(), InvisibleRenderer::new);
        event.registerEntityRenderer(ModEntities.SPELL_AREA.get(), InvisibleRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGIC_CIRCLE.get(), MagicCircleRenderer::new);
        event.registerEntityRenderer(ModEntities.SPELL_BEAM.get(), SpellBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.BARRIER_SHELL.get(), BarrierShellRenderer::new);
    }

    private static <T extends LivingEntity> void character(EntityRenderersEvent.RegisterRenderers event, EntityType<T> type,
                                                           CharacterKind kind, float shadow) {
        event.registerEntityRenderer(type, context -> new CharacterRenderer<T>(context, kind, shadow));
    }
}
