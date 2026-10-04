package com.hunternado.frieren.combat;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Damage source that carries how hard it is for defensive magic to stop it.
 * {@code barrierCost} &gt; 1 means barriers spend more mana per point absorbed (Reelseiden cuts through
 * barriers); &lt; 1 means barriers stop it cheaply (Qual's Zoltraak versus modern defensive magic).
 */
public class SpellDamageSource extends DamageSource {
    private final float barrierCost;
    private final boolean demonBane;

    public SpellDamageSource(Holder<DamageType> type, @Nullable Entity direct, @Nullable Entity causing, float barrierCost, boolean demonBane) {
        super(type, direct, causing);
        this.barrierCost = barrierCost;
        this.demonBane = demonBane;
    }

    public float barrierCost() {
        return barrierCost;
    }

    /** Whether the attacker carried a Demon Hunter's Insignia. */
    public boolean demonBane() {
        return demonBane;
    }
}
