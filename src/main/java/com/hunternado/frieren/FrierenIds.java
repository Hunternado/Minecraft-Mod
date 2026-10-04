package com.hunternado.frieren;

import net.minecraft.resources.Identifier;

/**
 * Central helper for namespaced identifiers so no code builds "frieren:..." strings by hand.
 */
public final class FrierenIds {
    private FrierenIds() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FrierenMod.MODID, path);
    }

    /** Translation key helper: {@code key("spell", "zoltraak")} -> {@code spell.frieren.zoltraak}. */
    public static String key(String category, String path) {
        return category + "." + FrierenMod.MODID + "." + path;
    }
}
