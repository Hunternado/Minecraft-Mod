package com.hunternado.frieren.client.render;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.entity.creature.MimicEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;

public class MimicRenderer extends LivingEntityRenderer<MimicEntity, MimicRenderState, MimicModel> {
    private static final Identifier TEXTURE = FrierenIds.id("textures/entity/mimic.png");

    public MimicRenderer(EntityRendererProvider.Context context) {
        super(context, new MimicModel(context.bakeLayer(ModModelLayers.MIMIC)), 0.6F);
    }

    @Override
    public MimicRenderState createRenderState() {
        return new MimicRenderState();
    }

    @Override
    public void extractRenderState(MimicEntity entity, MimicRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.revealed = entity.isRevealed();
        state.time = entity.tickCount + partialTicks;
    }

    @Override
    public Identifier getTextureLocation(MimicRenderState state) {
        return TEXTURE;
    }
}
