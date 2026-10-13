"""
3D held items: element models for staffs and weapons, used in hands (characters and players) while inventories,
item frames and dropped items keep the flat icon from tools/gen_assets.py. Writes per item:
  * assets/frieren/items/<id>.json                 display_context select: flat icon in gui/ground/fixed/on_shelf
  * assets/frieren/models/item/<id>_in_hand.json   elements + display transforms
  * assets/frieren/textures/item/held/<id>.png     32x32 swatch atlas the elements sample

Display transforms are derived with tools/models/held.py (the game's exact hand transform chain) and previewed by
tools/preview_held.py. Item models run along +Y (grip near y=8, the model centre the hand transform pivots on).
"""
import json
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "models"))
from PIL import Image  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(REPO, "src/main/resources/assets/frieren")
NS = "frieren"
ATLAS = 32  # texture pixels for the 16-unit UV space: 2 px per unit, swatch cells of 2x2 units


def hexc(v):
    v = v.lstrip("#")
    return tuple(int(v[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def shade(c, f):
    return tuple(max(0, min(255, int(c[i] * f))) for i in range(3)) + (255,)


# ---------------------------------------------------------------------------------------------
# Swatches: each material is a 4x4 px cell; 'grain' cells carry lines along v so long faces read as wood
# ---------------------------------------------------------------------------------------------
class Atlas:
    def __init__(self):
        self.cells = {}
        self.img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))

    def cell(self, name, color, pattern="flat"):
        if name in self.cells:
            return self.cells[name]
        k = len(self.cells)
        u, v = (k % 8) * 2, (k // 8) * 2
        px = self.img.load()
        base = hexc(color)
        for y in range(4):
            for x in range(4):
                c = base
                if pattern == "grain":
                    c = shade(base, (1.0, 0.86, 0.95, 0.8)[x])
                elif pattern == "metal":
                    c = shade(base, (1.25, 1.08, 0.95, 0.82)[x])
                elif pattern == "gem":
                    c = shade(base, 1.0)
                    if (x, y) in ((0, 0), (1, 0), (0, 1)):
                        c = shade(base, 1.7)
                    elif x == 3 or y == 3:
                        c = shade(base, 0.7)
                elif pattern == "wrap":
                    c = shade(base, 1.0 if (y % 2 == 0) else 0.75)
                px[u * 2 + x, v * 2 + y] = c
        self.cells[name] = (u, v)
        return self.cells[name]


def shifted(elements, dy):
    """Moves elements along +Y so the grip lands on y = 8, where the hand transform pivots."""
    for el in elements:
        el["from"][1] += dy
        el["to"][1] += dy
    return elements


def box(atlas, frm, to, name, color, pattern="flat", faces=("north", "south", "east", "west", "up", "down")):
    u, v = atlas.cell(name, color, pattern)
    uv = [u + 0.25, v + 0.25, u + 1.75, v + 1.75]   # sample inside the cell: no bleeding from neighbours
    return {"from": list(frm), "to": list(to), "faces": {f: {"uv": uv, "texture": "#tex"} for f in faces}}


# ---------------------------------------------------------------------------------------------
# Display transforms (designed with tools/models/held.py; see tools/preview_held.py)
# ---------------------------------------------------------------------------------------------
def mirror(t):
    """Left-hand transform from a right-hand one: mirror across the body's centre plane."""
    r, tr = t.get("rotation", [0, 0, 0]), t.get("translation", [0, 0, 0])
    return {"rotation": [r[0], -r[1], -r[2]], "translation": [-tr[0], tr[1], tr[2]], "scale": t.get("scale", [1, 1, 1])}


def display(third, first):
    return {
        "thirdperson_righthand": third, "thirdperson_lefthand": mirror(third),
        "firstperson_righthand": first, "firstperson_lefthand": mirror(first),
        "head": {"rotation": [0, 0, 0], "translation": [0, 4, 0], "scale": [0.6, 0.6, 0.6]},
    }


# All items rest upright-ish in a hanging hand, gripped at y = 8 (the model centre) and held 1 px in front of the
# arm and 1.5 px outward, so nothing runs through the arm, shoulder or face. While a character casts or attacks,
# CharacterModel.translateToHand cancels the arm's raise at the wrist and adds an aim tilt, so staffs point at the
# target instead of flipping back over the shoulder.
HAND = {"translation": [1.5, 1, 1.25]}
STAFF = display({"rotation": [80, 0, 0], **HAND, "scale": [1, 1, 1]},
                {"rotation": [-20, 0, 15], "translation": [-1, -3, 0], "scale": [0.6, 0.6, 0.6]})
AXE = display({"rotation": [55, 0, 0], **HAND, "scale": [1, 1, 1]},
              {"rotation": [-10, 0, 20], "translation": [-1, -3, 0], "scale": [0.7, 0.7, 0.7]})
SWORD = display({"rotation": [45, 0, 0], **HAND, "scale": [0.95, 0.95, 0.95]},
                {"rotation": [-10, 0, 25], "translation": [-1, -3, 0], "scale": [0.7, 0.7, 0.7]})
SCALE = display({"rotation": [90, 0, 0], **HAND, "scale": [0.8, 0.8, 0.8]},
                {"rotation": [0, 0, 10], "translation": [-1, -3, 0], "scale": [0.6, 0.6, 0.6]})


# ---------------------------------------------------------------------------------------------
# The items (coordinates in block-model units; +Y up, grip at y ~ 8)
# ---------------------------------------------------------------------------------------------
def sages_staff(a):
    """Frieren's staff: slender wooden shaft, gold collar, and a gold crescent cradling a red orb."""
    wood, gold, red = "#8A5A36", "#D8B04A", "#C8283A"
    return [
        box(a, (7.25, -4, 7.25), (8.75, 20, 8.75), "wood", wood, "grain"),
        box(a, (7.0, -4.5, 7.0), (9.0, -3.5, 9.0), "gold", gold, "metal"),            # ferrule
        box(a, (6.75, 18.5, 6.75), (9.25, 20.5, 9.25), "gold", gold, "metal"),       # collar
        box(a, (6.25, 20.5, 7.25), (9.75, 21.5, 8.75), "gold", gold, "metal"),       # cradle base
        box(a, (5.25, 21.0, 7.25), (6.5, 25.5, 8.75), "gold", gold, "metal"),        # crescent arms
        box(a, (9.5, 21.0, 7.25), (10.75, 25.5, 8.75), "gold", gold, "metal"),
        box(a, (5.75, 25.25, 7.25), (6.75, 27.25, 8.75), "gold", gold, "metal"),     # tips curling inward
        box(a, (9.25, 25.25, 7.25), (10.25, 27.25, 8.75), "gold", gold, "metal"),
        box(a, (6.75, 21.75, 6.75), (9.25, 24.25, 9.25), "red", red, "gem"),         # the orb
        box(a, (7.25, 24.0, 7.25), (8.75, 24.75, 8.75), "red", red, "gem"),
    ]


def mage_staff(a):
    """Association mage staff (Fern carries one): dark wood, silver head with four prongs around a blue crystal."""
    wood, silver, blue = "#5C3D28", "#C9CCD6", "#7FC4FF"
    return [
        box(a, (7.25, -4, 7.25), (8.75, 21, 8.75), "wood", wood, "grain"),
        box(a, (7.0, -4.5, 7.0), (9.0, -3.5, 9.0), "silver", silver, "metal"),
        box(a, (6.75, 20.0, 6.75), (9.25, 22.0, 9.25), "silver", silver, "metal"),
        box(a, (6.0, 22.0, 7.5), (7.0, 26.0, 8.5), "silver", silver, "metal"),
        box(a, (9.0, 22.0, 7.5), (10.0, 26.0, 8.5), "silver", silver, "metal"),
        box(a, (7.5, 22.0, 6.0), (8.5, 26.0, 7.0), "silver", silver, "metal"),
        box(a, (7.5, 22.0, 9.0), (8.5, 26.0, 10.0), "silver", silver, "metal"),
        box(a, (7.0, 22.25, 7.0), (9.0, 26.25, 9.0), "blue", blue, "gem"),
        box(a, (7.5, 26.25, 7.5), (8.5, 27.25, 8.5), "blue", blue, "gem"),
    ]


def apprentice_staff(a):
    """A first staff: plain wood with a knotted head and a small pale crystal bound on with leather."""
    wood, knot, leather, crystal = "#9A6A3C", "#7A5230", "#5A3A22", "#BFE6FF"
    return [
        box(a, (7.25, -4, 7.25), (8.75, 22, 8.75), "wood", wood, "grain"),
        box(a, (6.75, 20.0, 6.75), (9.25, 23.0, 9.25), "knot", knot, "grain"),
        box(a, (6.5, 21.0, 6.5), (9.5, 22.0, 9.5), "leather", leather, "wrap"),
        box(a, (7.25, 23.0, 7.25), (8.75, 25.5, 8.75), "crystal", crystal, "gem"),
        box(a, (7.5, 9.0, 7.0), (8.5, 12.0, 9.0), "leather", leather, "wrap"),       # grip wrap
    ]


def guillotine_staff(a):
    """Aura's guillotine staff: black shaft, a silver guillotine crescent and a crimson gem."""
    shaft, steel, red, dark = "#2A1A2A", "#C8CCD8", "#B0263A", "#4A3A4A"
    return [
        box(a, (7.25, -4, 7.25), (8.75, 21, 8.75), "shaft", shaft, "grain"),
        box(a, (6.75, 19.5, 6.75), (9.25, 21.5, 9.25), "dark", dark, "metal"),
        box(a, (8.75, 21.0, 7.5), (12.5, 22.0, 8.5), "steel", steel, "metal"),       # blade arm
        box(a, (11.5, 22.0, 7.5), (12.5, 27.0, 8.5), "steel", steel, "metal"),       # the falling blade
        box(a, (9.5, 25.5, 7.75), (11.5, 27.0, 8.25), "steel", steel, "metal"),
        box(a, (6.75, 21.5, 6.75), (9.25, 24.0, 9.25), "red", red, "gem"),
        box(a, (7.25, 24.0, 7.25), (8.75, 25.5, 8.75), "dark", dark, "metal"),
    ]


def warriors_axe(a):
    """Stark's (and once Eisen's) axe: long haft with leather wraps, broad steel bit and a back spike."""
    haft, steel, edge, leather = "#5C3E28", "#9AA0AA", "#E4E8EE", "#3E2A1C"
    return [
        box(a, (7.25, -4, 7.25), (8.75, 25, 8.75), "haft", haft, "grain"),
        box(a, (7.0, 2.0, 7.0), (9.0, 9.0, 9.0), "leather", leather, "wrap"),        # grip
        box(a, (6.75, 19.0, 6.75), (9.25, 25.0, 9.25), "steel", steel, "metal"),     # eye of the axe
        box(a, (9.25, 18.0, 7.5), (12.5, 26.0, 8.5), "steel", steel, "metal"),       # bit
        box(a, (12.5, 16.5, 7.75), (14.0, 27.5, 8.25), "edge", edge, "metal"),       # flared cutting edge
        box(a, (3.5, 21.0, 7.6), (6.75, 22.5, 8.4), "steel", steel, "metal"),        # back spike
        box(a, (7.5, 25.0, 7.5), (8.5, 27.0, 8.5), "steel", steel, "metal"),         # top spike
    ]


def heros_sword(a):
    """The replica of the Hero's Sword Himmel carried: long blade, gold crossguard, blue pommel gem."""
    blade, edge, gold, grip, blue = "#C9D1DC", "#EEF2F7", "#D4AF37", "#3D2A4A", "#3C7BD6"
    return [
        box(a, (7.0, 0.0, 7.5), (9.0, 2.5, 8.5), "blue", blue, "gem"),               # pommel
        box(a, (7.4, 2.5, 7.4), (8.6, 7.0, 8.6), "grip", grip, "wrap"),
        box(a, (4.5, 7.0, 7.25), (11.5, 8.25, 8.75), "gold", gold, "metal"),         # crossguard
        box(a, (7.25, 7.5, 7.0), (8.75, 9.5, 9.0), "gold", gold, "metal"),
        box(a, (7.0, 8.25, 7.75), (9.0, 26.0, 8.25), "blade", blade, "metal"),
        box(a, (7.5, 26.0, 7.8), (8.5, 28.0, 8.2), "edge", edge, "metal"),           # tip
        box(a, (7.75, 8.25, 7.7), (8.25, 25.5, 8.3), "edge", edge, "metal"),         # fuller highlight
    ]


def demon_kings_blade(a):
    """The Demon King's greatsword: black blade veined crimson, a gold crossguard with upswept horns."""
    blade, vein, gold, grip = "#1E1822", "#C8283A", "#B8902E", "#2A1A20"
    return [
        box(a, (7.0, -1.0, 7.0), (9.0, 1.0, 9.0), "gold", gold, "metal"),
        box(a, (7.4, 1.0, 7.4), (8.6, 7.0, 8.6), "grip", grip, "wrap"),
        box(a, (3.5, 7.0, 7.25), (12.5, 8.5, 8.75), "gold", gold, "metal"),
        box(a, (3.0, 8.5, 7.5), (4.25, 11.0, 8.5), "gold", gold, "metal"),           # horns of the guard
        box(a, (11.75, 8.5, 7.5), (13.0, 11.0, 8.5), "gold", gold, "metal"),
        box(a, (6.5, 8.5, 7.7), (9.5, 25.5, 8.3), "blade", blade, "metal"),
        box(a, (7.75, 9.0, 7.6), (8.25, 24.5, 8.4), "vein", vein, "gem"),
        box(a, (7.25, 25.5, 7.75), (8.75, 28.0, 8.25), "blade", blade, "metal"),
    ]


def scale_of_obedience(a):
    """Aura's Scale of Obedience: a gold balance on a short handle, violet gem at the top, two hanging pans."""
    gold, gem, chain = "#D8B04A", "#B04CD8", "#A88A3A"
    out = [
        box(a, (7.25, 2.0, 7.25), (8.75, 18.0, 8.75), "gold", gold, "metal"),        # post
        box(a, (2.0, 16.0, 7.5), (14.0, 17.0, 8.5), "gold", gold, "metal"),          # beam
        box(a, (7.0, 18.0, 7.0), (9.0, 20.0, 9.0), "gem", gem, "gem"),
        box(a, (6.5, 1.0, 6.5), (9.5, 2.0, 9.5), "gold", gold, "metal"),             # base knob
    ]
    for x in (2.5, 12.5):
        out.append(box(a, (x, 11.0, 7.75), (x + 1.0, 16.0, 8.25), "chain", chain, "flat"))
        out.append(box(a, (x - 1.5, 10.0, 6.5), (x + 2.5, 11.0, 9.5), "gold", gold, "metal"))
    return out


ITEMS = {
    # id: (elements, display transforms, shift along +Y that puts the grip on y = 8)
    "sages_staff": (sages_staff, STAFF, 4.75),
    "mage_staff": (mage_staff, STAFF, 4.75),
    "apprentice_staff": (apprentice_staff, STAFF, 5.0),
    "guillotine_staff": (guillotine_staff, STAFF, 5.0),
    "warriors_axe": (warriors_axe, AXE, 4.0),
    "heros_sword": (heros_sword, SWORD, 3.0),
    "demon_kings_blade": (demon_kings_blade, SWORD, 4.0),
    "scale_of_obedience": (scale_of_obedience, SCALE, 5.0),
}
HELD_ITEMS = set(ITEMS)


def build(item_id):
    """(elements, display, atlas image) for one item."""
    factory, disp, grip_shift = ITEMS[item_id]
    atlas = Atlas()
    elements = shifted(factory(atlas), grip_shift)
    return elements, disp, atlas.img


def write_json(rel, data):
    path = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    for item_id in ITEMS:
        elements, disp, img = build(item_id)
        tex = f"{NS}:item/held/{item_id}"
        os.makedirs(os.path.join(ASSETS, "textures/item/held"), exist_ok=True)
        img.save(os.path.join(ASSETS, f"textures/item/held/{item_id}.png"))
        write_json(f"models/item/{item_id}_in_hand.json", {
            "textures": {"tex": tex, "particle": tex},
            "gui_light": "front",
            "elements": elements,
            "display": disp,
        })
        write_json(f"items/{item_id}.json", {"model": {
            "type": "minecraft:select",
            "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"],
                       "model": {"type": "minecraft:model", "model": f"{NS}:item/{item_id}"}}],
            "fallback": {"type": "minecraft:model", "model": f"{NS}:item/{item_id}_in_hand"},
        }})
    print(f"generated {len(ITEMS)} held item models")


if __name__ == "__main__":
    main()
