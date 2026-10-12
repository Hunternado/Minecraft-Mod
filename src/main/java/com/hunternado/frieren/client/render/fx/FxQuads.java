package com.hunternado.frieren.client.render.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;

/** Vertex helpers for full-bright, tinted effect geometry on entity render types. */
final class FxQuads {
    private FxQuads() {}

    static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb) {
        consumer.addVertex(pose, x, y, z)
            .setColor(argb)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightCoordsUtil.FULL_BRIGHT)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /** A square in the local XY plane centred on the origin, both windings so it shows from either side. */
    static void squareXY(VertexConsumer c, PoseStack.Pose pose, float half, int argb) {
        vertex(c, pose, -half, -half, 0.0F, 0.0F, 1.0F, argb);
        vertex(c, pose, half, -half, 0.0F, 1.0F, 1.0F, argb);
        vertex(c, pose, half, half, 0.0F, 1.0F, 0.0F, argb);
        vertex(c, pose, -half, half, 0.0F, 0.0F, 0.0F, argb);
        vertex(c, pose, -half, half, 0.0F, 0.0F, 0.0F, argb);
        vertex(c, pose, half, half, 0.0F, 1.0F, 0.0F, argb);
        vertex(c, pose, half, -half, 0.0F, 1.0F, 1.0F, argb);
        vertex(c, pose, -half, -half, 0.0F, 0.0F, 1.0F, argb);
    }

    static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
