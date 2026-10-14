package com.hunternado.frieren.client.render.character;

import com.hunternado.frieren.entity.statue.HimmelEchoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * An echo of the hero party (Himmel, Eisen or Heiter): their own model, translucent and faintly blue, fading in and
 * out. Translucent enough to read as a memory, opaque enough that their colours still show.
 */
public class HimmelEchoRenderer extends CharacterRenderer<HimmelEchoEntity> {
    public HimmelEchoRenderer(EntityRendererProvider.Context context, CharacterKind kind) {
        super(context, kind, 0.0F);
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
        int alpha = Math.max(0, Math.min(255, (int) (state.variant * 0.78F)));
        return (alpha << 24) | 0xDCE8FF;
    }
}
