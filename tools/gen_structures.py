"""
Generates the eight structure templates (gzipped NBT) used by the mod's jigsaw structures:
data/frieren/structure/<name>.nbt, referenced by data/frieren/worldgen/template_pool/<name>/start.json.

The builder writes the vanilla StructureTemplate format (DataVersion, size, palette, blocks, entities). Every
vanilla block state is validated against the 26.2 block report when it is available, so a typo in a block id or
property fails generation instead of silently producing air in-game.

Rules followed so placed blocks look right without neighbour updates (jigsaw pieces use "known shape"):
no panes, fences or walls (their connection states would be frozen); full glass blocks instead.
"""
import gzip
import io
import json
import math
import os
import random
import struct

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "frieren", "structure")
BLOCK_REPORT = "/home/user/mc/26.2-summary/blocks/data.min.json"
DATA_VERSION = 4903  # Minecraft 26.2
NS = "frieren"
MOD_BLOCKS = {"spell_research_desk", "summoning_altar", "mana_crystal_block", "blue_moon_weed", "cursed_gold_block",
              "mana_crystal_ore", "deepslate_mana_crystal_ore"}


# ---------------------------------------------------------------------------------------------
# NBT encoding
# ---------------------------------------------------------------------------------------------
class Int:
    def __init__(self, v):
        self.v = int(v)


class Double:
    def __init__(self, v):
        self.v = float(v)


class List:
    def __init__(self, tag_type, items):
        self.tag_type = tag_type
        self.items = items


TAG_END, TAG_INT, TAG_DOUBLE, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 6, 8, 9, 10


def tag_type(value):
    if isinstance(value, bool):
        raise TypeError("use Int(0/1) for booleans")
    if isinstance(value, Int) or isinstance(value, int):
        return TAG_INT
    if isinstance(value, Double) or isinstance(value, float):
        return TAG_DOUBLE
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, List):
        return TAG_LIST
    if isinstance(value, dict):
        return TAG_COMPOUND
    raise TypeError(f"unsupported NBT value {value!r}")


def write_string(out, s):
    data = s.encode("utf-8")
    out.write(struct.pack(">H", len(data)))
    out.write(data)


def write_payload(out, value):
    t = tag_type(value)
    if t == TAG_INT:
        out.write(struct.pack(">i", value.v if isinstance(value, Int) else value))
    elif t == TAG_DOUBLE:
        out.write(struct.pack(">d", value.v if isinstance(value, Double) else value))
    elif t == TAG_STRING:
        write_string(out, value)
    elif t == TAG_LIST:
        element = value.tag_type if value.items else TAG_END
        out.write(struct.pack(">bi", element, len(value.items)))
        for item in value.items:
            if tag_type(item) != value.tag_type:
                raise TypeError("mixed list")
            write_payload(out, item)
    elif t == TAG_COMPOUND:
        for key, item in value.items():
            out.write(struct.pack(">b", tag_type(item)))
            write_string(out, key)
            write_payload(out, item)
        out.write(struct.pack(">b", TAG_END))


def encode_root(compound):
    out = io.BytesIO()
    out.write(struct.pack(">b", TAG_COMPOUND))
    write_string(out, "")
    write_payload(out, compound)
    return out.getvalue()


# ---------------------------------------------------------------------------------------------
# Template builder
# ---------------------------------------------------------------------------------------------
_REPORT = None


def block_report():
    global _REPORT
    if _REPORT is None:
        _REPORT = json.load(open(BLOCK_REPORT)) if os.path.exists(BLOCK_REPORT) else {}
    return _REPORT


def validate_state(name, props):
    namespace, path = name.split(":")
    if namespace == NS:
        if path not in MOD_BLOCKS or props:
            raise SystemExit(f"bad mod block state {name} {props}")
        return
    report = block_report()
    if not report:
        return
    if path not in report:
        raise SystemExit(f"unknown vanilla block {name}")
    options = report[path][0]
    for key, value in props.items():
        if key not in options or value not in options[key]:
            raise SystemExit(f"bad property {key}={value} for {name}")


