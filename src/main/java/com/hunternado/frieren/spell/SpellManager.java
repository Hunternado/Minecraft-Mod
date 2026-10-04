package com.hunternado.frieren.spell;

import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.magic.SpellRarity;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads spell definitions from {@code data/<namespace>/frieren_spells/*.json} on the server and keeps the
 * client copy received over the network. The two copies are separate so an integrated server and its
 * client never share mutable state.
 */
public final class SpellManager extends SimpleJsonResourceReloadListener<SpellDefinition> {
    public static final String FOLDER = "frieren_spells";

    private static final Comparator<SpellDefinition> ORDER =
        Comparator.comparing((SpellDefinition def) -> def.rarity().ordinal()).thenComparing(def -> def.id().toString());

    private static volatile Map<Identifier, SpellDefinition> serverSpells = Map.of();
    private static volatile Map<Identifier, SpellDefinition> clientSpells = Map.of();

    public SpellManager(HolderLookup.Provider registries) {
        super(registries.createSerializationContext(JsonOps.INSTANCE), SpellDefinition.FILE_CODEC, FileToIdConverter.json(FOLDER));
    }

    @Override
    protected void apply(Map<Identifier, SpellDefinition> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<SpellDefinition> valid = new ArrayList<>();
        loaded.forEach((id, definition) -> {
            if (SpellBehaviors.get(definition.behavior()) == null) {
                FrierenMod.LOGGER.error("Spell {} references unknown behavior {}; it will not be available", id, definition.behavior());
                return;
            }
            valid.add(definition.withId(id));
        });
        valid.sort(ORDER);
        Map<Identifier, SpellDefinition> map = new LinkedHashMap<>();
        for (SpellDefinition definition : valid) {
            map.put(definition.id(), definition);
        }
        serverSpells = java.util.Collections.unmodifiableMap(map);
        FrierenMod.LOGGER.info("Loaded {} Frieren spell definitions", map.size());
    }

    // ---- Server side ------------------------------------------------------------------------

    public static @Nullable SpellDefinition server(Identifier id) {
        return serverSpells.get(id);
    }

    public static Collection<SpellDefinition> serverDefinitions() {
        return serverSpells.values();
    }

    public static @Nullable SpellDefinition randomOfRarity(SpellRarity rarity, RandomSource random) {
        List<SpellDefinition> candidates = new ArrayList<>();
        for (SpellDefinition definition : serverSpells.values()) {
            if (definition.rarity() == rarity) {
                candidates.add(definition);
            }
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    // ---- Client side ------------------------------------------------------------------------

    public static void setClientDefinitions(List<SpellDefinition> definitions) {
        List<SpellDefinition> sorted = new ArrayList<>(definitions);
        sorted.sort(ORDER);
        Map<Identifier, SpellDefinition> map = new LinkedHashMap<>();
        for (SpellDefinition definition : sorted) {
            map.put(definition.id(), definition);
        }
        clientSpells = java.util.Collections.unmodifiableMap(map);
    }

    public static @Nullable SpellDefinition client(Identifier id) {
        return clientSpells.get(id);
    }

    public static Collection<SpellDefinition> clientDefinitions() {
        return clientSpells.values();
    }

    /** For UI code that may run before the client copy arrives (e.g. creative tab in singleplayer). */
    public static Collection<SpellDefinition> clientOrServerDefinitions() {
        return clientSpells.isEmpty() ? serverSpells.values() : clientSpells.values();
    }

    // ---- Side-aware ------------------------------------------------------------------------

    public static @Nullable SpellDefinition get(Level level, Identifier id) {
        return level.isClientSide() ? client(id) : server(id);
    }
}
