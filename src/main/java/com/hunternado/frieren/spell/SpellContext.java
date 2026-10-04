package com.hunternado.frieren.spell;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.data.SpellProgress;
import com.hunternado.frieren.item.StaffItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Everything a {@link SpellBehavior} needs for one cast. Created only by {@link SpellCaster} on the
 * server, after validation and payment.
 */
public final class SpellContext {
    private final ServerPlayer caster;
    private final ServerLevel level;
    private final MagicData data;
    private final SpellDefinition spell;
    private final SpellProgress progress;
    private final StaffItem.Tier staff;
    private final float power;
    private int hits;

    public SpellContext(ServerPlayer caster, ServerLevel level, MagicData data, SpellDefinition spell, SpellProgress progress,
                        StaffItem.Tier staff, float power) {
        this.caster = caster;
        this.level = level;
        this.data = data;
        this.spell = spell;
        this.progress = progress;
        this.staff = staff;
        this.power = power;
    }

    public ServerPlayer caster() { return caster; }
    public ServerLevel level() { return level; }
    public MagicData data() { return data; }
    public SpellDefinition spell() { return spell; }
    public SpellProgress progress() { return progress; }
    public StaffItem.Tier staff() { return staff; }

    /** Combined multiplier from mastery, staff and server config. */
    public float power() { return power; }

    public double param(String key, double fallback) {
        return spell.param(key, fallback);
    }

    /** A parameter scaled by {@link #power()}; use for damage / healing / strength values. */
    public float scaled(String key, double fallback) {
        return (float) (spell.param(key, fallback) * power);
    }

    public float masteryFraction() {
        return progress.masteryFraction();
    }

    public int color() {
        return spell.color();
    }

    /** Behaviors call this when they affect a living target so mastery rewards meaningful casts. */
    public void markHit() {
        hits++;
    }

    public int hits() {
        return hits;
    }

    /**
     * Pays an extra amount of mana (channelled pulses). Returns false without paying if there is not enough.
     */
    public boolean spendExtraMana(float amount) {
        float cost = SpellCaster.scaleCost(amount, this);
        if (!data.trySpendMana(cost)) {
            return false;
        }
        com.hunternado.frieren.mana.ManaHelper.onManaSpent(caster, data, cost);
        data.pauseRegenUntil(level.getGameTime() + 40L);
        return true;
    }
}
