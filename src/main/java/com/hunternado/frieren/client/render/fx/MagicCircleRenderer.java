package com.hunternado.frieren.client.render.fx;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/** Draws a magic circle: grows in, rotates (inner ring counter-rotating), then fades out. */
public class MagicCircleRenderer extends EntityRenderer<MagicCircleEntity, MagicCircleRenderer.State> {
    private static final Identifier[] TEXTURES = {
        FrierenIds.id("textures/effect/circle_arcane.png"),
        FrierenIds.id("textures/effect/circle_holy.png"),
        FrierenIds.id("textures/effect/circle_demon.png"),
        FrierenIds.id("textures/effect/circle_flora.png"),
    };

    public static class State extends EntityRenderState {
        public float age;
        public float life;
        public float radius;
        public int color;
        public int style;
        public boolean flat;
        public float yaw;
        public float pitch;
    }

    public MagicCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MagicCircleEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.life = Math.max(1, entity.life());
        state.radius = entity.radius();
        state.color = entity.color();
        state.style = Math.max(0, Math.min(TEXTURES.length - 1, entity.style()));
        state.flat = entity.flat();
        state.yaw = entity.getYRot();
        state.pitch = entity.getXRot();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float grow = Math.min(1.0F, state.age / 5.0F);
        float ease = 1.0F - (1.0F - grow) * (1.0F - grow);
        float fadeStart = state.life - 6.0F;
        float alpha = state.age < fadeStart ? 0.95F : Math.max(0.0F, 0.95F * (state.life - state.age) / 6.0F);
        float radius = state.radius * ease;
        if (radius <= 0.01F || alpha <= 0.01F) {
            return;
        }
        Identifier texture = TEXTURES[state.style];
        int outer = FxQuads.argb(state.color, alpha);
        int inner = FxQuads.argb(com.hunternado.frieren.client.fx.MagicFx.brighten(state.color, 0.45F), alpha * 0.9F);
        poseStack.pushPose();
        if (state.flat) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.age * 2.2F));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(texture),
            (pose, consumer) -> FxQuads.squareXY(consumer, pose, radius, outer));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, state.flat ? -0.02F : -0.04F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.age * 3.5F));
        float innerRadius = radius * 0.55F;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(texture),
            (pose, consumer) -> FxQuads.squareXY(consumer, pose, innerRadius, inner));
        poseStack.popPose();
        poseStack.popPose();
    }
}
