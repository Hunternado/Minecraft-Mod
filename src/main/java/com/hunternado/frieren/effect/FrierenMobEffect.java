package com.hunternado.frieren.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Plain effect whose gameplay is implemented through attribute modifiers and checks elsewhere
 * (casting is blocked by {@code SpellCaster}, gilded damage is cancelled in {@code CombatEvents}).
 * Exists only because {@link MobEffect}'s constructor is protected.
 */
public class FrierenMobEffect extends MobEffect {
    public FrierenMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
