"""
Self-contained consistency check for the mod's resources and their links to the Java code. Needs only Python 3.

Checks:
  * every JSON file parses; every structure template decodes;
  * item model definitions -> models -> textures, blockstates -> models (frieren namespace);
  * every registered item has an item model definition, every block a blockstate and (unless it drops nothing) a loot table;
  * every entity type has a loot table and a lang name;
  * loot tables, recipes, tags, advancements and structure templates only reference registered frieren ids;
  * spell definitions use registered behaviours; sounds.json matches ModSounds;
  * every translation key used by the Java code exists in en_us.json, and literal-key placeholders match the
    number of arguments passed at the call site (a placeholder past the end prints the raw format string).

Exit code 1 if anything is wrong.
"""
import gzip
import io
import json
import os
import re
import struct
import sys

REPO = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
JAVA = os.path.join(REPO, "src", "main", "java", "com", "hunternado", "frieren")
RES = os.path.join(REPO, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "frieren")
DATA = os.path.join(RES, "data", "frieren")
NS = "frieren"

errors = []


def error(msg):
    errors.append(msg)


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def load_json(path):
    try:
        return json.loads(read(path))
    except Exception as e:  # noqa: BLE001 - report any parse failure
        error(f"invalid JSON {os.path.relpath(path, REPO)}: {e}")
        return None


def walk(root, ext):
    for dirpath, _, files in os.walk(root):
        for name in files:
            if name.endswith(ext):
                yield os.path.join(dirpath, name)


def java_sources():
    return {p: read(p) for p in walk(JAVA, ".java")}


# ---------------------------------------------------------------------------------------------
# Registry ids from the Java code
# ---------------------------------------------------------------------------------------------
def registry_ids(src):
    def ids(file, pattern):
        return set(re.findall(pattern, src[os.path.join(JAVA, *file.split("/"))]))

    items = ids("registry/ModItems.java", r'= (?:simple|grimoire|staff|armor|spawnEgg|blockItem)\("([a-z0-9_]+)"')
    blocks = ids("registry/ModBlocks.java", r'register\("([a-z0-9_]+)"')
    entities = ids("registry/ModEntities.java", r'register\("([a-z0-9_]+)"')
    sounds = ids("registry/ModSounds.java", r'register\("([a-z0-9_.]+)"\)')
    effects = ids("registry/ModEffects.java", r'register\("([a-z0-9_]+)"')
    components = ids("registry/ModDataComponents.java", r'register\("([a-z0-9_]+)"')
    behaviours = ids("spell/SpellBehaviors.java", r'register\(FrierenIds\.id\("([a-z0-9_]+)"\)')
    if not (items and blocks and entities and sounds and behaviours):
        error("could not read registry ids from the Java sources")
    return items, blocks, entities, sounds, effects, components, behaviours


# ---------------------------------------------------------------------------------------------
# NBT (structure templates)
# ---------------------------------------------------------------------------------------------
def read_nbt(path):
    data = io.BytesIO(gzip.decompress(open(path, "rb").read()))

    def rd(fmt):
        return struct.unpack(fmt, data.read(struct.calcsize(fmt)))[0]

    def rstr():
        return data.read(rd(">H")).decode("utf-8")

    def payload(t):
        if t == 1:
            return rd(">b")
        if t == 2:
            return rd(">h")
        if t == 3:
            return rd(">i")
        if t == 4:
            return rd(">q")
        if t == 5:
            return rd(">f")
        if t == 6:
            return rd(">d")
        if t == 8:
            return rstr()
        if t == 9:
            element, n = rd(">b"), rd(">i")
            return [payload(element) for _ in range(n)]
        if t == 10:
            out = {}
            while True:
                tt = rd(">b")
                if tt == 0:
                    return out
                key = rstr()
                out[key] = payload(tt)
        if t == 7:
            return data.read(rd(">i"))
        if t == 11:
            return [rd(">i") for _ in range(rd(">i"))]
        if t == 12:
            return [rd(">q") for _ in range(rd(">i"))]
        raise ValueError(f"tag {t}")

    if rd(">b") != 10:
        raise ValueError("root is not a compound")
    rstr()
    return payload(10)


# ---------------------------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------------------------
def check_json_files():
    for path in list(walk(RES, ".json")) + list(walk(RES, ".mcmeta")):
        load_json(path)
    # Minecraft only loads data/asset files by extension; anything else is silently ignored.
    allowed_ext = (".json", ".png", ".nbt", ".mcmeta", ".toml", ".ogg")
    for dirpath, _, files in os.walk(RES):
        for name in files:
            if not name.endswith(allowed_ext):
                error(f"{os.path.relpath(os.path.join(dirpath, name), REPO)} has no recognised extension and will be ignored")


