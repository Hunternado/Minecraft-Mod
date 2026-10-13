package com.hunternado.frieren.client.render.character;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;

/** Render state shared by every character model; filled by {@link CharacterRenderer}. Armed: hands hold items. */
public class CharacterRenderState extends ArmedEntityRenderState {
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
    /** The main-hand item is a staff: it aims further forward while casting than a weapon does. */
    public boolean holdingStaff;
}