class Template:
    def __init__(self, name, sx, sy, sz, seed):
        self.name = name
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.entities = []
        self.rng = random.Random(seed)

    def inside(self, x, y, z):
        return 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]

    def set(self, x, y, z, block, nbt=None, **props):
        if not self.inside(x, y, z):
            raise SystemExit(f"{self.name}: block {block} at {(x, y, z)} outside {self.size}")
        name = block if ":" in block else "minecraft:" + block
        props = {k: str(v).lower() for k, v in props.items()}
        validate_state(name, props)
        self.blocks[(x, y, z)] = (name, tuple(sorted(props.items())), nbt)

    def get(self, x, y, z):
        entry = self.blocks.get((x, y, z))
        return entry[0] if entry else None

    def fill(self, x0, y0, z0, x1, y1, z1, block, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, block, **props)

    def air(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, "air")

    def walls(self, x0, y0, z0, x1, y1, z1, block, **props):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, z0, block, **props)
                self.set(x, y, z1, block, **props)
            for z in range(z0, z1 + 1):
                self.set(x0, y, z, block, **props)
                self.set(x1, y, z, block, **props)

    def weathered(self, block_choices):
        """Picks a block from [(block, weight)] deterministically."""
        total = sum(w for _, w in block_choices)
        roll = self.rng.random() * total
        for block, weight in block_choices:
            roll -= weight
            if roll <= 0:
                return block
        return block_choices[-1][0]

    def chest(self, x, y, z, loot, facing="north"):
        self.set(x, y, z, "chest", {"id": "minecraft:chest", "LootTable": f"{NS}:chests/{loot}"}, facing=facing)

    def entity(self, x, y, z, entity_id):
        if not self.inside(int(x), int(y), int(z)):
            raise SystemExit(f"{self.name}: entity {entity_id} outside template")
        self.entities.append({
            "pos": List(TAG_DOUBLE, [Double(x + 0.5), Double(y), Double(z + 0.5)]),
            "blockPos": List(TAG_INT, [Int(x), Int(y), Int(z)]),
            "nbt": {"id": entity_id},
        })

    def save(self):
        palette = []
        index = {}
        blocks = []
        for (x, y, z), (name, props, nbt) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            key = (name, props)
            if key not in index:
                index[key] = len(palette)
                entry = {"Name": name}
                if props:
                    entry["Properties"] = {k: v for k, v in props}
                palette.append(entry)
            block = {"pos": List(TAG_INT, [Int(x), Int(y), Int(z)]), "state": Int(index[key])}
            if nbt:
                block["nbt"] = nbt
            blocks.append(block)
        root = {
            "DataVersion": Int(DATA_VERSION),
            "size": List(TAG_INT, [Int(v) for v in self.size]),
            "palette": List(TAG_COMPOUND, palette),
            "blocks": List(TAG_COMPOUND, blocks),
            "entities": List(TAG_COMPOUND, self.entities),
        }
        os.makedirs(ROOT, exist_ok=True)
        path = os.path.join(ROOT, f"{self.name}.nbt")
        with open(path, "wb") as f:
            # mtime=0 keeps the output byte-identical across runs.
            with gzip.GzipFile(fileobj=f, mode="wb", mtime=0) as gz:
                gz.write(encode_root(root))
        chests = sum(1 for _, _, nbt in self.blocks.values() if nbt)
        print(f"  {self.name}: {self.size}, {len(blocks)} blocks, {len(palette)} states, {chests} chests, "
              f"{len(self.entities)} entities")


STONE_BRICKS = [("stone_bricks", 7), ("mossy_stone_bricks", 2), ("cracked_stone_bricks", 1)]
OLD_BRICKS = [("mossy_stone_bricks", 4), ("stone_bricks", 3), ("cracked_stone_bricks", 3)]
DEEP_BRICKS = [("deepslate_bricks", 6), ("cracked_deepslate_bricks", 3), ("polished_deepslate", 1)]
BLACK_BRICKS = [("polished_blackstone_bricks", 6), ("cracked_polished_blackstone_bricks", 2), ("blackstone", 2)]