def frieren_ids_in(obj, out):
    if isinstance(obj, dict):
        for value in obj.values():
            frieren_ids_in(value, out)
    elif isinstance(obj, list):
        for value in obj:
            frieren_ids_in(value, out)
    elif isinstance(obj, str) and obj.startswith(NS + ":"):
        out.add(obj[len(NS) + 1:])


def check_models(items, blocks):
    def model_exists(ref):
        ns, path = ref.split(":") if ":" in ref else ("minecraft", ref)
        return ns != NS or os.path.exists(os.path.join(ASSETS, "models", path + ".json"))

    def texture_exists(ref):
        ns, path = ref.split(":") if ":" in ref else ("minecraft", ref)
        return ns != NS or os.path.exists(os.path.join(ASSETS, "textures", path + ".png"))

    for item in items:
        path = os.path.join(ASSETS, "items", item + ".json")
        if not os.path.exists(path):
            error(f"item {item} has no item model definition")
            continue
        definition = load_json(path) or {}
        refs = []

        def collect(node):
            if isinstance(node, dict):
                if node.get("type") in ("minecraft:model", "model") and "model" in node:
                    refs.append(node["model"])
                for value in node.values():
                    collect(value)
            elif isinstance(node, list):
                for value in node:
                    collect(value)

        collect(definition.get("model"))
        for ref in refs:
            if not model_exists(ref):
                error(f"items/{item}.json references missing model {ref}")
    for path in walk(os.path.join(ASSETS, "models"), ".json"):
        model = load_json(path) or {}
        parent = model.get("parent")
        if parent and not model_exists(parent):
            error(f"{os.path.relpath(path, ASSETS)} has missing parent {parent}")
        for ref in (model.get("textures") or {}).values():
            if not ref.startswith("#") and not texture_exists(ref):
                error(f"{os.path.relpath(path, ASSETS)} references missing texture {ref}")
    for block in blocks:
        path = os.path.join(ASSETS, "blockstates", block + ".json")
        if not os.path.exists(path):
            error(f"block {block} has no blockstate")
            continue
        state = load_json(path) or {}
        for variant in (state.get("variants") or {}).values():
            for entry in (variant if isinstance(variant, list) else [variant]):
                if not model_exists(entry["model"]):
                    error(f"blockstates/{block}.json references missing model {entry['model']}")


def check_loot_and_data(items, blocks, entities, components):
    known = items | blocks | entities
    no_drop_blocks = {"mana_light"}
    for block in blocks - no_drop_blocks:
        if not os.path.exists(os.path.join(DATA, "loot_table", "blocks", block + ".json")):
            error(f"block {block} has no loot table")
    for entity in entities - {"mana_bolt", "spell_area"}:
        if not os.path.exists(os.path.join(DATA, "loot_table", "entities", entity + ".json")):
            error(f"entity {entity} has no loot table")
    loot_ids = {os.path.relpath(p, os.path.join(DATA, "loot_table"))[:-5].replace(os.sep, "/")
                for p in walk(os.path.join(DATA, "loot_table"), ".json")}
    spells = {os.path.basename(p)[:-5] for p in walk(os.path.join(DATA, "frieren_spells"), ".json")}
    structures = {os.path.basename(p)[:-4] for p in walk(os.path.join(DATA, "structure"), ".nbt")}
    damage_types = {os.path.basename(p)[:-5] for p in walk(os.path.join(DATA, "damage_type"), ".json")}
    advancements = {os.path.basename(p)[:-5] for p in walk(os.path.join(DATA, "advancement"), ".json")}
    tag_dirs = os.path.join(DATA, "tags")
    tags = {os.path.relpath(p, tag_dirs)[:-5].replace(os.sep, "/") for p in walk(tag_dirs, ".json")}
    allowed = known | spells | components | damage_types | advancements | {"main", "magic_data", "milestone"} \
        | {"chests/" + n for n in structures} | loot_ids | structures | {"mana_crystal_ore"}

    for sub in ("loot_table", "recipe", "advancement", "tags", "worldgen", "forge"):
        for path in walk(os.path.join(DATA, sub), ".json"):
            obj = load_json(path)
            refs = set()
            frieren_ids_in(obj, refs)
            for ref in refs:
                base = ref.split("/")[0]
                if ref in allowed or base in structures or ref.startswith("has_structure/") \
                        or ref in {t.split("/", 1)[1] for t in tags}:
                    continue
                error(f"{os.path.relpath(path, DATA)} references unknown id {NS}:{ref}")
    for path in walk(os.path.join(DATA, "worldgen", "template_pool"), ".json"):
        pool = load_json(path) or {}
        for element in pool.get("elements", []):
            location = element["element"].get("location", "")
            if location.startswith(NS + ":") and location[len(NS) + 1:] not in structures:
                error(f"{os.path.relpath(path, DATA)} points to missing template {location}")
    for name in structures:
        path = os.path.join(DATA, "structure", name + ".nbt")
        try:
            nbt = read_nbt(path)
        except Exception as e:  # noqa: BLE001
            error(f"structure/{name}.nbt does not decode: {e}")
            continue
        for key in ("DataVersion", "size", "palette", "blocks", "entities"):
            if key not in nbt:
                error(f"structure/{name}.nbt lacks {key}")
        for state in nbt.get("palette", []):
            block = state["Name"]
            if block.startswith(NS + ":") and block[len(NS) + 1:] not in blocks:
                error(f"structure/{name}.nbt uses unknown block {block}")
        for block in nbt.get("blocks", []):
            table = (block.get("nbt") or {}).get("LootTable")
            if table and (not table.startswith(NS + ":") or table[len(NS) + 1:] not in loot_ids):
                error(f"structure/{name}.nbt chest uses missing loot table {table}")
        for entity in nbt.get("entities", []):
            eid = entity["nbt"]["id"]
            if eid.startswith(NS + ":") and eid[len(NS) + 1:] not in entities:
                error(f"structure/{name}.nbt places unknown entity {eid}")


