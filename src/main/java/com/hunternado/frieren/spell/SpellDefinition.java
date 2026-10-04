package com.hunternado.frieren.spell;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.magic.SpellRarity;
import com.hunternado.frieren.magic.SpellSchool;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Data-driven spell metadata, loaded from {@code data/<namespace>/frieren_spells/<path>.json}.
 * The {@code behavior} names a code-backed {@link SpellBehavior}; {@code params} feed that behavior.
 *
 * <pre>{@code
 * {
 *   "behavior": "frieren:beam",
 *   "school": "offensive",
 *   "rarity": "common",
 *   "mana_cost": 10,
 *   "cooldown": 16,
 *   "cast_time": 0,
 *   "color": "#B8E0FF",
 *   "params": { "damage": 6.0, "range": 32.0 }
 * }
 * }</pre>
 */
public record SpellDefinition(
    Identifier id,
    Identifier behavior,
    SpellSchool school,
    SpellRarity rarity,
    float manaCost,
    float healthCost,
    int cooldown,
    int castTime,
    int maxChannelTicks,
    int color,
    Map<String, Double> params
) {
    /** JSON shape (without the id, which comes from the file path). */
    private record Data(Identifier behavior, SpellSchool school, SpellRarity rarity, float manaCost, float healthCost,
                        int cooldown, int castTime, int maxChannelTicks, String color, Map<String, Double> params) {}

    private static final Codec<Data> DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("behavior").forGetter(Data::behavior),
        SpellSchool.CODEC.fieldOf("school").forGetter(Data::school),
        SpellRarity.CODEC.fieldOf("rarity").forGetter(Data::rarity),
        Codec.floatRange(0.0F, 10000.0F).fieldOf("mana_cost").forGetter(Data::manaCost),
        Codec.floatRange(0.0F, 1000.0F).optionalFieldOf("health_cost", 0.0F).forGetter(Data::healthCost),
        Codec.intRange(0, 72000).optionalFieldOf("cooldown", 20).forGetter(Data::cooldown),
        Codec.intRange(0, 200).optionalFieldOf("cast_time", 0).forGetter(Data::castTime),
        Codec.intRange(0, 72000).optionalFieldOf("channel", 0).forGetter(Data::maxChannelTicks),
        Codec.STRING.optionalFieldOf("color", "#B8E0FF").forGetter(Data::color),
        Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("params", Map.of()).forGetter(Data::params)
    ).apply(instance, Data::new));

    /** Codec of the JSON file; the id is filled in by {@link SpellManager} from the file name. */
    public static final Codec<SpellDefinition> FILE_CODEC = DATA_CODEC.xmap(
        data -> new SpellDefinition(FrierenIds.id("unbound"), data.behavior(), data.school(), data.rarity(), data.manaCost(),
            data.healthCost(), data.cooldown(), data.castTime(), data.maxChannelTicks(), parseColor(data.color()), Map.copyOf(data.params())),
        def -> new Data(def.behavior(), def.school(), def.rarity(), def.manaCost(), def.healthCost(), def.cooldown(), def.castTime(),
            def.maxChannelTicks(), String.format("#%06X", def.color() & 0xFFFFFF), def.params())
    );

    public SpellDefinition withId(Identifier newId) {
        return new SpellDefinition(newId, behavior, school, rarity, manaCost, healthCost, cooldown, castTime, maxChannelTicks, color, params);
    }

    public boolean isChannelled() {
        return maxChannelTicks > 0;
    }

    public double param(String key, double fallback) {
        Double value = params.get(key);
        return value != null ? value : fallback;
    }

    public MageRank requiredRank() {
        return MageRank.requiredFor(rarity);
    }

    public String translationKey() {
        return "spell." + id.getNamespace() + "." + id.getPath();
    }

    public Component displayName() {
        return Component.translatable(translationKey()).withStyle(rarity.color());
    }

    public Component description() {
        return Component.translatable(translationKey() + ".desc");
    }

    // ---- Network (manual, so it never depends on codec/NBT helpers that move between versions) ----

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(id.toString());
        buf.writeUtf(behavior.toString());
        buf.writeVarInt(school.ordinal());
        buf.writeVarInt(rarity.ordinal());
        buf.writeFloat(manaCost);
        buf.writeFloat(healthCost);
        buf.writeVarInt(cooldown);
        buf.writeVarInt(castTime);
        buf.writeVarInt(maxChannelTicks);
        buf.writeInt(color);
        buf.writeVarInt(params.size());
        params.forEach((key, value) -> {
            buf.writeUtf(key);
            buf.writeDouble(value);
        });
    }

    public static SpellDefinition read(FriendlyByteBuf buf) {
        Identifier id = Identifier.parse(buf.readUtf());
        Identifier behavior = Identifier.parse(buf.readUtf());
        SpellSchool school = SpellSchool.values()[Math.floorMod(buf.readVarInt(), SpellSchool.values().length)];
        SpellRarity rarity = SpellRarity.values()[Math.floorMod(buf.readVarInt(), SpellRarity.values().length)];
        float manaCost = buf.readFloat();
        float healthCost = buf.readFloat();
        int cooldown = buf.readVarInt();
        int castTime = buf.readVarInt();
        int channel = buf.readVarInt();
        int color = buf.readInt();
        int count = buf.readVarInt();
        Map<String, Double> params = new HashMap<>();
        for (int i = 0; i < count; i++) {
            params.put(buf.readUtf(), buf.readDouble());
        }
        return new SpellDefinition(id, behavior, school, rarity, manaCost, healthCost, cooldown, castTime, channel, color, Map.copyOf(params));
    }

    private static int parseColor(String hex) {
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xB8E0FF;
        }
    }
}
