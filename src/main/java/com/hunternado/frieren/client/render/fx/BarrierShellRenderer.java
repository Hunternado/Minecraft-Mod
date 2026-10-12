package com.hunternado.frieren.client.render.fx;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.entity.fx.BarrierShellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Draws Defensive Magic's hexagonal shell as a slowly turning, flickering sphere that fades out. */
public class BarrierShellRenderer extends EntityRenderer<BarrierShellEntity, BarrierShellRenderer.State> {
    private static final Identifier HEX = FrierenIds.id("textures/effect/hex_shield.png");
    private static final int LAT = 10;
    private static final int LON = 16;

    public static class State extends EntityRenderState {
        public float age;
        public float remaining;
        public float radius;
        public int color;
    }

    public BarrierShellRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BarrierShellEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.remaining = entity.expires() - state.age;
        state.radius = entity.radius();
        state.color = entity.color();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float alpha = Mth.clamp(state.remaining / 6.0F, 0.0F, 1.0F) * (0.55F + 0.15F * Mth.sin(state.age * 1.3F));
        if (alpha <= 0.02F) {
            return;
        }
        float pop = 1.0F + 0.08F * Math.max(0.0F, 1.0F - state.age / 4.0F);
        float r = state.radius * pop;
        int argb = FxQuads.argb(state.color, alpha);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 1.5F));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(HEX),
            (pose, consumer) -> sphere(consumer, pose, r, argb));
        poseStack.popPose();
    }

    private static void sphere(VertexConsumer c, PoseStack.Pose pose, float r, int argb) {
        for (int i = 0; i < LAT; i++) {
            float t0 = Mth.PI * i / LAT - Mth.HALF_PI;
            float t1 = Mth.PI * (i + 1) / LAT - Mth.HALF_PI;
            for (int j = 0; j < LON; j++) {
                float p0 = 2.0F * Mth.PI * j / LON;
                float p1 = 2.0F * Mth.PI * (j + 1) / LON;
                float u0 = j * 4.0F / LON;
                float u1 = (j + 1) * 4.0F / LON;
                float v0 = i * 2.0F / LAT;
                float v1 = (i + 1) * 2.0F / LAT;
                point(c, pose, r, t0, p0, u0, v0, argb);
                point(c, pose, r, t0, p1, u1, v0, argb);
                point(c, pose, r, t1, p1, u1, v1, argb);
                point(c, pose, r, t1, p0, u0, v1, argb);
                point(c, pose, r, t1, p0, u0, v1, argb);
                point(c, pose, r, t1, p1, u1, v1, argb);
                point(c, pose, r, t0, p1, u1, v0, argb);
                point(c, pose, r, t0, p0, u0, v0, argb);
            }
        }
    }

    private static void point(VertexConsumer c, PoseStack.Pose pose, float r, float theta, float phi, float u, float v, int argb) {
        float x = r * Mth.cos(theta) * Mth.cos(phi);
        float y = r * Mth.sin(theta);
        float z = r * Mth.cos(theta) * Mth.sin(phi);
        FxQuads.vertex(c, pose, x, y, z, u, v, argb);
    }
}