# ---------------------------------------------------------------------------------------------
# Structures
# ---------------------------------------------------------------------------------------------
def mage_tower():
    t = Template("mage_tower", 11, 25, 11, 1)
    cx = cz = 5

    def dist(x, z):
        return math.hypot(x - cx, z - cz)

    for x in range(11):
        for z in range(11):
            d = dist(x, z)
            if d <= 4.6:
                t.set(x, 0, z, "cobblestone" if d > 3.6 else "stone_bricks")
                for y in range(1, 21):
                    if d > 3.6:
                        t.set(x, y, z, t.weathered(STONE_BRICKS))
                    else:
                        t.set(x, y, z, "air")
                for floor_y in (6, 12, 18):
                    if d <= 3.6:
                        t.set(x, floor_y, z, "spruce_planks")
                t.set(x, 21, z, "dark_oak_planks")
            if d <= 3.6:
                t.set(x, 22, z, "deepslate_tiles")
            if d <= 2.3:
                t.set(x, 23, z, "deepslate_tiles")
    t.set(cx, 24, cz, "end_rod", facing="up")

    # Ladder up the north wall, through every floor.
    for y in range(1, 21):
        t.set(5, y, 2, "ladder", facing="south")
    # Door in the south wall.
    t.set(5, 1, 9, "spruce_door", facing="north", half="lower", hinge="left", open="false", powered="false")
    t.set(5, 2, 9, "spruce_door", facing="north", half="upper", hinge="left", open="false", powered="false")
    # Windows on every floor.
    for y in (3, 9, 15):
        for (x, z) in ((1, 5), (9, 5), (3, 2), (7, 8)):
            t.set(x, y, z, "glass")
            t.set(x, y + 1, z, "glass")

    # Ground floor: hearth and storage.
    t.set(3, 1, 6, "furnace", facing="east")
    t.set(3, 1, 4, "crafting_table")
    t.set(7, 1, 4, "barrel", facing="up")
    t.set(7, 1, 6, "lantern")
    # Middle floor: alchemy.
    t.set(3, 7, 5, "brewing_stand")
    t.set(7, 7, 5, "cauldron")
    t.set(4, 7, 7, "lantern")
    for (x, z) in ((2, 4), (2, 6), (8, 4), (8, 6), (4, 8), (6, 8)):
        t.set(x, 7, z, "bookshelf")
        t.set(x, 8, z, "bookshelf")
    # Top floor: study.
    t.set(5, 19, 5, "enchanting_table")
    t.set(3, 19, 6, f"{NS}:spell_research_desk")
    t.set(7, 19, 6, "lectern", facing="west")
    t.chest(5, 19, 8, "mage_tower", facing="north")
    t.set(7, 19, 4, "lantern")
    for (x, z) in ((2, 4), (2, 6), (8, 4), (8, 6), (3, 3), (7, 3), (3, 7), (7, 7)):
        if t.get(x, 19, z) == "minecraft:air":
            t.set(x, 19, z, "bookshelf")
            t.set(x, 20, z, "bookshelf")

    t.entity(5, 1, 5, f"{NS}:wandering_mage")
    t.save()


def forgotten_library():
    t = Template("forgotten_library", 17, 9, 13, 2)
    for x in range(17):
        for z in range(13):
            t.set(x, 0, z, t.weathered(OLD_BRICKS))
            t.set(x, 7, z, t.weathered(OLD_BRICKS))
    for y in range(1, 7):
        for x in range(17):
            for z in range(13):
                edge = x in (0, 16) or z in (0, 12)
                t.set(x, y, z, t.weathered(OLD_BRICKS) if edge else "air")
    # Collapsed roof corner open to the sky.
    t.air(12, 7, 1, 15, 7, 3)
    t.fill(12, 1, 1, 14, 1, 2, "gravel")

    # Bookshelf aisles.
    for z in (3, 6, 9):
        for x in range(3, 14):
            if x == 8:
                continue
            for y in range(1, 4):
                t.set(x, y, z, "bookshelf" if t.rng.random() > 0.15 else "air")
    # Cobwebs in the upper gloom.
    for _ in range(26):
        x, y, z = t.rng.randint(1, 15), t.rng.randint(3, 6), t.rng.randint(1, 11)
        if t.get(x, y, z) == "minecraft:air":
            t.set(x, y, z, "cobweb")
    for (x, z) in ((2, 2), (14, 10), (2, 10), (8, 5)):
        t.set(x, 1, z, "soul_lantern")
    t.set(8, 1, 7, "lectern", facing="south")

    t.chest(1, 1, 1, "forgotten_library", facing="south")
    t.chest(15, 1, 11, "forgotten_library", facing="north")
    # One of the "chests" bites.
    t.entity(15, 1, 1, f"{NS}:mimic")

    # Entrance in the south wall just above the buried floor, with a ladder back out.
    t.air(8, 4, 12, 8, 5, 12)
    for y in range(1, 4):
        t.set(8, y, 11, "ladder", facing="north")
    t.save()


