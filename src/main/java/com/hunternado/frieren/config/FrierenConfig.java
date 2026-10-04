package com.hunternado.frieren.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.function.Supplier;

/**
 * Server (per-world, synced to clients) balance configuration. Only values that are genuinely worth
 * tuning are exposed. Structure frequency is data-driven (structure sets) and is tuned with a datapack,
 * see README.
 */
public final class FrierenConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue BASE_MAX_MANA;
    private static final ForgeConfigSpec.DoubleValue MANA_REGEN_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue MANA_GROWTH_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue SPELL_DAMAGE_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue SPELL_COST_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue SPELL_COOLDOWN_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue MASTERY_GAIN_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue SPELL_GRIEFING;
    private static final ForgeConfigSpec.BooleanValue FLIGHT_MAGIC_ENABLED;
    private static final ForgeConfigSpec.DoubleValue BOSS_HEALTH_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue BOSS_DAMAGE_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue EXAM_TIME_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue DEMON_DECEPTION;

    static {
        BUILDER.comment("Mana and progression").push("mana");
        BASE_MAX_MANA = BUILDER
            .comment("Maximum mana every player starts with before growth, rank and equipment bonuses.")
            .defineInRange("baseMaxMana", 50, 10, 1000);
        MANA_REGEN_MULTIPLIER = BUILDER
            .comment("Multiplier applied to all passive mana regeneration.")
            .defineInRange("manaRegenMultiplier", 1.0D, 0.0D, 10.0D);
        MANA_GROWTH_MULTIPLIER = BUILDER
            .comment("Multiplier for how quickly spending mana raises maximum mana (training).")
            .defineInRange("manaGrowthMultiplier", 1.0D, 0.0D, 10.0D);
        MASTERY_GAIN_MULTIPLIER = BUILDER
            .comment("Multiplier for spell mastery gained per cast.")
            .defineInRange("masteryGainMultiplier", 1.0D, 0.0D, 10.0D);
        BUILDER.pop();

        BUILDER.comment("Spell balance").push("spells");
        SPELL_DAMAGE_MULTIPLIER = BUILDER
            .comment("Multiplier for all spell damage and healing.")
            .defineInRange("spellDamageMultiplier", 1.0D, 0.0D, 10.0D);
        SPELL_COST_MULTIPLIER = BUILDER
            .comment("Multiplier for all spell mana costs.")
            .defineInRange("spellManaCostMultiplier", 1.0D, 0.0D, 10.0D);
        SPELL_COOLDOWN_MULTIPLIER = BUILDER
            .comment("Multiplier for all spell cooldowns.")
            .defineInRange("spellCooldownMultiplier", 1.0D, 0.0D, 10.0D);
        SPELL_GRIEFING = BUILDER
            .comment("If true, spells such as Vollzanbel may ignite blocks and Diagolze may transmute stone. Also requires the mobGriefing game rule.")
            .define("allowSpellGriefing", false);
        FLIGHT_MAGIC_ENABLED = BUILDER
            .comment("If false, Flight Magic cannot be cast on this server.")
            .define("flightMagicEnabled", true);
        BUILDER.pop();

        BUILDER.comment("Enemies, bosses and exams").push("challenge");
        BOSS_HEALTH_MULTIPLIER = BUILDER
            .comment("Multiplier for boss (Aura, Qual, Spiegel) maximum health.")
            .defineInRange("bossHealthMultiplier", 1.0D, 0.1D, 20.0D);
        BOSS_DAMAGE_MULTIPLIER = BUILDER
            .comment("Multiplier for boss damage output.")
            .defineInRange("bossDamageMultiplier", 1.0D, 0.1D, 20.0D);
        EXAM_TIME_MULTIPLIER = BUILDER
            .comment("Multiplier for exam time limits.")
            .defineInRange("examTimeMultiplier", 1.0D, 0.25D, 10.0D);
        DEMON_DECEPTION = BUILDER
            .comment("If true, wounded demons may plead and then strike players who stop attacking.")
            .define("demonDeception", true);
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private FrierenConfig() {}

    public static int baseMaxMana() { return safe(BASE_MAX_MANA::get, 50); }
    public static double manaRegenMultiplier() { return safe(MANA_REGEN_MULTIPLIER::get, 1.0D); }
    public static double manaGrowthMultiplier() { return safe(MANA_GROWTH_MULTIPLIER::get, 1.0D); }
    public static double masteryGainMultiplier() { return safe(MASTERY_GAIN_MULTIPLIER::get, 1.0D); }
    public static double spellDamageMultiplier() { return safe(SPELL_DAMAGE_MULTIPLIER::get, 1.0D); }
    public static double spellCostMultiplier() { return safe(SPELL_COST_MULTIPLIER::get, 1.0D); }
    public static double spellCooldownMultiplier() { return safe(SPELL_COOLDOWN_MULTIPLIER::get, 1.0D); }
    public static boolean spellGriefing() { return safe(SPELL_GRIEFING::get, false); }
    public static boolean flightMagicEnabled() { return safe(FLIGHT_MAGIC_ENABLED::get, true); }
    public static double bossHealthMultiplier() { return safe(BOSS_HEALTH_MULTIPLIER::get, 1.0D); }
    public static double bossDamageMultiplier() { return safe(BOSS_DAMAGE_MULTIPLIER::get, 1.0D); }
    public static double examTimeMultiplier() { return safe(EXAM_TIME_MULTIPLIER::get, 1.0D); }
    public static boolean demonDeception() { return safe(DEMON_DECEPTION::get, true); }

    /**
     * Server configs only exist while a world is loaded. Client UI may ask for values in other states, so
     * fall back to defaults instead of throwing.
     */
    private static <T> T safe(Supplier<T> getter, T fallback) {
        try {
            T value = getter.get();
            return value != null ? value : fallback;
        } catch (IllegalStateException e) {
            return fallback;
        }
    }
}
