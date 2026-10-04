package com.hunternado.frieren.magic;

import com.hunternado.frieren.FrierenIds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Continental Magic Association certification. Ranks gate which spell rarities can be deciphered, how far
 * mana capacity can grow through training, and how strong the player's mana detection is.
 */
public enum MageRank {
    UNRANKED("unranked", ChatFormatting.GRAY, 60, 1.00F, 0, 1, SpellRarity.COMMON),
    FIFTH_CLASS("fifth_class", ChatFormatting.WHITE, 110, 1.10F, 10, 2, SpellRarity.UNCOMMON),
    FOURTH_CLASS("fourth_class", ChatFormatting.GREEN, 170, 1.20F, 20, 3, SpellRarity.RARE),
    THIRD_CLASS("third_class", ChatFormatting.AQUA, 250, 1.35F, 35, 4, SpellRarity.RARE),
    SECOND_CLASS("second_class", ChatFormatting.GOLD, 380, 1.50F, 50, 5, SpellRarity.ANCIENT),
    FIRST_CLASS("first_class", ChatFormatting.LIGHT_PURPLE, 600, 1.75F, 80, 7, SpellRarity.LEGENDARY);

    private final String name;
    private final ChatFormatting color;
    private final int growthCap;
    private final float regenMultiplier;
    private final int maxManaBonus;
    private final int detectionPower;
    private final SpellRarity highestStandardRarity;

    MageRank(String name, ChatFormatting color, int growthCap, float regenMultiplier, int maxManaBonus,
             int detectionPower, SpellRarity highestStandardRarity) {
        this.name = name;
        this.color = color;
        this.growthCap = growthCap;
        this.regenMultiplier = regenMultiplier;
        this.maxManaBonus = maxManaBonus;
        this.detectionPower = detectionPower;
        this.highestStandardRarity = highestStandardRarity;
    }

    public String serializedName() {
        return name;
    }

    /** Maximum mana growth obtainable through ordinary training at this rank. */
    public int growthCap() {
        return growthCap;
    }

    public float regenMultiplier() {
        return regenMultiplier;
    }

    public int maxManaBonus() {
        return maxManaBonus;
    }

    /** Abstract detection strength compared against concealment levels (Serie's interview, concealed demons). */
    public int detectionPower() {
        return detectionPower;
    }

    public boolean canLearn(SpellRarity rarity) {
        return switch (rarity) {
            case DEMON -> this.ordinal() >= THIRD_CLASS.ordinal();
            case FORBIDDEN -> this == FIRST_CLASS;
            default -> rarity.ordinal() <= highestStandardRarity.ordinal();
        };
    }

    /** The lowest rank that can learn the given rarity. */
    public static MageRank requiredFor(SpellRarity rarity) {
        for (MageRank rank : values()) {
            if (rank.canLearn(rarity)) {
                return rank;
            }
        }
        return FIRST_CLASS;
    }

    public @Nullable MageRank next() {
        int next = ordinal() + 1;
        return next < values().length ? values()[next] : null;
    }

    public boolean atLeast(MageRank other) {
        return ordinal() >= other.ordinal();
    }

    public Component displayName() {
        return Component.translatable(FrierenIds.key("rank", name)).withStyle(color);
    }

    public static MageRank byName(String name) {
        for (MageRank rank : values()) {
            if (rank.name.equals(name)) {
                return rank;
            }
        }
        return UNRANKED;
    }
}