def demon_camp():
    t = Template("demon_camp", 15, 6, 15, 3)
    for x in range(15):
        for z in range(15):
            t.set(x, 0, z, t.weathered([("coarse_dirt", 5), ("dirt_path", 2), ("gravel", 1), ("podzol", 2)]))
            for y in range(1, 6):
                t.set(x, y, z, "air")

    def tent(x0, z0, wool):
        # A-frame tent along z, 5 long, open at the south end.
        for z in range(z0, z0 + 5):
            t.set(x0, 1, z, wool)
            t.set(x0 + 4, 1, z, wool)
            t.set(x0 + 1, 2, z, wool)
            t.set(x0 + 3, 2, z, wool)
            t.set(x0 + 2, 3, z, wool)
        # Closed triangular back wall.
        for y, (a, b) in ((1, (0, 4)), (2, (1, 3)), (3, (2, 2))):
            for x in range(x0 + a, x0 + b + 1):
                t.set(x, y, z0, wool)
        t.set(x0 + 2, 1, z0 + 3, "red_carpet")

    tent(1, 1, "red_wool")
    tent(9, 1, "black_wool")
    t.chest(11, 1, 2, "demon_camp", facing="south")
    t.set(3, 1, 2, "barrel", facing="up")

    # Campfire with log benches.
    t.set(7, 1, 9, "campfire", lit="true", facing="north")
    t.set(7, 1, 11, "oak_log", axis="x")
    t.set(5, 1, 9, "oak_log", axis="z")
    t.set(9, 1, 9, "oak_log", axis="z")
    # Trophy posts.
    for (x, z) in ((0, 14), (14, 14), (0, 7), (14, 7)):
        t.set(x, 1, z, "spruce_log", axis="y")
        t.set(x, 2, z, "spruce_log", axis="y")
        t.set(x, 3, z, "skeleton_skull", rotation="8")
    t.set(3, 1, 12, "bone_block", axis="y")
    t.set(11, 1, 12, "hay_block", axis="y")

    t.entity(7, 1, 7, f"{NS}:demon_soldier")
    t.entity(4, 1, 11, f"{NS}:demon_soldier")
    t.entity(10, 1, 11, f"{NS}:demon_mage")
    t.save()


def association_outpost():
    t = Template("association_outpost", 13, 11, 11, 4)
    t.fill(0, 0, 0, 12, 0, 10, "polished_andesite")
    t.fill(1, 0, 1, 11, 0, 9, "stone_bricks")

    def roof_y(z):
        return 5 + min(z, 10 - z)

    # Clear the whole volume first so terrain never intrudes.
    for x in range(13):
        for z in range(11):
            for y in range(1, 11):
                t.set(x, y, z, "air")
    # Walls: stone base, spruce frame.
    for y in range(1, 5):
        block = "stone_bricks" if y == 1 else "spruce_planks"
        t.walls(1, y, 1, 11, y, 9, block)
    for (x, z) in ((1, 1), (11, 1), (1, 9), (11, 9)):
        t.fill(x, 1, z, x, 4, z, "stripped_spruce_log", axis="y")
    # Gable ends.
    for z in range(1, 10):
        for y in range(5, roof_y(z)):
            t.set(1, y, z, "spruce_planks")
            t.set(11, y, z, "spruce_planks")
    # Roof (ridge along x at z=5).
    for x in range(0, 13):
        for z in range(0, 11):
            y = roof_y(z)
            if z < 5:
                t.set(x, y, z, "spruce_stairs", facing="south", half="bottom", shape="straight")
            elif z > 5:
                t.set(x, y, z, "spruce_stairs", facing="north", half="bottom", shape="straight")
            else:
                t.set(x, 9, z, "spruce_planks")
    # Door and windows.
    t.set(6, 1, 9, "spruce_door", facing="north", half="lower", hinge="left", open="false", powered="false")
    t.set(6, 2, 9, "spruce_door", facing="north", half="upper", hinge="left", open="false", powered="false")
    for (x, z) in ((3, 9), (9, 9), (3, 1), (9, 1)):
        t.set(x, 2, z, "glass")
        t.set(x, 3, z, "glass")
    for z in (4, 6):
        t.set(1, 2, z, "glass")
        t.set(11, 2, z, "glass")
    # Interior: the examiner's desk and the waiting area.
    t.fill(4, 1, 3, 8, 1, 3, "spruce_slab", type="top")
    t.set(6, 1, 2, "lectern", facing="south")
    t.set(2, 1, 2, "bookshelf")
    t.set(2, 2, 2, "bookshelf")
    t.set(10, 1, 2, "bookshelf")
    t.set(10, 2, 2, "bookshelf")
    t.chest(10, 1, 4, "association_outpost", facing="west")
    t.set(2, 1, 6, "spruce_stairs", facing="east", half="bottom", shape="straight")
    t.set(2, 1, 7, "spruce_stairs", facing="east", half="bottom", shape="straight")
    t.set(2, 1, 4, "lantern")
    t.set(10, 1, 7, "lantern")
    t.set(5, 3, 2, "blue_wall_banner", facing="south")
    t.set(7, 3, 2, "blue_wall_banner", facing="south")
    t.fill(4, 1, 5, 8, 1, 7, "blue_carpet")

    t.entity(6, 1, 4, f"{NS}:examiner")
    t.entity(3, 1, 7, f"{NS}:wandering_mage")
    t.save()


