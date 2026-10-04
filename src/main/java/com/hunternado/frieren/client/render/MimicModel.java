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

/** A chest with a hinged lid (and teeth on the texture). The lid chomps once the mimic is revealed. */
public class MimicModel extends EntityModel<MimicRenderState> {
    private final ModelPart lid;

    public MimicModel(ModelPart root) {
        super(root);
        this.lid = root.getChild("lid");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base",
            CubeListBuilder.create().texOffs(0, 19).addBox(-7.0F, 0.0F, -7.0F, 14.0F, 10.0F, 14.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("lid",
            CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -5.0F, -14.0F, 14.0F, 5.0F, 14.0F, CubeDeformation.NONE)
                .texOffs(0, 0).addBox(-1.0F, -2.0F, -15.0F, 2.0F, 4.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 14.0F, 7.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MimicRenderState state) {
        this.resetPose();
        if (state.revealed) {
            this.lid.xRot = -Math.abs(Mth.sin(state.time * 0.35F)) * 0.9F;
        }
    }
}
