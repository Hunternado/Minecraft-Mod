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
    private final ModelPart hairBack2;
    private final ModelPart lockLeft;
    private final ModelPart lockLeft2;
    private final ModelPart lockRight;
    private final ModelPart lockRight2;
    private final ModelPart tailLeft;
    private final ModelPart tailLeft2;
    private final ModelPart tailLeft3;
    private final ModelPart tailRight;
    private final ModelPart tailRight2;
    private final ModelPart tailRight3;
    private final ModelPart body;
    private final ModelPart cape;
    private final ModelPart cape2;
    private final ModelPart skirt;
    private final ModelPart skirt2;
    private final ModelPart coatLeft;
    private final ModelPart coatRight;
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
        this.hairBack2 = hairBack.getChild("hair_back_2");
        this.lockLeft = head.getChild("lock_left");
        this.lockLeft2 = lockLeft.getChild("lock_left_2");
        this.lockRight = head.getChild("lock_right");
        this.lockRight2 = lockRight.getChild("lock_right_2");
        this.tailLeft = head.getChild("tail_left");
        this.tailLeft2 = tailLeft.getChild("tail_left_2");
        this.tailLeft3 = tailLeft2.getChild("tail_left_3");
        this.tailRight = head.getChild("tail_right");
        this.tailRight2 = tailRight.getChild("tail_right_2");
        this.tailRight3 = tailRight2.getChild("tail_right_3");
        this.body = root.getChild("body");
        this.cape = body.getChild("cape");
        this.cape2 = cape.getChild("cape_2");
        this.skirt = body.getChild("skirt");
        this.skirt2 = skirt.getChild("skirt_2");
        this.coatLeft = body.getChild("coat_left");
        this.coatRight = body.getChild("coat_right");
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

        // Hair and cloth: trail behind when moving and sway gently when idle; every link of a chain adds a little
        // more, so long hair and capes bend instead of swinging like boards. Hair hangs from the head but should
        // fall with gravity, so it partly cancels the head's pitch.
        float sway = Mth.sin(time * 0.09F) * 0.04F;
        float sway2 = Mth.sin(time * 0.09F - 0.8F) * 0.05F;
        float trail = amount * 0.45F;
        float gravity = -head.xRot * 0.7F;
        hairBack.xRot += trail * 0.7F + sway * 0.5F + gravity;
        hairBack2.xRot += trail * 0.5F + sway2 * 0.5F;
        tailRight.xRot += trail * 0.6F + gravity;
        tailLeft.xRot += trail * 0.6F + gravity;
        tailRight.zRot += sway;
        tailLeft.zRot -= sway;
        tailRight2.xRot += trail * 0.35F;
        tailLeft2.xRot += trail * 0.35F;
        tailRight2.zRot += sway2;
        tailLeft2.zRot -= sway2;
        tailRight3.zRot += sway2 * 0.8F;
        tailLeft3.zRot -= sway2 * 0.8F;
        lockRight.xRot += gravity * 0.6F + trail * 0.2F;
        lockLeft.xRot += gravity * 0.6F + trail * 0.2F;
        lockRight2.zRot += sway * 0.5F;
        lockLeft2.zRot -= sway * 0.5F;
        cape.xRot += amount * 0.55F + sway * 0.6F;
        cape2.xRot += amount * 0.35F + sway2 * 0.6F;
        skirt.xRot += Math.abs(Mth.sin(walk)) * 0.04F * amount;
        skirt2.xRot += amount * 0.25F + sway2 * 0.4F;
        // Open coat panels swing forward with the leg beneath them instead of letting it poke through.
        coatRight.xRot += Math.min(0.0F, rightLeg.xRot) * 0.55F + sway * 0.3F;
        coatLeft.xRot += Math.min(0.0F, leftLeg.xRot) * 0.55F - sway * 0.3F;

        if (state.casting) {
            rightArm.xRot = -Mth.HALF_PI + head.xRot * 0.6F;
            rightArm.yRot = head.yRot * 0.6F - 0.1F;
            rightArm.zRot = 0.0F;
            leftArm.xRot = -0.55F;
            leftArm.yRot = 0.3F;
            // Mana flowing outward lifts hair and cloth.
            float flutter = Mth.sin(time * 0.35F) * 0.05F;
            hairBack.xRot += 0.18F + flutter;
            hairBack2.xRot += 0.1F - flutter;
            tailRight.xRot += 0.22F + flutter;
            tailLeft.xRot += 0.22F - flutter;
            tailRight2.xRot += 0.12F;
            tailLeft2.xRot += 0.12F;
            cape.xRot += 0.3F + flutter;
            cape2.xRot += 0.15F - flutter;
            skirt2.xRot += 0.12F + flutter;
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

    /** The torso part; CarriedItemLayer hangs carried weapons from it. */
    public ModelPart body() {
        return body;
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