def heros_statue():
    t = Template("heros_statue", 11, 12, 11, 5)
    cx = cz = 5
    for x in range(11):
        for z in range(11):
            d = math.hypot(x - cx, z - cz)
            if d <= 5.5:
                for y in range(1, 12):
                    t.set(x, y, z, "air")
                if d > 4.0:
                    t.set(x, 0, z, "grass_block", snowy="false")
                    if t.rng.random() < 0.55:
                        t.set(x, 1, z, f"{NS}:blue_moon_weed")
                else:
                    t.set(x, 0, z, "polished_andesite" if (x + z) % 2 == 0 else "stone_bricks")
    # Plinth.
    t.fill(4, 1, 4, 6, 1, 6, "stone_bricks")
    t.fill(4, 2, 4, 6, 2, 6, "chiseled_stone_bricks")
    # The statue: a hero raising a sword, long since oxidised (>24 copper blocks for the rust quest).
    t.fill(4, 3, 5, 4, 5, 5, "oxidized_copper")
    t.fill(6, 3, 5, 6, 5, 5, "oxidized_copper")
    t.fill(4, 6, 5, 6, 8, 5, "oxidized_copper")
    t.fill(4, 6, 6, 6, 8, 6, "oxidized_cut_copper")
    t.set(5, 9, 5, "oxidized_copper")
    t.set(5, 10, 5, "oxidized_cut_copper")
    t.fill(3, 7, 5, 3, 8, 5, "oxidized_copper")
    t.fill(7, 8, 5, 7, 9, 5, "oxidized_copper")
    t.set(7, 10, 5, "oxidized_lightning_rod", facing="up")
    # Benches facing the statue and the traveller's chest behind it.
    for x in (3, 7):
        t.set(x, 1, 1, "spruce_stairs", facing="north", half="bottom", shape="straight")
        t.set(x, 1, 9, "spruce_stairs", facing="south", half="bottom", shape="straight")
    t.chest(5, 1, 7, "heros_statue", facing="south")
    t.set(1, 1, 5, "lantern")
    t.set(9, 1, 5, "lantern")
    t.save()


def sealed_shrine():
    t = Template("sealed_shrine", 11, 9, 11, 6)
    for x in range(11):
        for z in range(11):
            t.set(x, 0, z, "chiseled_stone_bricks" if (x + z) % 4 == 0 else "polished_andesite")
            for y in range(1, 9):
                t.set(x, y, z, "air")
    # Corner pillars and the roof ring.
    for x in range(1, 10):
        for z in range(1, 10):
            if x in (1, 9) or z in (1, 9):
                t.set(x, 6, z, "stone_brick_slab", type="bottom")
    for (x, z) in ((1, 1), (9, 1), (1, 9), (9, 9)):
        t.fill(x, 1, z, x, 5, z, "stone_bricks")
        t.set(x, 6, z, "chiseled_stone_bricks")
    # Low walls with gaps at the cardinal points.
    for i in range(2, 9):
        if i == 5:
            continue
        for (x, z) in ((i, 1), (i, 9), (1, i), (9, i)):
            t.set(x, 1, z, t.weathered(OLD_BRICKS))
    # Altar dais.
    t.fill(4, 1, 4, 6, 1, 6, "polished_deepslate")
    t.set(5, 2, 5, f"{NS}:summoning_altar")
    for (x, z) in ((4, 4), (6, 4), (4, 6), (6, 6)):
        t.set(x, 2, z, "purple_candle", candles=str(t.rng.randint(1, 4)), lit="true")
    t.chest(5, 1, 8, "sealed_shrine", facing="north")
    t.set(2, 1, 2, "soul_lantern")
    t.set(8, 1, 8, "soul_lantern")
    t.save()


