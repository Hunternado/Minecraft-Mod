#!/usr/bin/env python3
"""
The Frieren-style building set, generated from one spec: plaster and half-timbering, dark timber beams, roof tiles,
town cobbles, Association stone, elven ruins, the Demon King's stone, leaded glass, mana lanterns, window flower
boxes, grimoire shelves and the Association carpet - with stairs, slabs and walls where builders want them.

Writes:
  * src/main/java/.../registry/ModBuildingBlocks.java     (GENERATED: blocks and block items)
  * assets: blockstates, block/item models, item definitions, textures (tools/building_art.py)
  * data: loot tables, recipes (crafting + stonecutting)
Tags and names are merged by tools/gen_data.py and tools/gen_lang.py from BLOCK_TAGS / NAMES here.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import building_art as art  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(REPO, "src/main/resources/assets/frieren")
DATA = os.path.join(REPO, "src/main/resources/data/frieren")
JAVA = os.path.join(REPO, "src/main/java/com/hunternado/frieren/registry/ModBuildingBlocks.java")
TEMPLATES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "building_templates")
NS = "frieren"

# Java property presets: (map colour, strength, resistance, sound, extra builder calls)
PROPS = {
    "plaster": ("TERRACOTTA_WHITE", 1.2, 3.0, "STONE", ".requiresCorrectToolForDrops()"),
    "timber": ("COLOR_BROWN", 2.0, 3.0, "WOOD", ".ignitedByLava()"),
    "tile": (None, 1.5, 6.0, "DEEPSLATE_TILES", ".requiresCorrectToolForDrops()"),
    "stone": (None, 1.5, 6.0, "STONE", ".requiresCorrectToolForDrops()"),
    "pale_stone": ("QUARTZ", 1.5, 6.0, "CALCITE", ".requiresCorrectToolForDrops()"),
    "dark_stone": ("COLOR_BLACK", 2.0, 6.0, "DEEPSLATE_BRICKS", ".requiresCorrectToolForDrops()"),
    "glass": ("NONE", 0.3, 0.3, "GLASS", ".noOcclusion().isValidSpawn((s, l, p, t) -> false)"
              ".isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)"
              ".isViewBlocking((s, l, p) -> false)"),
    "lantern": ("METAL", 3.5, 3.5, "LANTERN", ".lightLevel(state -> 15).noOcclusion().forceSolidOn()"
                ".pushReaction(PushReaction.DESTROY)"),
    "wood_decor": ("WOOD", 1.5, 1.5, "WOOD", ".ignitedByLava()"),
    "box": ("WOOD", 1.0, 1.0, "WOOD", ".noOcclusion().ignitedByLava()"),
    "carpet": ("COLOR_BLUE", 0.1, 0.1, "WOOL", ".ignitedByLava()"),
}


class B:
    """One block of the set."""

    def __init__(self, id, name, kind, props, textures, color=None, tools=("pickaxe",), base=None, recipe=None,
                 cut_from=None):
        self.id, self.name, self.kind, self.props = id, name, kind, props
        self.textures, self.color, self.tools, self.base = textures, color, tools, base
        self.recipe, self.cut_from = recipe, cut_from


SET = []
TEXTURES = {}   # texture name -> painter


def tex(name, painter):
    TEXTURES[name] = painter
    return f"{NS}:block/{name}"


def family(id, name, props, texture, shapes, color, tools=("pickaxe",), recipe=None, cut_from=None):
    """A base block plus its stairs/slab/wall, all stonecuttable from the base."""
    SET.append(B(id, name, "block", props, {"all": texture}, color, tools, recipe=recipe, cut_from=cut_from))
    singular = name[:-1] if name.endswith("Bricks") or name.endswith("Tiles") else name
    for shape in shapes:
        label = {"stairs": "Stairs", "slab": "Slab", "wall": "Wall"}[shape]
        SET.append(B(f"{id.removesuffix('s') if id.endswith(('bricks', 'tiles')) else id}_{shape}",
                     f"{singular} {label}", shape, props, {"all": texture}, color, tools, base=id))


# --- plaster and timber
family("whitewashed_plaster", "Whitewashed Plaster", "plaster", tex("whitewashed_plaster", art.plaster),
       ("stairs", "slab", "wall"), None, recipe=("shapeless", ["minecraft:clay_ball", "minecraft:sand", "minecraft:bone_meal"], 4))
for key, label, painter in (("frame", "Framed", art.timber_frame), ("cross", "Crossed", art.timber_cross),
                            ("diagonal", "Braced", art.timber_diagonal), ("posts", "Posted", art.timber_posts)):
    SET.append(B(f"half_timber_{key}", f"{label} Half-Timbering", "block", "plaster",
                 {"all": tex(f"half_timber_{key}", painter)}, None, ("pickaxe", "axe"),
                 cut_from="whitewashed_plaster"))
SET.append(B("dark_timber", "Dark Timber Beam", "pillar", "timber",
             {"side": tex("dark_timber", art.timber_side), "end": tex("dark_timber_top", art.timber_end)}, None,
             ("axe",), recipe=("shaped", ["L", "L"], {"L": "minecraft:stripped_dark_oak_log"}, 2)))

# --- roofs and streets
for key, label, color, mc, src in (("terracotta", "Terracotta", "#B8552E", "TERRACOTTA_ORANGE", "minecraft:terracotta"),
                                  ("slate", "Slate", "#4E5A6E", "COLOR_GRAY", "minecraft:cobbled_deepslate"),
                                  ("verdigris", "Verdigris", "#4F9A86", "COLOR_CYAN", "minecraft:cyan_terracotta")):
    family(f"{key}_roof_tiles", f"{label} Roof Tiles", "tile", tex(f"{key}_roof_tiles", lambda c=color: art.roof_tiles(c)),
           ("stairs", "slab"), mc, cut_from=src)
family("town_cobblestone", "Town Cobblestone", "stone", tex("town_cobblestone", art.cobbles), ("stairs", "slab", "wall"),
       "STONE", cut_from="minecraft:cobblestone")

# --- the Mage Association
family("association_stone_bricks", "Association Stone Bricks", "pale_stone",
       tex("association_stone_bricks", art.association_bricks), ("stairs", "slab", "wall"), None, cut_from="minecraft:calcite")
SET.append(B("chiseled_association_stone", "Chiseled Association Stone", "block", "pale_stone",
             {"all": tex("chiseled_association_stone", art.association_chiseled)}, None, cut_from="association_stone_bricks"))
SET.append(B("association_stone_pillar", "Association Stone Pillar", "pillar", "pale_stone",
             {"side": tex("association_stone_pillar", art.association_pillar_side),
              "end": tex("association_stone_pillar_top", art.association_pillar_top)}, None,
             cut_from="association_stone_bricks"))
SET.append(B("gilded_association_stone", "Gilded Association Stone", "block", "pale_stone",
             {"all": tex("gilded_association_stone", art.association_gilded)}, None,
             recipe=("shapeless", [f"{NS}:association_stone_bricks", "minecraft:gold_nugget"], 1)))

# --- ruins of the elves and the Demon King's castle
family("elven_ruin_bricks", "Elven Ruin Bricks", "stone", tex("elven_ruin_bricks", art.elven_bricks),
       ("stairs", "slab", "wall"), "COLOR_LIGHT_GRAY", cut_from="minecraft:mossy_stone_bricks")
SET.append(B("cracked_elven_ruin_bricks", "Cracked Elven Ruin Bricks", "block", "stone",
             {"all": tex("cracked_elven_ruin_bricks", lambda: art.elven_bricks(cracked=True))}, "COLOR_LIGHT_GRAY",
             cut_from="elven_ruin_bricks"))
family("demon_castle_bricks", "Demon Castle Bricks", "dark_stone", tex("demon_castle_bricks", art.demon_bricks),
       ("stairs", "slab", "wall"), None, cut_from="minecraft:polished_blackstone_bricks")
SET.append(B("chiseled_demon_castle_bricks", "Chiseled Demon Castle Bricks", "block", "dark_stone",
             {"all": tex("chiseled_demon_castle_bricks", art.demon_chiseled)}, None, cut_from="demon_castle_bricks"))

# --- glass, light and furnishings
SET.append(B("leaded_glass", "Leaded Glass", "glass", "glass", {"all": tex("leaded_glass", art.leaded_glass)}, None, (),
             recipe=("shaped", ["GGG", "GNG", "GGG"], {"G": "minecraft:glass", "N": "minecraft:iron_nugget"}, 8)))
SET.append(B("leaded_glass_pane", "Leaded Glass Pane", "pane", "glass",
             {"pane": f"{NS}:block/leaded_glass", "edge": tex("leaded_glass_pane_top", art.leaded_glass_edge)}, None, (),
             recipe=("shaped", ["GGG", "GGG"], {"G": f"{NS}:leaded_glass"}, 16)))
SET.append(B("mana_lantern", "Mana Lantern", "lantern", "lantern", {"lantern": tex("mana_lantern", art.lantern)}, None,
             ("pickaxe",), recipe=("shaped", [" N ", "NCN", "NNN"], {"N": "minecraft:iron_nugget", "C": f"{NS}:mana_crystal"}, 1)))
SET.append(B("window_flower_box", "Window Flower Box", "window_box", "box",
             {"side": tex("window_flower_box", art.planter_side), "soil": tex("window_flower_box_soil", art.soil),
              "flowers": tex("window_flower_box_flowers", art.flowers)}, None, ("axe",),
             recipe=("shaped", ["FFF", "PPP"], {"F": f"{NS}:blue_moon_weed", "P": "#minecraft:planks"}, 2)))
SET.append(B("grimoire_shelf", "Grimoire Shelf", "shelf", "wood_decor",
             {"side": tex("grimoire_shelf", art.grimoire_shelf), "end": tex("grimoire_shelf_top", art.planks_top)}, None,
             ("axe",), recipe=("shaped", ["PPP", "BNB", "PPP"], {"P": "#minecraft:planks", "B": "minecraft:book",
                                                                    "N": "minecraft:gold_nugget"}, 1)))
SET.append(B("association_carpet", "Association Carpet", "carpet", "carpet",
             {"wool": tex("association_carpet", art.association_carpet)}, None, (),
             recipe=("shaped", ["GBG"], {"G": "minecraft:gold_nugget", "B": "minecraft:blue_wool"}, 2)))

NAMES = {b.id: b.name for b in SET}
BLOCK_TAGS = {
    "minecraft/tags/block/mineable/pickaxe": [f"{NS}:{b.id}" for b in SET if "pickaxe" in b.tools],
    "minecraft/tags/block/mineable/axe": [f"{NS}:{b.id}" for b in SET if "axe" in b.tools],
    "minecraft/tags/block/stairs": [f"{NS}:{b.id}" for b in SET if b.kind == "stairs"],
    "minecraft/tags/block/slabs": [f"{NS}:{b.id}" for b in SET if b.kind == "slab"],
    "minecraft/tags/block/walls": [f"{NS}:{b.id}" for b in SET if b.kind == "wall"],
    "minecraft/tags/block/enchantment_power_provider": [f"{NS}:grimoire_shelf"],
    "minecraft/tags/item/stairs": [f"{NS}:{b.id}" for b in SET if b.kind == "stairs"],
    "minecraft/tags/item/slabs": [f"{NS}:{b.id}" for b in SET if b.kind == "slab"],
    "minecraft/tags/item/walls": [f"{NS}:{b.id}" for b in SET if b.kind == "wall"],
}


# ---------------------------------------------------------------------------------------------
# Writers
# ---------------------------------------------------------------------------------------------
def write_json(root, rel, obj):
    path = os.path.join(root, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def template(name, mapping):
    s = open(os.path.join(TEMPLATES, name + ".json")).read()
    for k, v in sorted(mapping.items(), key=lambda kv: -len(kv[0])):
        s = s.replace(k, v)
    return json.loads(s)


def model(name, parent, textures, extra=None):
    obj = {"parent": parent, "textures": textures}
    if extra:
        obj.update(extra)
    write_json(ASSETS, f"models/block/{name}.json", obj)


def item_def(id, model_ref):
    write_json(ASSETS, f"items/{id}.json", {"model": {"type": "minecraft:model", "model": model_ref}})


def base_textures(b):
    if b.base:
        base = next(x for x in SET if x.id == b.base)
        return base.textures
    return b.textures


CUTOUT = {"render_type": "minecraft:cutout"}


def lantern_model(hanging):
    """A small iron lantern with a glowing core, standing or hanging from a short chain."""
    y = 1 if hanging else 0
    el = [
        {"from": [5, y, 5], "to": [11, y + 7, 11], "faces": {d: {"uv": [8, 0, 16, 8], "texture": "#lantern"}
                                                           for d in ("north", "south", "east", "west")}},
        {"from": [5, y, 5], "to": [11, y + 7, 11], "faces": {"up": {"uv": [0, 0, 8, 8], "texture": "#lantern"},
                                                           "down": {"uv": [0, 0, 8, 8], "texture": "#lantern"}}},
        {"from": [6, y + 7, 6], "to": [10, y + 9, 10], "faces": {d: {"uv": [0, 8, 8, 16], "texture": "#lantern"}
                                                               for d in ("north", "south", "east", "west", "up")}},
        {"from": [7, y + 9, 7], "to": [9, y + 10, 9], "faces": {d: {"uv": [8, 8, 16, 16], "texture": "#lantern"}
                                                              for d in ("north", "south", "east", "west", "up")}},
    ]
    if hanging:
        el.append({"from": [7.5, y + 10, 7.5], "to": [8.5, 16, 8.5],
                   "faces": {d: {"uv": [0, 0, 2, 8], "texture": "#lantern"} for d in ("north", "south", "east", "west")}})
    return {"textures": {"lantern": f"{NS}:block/mana_lantern", "particle": f"{NS}:block/mana_lantern"},
            "ambientocclusion": False, "render_type": "minecraft:cutout", "elements": el}


def window_box_model():
    def faces(tex_ref, uv=(0, 0, 16, 16), dirs=("north", "south", "east", "west")):
        return {d: {"uv": list(uv), "texture": tex_ref} for d in dirs}
    el = [
        {"from": [0, 0, 3], "to": [16, 7, 13], "faces": {**faces("#side", (0, 4, 16, 11)),
                                                         "down": {"uv": [0, 3, 16, 13], "texture": "#side"}}},
        {"from": [1, 6, 4], "to": [15, 6.5, 12], "faces": {"up": {"uv": [1, 4, 15, 12], "texture": "#soil"}}},
        {"from": [1, 6.5, 8], "to": [15, 15, 8], "shade": False,
         "faces": {"north": {"uv": [1, 7, 15, 16], "texture": "#flowers"}, "south": {"uv": [1, 7, 15, 16], "texture": "#flowers"}}},
        {"from": [8, 6.5, 4], "to": [8, 15, 12], "shade": False,
         "faces": {"east": {"uv": [4, 7, 12, 16], "texture": "#flowers"}, "west": {"uv": [4, 7, 12, 16], "texture": "#flowers"}}},
    ]
    t = {"side": f"{NS}:block/window_flower_box", "soil": f"{NS}:block/window_flower_box_soil",
         "flowers": f"{NS}:block/window_flower_box_flowers", "particle": f"{NS}:block/window_flower_box"}
    return {"textures": t, "render_type": "minecraft:cutout", "elements": el}


def gen_assets():
    for name, painter in TEXTURES.items():
        path = os.path.join(ASSETS, "textures/block", name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        painter().save(path)
    # icons for blocks whose item is a flat sprite
    art.lantern_icon().save(os.path.join(ASSETS, "textures/item/mana_lantern.png"))
    for b in SET:
        t = base_textures(b)
        bid, ref = b.id, f"{NS}:block/{b.id}"
        if b.kind == "block":
            model(bid, "minecraft:block/cube_all", {"all": t["all"]})
            write_json(ASSETS, f"blockstates/{bid}.json", {"variants": {"": {"model": ref}}})
            item_def(bid, ref)
        elif b.kind in ("pillar", "shelf"):
            model(bid, "minecraft:block/cube_column", {"end": t["end"], "side": t["side"]})
            if b.kind == "pillar":
                model(bid + "_horizontal", "minecraft:block/cube_column_horizontal", {"end": t["end"], "side": t["side"]})
                write_json(ASSETS, f"blockstates/{bid}.json", {"variants": {
                    "axis=y": {"model": ref}, "axis=z": {"model": ref + "_horizontal", "x": 90},
                    "axis=x": {"model": ref + "_horizontal", "x": 90, "y": 90}}})
            else:
                write_json(ASSETS, f"blockstates/{bid}.json", {"variants": {"": {"model": ref}}})
            item_def(bid, ref)
        elif b.kind == "stairs":
            tx = {"bottom": t["all"], "top": t["all"], "side": t["all"]}
            model(bid, "minecraft:block/stairs", tx)
            model(bid + "_inner", "minecraft:block/inner_stairs", tx)
            model(bid + "_outer", "minecraft:block/outer_stairs", tx)
            write_json(ASSETS, f"blockstates/{bid}.json",
                       template("stairs", {"@STAIRS": ref, "@INNER": ref + "_inner", "@OUTER": ref + "_outer"}))
            item_def(bid, ref)
        elif b.kind == "slab":
            tx = {"bottom": t["all"], "top": t["all"], "side": t["all"]}
            model(bid, "minecraft:block/slab", tx)
            model(bid + "_top", "minecraft:block/slab_top", tx)
            write_json(ASSETS, f"blockstates/{bid}.json", {"variants": {
                "type=bottom": {"model": ref}, "type=top": {"model": ref + "_top"},
                "type=double": {"model": f"{NS}:block/{b.base}"}}})
            item_def(bid, ref)
        elif b.kind == "wall":
            for suffix, parent in (("_post", "template_wall_post"), ("_side", "template_wall_side"),
                                   ("_side_tall", "template_wall_side_tall"), ("_inventory", "wall_inventory")):
                model(bid + suffix, f"minecraft:block/{parent}", {"wall": t["all"]})
            write_json(ASSETS, f"blockstates/{bid}.json",
                       template("wall", {"@POST": ref + "_post", "@SIDE": ref + "_side", "@TALL": ref + "_side_tall"}))
            item_def(bid, ref + "_inventory")
        elif b.kind == "glass":
            model(bid, "minecraft:block/cube_all", {"all": t["all"]}, CUTOUT)
            write_json(ASSETS, f"blockstates/{bid}.json", {"variants": {"": {"model": ref}}})
            item_def(bid, ref)
        elif b.kind == "pane":
            for suffix in ("post", "side", "side_alt", "noside", "noside_alt"):
                model(f"{bid}_{suffix}", f"minecraft:block/template_glass_pane_{suffix}",
                      {"pane": t["pane"], "edge": t["edge"]}, CUTOUT)
            write_json(ASSETS, f"blockstates/{bid}.json", template("pane", {
                "@POST": ref + "_post", "@SIDE_ALT": ref + "_side_alt", "@SIDE": ref + "_side",
                "@NOSIDE_ALT": ref + "_noside_alt", "@NOSIDE": ref + "_noside"}))
            write_json(ASSETS, f"models/item/{bid}.json", {"parent": "minecraft:item/generated",
                                                           "textures": {"layer0": t["pane"]}})
            item_def(bid, f"{NS}:item/{bid}")
        elif b.kind == "lantern":
            write_json(ASSETS, f"models/block/{bid}.json", lantern_model(False))
            write_json(ASSETS, f"models/block/{bid}_hanging.json", lantern_model(True))
            write_json(ASSETS, f"blockstates/{bid}.json", {"variants": {
                "hanging=false": {"model": ref}, "hanging=true": {"model": ref + "_hanging"}}})
            write_json(ASSETS, f"models/item/{bid}.json", {"parent": "minecraft:item/generated",
                                                           "textures": {"layer0": f"{NS}:item/{bid}"}})
            item_def(bid, f"{NS}:item/{bid}")
        elif b.kind == "window_box":
            write_json(ASSETS, f"models/block/{bid}.json", window_box_model())
            write_json(ASSETS, f"blockstates/{bid}.json", {"variants": {"": {"model": ref}}})
            item_def(bid, ref)
        elif b.kind == "carpet":
            model(bid, "minecraft:block/carpet", {"wool": t["wool"]})
            write_json(ASSETS, f"blockstates/{bid}.json", {"variants": {"": {"model": ref}}})
            item_def(bid, ref)
        else:
            raise SystemExit(f"unknown kind {b.kind}")


def ingredient(i):
    return i


def gen_data():
    for b in SET:
        bid = f"{NS}:{b.id}"
        entry = {"type": "minecraft:item", "name": bid}
        if b.kind == "slab":
            entry["functions"] = [{"function": "minecraft:set_count", "count": 2.0, "conditions": [
                {"condition": "minecraft:block_state_property", "block": bid, "properties": {"type": "double"}}]},
                                  {"function": "minecraft:explosion_decay"}]
            pool = {"rolls": 1.0, "entries": [entry]}
        else:
            pool = {"rolls": 1.0, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}
        write_json(DATA, f"loot_table/blocks/{b.id}.json", {"type": "minecraft:block", "pools": [pool],
                                                             "random_sequence": f"{NS}:blocks/{b.id}"})
        # crafting
        if b.recipe:
            kind = b.recipe[0]
            if kind == "shapeless":
                _, items, count = b.recipe
                write_json(DATA, f"recipe/{b.id}.json", {"type": "minecraft:crafting_shapeless", "category": "building",
                                                         "ingredients": items, "result": {"id": bid, "count": count}})
            else:
                _, pattern, key, count = b.recipe
                write_json(DATA, f"recipe/{b.id}.json", {"type": "minecraft:crafting_shaped", "category": "building",
                                                         "pattern": pattern, "key": key,
                                                         "result": {"id": bid, "count": count}})
        # stonecutting into variants, and the base from its vanilla source
        if b.cut_from:
            src = b.cut_from if ":" in b.cut_from else f"{NS}:{b.cut_from}"
            write_json(DATA, f"recipe/{b.id}_stonecutting.json", {"type": "minecraft:stonecutting", "ingredient": src,
                                                                  "result": {"id": bid, "count": 1}})
        if b.base:
            src = f"{NS}:{b.base}"
            count = 2 if b.kind == "slab" else 1
            write_json(DATA, f"recipe/{b.id}_stonecutting.json", {"type": "minecraft:stonecutting", "ingredient": src,
                                                                  "result": {"id": bid, "count": count}})
            shaped = {"stairs": (["#  ", "## ", "###"], 4), "slab": (["###"], 6), "wall": (["###", "###"], 6)}[b.kind]
            write_json(DATA, f"recipe/{b.id}.json", {"type": "minecraft:crafting_shaped", "category": "building",
                                                     "pattern": shaped[0], "key": {"#": src},
                                                     "result": {"id": bid, "count": shaped[1]}})


# ---------------------------------------------------------------------------------------------
# Java
# ---------------------------------------------------------------------------------------------
def const(id):
    return id.upper()


def java_props(b):
    color, strength, resist, sound, extra = PROPS[b.props]
    color = b.color or color or "STONE"
    color_call = "" if color == "NONE" else f".mapColor(MapColor.{color})"
    return (f"BlockBehaviour.Properties.of().setId(BLOCKS.key(\"{b.id}\")){color_call}"
            f".strength({strength}F, {resist}F).sound(SoundType.{sound}){extra}")


def java_block(b):
    p = java_props(b)
    kinds = {
        "block": lambda: f"new Block({p})",
        "shelf": lambda: f"new Block({p})",
        "stairs": lambda: f"new StairBlock({const(b.base)}.get().defaultBlockState(), {p})",
        "slab": lambda: f"new SlabBlock({p})",
        "wall": lambda: f"new WallBlock({p}.forceSolidOn())",
        "pillar": lambda: f"new RotatedPillarBlock({p})",
        "glass": lambda: f"new TransparentBlock({p})",
        "pane": lambda: f"new IronBarsBlock({p})",
        "lantern": lambda: f"new LanternBlock({p})",
        "carpet": lambda: f"new CarpetBlock({p})",
        "window_box": lambda: f"new ShapedDecorBlock({p}, Block.box(0, 0, 3, 16, 7, 13))",
    }
    return kinds[b.kind]()


def gen_java():
    blocks = []
    items = []
    for b in SET:
        blocks.append(f"    public static final RegistryObject<Block> {const(b.id)} = BLOCKS.register(\"{b.id}\", () ->\n"
                      f"        {java_block(b)});")
        items.append(f"        item(\"{b.id}\", {const(b.id)});")
    java = f'''// GENERATED by tools/gen_building.py - edit the building set there, not this file.
package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.block.ShapedDecorBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** The Frieren-style building set: {len(SET)} blocks (stairs, slabs and walls included) with their block items. */
public final class ModBuildingBlocks {{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FrierenMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FrierenMod.MODID);
    /** Block items in display order, for the Building creative tab. */
    public static final List<RegistryObject<Item>> TAB_ORDER = new ArrayList<>();

{chr(10).join(blocks)}

    static {{
{chr(10).join(items)}
    }}

    private ModBuildingBlocks() {{}}

    private static void item(String name, RegistryObject<Block> block) {{
        TAB_ORDER.add(ITEMS.register(name, () -> new BlockItem(block.get(),
            new Item.Properties().setId(ITEMS.key(name)).useBlockDescriptionPrefix())));
    }}
}}
'''
    with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
        f.write(java)


def main():
    gen_assets()
    gen_data()
    gen_java()
    print(f"generated {len(SET)} building blocks")


if __name__ == "__main__":
    main()
