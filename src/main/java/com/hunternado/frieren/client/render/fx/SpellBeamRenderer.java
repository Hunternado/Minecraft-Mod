package com.hunternado.frieren.client.render.fx;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.client.fx.MagicFx;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Draws a spell beam as two crossed textured ribbons (soft halo) plus a narrower bright core, fading and thinning
 * over its short life. Lightning-style beams jitter their mid-points.
 */
public class SpellBeamRenderer extends EntityRenderer<SpellBeamEntity, SpellBeamRenderer.State> {
    private static final Identifier BEAM = FrierenIds.id("textures/effect/beam.png");

    public static class State extends EntityRenderState {
        public float dx;
        public float dy;
        public float dz;
        public float age;
        public float life;
        public float width;
        public int color;
        public int style;
        public long seed;
    }

    public SpellBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(SpellBeamEntity entity, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(entity.beamBounds());
    }

    @Override
    public void extractRenderState(SpellBeamEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        Vec3 d = entity.offset();
        state.dx = (float) d.x;
        state.dy = (float) d.y;
        state.dz = (float) d.z;
        state.age = entity.tickCount + partialTicks;
        state.life = Math.max(1, entity.life());
        state.width = entity.width();
        state.color = entity.color();
        state.style = entity.style();
        state.seed = entity.getId() * 31L + entity.tickCount;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float t = Math.min(1.0F, state.age / state.life);
        float alpha = (1.0F - t * t);
        float width = state.width * (1.0F - 0.6F * t) * (0.9F + 0.1F * (float) Math.sin(state.age * 2.0F));
        if (alpha <= 0.02F || width <= 0.005F) {
            return;
        }
        Vector3f dir = new Vector3f(state.dx, state.dy, state.dz);
        float length = dir.length();
        if (length < 0.05F) {
            return;
        }
        dir.normalize();
        Vector3f side = new Vector3f(dir).cross(0.0F, 1.0F, 0.0F);
        if (side.lengthSquared() < 1.0E-4F) {
            side.set(1.0F, 0.0F, 0.0F);
        }
        side.normalize();
        Vector3f up = new Vector3f(side).cross(dir).normalize();
        // Three layers: a wide faint glow, the coloured body, and a white-hot core (anime Zoltraak look).
        int glow = FxQuads.argb(state.color, alpha * 0.28F);
        int body = FxQuads.argb(MagicFx.brighten(state.color, 0.15F), alpha * 0.9F);
        int core = FxQuads.argb(MagicFx.brighten(state.color, 0.85F), alpha);
        boolean jagged = state.style == SpellBeamEntity.STYLE_LIGHTNING;
        float scroll = state.age * 0.35F;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(BEAM), (pose, consumer) -> {
            ribbon(consumer, pose, dir, side, length, width * 2.6F, glow, scroll * 0.5F, jagged, state.seed);
            ribbon(consumer, pose, dir, up, length, width * 2.6F, glow, scroll * 0.5F, jagged, state.seed);
            ribbon(consumer, pose, dir, side, length, width, body, scroll, jagged, state.seed);
            ribbon(consumer, pose, dir, up, length, width, body, scroll, jagged, state.seed);
            ribbon(consumer, pose, dir, side, length, width * 0.45F, core, scroll * 1.6F, jagged, state.seed);
            ribbon(consumer, pose, dir, up, length, width * 0.45F, core, scroll * 1.6F, jagged, state.seed);
        });
    }

    /** A strip along dir, spanning +-width/2 along axis, split into segments (jittered for lightning). */
    private static void ribbon(VertexConsumer c, PoseStack.Pose pose, Vector3f dir, Vector3f axis, float length, float width,
                               int argb, float scroll, boolean jagged, long seed) {
        int segments = Math.max(1, Math.min(48, (int) (length * 1.5F)));
        float half = width / 2.0F;
        java.util.Random random = new java.util.Random(seed);
        Vector3f prev = new Vector3f();
        for (int i = 0; i < segments; i++) {
            float l1 = length * (i + 1) / segments;
            Vector3f next = new Vector3f(dir).mul(l1);
            if (jagged && i < segments - 1) {
                next.add((random.nextFloat() - 0.5F) * 0.6F, (random.nextFloat() - 0.5F) * 0.6F, (random.nextFloat() - 0.5F) * 0.6F);
            }
            float v0 = prev.length() * 0.5F - scroll;
            float v1 = l1 * 0.5F - scroll;
            float ax = axis.x * half;
            float ay = axis.y * half;
            float az = axis.z * half;
            // front and back windings
            FxQuads.vertex(c, pose, prev.x - ax, prev.y - ay, prev.z - az, 0.0F, v0, argb);
            FxQuads.vertex(c, pose, prev.x + ax, prev.y + ay, prev.z + az, 1.0F, v0, argb);
            FxQuads.vertex(c, pose, next.x + ax, next.y + ay, next.z + az, 1.0F, v1, argb);
            FxQuads.vertex(c, pose, next.x - ax, next.y - ay, next.z - az, 0.0F, v1, argb);
            FxQuads.vertex(c, pose, next.x - ax, next.y - ay, next.z - az, 0.0F, v1, argb);
            FxQuads.vertex(c, pose, next.x + ax, next.y + ay, next.z + az, 1.0F, v1, argb);
            FxQuads.vertex(c, pose, prev.x + ax, prev.y + ay, prev.z + az, 1.0F, v0, argb);
            FxQuads.vertex(c, pose, prev.x - ax, prev.y - ay, prev.z - az, 0.0F, v0, argb);
            prev = next;
        }
    }
}
