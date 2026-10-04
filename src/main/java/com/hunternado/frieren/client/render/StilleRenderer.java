package com.hunternado.frieren.client.render;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.entity.creature.StilleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;

public class StilleRenderer extends LivingEntityRenderer<StilleEntity, StilleRenderState, StilleModel> {
    private static final Identifier TEXTURE = FrierenIds.id("textures/entity/stille.png");

    public StilleRenderer(EntityRendererProvider.Context context) {
        super(context, new StilleModel(context.bakeLayer(ModModelLayers.STILLE)), 0.15F);
    }

    @Override
    public StilleRenderState createRenderState() {
        return new StilleRenderState();
    }

    @Override
    public void extractRenderState(StilleEntity entity, StilleRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.time = entity.tickCount + partialTicks;
    }

    @Override
    public Identifier getTextureLocation(StilleRenderState state) {
        return TEXTURE;
    }
}
