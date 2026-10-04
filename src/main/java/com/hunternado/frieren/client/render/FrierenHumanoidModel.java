package com.hunternado.frieren.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Player-proportioned humanoid model with a hat/hair overlay, using the standard 64x64 skin layout so
 * textures are easy to replace. Animation is self-contained (walk cycle, head look, raised casting arm).
 */
public class FrierenHumanoidModel extends EntityModel<FrierenHumanoidRenderState> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public FrierenHumanoidModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE;
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, none), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat",
            CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.ZERO);
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, none), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FrierenHumanoidRenderState state) {
        this.resetPose();
        this.head.yRot = state.headYaw * Mth.DEG_TO_RAD;
        this.head.xRot = state.headPitch * Mth.DEG_TO_RAD;

        float swing = state.limbSwing;
        float amount = state.limbSwingAmount;
        this.rightLeg.xRot = Mth.cos(swing * 0.6662F) * 1.4F * amount;
        this.leftLeg.xRot = Mth.cos(swing * 0.6662F + Mth.PI) * 1.4F * amount;
        this.rightArm.xRot = Mth.cos(swing * 0.6662F + Mth.PI) * amount;
        this.leftArm.xRot = Mth.cos(swing * 0.6662F) * amount;

        // Idle breathing sway.
        float idle = Mth.sin(state.time * 0.067F) * 0.05F;
        this.rightArm.zRot = idle + 0.03F;
        this.leftArm.zRot = -idle - 0.03F;

        if (state.casting) {
            // Staff/spell arm raised toward the target.
            this.rightArm.xRot = -Mth.HALF_PI + this.head.xRot;
            this.rightArm.yRot = this.head.yRot - 0.1F;
        }
        this.body.yRot = 0.0F;
    }
}
