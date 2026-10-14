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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Loads spell definitions from {@code data/<namespace>/frieren_spells/*.json} on the server and keeps the
 * client copy received over the network. The two copies are separate so an integrated server and its
 * client never share mutable state.
 * <p>
 * Original magic (spells Serie built for one player) lives beside the data-pack spells in separate maps: the
 * server registers a player's originals when they log in or receive a new one, the client takes its own from the
 * synced player data. {@link #server(Identifier)} and {@link #client(Identifier)} look in both; the listings
 * ({@link #serverDefinitions()} and friends) only ever contain data-pack spells, so loot, privileges and creative
 * tabs never hand out somebody's personal spell.
 */
public final class SpellManager extends SimpleJsonResourceReloadListener<SpellDefinition> {
    public static final String FOLDER = "frieren_spells";

    private static final Comparator<SpellDefinition> ORDER =
        Comparator.comparing((SpellDefinition def) -> def.rarity().ordinal()).thenComparing(def -> def.id().toString());

    private static volatile Map<Identifier, SpellDefinition> serverSpells = Map.of();
    private static volatile Map<Identifier, SpellDefinition> clientSpells = Map.of();
    private static final Map<Identifier, SpellDefinition> SERVER_ORIGINALS = new ConcurrentHashMap<>();
    private static volatile Map<Identifier, SpellDefinition> clientOriginals = Map.of();

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
        SpellDefinition definition = serverSpells.get(id);
        return definition != null ? definition : SERVER_ORIGINALS.get(id);
    }

    /** Registers (or replaces) a player's original spells on the server. */
    public static void registerServerOriginals(Collection<SpellDefinition> originals) {
        for (SpellDefinition definition : originals) {
            SERVER_ORIGINALS.put(definition.id(), definition);
        }
    }

    public static void clearServerOriginals() {
        SERVER_ORIGINALS.clear();
    }

    public static Collection<SpellDefinition> serverDefinitions() {
        return serverSpells.values();
    }

    public static @Nullable SpellDefinition randomOfRarity(SpellRarity rarity, RandomSource random) {
        return randomOfRarity(rarity, random, definition -> true);
    }

    /**
     * A random loot-eligible spell of the rarity, chosen among those matching {@code prefer} when any do (an
     * unidentified grimoire favours spells its reader does not know yet).
     */
    public static @Nullable SpellDefinition randomOfRarity(SpellRarity rarity, RandomSource random,
                                                          Predicate<SpellDefinition> prefer) {
        List<SpellDefinition> candidates = new ArrayList<>();
        List<SpellDefinition> preferred = new ArrayList<>();
        for (SpellDefinition definition : serverSpells.values()) {
            if (definition.rarity() == rarity && !definition.isTaught()) {
                candidates.add(definition);
                if (prefer.test(definition)) {
                    preferred.add(definition);
                }
            }
        }
        List<SpellDefinition> pool = preferred.isEmpty() ? candidates : preferred;
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
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
        SpellDefinition definition = clientSpells.get(id);
        return definition != null ? definition : clientOriginals.get(id);
    }

    /** The local player's original spells, from their synced data. */
    public static void setClientOriginals(Collection<SpellDefinition> originals) {
        Map<Identifier, SpellDefinition> map = new LinkedHashMap<>();
        for (SpellDefinition definition : originals) {
            map.put(definition.id(), definition);
        }
        clientOriginals = java.util.Collections.unmodifiableMap(map);
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
