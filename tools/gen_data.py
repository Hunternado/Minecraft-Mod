#!/usr/bin/env python3
"""
Generates the data-driven JSON content of the mod (spells, loot tables, recipes, tags, damage types,
advancements, worldgen, biome modifiers) into src/main/resources/data.

Formats follow Minecraft 26.2 (checked against misode/mcmeta 26.2-data-json) and Forge 65.1.
Re-run after editing:  python3 tools/gen_data.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
DATA = os.path.join(ROOT, "data")
NS = "frieren"


def write(rel, obj):
    path = os.path.join(DATA, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ---------------------------------------------------------------------------------------------
# Spells: data/frieren/frieren_spells/<id>.json
# ---------------------------------------------------------------------------------------------
SPELLS = {
    # ---- Offensive ----
    "zoltraak": dict(behavior="beam", school="offensive", rarity="common", mana_cost=10, cooldown=16, color="#B8E0FF",
                     params=dict(damage=6.0, range=32.0, pierce=0, barrier_cost=1.0)),
    "zoltraak_barrage": dict(behavior="barrage", school="offensive", rarity="rare", mana_cost=8, cooldown=60, channel=40,
                             color="#A0D4FF", params=dict(damage=4.5, range=28.0, interval=4, pulse_cost=4.0, spread=5.0)),
    "qual_zoltraak": dict(behavior="beam", school="offensive", rarity="demon", mana_cost=22, cooldown=40, cast_time=8,
                          color="#9B2D6F", params=dict(damage=11.0, range=40.0, pierce=1, barrier_cost=0.33, demon_magic=1)),
    "vollzanbel": dict(behavior="flame_wave", school="offensive", rarity="rare", mana_cost=24, cooldown=80, cast_time=6,
                       color="#FF7A2A", params=dict(damage=8.0, range=8.0, angle=35.0, burn_seconds=5.0)),
    "judradjim": dict(behavior="lightning", school="offensive", rarity="rare", mana_cost=28, cooldown=100, cast_time=10,
                      color="#FFF2A0", params=dict(damage=11.0, range=40.0, radius=3.0)),
    "waldgose": dict(behavior="vortex", school="offensive", rarity="rare", mana_cost=32, cooldown=200, cast_time=10,
                     color="#C8F0E0", params=dict(damage=2.0, range=24.0, radius=4.5, duration=80)),
    "catastravia": dict(behavior="arrow_rain", school="offensive", rarity="ancient", mana_cost=60, cooldown=400, cast_time=20,
                        color="#FFFBE0", params=dict(damage=4.0, range=40.0, radius=6.0, duration=60)),
    "reelseiden": dict(behavior="slash", school="offensive", rarity="rare", mana_cost=18, cooldown=40,
                       color="#E8E8FF", params=dict(damage=9.0, range=5.0, angle=45.0, barrier_cost=3.0)),
    "diagolze": dict(behavior="gold_curse", school="offensive", rarity="forbidden", mana_cost=80, health_cost=4, cooldown=600,
                     cast_time=20, color="#FFD24A", params=dict(range=16.0, duration=120, boss_duration=20, damage=4.0)),
    # ---- Defensive ----
    "defensive_magic": dict(behavior="barrier", school="defensive", rarity="common", mana_cost=6, cooldown=20,
                            color="#9FD8FF", params=dict(upkeep=0.6, efficiency=1.5, melee_block=0.7)),
    "barrier_dome": dict(behavior="dome", school="defensive", rarity="uncommon", mana_cost=35, cooldown=400, cast_time=10,
                         color="#7FD4FF", params=dict(radius=5.0, duration=160)),
    "goddess_healing": dict(behavior="heal", school="defensive", rarity="uncommon", mana_cost=18, cooldown=60, cast_time=10,
                            color="#FFE9A8", params=dict(heal=6.0, range=12.0)),
    "curse_purification": dict(behavior="purify", school="defensive", rarity="uncommon", mana_cost=20, cooldown=200,
                               cast_time=10, color="#F6F2FF", params=dict(radius=6.0)),
    # ---- Utility ----
    "flight_magic": dict(behavior="flight", school="utility", rarity="uncommon", mana_cost=10, cooldown=40,
                         color="#E8F6FF", params=dict(drain_per_second=2.0)),
    "flower_field": dict(behavior="flower_field", school="utility", rarity="common", mana_cost=12, cooldown=200,
                         color="#FFB7D5", params=dict(radius=6.0, density=0.35, duration=200)),
    "mimic_detection": dict(behavior="mimic_detection", school="utility", rarity="common", mana_cost=6, cooldown=100,
                            color="#D9B8FF", params=dict(radius=20.0)),
    "clothes_cleaning": dict(behavior="mend", school="utility", rarity="uncommon", mana_cost=20, cooldown=600,
                             color="#C8FFF4", params=dict(repair=25.0)),
    "rust_removal": dict(behavior="rust_removal", school="utility", rarity="common", mana_cost=8, cooldown=40,
                         color="#E0A060", params=dict(range=6.0, radius=2.0)),
    "seeking_magic": dict(behavior="seek", school="utility", rarity="uncommon", mana_cost=15, cooldown=600, cast_time=20,
                          color="#A8FFB0", params=dict(search_radius=64.0)),
    "light_orb": dict(behavior="light_orb", school="utility", rarity="common", mana_cost=4, cooldown=20,
                      color="#FFF6C8", params=dict(range=16.0)),
    "gathering_magic": dict(behavior="gather", school="utility", rarity="common", mana_cost=5, cooldown=60,
                            color="#CDE8FF", params=dict(radius=10.0)),
    # ---- Special ----
    "azeryuze": dict(behavior="obedience", school="special", rarity="demon", mana_cost=50, cooldown=1200, cast_time=20,
                     color="#D4AF37", params=dict(range=16.0, duration=1200, ratio=6.0)),
    "lands_doppelganger": dict(behavior="decoy", school="special", rarity="rare", mana_cost=25, cooldown=400,
                               color="#9FD8FF", params=dict(duration=200, radius=16.0)),
}


def gen_spells():
    for sid, s in SPELLS.items():
        obj = {"behavior": f"{NS}:{s['behavior']}", "school": s["school"], "rarity": s["rarity"], "mana_cost": s["mana_cost"]}
        for key in ("health_cost", "cooldown", "cast_time", "channel", "color"):
            if key in s:
                obj[key] = s[key]
        obj["params"] = s["params"]
        write(f"{NS}/frieren_spells/{sid}.json", obj)


# ---------------------------------------------------------------------------------------------
# Loot tables
# ---------------------------------------------------------------------------------------------
def item(name, weight=1, count=None, functions=None, conditions=None):
    entry = {"type": "minecraft:item", "name": name}
    fns = list(functions or [])
    if count is not None:
        lo, hi = count
        fns.insert(0, {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": float(lo), "max": float(hi)}})
    if fns:
        entry["functions"] = fns
    if conditions:
        entry["conditions"] = conditions
    if weight != 1:
        entry["weight"] = weight
    return entry


def grimoire_of(spell, rarity, weight=1):
    return item(f"{NS}:{rarity}_grimoire", weight, functions=[
        {"function": "minecraft:set_components", "components": {f"{NS}:grimoire_spell": f"{NS}:{spell}"}}])


def empty(weight):
    return {"type": "minecraft:empty", "weight": weight}


def pool(entries, rolls=1, rolls_range=None, conditions=None):
    p = {"rolls": {"type": "minecraft:uniform", "min": float(rolls_range[0]), "max": float(rolls_range[1])} if rolls_range else float(rolls),
         "entries": entries, "bonus_rolls": 0.0}
    if conditions:
        p["conditions"] = conditions
    return p


KILLED_BY_PLAYER = [{"condition": "minecraft:killed_by_player"}]


def looting(fn_entries):
    return fn_entries


def gen_loot():
    # ---- Blocks ----
    def self_drop(block):
        write(f"{NS}/loot_table/blocks/{block}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{block}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{NS}:blocks/{block}"})

    for b in ("mana_crystal_block", "spell_research_desk", "summoning_altar", "cursed_gold_block", "blue_moon_weed"):
        self_drop(b)

    for ore in ("mana_crystal_ore", "deepslate_mana_crystal_ore"):
        write(f"{NS}/loot_table/blocks/{ore}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{
                "type": "minecraft:alternatives",
                "children": [
                    {"type": "minecraft:item", "name": f"{NS}:{ore}", "conditions": [{
                        "condition": "minecraft:match_tool",
                        "predicate": {"predicates": {"minecraft:enchantments": [
                            {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                    {"type": "minecraft:item", "name": f"{NS}:mana_crystal", "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1.0, "max": 2.0}},
                        {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                        {"function": "minecraft:explosion_decay"}]}]}]}],
            "random_sequence": f"{NS}:blocks/{ore}"})

    # ---- Entities ----
    def entity(name, pools):
        write(f"{NS}/loot_table/entities/{name}.json", {"type": "minecraft:entity", "pools": pools,
                                                         "random_sequence": f"{NS}:entities/{name}"})

    entity("demon_soldier", [
        pool([item(f"{NS}:demon_horn", count=(0, 2))]),
        pool([item(f"{NS}:mana_crystal", 3), empty(7)], conditions=KILLED_BY_PLAYER),
        pool([item(f"{NS}:sealed_demon_grimoire"), empty(39)], conditions=KILLED_BY_PLAYER),
    ])
    entity("demon_mage", [
        pool([item(f"{NS}:demon_horn", count=(1, 2))]),
        pool([item(f"{NS}:demonic_mana_core", 1), empty(4)], conditions=KILLED_BY_PLAYER),
        pool([item(f"{NS}:mana_crystal", count=(1, 3))]),
        pool([item(f"{NS}:sealed_demon_grimoire"), empty(14)], conditions=KILLED_BY_PLAYER),
    ])
    entity("aura_thrall", [pool([item("minecraft:iron_ingot", count=(0, 2)), item("minecraft:rotten_flesh", count=(0, 2))])])
    entity("mirror_replica", [pool([item(f"{NS}:mirror_shard", 1), empty(3)])])
    entity("mimic", [
        pool([item("minecraft:gold_ingot", 3, count=(2, 6)), item("minecraft:emerald", 2, count=(2, 5)),
              item(f"{NS}:mana_crystal", 3, count=(2, 4))], rolls_range=(2, 3)),
        pool([item(f"{NS}:uncommon_grimoire", 3), item(f"{NS}:rare_grimoire", 1)]),
    ])
    entity("aura", [
        pool([item(f"{NS}:guillotine_blade")]),
        pool([grimoire_of("azeryuze", "demon")]),
        pool([item(f"{NS}:legendary_grimoire")]),
        pool([item(f"{NS}:demonic_mana_core", count=(3, 5))]),
    ])
    entity("qual", [
        pool([item(f"{NS}:sealed_demon_grimoire", functions=[
            {"function": "minecraft:set_components", "components": {f"{NS}:grimoire_spell": f"{NS}:qual_zoltraak"}}])]),
        pool([item(f"{NS}:demonic_mana_core", count=(2, 3))]),
        pool([item(f"{NS}:rare_grimoire")]),
    ])
    entity("spiegel", [
        pool([item(f"{NS}:ancient_grimoire")]),
        pool([item(f"{NS}:mirror_shard", count=(2, 4))]),
        pool([item(f"{NS}:mana_crystal", count=(4, 8))]),
    ])
    # NPCs and constructs drop nothing.
    for name in ("wandering_mage", "examiner", "priest", "frieren", "fern", "stark", "serie", "stille", "mana_decoy"):
        entity(name, [])

    # ---- Chests ----
    def chest(name, pools):
        write(f"{NS}/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": pools,
                                                       "random_sequence": f"{NS}:chests/{name}"})

    common_junk = [item("minecraft:book", 10, count=(1, 3)), item("minecraft:paper", 10, count=(2, 6)),
                   item("minecraft:candle", 6, count=(1, 3)), item("minecraft:glass_bottle", 6, count=(1, 3)),
                   item(f"{NS}:mana_crystal", 8, count=(1, 4)), item("minecraft:emerald", 4, count=(1, 3)),
                   item(f"{NS}:mana_potion", 5)]
    chest("mage_tower", [
        pool(common_junk, rolls_range=(3, 6)),
        pool([item(f"{NS}:common_grimoire", 10), item(f"{NS}:uncommon_grimoire", 6), item(f"{NS}:rare_grimoire", 2)], rolls_range=(1, 2)),
        pool([item(f"{NS}:apprentice_staff", 3), item(f"{NS}:mage_hat", 2), item(f"{NS}:mage_boots", 2), empty(6)]),
    ])
    chest("forgotten_library", [
        pool(common_junk, rolls_range=(4, 7)),
        pool([item(f"{NS}:uncommon_grimoire", 8), item(f"{NS}:rare_grimoire", 4), item(f"{NS}:ancient_grimoire", 1)], rolls_range=(1, 3)),
        pool([item(f"{NS}:sealed_exam_grimoire")]),
        pool([grimoire_of("mimic_detection", "common", 3), grimoire_of("light_orb", "common", 3),
              grimoire_of("seeking_magic", "uncommon", 2), item(f"{NS}:concealment_charm", 1), empty(4)]),
    ])
    chest("demon_camp", [
        pool([item(f"{NS}:demon_horn", 8, count=(1, 4)), item("minecraft:bone", 8, count=(2, 6)),
              item("minecraft:iron_ingot", 5, count=(1, 4)), item("minecraft:gold_ingot", 3, count=(1, 3)),
              item(f"{NS}:mana_crystal", 5, count=(1, 3))], rolls_range=(3, 6)),
        pool([item(f"{NS}:sealed_demon_grimoire", 2), item(f"{NS}:demonic_mana_core", 3), empty(5)]),
    ])
    chest("association_outpost", [
        pool(common_junk, rolls_range=(2, 4)),
        pool([item(f"{NS}:mana_potion", 4, count=(1, 3)), item(f"{NS}:mage_coat", 1), item(f"{NS}:mage_trousers", 1),
              item(f"{NS}:spell_research_desk", 1), item(f"{NS}:mirror_shard", 1)]),
    ])
    chest("sealed_shrine", [
        pool(common_junk, rolls_range=(2, 4)),
        pool([item(f"{NS}:seal_of_corruption")]),
        pool([item(f"{NS}:rare_grimoire", 4), item(f"{NS}:ancient_grimoire", 1), item(f"{NS}:mage_staff", 2)]),
    ])
    chest("kings_tomb", [
        pool([item("minecraft:gold_ingot", 8, count=(2, 6)), item("minecraft:diamond", 2, count=(1, 2)),
              item(f"{NS}:mana_crystal", 8, count=(2, 6)), item("minecraft:emerald", 5, count=(2, 6))], rolls_range=(3, 6)),
        pool([item(f"{NS}:rare_grimoire", 6), item(f"{NS}:ancient_grimoire", 3), item(f"{NS}:legendary_grimoire", 1)], rolls_range=(1, 2)),
        pool([item(f"{NS}:mirror_shard", count=(1, 2))]),
        pool([item(f"{NS}:forbidden_grimoire", functions=[
            {"function": "minecraft:set_components", "components": {f"{NS}:grimoire_spell": f"{NS}:diagolze"}}]), empty(9)]),
    ])
    chest("aura_fortress", [
        pool([item("minecraft:gold_ingot", 8, count=(3, 8)), item("minecraft:diamond", 3, count=(1, 3)),
              item(f"{NS}:demonic_mana_core", 5, count=(1, 3)), item(f"{NS}:demon_horn", 6, count=(2, 5))], rolls_range=(4, 7)),
        pool([item(f"{NS}:sigil_of_the_guillotine")]),
        pool([item(f"{NS}:ancient_grimoire", 4), item(f"{NS}:legendary_grimoire", 1), item(f"{NS}:sages_staff", 2)]),
    ])
    chest("heros_statue", [
        pool([item("minecraft:bread", 6, count=(1, 3)), item(f"{NS}:blue_moon_weed", 4, count=(1, 2)),
              item("minecraft:copper_ingot", 4, count=(2, 5)), item(f"{NS}:mana_potion", 2)], rolls_range=(2, 4)),
        pool([grimoire_of("rust_removal", "common", 3), grimoire_of("flower_field", "common", 3)]),
    ])


# ---------------------------------------------------------------------------------------------
# Recipes
# ---------------------------------------------------------------------------------------------
def shaped(name, pattern, key, result, count=1, category="misc"):
    obj = {"type": "minecraft:crafting_shaped", "category": category, "key": key, "pattern": pattern, "result": {"id": result}}
    if count != 1:
        obj["result"]["count"] = count
    write(f"{NS}/recipe/{name}.json", obj)


def shapeless(name, ingredients, result, count=1, category="misc"):
    obj = {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients, "result": {"id": result}}
    if count != 1:
        obj["result"]["count"] = count
    write(f"{NS}/recipe/{name}.json", obj)


def gen_recipes():
    C = f"{NS}:mana_crystal"
    shaped("mana_crystal_block", ["###", "###", "###"], {"#": C}, f"{NS}:mana_crystal_block", category="building")
    shapeless("mana_crystal_from_block", [f"{NS}:mana_crystal_block"], C, count=9)
    shaped("apprentice_staff", ["  C", " S ", "S  "], {"C": C, "S": "minecraft:stick"}, f"{NS}:apprentice_staff", category="equipment")
    shaped("mage_staff", [" GC", " SG", "S  "], {"C": f"{NS}:mana_crystal_block", "S": "minecraft:stick", "G": "minecraft:gold_ingot"},
           f"{NS}:mage_staff", category="equipment")
    shaped("sages_staff", [" DC", " SD", "S  "], {"C": f"{NS}:demonic_mana_core", "S": f"{NS}:mage_staff", "D": "minecraft:diamond"},
           f"{NS}:sages_staff", category="equipment")
    shaped("guillotine_staff", [" BC", " SB", "N  "], {"B": f"{NS}:guillotine_blade", "C": f"{NS}:demonic_mana_core",
                                                      "S": f"{NS}:sages_staff", "N": "minecraft:netherite_ingot"},
           f"{NS}:guillotine_staff", category="equipment")
    cloth = "#minecraft:wool"
    shaped("mage_hat", [" W ", "WCW"], {"W": cloth, "C": C}, f"{NS}:mage_hat", category="equipment")
    shaped("mage_coat", ["W W", "WCW", "WWW"], {"W": cloth, "C": C}, f"{NS}:mage_coat", category="equipment")
    shaped("mage_trousers", ["WCW", "W W", "W W"], {"W": cloth, "C": C}, f"{NS}:mage_trousers", category="equipment")
    shaped("mage_boots", ["W W", "LCL"], {"W": cloth, "L": "minecraft:leather", "C": C}, f"{NS}:mage_boots", category="equipment")
    shapeless("mana_potion", ["minecraft:glass_bottle", C, C, "minecraft:sweet_berries"], f"{NS}:mana_potion")
    shapeless("mana_potion_blue_moon", ["minecraft:glass_bottle", C, f"{NS}:blue_moon_weed"], f"{NS}:mana_potion", count=2)
    shaped("spell_research_desk", ["BCB", "PPP", "P P"], {"B": "minecraft:book", "C": C, "P": "#minecraft:planks"},
           f"{NS}:spell_research_desk", category="building")
    shaped("summoning_altar", ["OCO", "ODO", "OOO"], {"O": "minecraft:crying_obsidian", "C": f"{NS}:demonic_mana_core", "D": "minecraft:diamond"},
           f"{NS}:summoning_altar", category="building")
    shaped("sigil_of_the_guillotine", ["HCH", "CGC", "HCH"], {"H": f"{NS}:demon_horn", "C": f"{NS}:demonic_mana_core", "G": "minecraft:gold_block"},
           f"{NS}:sigil_of_the_guillotine")
    shaped("seal_of_corruption", ["HCH", "CEC", "HCH"], {"H": f"{NS}:demon_horn", "C": f"{NS}:demonic_mana_core", "E": "minecraft:ender_eye"},
           f"{NS}:seal_of_corruption")
    shaped("mirror_shard", [" G ", "GCG", " G "], {"G": "minecraft:glass_pane", "C": C}, f"{NS}:mirror_shard")
    shaped("concealment_charm", ["SAS", "ACA", "SAS"], {"S": "minecraft:string", "A": "minecraft:amethyst_shard", "C": f"{NS}:mana_crystal_block"},
           f"{NS}:concealment_charm")
    shaped("stille_cage", ["III", "I I", "III"], {"I": "minecraft:iron_bars"}, f"{NS}:stille_cage")
    shapeless("journey_journal", ["minecraft:book", C], f"{NS}:journey_journal")


# ---------------------------------------------------------------------------------------------
# Tags, damage types
# ---------------------------------------------------------------------------------------------
def tag(rel, values, replace=False):
    write(rel + ".json", {"replace": replace, "values": values})


STRUCTURES = ["mage_tower", "forgotten_library", "demon_camp", "association_outpost", "heros_statue", "sealed_shrine",
              "kings_tomb", "aura_fortress"]


def gen_tags():
    tag(f"{NS}/tags/item/repairs_mage_attire", [f"{NS}:mana_crystal"])
    tag(f"{NS}/tags/item/grimoires", [f"{NS}:{r}_grimoire" for r in ("common", "uncommon", "rare", "ancient", "legendary", "demon", "forbidden")]
        + [f"{NS}:sealed_demon_grimoire"])
    tag(f"{NS}/tags/item/staffs", [f"{NS}:apprentice_staff", f"{NS}:mage_staff", f"{NS}:sages_staff", f"{NS}:guillotine_staff"])
    tag("minecraft/tags/item/head_armor", [f"{NS}:mage_hat"])
    tag("minecraft/tags/item/chest_armor", [f"{NS}:mage_coat"])
    tag("minecraft/tags/item/leg_armor", [f"{NS}:mage_trousers"])
    tag("minecraft/tags/item/foot_armor", [f"{NS}:mage_boots"])
    tag("minecraft/tags/block/mineable/pickaxe", [f"{NS}:mana_crystal_ore", f"{NS}:deepslate_mana_crystal_ore", f"{NS}:mana_crystal_block",
                                                    f"{NS}:summoning_altar", f"{NS}:cursed_gold_block"])
    tag("minecraft/tags/block/mineable/axe", [f"{NS}:spell_research_desk"])
    tag("minecraft/tags/block/needs_iron_tool", [f"{NS}:mana_crystal_ore", f"{NS}:deepslate_mana_crystal_ore"])
    tag("minecraft/tags/block/needs_diamond_tool", [f"{NS}:summoning_altar"])
    tag("minecraft/tags/block/small_flowers", [f"{NS}:blue_moon_weed"])
    tag("minecraft/tags/item/small_flowers", [f"{NS}:blue_moon_weed"])
    tag(f"{NS}/tags/entity_type/demons", [f"{NS}:demon_soldier", f"{NS}:demon_mage", f"{NS}:aura", f"{NS}:qual"])
    tag("minecraft/tags/damage_type/bypasses_armor", [f"{NS}:demon_magic", f"{NS}:curse"])
    tag("minecraft/tags/damage_type/is_projectile", [f"{NS}:zoltraak"])
    tag("minecraft/tags/damage_type/witch_resistant_to", [f"{NS}:spell", f"{NS}:zoltraak"])

    # Structure tags used by code (seeking magic, exam seal).
    tag(f"{NS}/tags/worldgen/structure/seekable", [f"{NS}:{s}" for s in STRUCTURES])
    tag(f"{NS}/tags/worldgen/structure/exam_library", [f"{NS}:forgotten_library"])
    tag(f"{NS}/tags/worldgen/structure/exam_labyrinth", [f"{NS}:kings_tomb"])

    biomes = {
        "mage_tower": ["#minecraft:is_forest", "minecraft:plains", "minecraft:meadow", "minecraft:cherry_grove", "minecraft:taiga"],
        "forgotten_library": ["#minecraft:is_forest", "minecraft:plains", "minecraft:taiga", "minecraft:savanna", "minecraft:desert"],
        "demon_camp": ["minecraft:plains", "minecraft:savanna", "minecraft:taiga", "minecraft:snowy_plains", "minecraft:sunflower_plains"],
        "association_outpost": ["minecraft:plains", "minecraft:meadow", "minecraft:sunflower_plains", "minecraft:forest", "minecraft:flower_forest"],
        "heros_statue": ["minecraft:plains", "minecraft:meadow", "minecraft:flower_forest", "minecraft:sunflower_plains"],
        "sealed_shrine": ["#minecraft:is_mountain", "minecraft:taiga", "minecraft:old_growth_spruce_taiga", "minecraft:windswept_hills"],
        "kings_tomb": ["#minecraft:is_overworld"],
        "aura_fortress": ["minecraft:plains", "minecraft:savanna", "minecraft:sunflower_plains", "minecraft:windswept_savanna"],
    }
    for s, values in biomes.items():
        tag(f"{NS}/tags/worldgen/biome/has_structure/{s}", values)


def gen_damage_types():
    for name, exhaustion in (("spell", 0.0), ("zoltraak", 0.1), ("demon_magic", 0.1), ("curse", 0.0), ("guillotine", 0.1)):
        write(f"{NS}/damage_type/{name}.json", {"exhaustion": exhaustion, "message_id": f"{NS}.{name}",
                                                "scaling": "when_caused_by_living_non_player"})


# ---------------------------------------------------------------------------------------------
# Advancements
# ---------------------------------------------------------------------------------------------
def adv(name, icon, criteria, parent=None, frame=None, hidden=False, background=None, toast=True, chat=True):
    display = {"icon": {"id": icon}, "title": {"translate": f"advancements.{NS}.{name}.title"},
               "description": {"translate": f"advancements.{NS}.{name}.description"}}
    if frame:
        display["frame"] = frame
    if hidden:
        display["hidden"] = True
    if background:
        display["background"] = background
    if not toast:
        display["show_toast"] = False
    if not chat:
        display["announce_to_chat"] = False
    obj = {"criteria": criteria, "display": display, "requirements": [list(criteria.keys())]}
    if parent:
        obj["parent"] = f"{NS}:{parent}"
    write(f"{NS}/advancement/{name}.json", obj)


def milestone(m):
    return {"m": {"trigger": f"{NS}:milestone", "conditions": {"milestone": m}}}


def has_item(i):
    return {"has": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": i}]}}}


def gen_advancements():
    adv("root", f"{NS}:journey_journal", has_item(f"{NS}:journey_journal"), background="minecraft:gui/advancements/backgrounds/stone",
        toast=False, chat=False)
    adv("first_spell", f"{NS}:common_grimoire", milestone("first_spell"), parent="root")
    adv("rank_fifth_class", f"{NS}:apprentice_staff", milestone("rank_fifth_class"), parent="first_spell")
    adv("rank_fourth_class", f"{NS}:mage_staff", milestone("rank_fourth_class"), parent="rank_fifth_class")
    adv("rank_third_class", f"{NS}:mage_hat", milestone("rank_third_class"), parent="rank_fourth_class")
    adv("rank_second_class", f"{NS}:sages_staff", milestone("rank_second_class"), parent="rank_third_class", frame="goal")
    adv("rank_first_class", f"{NS}:legendary_grimoire", milestone("rank_first_class"), parent="rank_second_class", frame="challenge")
    adv("learned_demon", f"{NS}:demon_grimoire", milestone("learned_demon"), parent="first_spell", frame="goal")
    adv("learned_forbidden", f"{NS}:forbidden_grimoire", milestone("learned_forbidden"), parent="learned_demon", frame="challenge", hidden=True)
    adv("slay_demon", f"{NS}:demon_horn", {"kill": {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": f"#{NS}:demons"}}]}}},
        parent="root")
    adv("aura_obeyed", f"{NS}:guillotine_blade", milestone("aura_obeyed"), parent="slay_demon", frame="challenge", hidden=True)
    adv("quest_aura_kill_yourself", f"{NS}:sigil_of_the_guillotine", milestone("quest_aura_kill_yourself"), parent="slay_demon", frame="challenge")
    adv("quest_mimic_hunter", f"{NS}:mimic_spawn_egg", milestone("quest_mimic_hunter"), parent="first_spell")
    adv("quest_the_heros_statue", f"{NS}:mirror_lotus_ring", milestone("quest_the_heros_statue"), parent="first_spell", frame="goal")
    adv("quest_a_field_of_flowers", f"{NS}:blue_moon_weed", milestone("quest_a_field_of_flowers"), parent="first_spell")


# ---------------------------------------------------------------------------------------------
# Worldgen: ore, structures, biome modifiers
# ---------------------------------------------------------------------------------------------
def gen_worldgen():
    write(f"{NS}/worldgen/configured_feature/mana_crystal_ore.json", {
        "type": "minecraft:ore",
        "config": {"discard_chance_on_air_exposure": 0.0, "size": 6, "targets": [
            {"state": {"Name": f"{NS}:mana_crystal_ore"},
             "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
            {"state": {"Name": f"{NS}:deepslate_mana_crystal_ore"},
             "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}}]}})
    write(f"{NS}/worldgen/placed_feature/mana_crystal_ore.json", {
        "feature": f"{NS}:mana_crystal_ore",
        "placement": [
            {"type": "minecraft:count", "count": 7},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid",
                                                           "min_inclusive": {"absolute": -48}, "max_inclusive": {"absolute": 48}}},
            {"type": "minecraft:biome"}]})
    write(f"{NS}/forge/biome_modifier/mana_crystal_ore.json", {
        "type": "forge:add_features", "biomes": "#minecraft:is_overworld", "features": f"{NS}:mana_crystal_ore",
        "step": "underground_ores"})
    write(f"{NS}/forge/biome_modifier/demon_spawns.json", {
        "type": "forge:add_spawns",
        "biomes": ["minecraft:plains", "minecraft:savanna", "minecraft:taiga", "minecraft:forest", "minecraft:dark_forest",
                   "minecraft:windswept_hills", "minecraft:snowy_plains", "minecraft:sunflower_plains", "minecraft:old_growth_spruce_taiga"],
        "spawners": [
            {"type": f"{NS}:demon_soldier", "weight": 8, "minCount": 1, "maxCount": 2},
            {"type": f"{NS}:demon_mage", "weight": 4, "minCount": 1, "maxCount": 1}]})

    # (name, size_config, surface?, separation, spacing, salt, terrain, start_height)
    defs = {
        "mage_tower": dict(spacing=36, separation=12, salt=73810451),
        "forgotten_library": dict(spacing=42, separation=14, salt=91827364, terrain="bury"),
        "demon_camp": dict(spacing=32, separation=10, salt=55102938),
        "association_outpost": dict(spacing=40, separation=14, salt=66123987),
        "heros_statue": dict(spacing=48, separation=16, salt=12093847),
        "sealed_shrine": dict(spacing=50, separation=16, salt=33440912),
        "kings_tomb": dict(spacing=56, separation=20, salt=88231904, underground=True),
        "aura_fortress": dict(spacing=80, separation=28, salt=44120987),
    }
    for name, d in defs.items():
        underground = d.get("underground", False)
        structure = {
            "type": "minecraft:jigsaw",
            "biomes": f"#{NS}:has_structure/{name}",
            "max_distance_from_center": 80,
            "size": 1,
            "spawn_overrides": {},
            "start_pool": f"{NS}:{name}/start",
            "step": "underground_structures" if underground else "surface_structures",
            "terrain_adaptation": "encapsulate" if underground else d.get("terrain", "beard_thin"),
            "use_expansion_hack": False,
        }
        if underground:
            structure["start_height"] = {"type": "minecraft:uniform", "min_inclusive": {"absolute": -30}, "max_inclusive": {"absolute": 10}}
        else:
            structure["project_start_to_heightmap"] = "WORLD_SURFACE_WG"
            # Surface templates keep their floor (y=0) in the ground layer; buried ones sink deeper.
            structure["start_height"] = {"absolute": -4 if d.get("terrain") == "bury" else -1}
        write(f"{NS}/worldgen/structure/{name}.json", structure)
        write(f"{NS}/worldgen/structure_set/{name}.json", {
            "placement": {"type": "minecraft:random_spread", "salt": d["salt"], "separation": d["separation"], "spacing": d["spacing"]},
            "structures": [{"structure": f"{NS}:{name}", "weight": 1}]})
        write(f"{NS}/worldgen/template_pool/{name}/start.json", {
            "elements": [{"element": {"element_type": "minecraft:single_pool_element", "location": f"{NS}:{name}",
                                      "processors": "minecraft:empty", "projection": "rigid"}, "weight": 1}],
            "fallback": "minecraft:empty"})


def main():
    gen_spells()
    gen_loot()
    gen_recipes()
    gen_tags()
    gen_damage_types()
    gen_advancements()
    gen_worldgen()
    print("data generated in", os.path.normpath(DATA))


if __name__ == "__main__":
    main()
