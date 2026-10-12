package com.hunternado.frieren.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * The mimic. Disguised it is an ordinary chest (teeth and tongue hidden). Revealed, the lid gapes and chomps,
 * the tongue lolls out over the front edge and the whole chest hops as it walks.
 */
public class MimicModel extends EntityModel<MimicRenderState> {
    private final ModelPart body;
    private final ModelPart lid;
    private final ModelPart lowerTeeth;
    private final ModelPart upperTeeth;
    private final ModelPart tongue;

    public MimicModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.lid = root.getChild("lid");
        this.lowerTeeth = body.getChild("lower_teeth");
        this.upperTeeth = lid.getChild("upper_teeth");
        this.tongue = body.getChild("tongue");
    }

    public static LayerDefinition createLayer() {
        return MimicLayer.create();
    }

    @Override
    public void setupAnim(MimicRenderState state) {
        this.resetPose();
        boolean open = state.revealed;
        lowerTeeth.visible = open;
        upperTeeth.visible = open;
        tongue.visible = open;
        if (!open) {
            return;
        }
        float chomp = Math.abs(Mth.sin(state.time * 0.35F));
        lid.xRot = -(0.25F + chomp * 0.8F);
        tongue.xRot = 0.55F + Mth.sin(state.time * 0.18F) * 0.15F;
        tongue.yRot = Mth.sin(state.time * 0.13F) * 0.25F;
        float hop = Math.abs(Mth.sin(state.walkAnimationPos * 0.9F)) * Math.min(1.0F, state.walkAnimationSpeed) * 3.0F;
        body.y -= hop;
        lid.y -= hop;
    }
}
