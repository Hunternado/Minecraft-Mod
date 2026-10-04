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

/** Tiny round bird with fast-flapping wings. */
public class StilleModel extends EntityModel<StilleRenderState> {
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public StilleModel(ModelPart root) {
        super(root);
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -3.0F, -2.5F, 4.0F, 4.0F, 5.0F, CubeDeformation.NONE)
                .texOffs(0, 10).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
                .texOffs(18, 0).addBox(-1.0F, -2.0F, 2.5F, 2.0F, 1.0F, 3.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 22.0F, 0.0F));
        root.addOrReplaceChild("left_wing",
            CubeListBuilder.create().texOffs(0, 16).addBox(0.0F, 0.0F, -2.0F, 5.0F, 1.0F, 4.0F, CubeDeformation.NONE),
            PartPose.offset(2.0F, 20.0F, 0.0F));
        root.addOrReplaceChild("right_wing",
            CubeListBuilder.create().texOffs(0, 22).addBox(-5.0F, 0.0F, -2.0F, 5.0F, 1.0F, 4.0F, CubeDeformation.NONE),
            PartPose.offset(-2.0F, 20.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(StilleRenderState state) {
        this.resetPose();
        float flap = Mth.sin(state.time * 2.2F) * 0.9F;
        this.leftWing.zRot = flap;
        this.rightWing.zRot = -flap;
    }
}