def check_spells(behaviours):
    for path in walk(os.path.join(DATA, "frieren_spells"), ".json"):
        spell = load_json(path) or {}
        behaviour = spell.get("behavior", "")
        if not behaviour.startswith(NS + ":") or behaviour[len(NS) + 1:] not in behaviours:
            error(f"spell {os.path.basename(path)} uses unknown behaviour {behaviour}")


def check_sounds(sounds):
    defined = set((load_json(os.path.join(ASSETS, "sounds.json")) or {}).keys())
    for missing in sorted(sounds - defined):
        error(f"sound event {missing} registered but not in sounds.json")
    for extra in sorted(defined - sounds):
        error(f"sounds.json defines {extra} but ModSounds does not register it")


# ---------------------------------------------------------------------------------------------
# Language
# ---------------------------------------------------------------------------------------------
LITERAL_KEY = re.compile(r'"((?:[a-zA-Z]+\.)+' + NS + r'\.[a-z0-9_.]+)"')


def call_arguments(text, start):
    """Splits the argument list of the call that contains the string literal starting at `start`."""
    depth, i, current, args, in_string = 1, start, "", [], False
    while i < len(text):
        c = text[i]
        if in_string:
            current += c
            if c == "\\":
                current += text[i + 1]
                i += 2
                continue
            if c == '"':
                in_string = False
        elif c == '"':
            in_string = True
            current += c
        elif c in "([{":
            depth += 1
            current += c
        elif c in ")]}":
            depth -= 1
            if depth == 0:
                args.append(current)
                break
            current += c
        elif c == "," and depth == 1:
            args.append(current)
            current = ""
        else:
            current += c
        i += 1
    return [a.strip() for a in args]


def placeholder_count(value):
    explicit = [int(n) for n in re.findall(r"%(\d+)\$s", value)]
    implicit = len(re.findall(r"%s", value))
    return max(explicit + [implicit]) if explicit else implicit


