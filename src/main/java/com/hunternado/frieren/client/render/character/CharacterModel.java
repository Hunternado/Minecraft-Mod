package com.hunternado.frieren.client.render.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * One model class animates every character: all rigs share the same part names (parts a character does not use are
 * empty). Hair, twin tails and capes swing with movement and lift while casting. Characters hold real items, placed
 * by {@link #translateToHand} exactly like a player's.
 */
public class CharacterModel extends EntityModel<CharacterRenderState> implements ArmedModel<CharacterRenderState> {
    /** Where ItemInHandLayer puts the grip: 10 px down the arm from the shoulder pivot. */
    private static final float HAND = 10.0F / 16.0F;
    /** While casting, a staff's tip tilts this far toward the target (55 degrees past upright)... */
    private static final float STAFF_AIM = 55.0F * Mth.DEG_TO_RAD;
    /** ...and a weapon this far (35 degrees), which levels a sword or axe in front of the character. */
    private static final float WEAPON_AIM = 35.0F * Mth.DEG_TO_RAD;

    private final CharacterKind kind;
    private final ModelPart head;
    private final ModelPart hairBack;
    private final ModelPart tailLeft;
    private final ModelPart tailRight;
    private final ModelPart body;
    private final ModelPart cape;
    private final ModelPart skirt;
    private final ModelPart rightArm;
    private final ModelPart prop;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public CharacterModel(ModelPart root, CharacterKind kind) {
        super(root);
        this.kind = kind;
        this.head = root.getChild("head");
        this.hairBack = head.getChild("hair_back");
        this.tailLeft = head.getChild("tail_left");
        this.tailRight = head.getChild("tail_right");
        this.body = root.getChild("body");
        this.cape = body.getChild("cape");
        this.skirt = body.getChild("skirt");
        this.rightArm = root.getChild("right_arm");
        this.prop = rightArm.getChild("prop");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    @Override
    public void setupAnim(CharacterRenderState state) {
        this.resetPose();
        if (state.statue) {
            statuePose();
            return;
        }
        float time = state.ageInTicks;
        float swing = state.walkAnimationPos;
        float amount = Math.min(1.0F, state.walkAnimationSpeed);
        float legScale = kind.longRobe() ? 0.45F : 1.0F;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        float walk = swing * 0.6662F;
        rightLeg.xRot = Mth.cos(walk) * 1.4F * amount * legScale;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.4F * amount * legScale;
        rightArm.xRot = Mth.cos(walk + Mth.PI) * 0.9F * amount;
        leftArm.xRot = Mth.cos(walk) * 0.9F * amount;

        float breathe = Mth.sin(time * 0.067F) * 0.05F;
        rightArm.zRot += breathe + 0.05F;
        leftArm.zRot -= breathe + 0.05F;

        // Hair and cloth: trail behind when moving, sway gently when idle.
        float sway = Mth.sin(time * 0.09F) * 0.04F;
        float trail = amount * 0.45F;
        hairBack.xRot += trail + sway * 0.5F;
        tailRight.xRot += trail;
        tailLeft.xRot += trail;
        tailRight.zRot += sway;
        tailLeft.zRot -= sway;
        cape.xRot += amount * 0.7F + sway * 0.6F;
        skirt.xRot += Math.abs(Mth.sin(walk)) * 0.04F * amount;

        if (state.casting) {
            rightArm.xRot = -Mth.HALF_PI + head.xRot * 0.6F;
            rightArm.yRot = head.yRot * 0.6F - 0.1F;
            rightArm.zRot = 0.0F;
            leftArm.xRot = -0.55F;
            leftArm.yRot = 0.3F;
            // Mana flowing outward lifts hair and cloth.
            float flutter = Mth.sin(time * 0.35F) * 0.05F;
            hairBack.xRot += 0.18F + flutter;
            tailRight.xRot += 0.22F + flutter;
            tailLeft.xRot += 0.22F - flutter;
            cape.xRot += 0.3F + flutter;
        }
        if (state.raisedArm) {
            leftArm.xRot = -2.7F;
            leftArm.yRot = -0.1F;
            leftArm.zRot = -0.15F;
        }
        if (state.attack > 0.0F) {
            float arc = Mth.sin(state.attack * Mth.PI);
            rightArm.xRot = -1.1F - arc * 1.4F;
            body.yRot = arc * 0.25F;
        }
    }

    /**
     * Same placement as a player's hand ({@code PlayerModel.translateToHand}): root, then the arm, slim arms nudged
     * half a pixel toward the body, shorter arms moving the grip up. Items rest upright in a hanging hand, which
     * would flip them back over the shoulder once the arm rises, so the wrist turns: while casting or swinging,
     * the arm's nominal raise is cancelled at the hand and an aim tilt added (the swing itself still moves the
     * item); Aura's raised left hand keeps the scale upright.
     */
    @Override
    public void translateToHand(CharacterRenderState state, HumanoidArm arm, PoseStack poseStack) {
        this.root().translateAndRotate(poseStack);
        ModelPart part = arm == HumanoidArm.RIGHT ? rightArm : leftArm;
        if (kind.slim()) {
            float nudge = arm == HumanoidArm.RIGHT ? 0.5F : -0.5F;
            part.x += nudge;
            part.translateAndRotate(poseStack);
            part.x -= nudge;
        } else {
            part.translateAndRotate(poseStack);
        }
        if (kind.armLength() != 12.0F) {
            poseStack.translate(0.0F, (kind.armLength() - 12.0F) / 16.0F, 0.0F);
        }
        float wrist = 0.0F;
        if (arm == HumanoidArm.RIGHT && (state.casting || state.attack > 0.0F) && !state.statue) {
            wrist = Mth.HALF_PI + (state.holdingStaff ? STAFF_AIM : WEAPON_AIM);
        } else if (arm == HumanoidArm.LEFT && state.raisedArm) {
            wrist = -leftArm.xRot;
        }
        if (wrist != 0.0F) {
            poseStack.translate(0.0F, HAND, 0.0F);
            poseStack.mulPose(Axis.XP.rotation(wrist));
            poseStack.translate(0.0F, -HAND, 0.0F);
        }
    }

    /** The Hero's statue: sword raised to the sky, chin up, cape caught in the wind. */
    private void statuePose() {
        rightArm.xRot = -2.75F;
        rightArm.yRot = 0.1F;
        prop.xRot = Mth.PI;
        prop.y += 4.0F; // slide the flipped sword out of the fist so the crossguard shows above it
        leftArm.xRot = 0.1F;
        leftArm.zRot = -0.25F;
        head.xRot = -0.12F;
        cape.xRot = 0.32F;
        rightLeg.xRot = -0.12F;
        leftLeg.xRot = 0.12F;
    }
}
