package com.hunternado.frieren.client.render.character;

/**
 * Optional pose hints an entity can expose to the character renderer. Implemented by entities on both sides; the
 * values must come from synced data so clients see the same pose.
 */
public interface CharacterPoses {
    /** True while the entity is channelling or casting a spell. */
    default boolean isCastingPose() {
        return false;
    }

    /** True while the entity holds its left hand up (e.g. the Scale of Obedience during the weighing). */
    default boolean isRaisedArmPose() {
        return false;
    }
}
