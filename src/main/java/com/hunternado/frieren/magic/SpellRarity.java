package com.hunternado.frieren.magic;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.util.EnumCodecs;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Spell rarity. Standard rarities form a ladder gated by {@link MageRank}; DEMON and FORBIDDEN are side
 * branches with their own unlock rules.
 */
public enum SpellRarity {
    COMMON("common", ChatFormatting.WHITE, 40, true),
    UNCOMMON("uncommon", ChatFormatting.GREEN, 60, true),
    RARE("rare", ChatFormatting.AQUA, 80, true),
    ANCIENT("ancient", ChatFormatting.GOLD, 120, true),
    LEGENDARY("legendary", ChatFormatting.LIGHT_PURPLE, 160, true),
    DEMON("demon", ChatFormatting.DARK_RED, 100, false),
    FORBIDDEN("forbidden", ChatFormatting.DARK_PURPLE, 200, false);

    public static final Codec<SpellRarity> CODEC = EnumCodecs.byName(values(), SpellRarity::serializedName);

    private final String name;
    private final ChatFormatting color;
    private final int decipherTicks;
    private final boolean standard;

    SpellRarity(String name, ChatFormatting color, int decipherTicks, boolean standard) {
        this.name = name;
        this.color = color;
        this.decipherTicks = decipherTicks;
        this.standard = standard;
    }

    public String serializedName() {
        return name;
    }

    public ChatFormatting color() {
        return color;
    }

    /** How long reading a grimoire of this rarity takes, in ticks. */
    public int decipherTicks() {
        return decipherTicks;
    }

    /** Standard rarities are unlocked by rank in order; demon/forbidden have special rules. */
    public boolean isStandard() {
        return standard;
    }

    public Component displayName() {
        return Component.translatable(FrierenIds.key("rarity", name)).withStyle(color);
    }

    public static @Nullable SpellRarity byName(String name) {
        for (SpellRarity rarity : values()) {
            if (rarity.name.equals(name)) {
                return rarity;
            }
        }
        return null;
    }
}
