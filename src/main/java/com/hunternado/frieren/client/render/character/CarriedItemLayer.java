package com.hunternado.frieren.client.render.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * A signature weapon carried while out of combat; CharacterRenderer moves it to the hand once the character fights.
 * The item renders in the NONE display context: the 3D held models fall back to an identity transform there, and
 * swords select their sheathed model. Designed with tools/models/held.py (carry_matrix), which mirrors this chain.
 * <ul>
 *   <li>BACK: an axe strapped flat on the back, nearly upright, its head over the right shoulder and the haft down
 *   to the left hip, so only the blade shows above the shoulder from the front;</li>
 *   <li>HIP: a sheathed sword hanging at the left hip, behind the arm and in front of any cape, guard along the
 *   body so it pokes through neither.</li>
 * </ul>
 */
public class CarriedItemLayer extends RenderLayer<CharacterRenderState, CharacterModel> {
    private static final float PX = 1.0F / 16.0F;
    // BACK: weapon middle 8 px below the neck, 3.6 px behind the body's centre (half a pixel clear of the back plus
    // half the axe's eye); 180 degrees turns the item model (y up) into model space (y down), 165 leans it 15.
    private static final float BACK_Y = 8.0F * PX;
    private static final float BACK_Z = 3.6F * PX;
    private static final float BACK_TILT = 165.0F;
    private static final float BACK_SCALE = 0.8F;
    /** Item-model pixels from the model centre to the weapon's middle. */
    private static final float BACK_MIDDLE = 7.0F * PX;
    // HIP: the scabbard's mouth on the belt at the left side (+x), just behind the arm's back face.
    private static final float HIP_X = 5.9F * PX;
    private static final float HIP_Y = 9.0F * PX;
    private static final float HIP_Z = 2.4F * PX;
    private static final float HIP_SCALE = 0.62F;
    /** Item-model pixels from the model centre down to the scabbard's mouth. */
    private static final float HIP_MOUTH = 3.25F * PX;

    public CarriedItemLayer(RenderLayerParent<CharacterRenderState, CharacterModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, CharacterRenderState state, float yRot, float xRot) {
        if (state.carriedItemState.isEmpty()) {
            return;
        }
        CharacterModel model = getParentModel();
        poseStack.pushPose();
        model.root().translateAndRotate(poseStack);
        model.body().translateAndRotate(poseStack);
        if (state.carry == CharacterKind.Carry.HIP) {
            poseStack.translate(HIP_X, HIP_Y, HIP_Z);
            poseStack.scale(HIP_SCALE, HIP_SCALE, HIP_SCALE);
            poseStack.translate(0.0F, -HIP_MOUTH, 0.0F);
        } else {
            poseStack.translate(0.0F, BACK_Y, BACK_Z);
            poseStack.mulPose(Axis.ZP.rotationDegrees(BACK_TILT));
            poseStack.scale(BACK_SCALE, BACK_SCALE, BACK_SCALE);
            poseStack.translate(0.0F, -BACK_MIDDLE, 0.0F);
        }
        state.carriedItemState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
