package com.hunternado.frieren.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Generic renderer for every humanoid entity of the mod; only the texture differs. */
public class FrierenHumanoidRenderer<T extends LivingEntity> extends LivingEntityRenderer<T, FrierenHumanoidRenderState, FrierenHumanoidModel> {
    private final Identifier texture;

    public FrierenHumanoidRenderer(EntityRendererProvider.Context context, Identifier texture, float shadowRadius) {
        super(context, new FrierenHumanoidModel(context.bakeLayer(ModModelLayers.HUMANOID)), shadowRadius);
        this.texture = texture;
    }

    @Override
    public FrierenHumanoidRenderState createRenderState() {
        return new FrierenHumanoidRenderState();
    }

    @Override
    public void extractRenderState(T entity, FrierenHumanoidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        float speed = (float) Math.sqrt(dx * dx + dz * dz);
        state.time = entity.tickCount + partialTicks;
        state.limbSwing = state.time * 0.9F;
        state.limbSwingAmount = Math.min(1.0F, speed * 4.0F);
        state.headYaw = Mth.wrapDegrees(entity.getYHeadRot() - entity.yBodyRot);
        state.headPitch = entity.getXRot();
        state.casting = entity instanceof Mob mob && mob.isAggressive();
    }

    @Override
    public Identifier getTextureLocation(FrierenHumanoidRenderState state) {
        return texture;
    }
}
