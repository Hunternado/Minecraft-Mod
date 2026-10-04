package com.hunternado.frieren.magic;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.util.EnumCodecs;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Broad classification used for UI grouping, exam requirements and quest hooks. */
public enum SpellSchool {
    OFFENSIVE("offensive", ChatFormatting.RED),
    DEFENSIVE("defensive", ChatFormatting.BLUE),
    UTILITY("utility", ChatFormatting.YELLOW),
    SPECIAL("special", ChatFormatting.DARK_AQUA);

    public static final Codec<SpellSchool> CODEC = EnumCodecs.byName(values(), SpellSchool::serializedName);

    private final String name;
    private final ChatFormatting color;

    SpellSchool(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    public String serializedName() {
        return name;
    }

    public Component displayName() {
        return Component.translatable(FrierenIds.key("school", name)).withStyle(color);
    }
}
