package com.hunternado.frieren.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only presentation options. Never read from server logic. */
public final class FrierenClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue SHOW_HUD;
    private static final ForgeConfigSpec.IntValue HUD_OFFSET_X;
    private static final ForgeConfigSpec.IntValue HUD_OFFSET_Y;
    private static final ForgeConfigSpec.BooleanValue HIDE_HUD_WITHOUT_SPELLS;

    static {
        BUILDER.comment("Mana / spell HUD").push("hud");
        SHOW_HUD = BUILDER.comment("Show the mana bar and spell slots.").define("showHud", true);
        HIDE_HUD_WITHOUT_SPELLS = BUILDER
            .comment("Hide the HUD entirely until you have learned your first spell.")
            .define("hideHudWithoutSpells", true);
        HUD_OFFSET_X = BUILDER.comment("Horizontal offset of the HUD from the bottom-left corner.")
            .defineInRange("offsetX", 6, 0, 4000);
        HUD_OFFSET_Y = BUILDER.comment("Vertical offset of the HUD from the bottom edge.")
            .defineInRange("offsetY", 6, 0, 4000);
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private FrierenClientConfig() {}

    public static boolean showHud() { return get(SHOW_HUD, true); }
    public static boolean hideHudWithoutSpells() { return get(HIDE_HUD_WITHOUT_SPELLS, true); }
    public static int hudOffsetX() { return get(HUD_OFFSET_X, 6); }
    public static int hudOffsetY() { return get(HUD_OFFSET_Y, 6); }

    private static <T> T get(ForgeConfigSpec.ConfigValue<T> value, T fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }
}
