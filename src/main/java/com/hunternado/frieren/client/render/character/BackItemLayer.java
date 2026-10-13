package com.hunternado.frieren.client.render.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * A weapon carried across the back while out of combat, the way Stark wears his axe: head above the right shoulder,
 * haft running down to the left hip. CharacterRenderer moves it to the hand once the character fights.
 * The item renders in the NONE display context, which the 3D held models fall back to (identity transform).
 */
public class BackItemLayer extends RenderLayer<CharacterRenderState, CharacterModel> {
    /** Half a pixel clear of the back plus half the thickest part of the weapon (the axe's eye). */
    private static final float BACK_OFFSET = 3.6F / 16.0F;
    /** Tilt from upright toward the right shoulder; 180 degrees turns the item model (y up) into model space (y down). */
    private static final float TILT = 145.0F;
    private static final float SCALE = 0.85F;
    /** Item-model pixels from the grip (y = 8) to the weapon's middle, which should sit in the middle of the back. */
    private static final float GRIP_TO_MIDDLE = 7.0F / 16.0F;

    public BackItemLayer(RenderLayerParent<CharacterRenderState, CharacterModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, CharacterRenderState state, float yRot, float xRot) {
        if (state.backItemState.isEmpty()) {
            return;
        }
        CharacterModel model = getParentModel();
        poseStack.pushPose();
        model.root().translateAndRotate(poseStack);
        model.body().translateAndRotate(poseStack);
        poseStack.translate(0.0F, 6.0F / 16.0F, BACK_OFFSET);
        poseStack.mulPose(Axis.ZP.rotationDegrees(TILT));
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(0.0F, -GRIP_TO_MIDDLE, 0.0F);
        state.backItemState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
