package com.hunternado.frieren.client.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/** For particle-only magic constructs: renders nothing itself (visuals come from client-side particles). */
public class InvisibleRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState> {
    public InvisibleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