def kings_tomb():
    size = 23
    t = Template("kings_tomb", size, 8, size, 7)
    for x in range(size):
        for z in range(size):
            t.set(x, 0, z, "deepslate_tiles")
            for y in range(1, 5):
                t.set(x, y, z, t.weathered(DEEP_BRICKS))
            t.set(x, 5, z, "deepslate_bricks")
            t.set(x, 6, z, "deepslate_bricks")
            t.set(x, 7, z, "deepslate_bricks")

    # Carve a perfect maze on the odd grid (11x11 cells) with a fixed seed.
    cells = size // 2
    # Cells under the central chamber (blocks 7..15) are excluded, so the maze winds around it and every
    # remaining cell stays connected through the outer ring.
    visited = {(cx, cz) for cx in range(3, 8) for cz in range(3, 8)}
    stack = [(0, 0)]
    visited.add((0, 0))

    def carve(cx, cz):
        for y in range(1, 5):
            t.set(cx, y, cz, "air")

    carve(1, 1)
    while stack:
        cx, cz = stack[-1]
        neighbours = [(cx + dx, cz + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))
                      if 0 <= cx + dx < cells and 0 <= cz + dz < cells and (cx + dx, cz + dz) not in visited]
        if not neighbours:
            stack.pop()
            continue
        nx, nz = t.rng.choice(neighbours)
        carve(2 * cx + 1 + (nx - cx), 2 * cz + 1 + (nz - cz))
        carve(2 * nx + 1, 2 * nz + 1)
        visited.add((nx, nz))
        stack.append((nx, nz))

    # Central burial chamber, taller than the corridors.
    for x in range(7, 16):
        for z in range(7, 16):
            edge = x in (7, 15) or z in (7, 15)
            for y in range(1, 7):
                t.set(x, y, z, "polished_deepslate" if edge else "air")
    # Doorways on all four sides, each joined to the adjacent maze cell (which a perfect maze always reaches).
    for (x, z, ox, oz) in ((11, 7, 11, 6), (11, 15, 11, 16), (7, 11, 6, 11), (15, 11, 16, 11)):
        t.air(x, 1, z, x, 3, z)
        t.air(ox, 1, oz, ox, 4, oz)
    t.fill(10, 1, 10, 12, 1, 12, "polished_deepslate")
    t.set(11, 2, 11, f"{NS}:summoning_altar")
    t.chest(8, 1, 8, "kings_tomb", facing="south")
    t.chest(14, 1, 14, "kings_tomb", facing="north")
    for (x, z) in ((8, 14), (14, 8)):
        t.set(x, 1, z, "soul_lantern")
    t.set(11, 1, 9, "red_carpet")
    t.set(11, 1, 13, "red_carpet")
    # Entrance shaft at the maze start, plus sparse light at a few corridor cells.
    t.air(1, 5, 1, 1, 7, 1)
    for y in range(1, 8):
        t.set(1, y, 0, "deepslate_bricks")
        t.set(1, y, 1, "ladder", facing="south")
    for (x, z) in ((1, 21), (21, 1), (21, 21), (5, 13), (17, 9)):
        if t.get(x, 1, z) == "minecraft:air":
            t.set(x, 1, z, "soul_lantern")
    t.save()


