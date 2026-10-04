package com.hunternado.frieren.entity.boss;

/** Marker for bosses: immune to obedience/curse shortcuts, credit every nearby player on death. */
public interface FrierenBoss {
    /** Permanent mana growth granted to each participating player. */
    int bonusGrowthReward();
}
