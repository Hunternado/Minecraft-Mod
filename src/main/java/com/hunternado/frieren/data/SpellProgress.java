package com.hunternado.frieren.data;

import net.minecraft.nbt.CompoundTag;

/** Per-spell progression for one player. */
public final class SpellProgress {
    public static final int MAX_MASTERY = 100;

    private float mastery;
    private int casts;

    public SpellProgress() {}

    public SpellProgress(float mastery, int casts) {
        this.mastery = clamp(mastery);
        this.casts = Math.max(0, casts);
    }

    public int mastery() {
        return (int) mastery;
    }

    public float rawMastery() {
        return mastery;
    }

    public int casts() {
        return casts;
    }

    /** 0..1 fraction used by spell scaling. */
    public float masteryFraction() {
        return mastery / MAX_MASTERY;
    }

    public void addMastery(float amount) {
        this.mastery = clamp(this.mastery + amount);
    }

    public void setMastery(float mastery) {
        this.mastery = clamp(mastery);
    }

    public void incrementCasts() {
        this.casts++;
    }

    public SpellProgress copy() {
        return new SpellProgress(mastery, casts);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("mastery", mastery);
        tag.putInt("casts", casts);
        return tag;
    }

    public static SpellProgress load(CompoundTag tag) {
        return new SpellProgress(tag.getFloatOr("mastery", 0.0F), tag.getIntOr("casts", 0));
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(MAX_MASTERY, value));
    }
}