def aura_fortress():
    size = 27
    t = Template("aura_fortress", size, 18, size, 8)
    for x in range(size):
        for z in range(size):
            t.set(x, 0, z, "polished_blackstone_bricks" if (x * 7 + z * 3) % 5 else "gilded_blackstone")
            for y in range(1, 18):
                t.set(x, y, z, "air")
    # Curtain wall with crenellations.
    for y in range(1, 8):
        for i in range(1, size - 1):
            for (x, z) in ((i, 1), (i, size - 2), (1, i), (size - 2, i)):
                t.set(x, y, z, t.weathered(BLACK_BRICKS))
    for i in range(1, size - 1, 2):
        for (x, z) in ((i, 1), (i, size - 2), (1, i), (size - 2, i)):
            t.set(x, 8, z, "polished_blackstone_bricks")
    # Corner towers.
    for (tx, tz) in ((0, 0), (size - 5, 0), (0, size - 5), (size - 5, size - 5)):
        for y in range(1, 12):
            for x in range(tx, tx + 5):
                for z in range(tz, tz + 5):
                    edge = x in (tx, tx + 4) or z in (tz, tz + 4)
                    t.set(x, y, z, t.weathered(BLACK_BRICKS) if edge else ("polished_blackstone" if y in (6, 11) else "air"))
        for x in range(tx, tx + 5):
            for z in range(tz, tz + 5):
                if (x in (tx, tx + 4) or z in (tz, tz + 4)) and (x + z) % 2 == 0:
                    t.set(x, 12, z, "polished_blackstone_bricks")
        t.set(tx + 2, 12, tz + 2, "soul_lantern")
        # Doorway facing the courtyard and a ladder up the outer wall to the battlements.
        door_x = tx + 4 if tx == 0 else tx
        door_z = tz + 2 if tz == 0 else tz + 2
        t.air(door_x, 1, door_z, door_x, 2, door_z)
        ladder_x = tx + 1 if tx == 0 else tx + 3
        wall_facing = "east" if tx == 0 else "west"
        for y in range(1, 12):
            t.set(ladder_x, y, tz + 2, "ladder", facing=wall_facing)
    # Gate in the south wall.
    t.air(12, 1, size - 2, 14, 4, size - 2)
    # The keep.
    kx0, kx1, kz0, kz1 = 7, 19, 5, 15
    for y in range(1, 12):
        for x in range(kx0, kx1 + 1):
            for z in range(kz0, kz1 + 1):
                edge = x in (kx0, kx1) or z in (kz0, kz1)
                if edge:
                    t.set(x, y, z, t.weathered(BLACK_BRICKS))
    t.fill(kx0, 12, kz0, kx1, 12, kz1, "polished_blackstone")
    for x in range(kx0, kx1 + 1, 2):
        t.set(x, 13, kz0, "polished_blackstone_bricks")
        t.set(x, 13, kz1, "polished_blackstone_bricks")
    t.air(12, 1, kz1, 14, 4, kz1)
    for (x, y) in ((9, 5), (17, 5), (9, 9), (17, 9)):
        t.set(x, y, kz1, "tinted_glass")
    # Throne room.
    t.fill(10, 1, kz0 + 1, 16, 1, kz1 - 1, "red_carpet")
    t.fill(11, 1, kz0 + 1, 15, 1, kz0 + 3, "polished_blackstone")
    t.set(13, 2, kz0 + 2, f"{NS}:summoning_altar")
    t.set(13, 2, kz0 + 1, "polished_blackstone_brick_stairs", facing="north", half="bottom", shape="straight")
    t.chest(11, 2, kz0 + 1, "aura_fortress", facing="south")
    t.chest(15, 2, kz0 + 1, "aura_fortress", facing="south")
    for (x, z) in ((kx0 + 1, kz0 + 1), (kx1 - 1, kz0 + 1), (kx0 + 1, kz1 - 1), (kx1 - 1, kz1 - 1)):
        t.set(x, 1, z, "soul_lantern")
    t.set(10, 4, kz0 + 1, "red_wall_banner", facing="south")
    t.set(16, 4, kz0 + 1, "red_wall_banner", facing="south")
    # Courtyard: execution yard dressing and the headless guard.
    for (x, z) in ((4, 19), (22, 19), (4, 8), (22, 8)):
        t.set(x, 1, z, "soul_campfire", lit="true", facing="north")
    for (x, z) in ((5, 21), (21, 21), (13, 18), (9, 20), (17, 20), (13, 22)):
        t.entity(x, 1, z, f"{NS}:aura_thrall")
    t.save()


def main():
    print("structures:")
    mage_tower()
    forgotten_library()
    demon_camp()
    association_outpost()
    heros_statue()
    sealed_shrine()
    kings_tomb()
    aura_fortress()


if __name__ == "__main__":
    main()
