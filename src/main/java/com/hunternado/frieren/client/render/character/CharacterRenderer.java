package com.hunternado.frieren.client.render.character;

import com.hunternado.frieren.item.StaffItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders any character of {@link CharacterKind}: its own model layer, skin and scale, and the items in its hands.
 * A character that holds nothing is drawn with its signature item (Frieren's staff, Stark's axe, ...); that is
 * purely visual, so it never drops, syncs or changes combat. Anything the entity really holds is drawn instead.
 */
public class CharacterRenderer<T extends LivingEntity> extends LivingEntityRenderer<T, CharacterRenderState, CharacterModel> {
    private final CharacterKind kind;

    public CharacterRenderer(EntityRendererProvider.Context context, CharacterKind kind, float shadowRadius) {
        super(context, new CharacterModel(context.bakeLayer(kind.layer()), kind), shadowRadius * kind.scale());
        this.kind = kind;
        this.addLayer(new ItemInHandLayer<>(this));
        this.addLayer(new BackItemLayer(this));
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
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTicks);
        state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        state.backItemState.clear();
        if (showsSignatureItems()) {
            if (state.rightHandItemState.isEmpty()) {
                boolean fighting = state.casting || state.attack > 0.0F;
                if (kind.backCarry() && !fighting) {
                    // Out of combat the weapon rides on the back; it comes to hand when the fight starts.
                    if (!kind.mainHandStack().isEmpty()) {
                        this.itemModelResolver.updateForLiving(state.backItemState, kind.mainHandStack(), ItemDisplayContext.NONE, entity);
                    }
                } else {
                    hold(entity, state, kind.mainHandStack(), true);
                }
            }
            if (state.leftHandItemState.isEmpty()) {
                hold(entity, state, kind.offHandStack(), false);
            }
        }
        state.holdingStaff = state.rightHandItemStack.getItem() instanceof StaffItem;
    }

    private void hold(T entity, CharacterRenderState state, ItemStack stack, boolean right) {
        if (stack.isEmpty()) {
            return;
        }
        if (right) {
            this.itemModelResolver.updateForLiving(state.rightHandItemState, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
            state.rightHandItemStack = stack;
        } else {
            this.itemModelResolver.updateForLiving(state.leftHandItemState, stack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
            state.leftHandItemStack = stack;
        }
    }

    /** Whether characters drawn by this renderer hold their signature items when empty-handed. */
    protected boolean showsSignatureItems() {
        return true;
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
