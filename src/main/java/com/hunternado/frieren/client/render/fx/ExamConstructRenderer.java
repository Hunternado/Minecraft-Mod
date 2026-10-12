package com.hunternado.frieren.client.render.fx;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.entity.trial.ExamConstructEntity;
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

/** The practical exam's mana construct: a spinning, glowing octahedron over a small rotating rune ring. */
public class ExamConstructRenderer extends EntityRenderer<ExamConstructEntity, ExamConstructRenderer.State> {
    private static final Identifier CRYSTAL = FrierenIds.id("textures/effect/crystal.png");
    private static final Identifier RING = FrierenIds.id("textures/effect/circle_arcane.png");
    private static final int COLOR = 0x9FD8FF;

    public static class State extends EntityRenderState {
        public float age;
        public float hurt;
    }

    public ExamConstructRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ExamConstructEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.hurt = entity.hurtTime > 0 ? entity.hurtTime / 10.0F : 0.0F;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int crystal = FxQuads.argb(state.hurt > 0.0F ? 0xFFFFFF : COLOR, 0.9F);
        int ring = FxQuads.argb(COLOR, 0.75F);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.45F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 4.0F));
        float pulse = 1.0F + 0.06F * (float) Math.sin(state.age * 0.25F) + 0.2F * state.hurt;
        poseStack.scale(pulse, pulse, pulse);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(CRYSTAL),
            (pose, consumer) -> octahedron(consumer, pose, 0.28F, 0.42F, crystal));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.02F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.age * 2.5F));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(RING),
            (pose, consumer) -> FxQuads.squareXY(consumer, pose, 0.5F, ring));
        poseStack.popPose();
    }

    /** Eight triangular faces (each drawn as a quad with a repeated apex), both windings. */
    private static void octahedron(VertexConsumer c, PoseStack.Pose pose, float r, float h, int argb) {
        float[][] ring = {{r, 0.0F, 0.0F}, {0.0F, 0.0F, r}, {-r, 0.0F, 0.0F}, {0.0F, 0.0F, -r}};
        for (int i = 0; i < 4; i++) {
            float[] a = ring[i];
            float[] b = ring[(i + 1) % 4];
            for (int apex = -1; apex <= 1; apex += 2) {
                float y = apex * h;
                float shade = apex > 0 ? 1.0F : 0.75F;
                int color = (argb & 0xFF000000) | (scale((argb >> 16) & 0xFF, shade) << 16) | (scale((argb >> 8) & 0xFF, shade) << 8)
                    | scale(argb & 0xFF, shade);
                FxQuads.vertex(c, pose, a[0], a[1], a[2], 0.0F, 1.0F, color);
                FxQuads.vertex(c, pose, b[0], b[1], b[2], 1.0F, 1.0F, color);
                FxQuads.vertex(c, pose, 0.0F, y, 0.0F, 0.5F, 0.0F, color);
                FxQuads.vertex(c, pose, 0.0F, y, 0.0F, 0.5F, 0.0F, color);
                FxQuads.vertex(c, pose, 0.0F, y, 0.0F, 0.5F, 0.0F, color);
                FxQuads.vertex(c, pose, 0.0F, y, 0.0F, 0.5F, 0.0F, color);
                FxQuads.vertex(c, pose, b[0], b[1], b[2], 1.0F, 1.0F, color);
                FxQuads.vertex(c, pose, a[0], a[1], a[2], 0.0F, 1.0F, color);
            }
        }
    }

    private static int scale(int channel, float factor) {
        return Math.max(0, Math.min(255, (int) (channel * factor)));
    }
}
