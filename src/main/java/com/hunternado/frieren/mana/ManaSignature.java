package com.hunternado.frieren.mana;

/**
 * Implemented by entities that have a mana signature (demons, mage NPCs, mimics). Players are handled
 * through their {@code MagicData} instead.
 */
public interface ManaSignature {
    /** True magical capacity. */
    float trueMana();

    /** How well the entity hides its mana (0 = not at all). Detection power must exceed this to see through. */
    int concealLevel();

    /** Fraction of the signature shown to detectors that cannot see through (1 = nothing hidden). */
    default float apparentFraction() {
        return concealLevel() <= 0 ? 1.0F : Math.max(0.05F, 1.0F - concealLevel() * 0.1F);
    }

    /** Category used by the client to colour the readout. */
    default byte senseKind() {
        return 1;
    }
}
