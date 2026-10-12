package com.hunternado.frieren.client.render.character;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Renders any character of {@link CharacterKind}: its own model layer, skin and scale. */
public class CharacterRenderer<T extends LivingEntity> extends LivingEntityRenderer<T, CharacterRenderState, CharacterModel> {
    private final CharacterKind kind;

    public CharacterRenderer(EntityRendererProvider.Context context, CharacterKind kind, float shadowRadius) {
        super(context, new CharacterModel(context.bakeLayer(kind.layer()), kind), shadowRadius * kind.scale());
        this.kind = kind;
    }

    @Override
    public CharacterRenderState createRenderState() {
        return new CharacterRenderState();
    }

    @Override
    public void extractRenderState(T entity, CharacterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        boolean aggressive = entity instanceof Mob mob && mob.isAggressive();
        boolean castingHint = entity instanceof CharacterPoses poses && poses.isCastingPose();
        state.casting = aggressive || castingHint;
        state.raisedArm = entity instanceof CharacterPoses poses && poses.isRaisedArmPose();
        state.attack = entity.getAttackAnim(partialTicks);
    }

    @Override
    protected void scale(CharacterRenderState state, PoseStack poseStack) {
        float s = kind.scale();
        poseStack.scale(s, s, s);
    }

    @Override
    public Identifier getTextureLocation(CharacterRenderState state) {
        return kind.texture();
    }
}
