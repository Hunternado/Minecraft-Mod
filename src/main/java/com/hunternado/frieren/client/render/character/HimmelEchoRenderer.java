package com.hunternado.frieren.client.render.character;

import com.hunternado.frieren.entity.statue.HimmelEchoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Himmel's echo: his own model, translucent and pale blue, fading in and out. */
public class HimmelEchoRenderer extends CharacterRenderer<HimmelEchoEntity> {
    public HimmelEchoRenderer(EntityRendererProvider.Context context) {
        super(context, CharacterKind.HIMMEL, 0.0F);
    }

    @Override
    public void extractRenderState(HimmelEchoEntity entity, CharacterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = Math.round(entity.presence(partialTicks) * 255.0F);
    }

    /** A translucent memory: an opaque sword in his hand would break the effect. */
    @Override
    protected boolean showsSignatureItems() {
        return false;
    }

    @Override
    protected RenderType getRenderType(CharacterRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderTypes.entityTranslucent(getTextureLocation(state));
    }

    @Override
    protected int getModelTint(CharacterRenderState state) {
        int alpha = Math.max(0, Math.min(255, (int) (state.variant * 0.6F)));
        return (alpha << 24) | 0xC8DCFF;
    }
}
