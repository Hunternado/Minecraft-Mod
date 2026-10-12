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
    /** Frozen heroic statue pose (the Hero's statue); no animation at all. */
    public boolean statue;
    /** Texture variant for entities with several skins (statue patina stage). */
    public int variant;
}
