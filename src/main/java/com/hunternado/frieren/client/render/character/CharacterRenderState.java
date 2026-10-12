package com.hunternado.frieren.client.render.character;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render state shared by every character model; filled by {@link CharacterRenderer}. */
public class CharacterRenderState extends LivingEntityRenderState {
    /** Casting pose: staff arm raised toward the target, hair and cloth lifted by mana. */
    public boolean casting;
    /** Left arm held high (Aura raising the Scale of Obedience). */
    public boolean raisedArm;
    /** Melee swing progress, 0..1. */
    public float attack;
}