def check_lang(src, items, blocks, entities, effects, sounds):
    lang = load_json(os.path.join(ASSETS, "lang", "en_us.json")) or {}

    def need(key, why):
        if key not in lang:
            error(f"missing lang key {key} ({why})")

    for item in items:
        if item not in blocks:
            need(f"item.{NS}.{item}", "item")
    for block in blocks:
        need(f"block.{NS}.{block}", "block")
    for entity in entities:
        need(f"entity.{NS}.{entity}", "entity")
    for effect in effects:
        need(f"effect.{NS}.{effect}", "effect")
    for sound in sounds:
        need(f"subtitles.{NS}.{sound}", "sound subtitle")
    for path in walk(os.path.join(DATA, "frieren_spells"), ".json"):
        spell = os.path.basename(path)[:-5]
        need(f"spell.{NS}.{spell}", "spell name")
        need(f"spell.{NS}.{spell}.desc", "spell description")
    for path in walk(os.path.join(DATA, "advancement"), ".json"):
        refs = set()

        def translations(node):
            if isinstance(node, dict):
                if "translate" in node:
                    refs.add(node["translate"])
                for value in node.values():
                    translations(value)
            elif isinstance(node, list):
                for value in node:
                    translations(value)

        translations(load_json(path))
        for ref in refs:
            need(ref, "advancement")
    for path in walk(os.path.join(DATA, "damage_type"), ".json"):
        msg = (load_json(path) or {}).get("message_id", "")
        need(f"death.attack.{msg}", "death message")
        need(f"death.attack.{msg}.player", "death message")

    # Enum-driven keys: read the ids straight from the enums.
    def enum_ids(file, pattern=r'^\s+[A-Z_]+\("([a-z_]+)"'):
        return re.findall(pattern, src[os.path.join(JAVA, *file.split("/"))], re.M)

    for rank in enum_ids("magic/MageRank.java"):
        need(f"rank.{NS}.{rank}", "rank")
    for rarity in enum_ids("magic/SpellRarity.java"):
        need(f"rarity.{NS}.{rarity}", "rarity")
    for school in enum_ids("magic/SpellSchool.java"):
        need(f"school.{NS}.{school}", "school")
    for stage in enum_ids("exam/ExamType.java", r'^\s+[A-Z_]+\("([a-z_]+)", \d+, \d+\)'):
        need(f"exam.stage.{NS}.{stage}", "exam stage")
        need(f"exam.stage.{NS}.{stage}.desc", "exam stage")
    for quest in enum_ids("quest/Quest.java"):
        for suffix in ("", ".desc", ".reward"):
            need(f"quest.{NS}.{quest}{suffix}", "quest")
    for kind in enum_ids("item/ArtifactItem.java"):
        need(f"tooltip.{NS}.{kind}", "artifact")
        need(f"tooltip.{NS}.{kind}.effect", "artifact")
    for boss in enum_ids("item/SummoningSigilItem.java"):
        need(f"tooltip.{NS}.sigil_{boss}", "sigil")
        need(f"message.{NS}.summoned.{boss}", "summon message")
    for tier in enum_ids("item/StaffItem.java", r'^\s+[A-Z_]+\("([a-z_]+)", [0-9.]+F'):
        need(f"tooltip.{NS}.staff_{tier}", "staff tier")
    for key in re.findall(r'new TooltipItem\([^;]*?"([a-z_]+)"\)', src[os.path.join(JAVA, "registry", "ModItems.java")]):
        need(f"tooltip.{NS}.{key}", "tooltip item")
    for phase in re.findall(r"enum Phase \{([^}]*)\}", src[os.path.join(JAVA, "entity", "boss", "AuraEntity.java")])[0].split(","):
        need(f"aura.{NS}.phase.{phase.strip().lower()}", "aura phase")
    plea_lines = int(re.search(r"PLEA_LINES = (\d+)", src[os.path.join(JAVA, "entity", "demon", "DemonEntity.java")]).group(1))
    for i in range(plea_lines):
        need(f"demon.{NS}.plea.{i}", "demon plea")
    for direction in re.search(r'String\[\] names = \{([^}]*)\}', src[os.path.join(JAVA, "spell", "behavior", "SeekBehavior.java")]).group(1).split(","):
        need(f"direction.{NS}.{direction.strip().strip(chr(34))}", "compass")
    for name in re.findall(r'key\("([a-z_]+)", GLFW', src[os.path.join(JAVA, "client", "KeyBindings.java")]):
        need(f"key.{NS}.{name}", "key mapping")
    need(f"key.category.{NS}.magic", "key category")
    need(f"itemGroup.{NS}.{NS}", "creative tab")
    # NPC dialogue: every subclass declares npcId() and optionally dialogueLines().
    for path, text in src.items():
        npc_id = re.search(r'String npcId\(\)\s*\{\s*return "([a-z_]+)";', text)
        if npc_id:
            lines = re.search(r"int dialogueLines\(\)\s*\{\s*return (\d+);", text)
            for i in range(int(lines.group(1)) if lines else 3):
                need(f"npc.{NS}.{npc_id.group(1)}.line.{i}", "npc dialogue")
    for npc in ("fern", "stark"):
        for i in range(3):
            need(f"npc.{NS}.{npc}.line.{i}", "companion dialogue")

    # Literal keys + placeholder/argument agreement.
    for path, text in src.items():
        for match in LITERAL_KEY.finditer(text):
            key = match.group(1)
            if key.endswith("."):
                continue
            need(key, os.path.relpath(path, JAVA))
            if key not in lang:
                continue
            before = text[max(0, match.start() - 40):match.start()]
            if not re.search(r"translatable\(\s*(?:\w+\s*\?\s*)?$", before):
                continue  # key passed through a variable or ternary; checked where it is used
            args = call_arguments(text, match.start())
            given = len(args) - 1
            wanted = placeholder_count(lang[key])
            if wanted > given:
                error(f"{key}: lang expects {wanted} argument(s), {os.path.relpath(path, JAVA)} passes {given}")


def main():
    src = java_sources()
    items, blocks, entities, sounds, effects, components, behaviours = registry_ids(src)
    check_json_files()
    check_models(items, blocks)
    check_loot_and_data(items, blocks, entities, components)
    check_spells(behaviours)
    check_sounds(sounds)
    check_lang(src, items, blocks, entities, effects, sounds)
    for message in errors:
        print("ERROR", message)
    print(f"validated: {len(items)} items, {len(blocks)} blocks, {len(entities)} entities, {len(sounds)} sounds; "
          f"{len(errors)} problem(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
