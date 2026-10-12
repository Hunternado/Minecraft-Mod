package com.hunternado.frieren.client.render.character;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.entity.statue.HeroStatueEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** The Hero's statue: Himmel's model frozen in its statue pose, skinned by patina stage. */
public class HeroStatueRenderer extends CharacterRenderer<HeroStatueEntity> {
    private static final Identifier[] STAGES = {
        FrierenIds.id("textures/entity/character/hero_statue.png"),
        FrierenIds.id("textures/entity/character/hero_statue_weathered.png"),
        FrierenIds.id("textures/entity/character/hero_statue_exposed.png"),
        FrierenIds.id("textures/entity/character/hero_statue_polished.png"),
    };

    public HeroStatueRenderer(EntityRendererProvider.Context context) {
        super(context, CharacterKind.HERO_STATUE, 0.6F);
    }

    @Override
    public void extractRenderState(HeroStatueEntity entity, CharacterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.statue = true;
        state.casting = false;
        state.attack = 0.0F;
        state.variant = entity.stage();
    }

    @Override
    public Identifier getTextureLocation(CharacterRenderState state) {
        return STAGES[Math.max(0, Math.min(STAGES.length - 1, state.variant))];
    }
}
