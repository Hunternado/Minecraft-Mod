#!/usr/bin/env python3
"""
Generates client assets: item/block models, blockstates, item model definitions, sounds.json, the
equipment asset, and all textures (original procedural placeholder art).

Re-run after editing:  python3 tools/gen_assets.py
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixelart import CLEAR, Image, hex_color, mix, paint_box, shade, solid  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "frieren")
NS = "frieren"

OUTLINE = hex_color("#1B1426")


def write_json(rel, obj):
    path = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def save(img, rel):
    path = os.path.join(ASSETS, "textures", rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


# =============================================================================================
# Item icons (16x16)
# =============================================================================================
def icon():
    return Image(16, 16)


def grimoire_icon(cover, accent, glyph, chains=False):
    img = icon()
    cover_c, accent_c, glyph_c = hex_color(cover), hex_color(accent), hex_color(glyph)
    # Pages (right edge) and cover.
    img.rect(3, 2, 12, 13, hex_color("#EFE6CF"))
    img.rect(2, 2, 11, 13, cover_c)
    img.rect(2, 2, 3, 13, shade(cover_c, 0.7))
    for y in range(3, 13):
        img.set(12, y, hex_color("#D8CBB0") if y % 2 else hex_color("#EFE6CF"))
    # Corner fittings and clasp.
    for (x, y) in ((4, 3), (10, 3), (4, 12), (10, 12)):
        img.set(x, y, accent_c)
    img.rect(11, 7, 13, 8, accent_c)
    # Glyph: a small magic circle.
    img.circle(7, 7.5, 2.4, glyph_c, fill=False)
    img.set(7, 7, glyph_c)
    img.set(7, 8, glyph_c)
    if chains:
        chain = hex_color("#8E8E9A")
        for i in range(2, 14):
            img.set(i, i, chain if i % 2 else shade(chain, 0.7))
            img.set(15 - i, i, chain if i % 2 else shade(chain, 0.7))
    img.outline_shape(OUTLINE)
    return img


def staff_icon(shaft, gem, head):
    img = icon()
    shaft_c, gem_c, head_c = hex_color(shaft), hex_color(gem), hex_color(head)
    for i in range(0, 11):
        img.set(2 + i, 13 - i, shaft_c)
        img.set(3 + i, 13 - i, shade(shaft_c, 0.75))
    # Head ornament around the gem.
    img.circle(12, 3.5, 2.6, head_c, fill=False)
    img.circle(12, 3.5, 1.6, gem_c)
    img.set(11, 2, shade(gem_c, 1.4))
    img.outline_shape(OUTLINE)
    return img


def gem_icon(color, highlight):
    img = icon()
    c, h = hex_color(color), hex_color(highlight)
    pts = [(8, 2), (12, 6), (8, 14), (4, 6)]
    for y in range(2, 15):
        for x in range(3, 14):
            # Point-in-diamond test.
            if abs(x - 8) / 4.5 + abs(y - 7) / (7.0 if y >= 7 else 5.0) <= 1.0:
                img.set(x, y, shade(c, 0.85 + 0.3 * (x < 8)))
    img.line(6, 5, 8, 3, h)
    img.set(9, 6, h)
    img.outline_shape(OUTLINE)
    return img


def horn_icon():
    img = icon()
    c = hex_color("#3A2232")
    for i in range(10):
        r = 1.8 - i * 0.12
        img.circle(4 + i, 13 - i * 1.1 - (i * i) * 0.04, max(0.6, r), shade(c, 1.0 + i * 0.04))
    img.set(13, 2, hex_color("#C9B8C0"))
    img.outline_shape(OUTLINE)
    return img


def orb_icon(color, glow):
    img = icon()
    img.circle(8, 8, 5, hex_color(glow))
    img.circle(8, 8, 3.6, hex_color(color))
    img.set(6, 6, (255, 255, 255, 255))
    img.outline_shape(OUTLINE)
    return img


def blade_icon():
    img = icon()
    steel, edge, handle = hex_color("#C8CCD8"), hex_color("#B0263A"), hex_color("#3A2232")
    for y in range(2, 11):
        for x in range(3, 13):
            if x - 3 <= (y - 2) + 2 and x <= 12:
                img.set(x, y, steel if x < 11 else edge)
    img.rect(6, 11, 8, 14, handle)
    img.rect(4, 11, 10, 11, hex_color("#D4AF37"))
    img.outline_shape(OUTLINE)
    return img


def potion_icon(liquid, flask=False):
    img = icon()
    glass, liquid_c = hex_color("#CFE7F2"), hex_color(liquid)
    if flask:
        img.rect(5, 5, 11, 14, hex_color("#7A5230"))
        img.rect(6, 7, 10, 13, liquid_c)
        img.rect(7, 2, 9, 4, hex_color("#5A3A20"))
    else:
        img.circle(8, 10, 4.2, glass)
        img.circle(8, 10.5, 3.2, liquid_c)
        img.rect(7, 3, 9, 6, glass)
        img.rect(7, 2, 9, 2, hex_color("#8B5A2B"))
        img.set(6, 9, (255, 255, 255, 255))
    img.outline_shape(OUTLINE)
    return img


def ring_icon():
    img = icon()
    img.circle(8, 9, 4.2, hex_color("#D4AF37"), fill=False)
    img.circle(8, 9, 3.4, hex_color("#B8962C"), fill=False)
    # Mirror-lotus gem.
    img.circle(8, 4.5, 2.0, hex_color("#9FD8FF"))
    img.set(7, 4, (255, 255, 255, 255))
    img.outline_shape(OUTLINE)
    return img


def charm_icon():
    img = icon()
    img.line(4, 2, 8, 7, hex_color("#8E8E9A"))
    img.line(12, 2, 8, 7, hex_color("#8E8E9A"))
    img.circle(8, 10, 3.5, hex_color("#5B4A8A"))
    img.circle(8, 10, 1.8, hex_color("#B7A8FF"))
    img.outline_shape(OUTLINE)
    return img


def insignia_icon():
    img = icon()
    img.rect(4, 3, 12, 10, hex_color("#B0263A"))
    for y in range(11, 15):
        img.rect(4 + (y - 10), y, 12 - (y - 10), y, hex_color("#B0263A"))
    img.line(5, 5, 11, 11, hex_color("#E8E8F0"))
    img.line(11, 5, 5, 11, hex_color("#E8E8F0"))
    img.outline_shape(OUTLINE)
    return img


def seal_icon():
    img = icon()
    img.circle(8, 8, 6, hex_color("#B0263A"))
    img.circle(8, 8, 4, hex_color("#D04050"), fill=False)
    img.line(8, 4, 8, 12, hex_color("#F3D27A"))
    img.line(4, 8, 12, 8, hex_color("#F3D27A"))
    img.outline_shape(OUTLINE)
    return img


def cage_icon():
    img = icon()
    bar = hex_color("#9AA0AA")
    img.rect(3, 3, 12, 3, bar)
    img.rect(3, 13, 12, 13, bar)
    for x in (3, 6, 9, 12):
        img.rect(x, 3, x, 13, bar)
    img.rect(7, 1, 8, 2, bar)
    img.outline_shape(OUTLINE)
    return img


def journal_icon():
    img = icon()
    img.rect(3, 2, 12, 13, hex_color("#7A5230"))
    img.rect(4, 3, 12, 12, hex_color("#8E6238"))
    img.rect(12, 3, 13, 12, hex_color("#EFE6CF"))
    # Pressed flower on the cover.
    img.set(8, 6, hex_color("#5FA8FF"))
    img.set(7, 7, hex_color("#5FA8FF"))
    img.set(9, 7, hex_color("#5FA8FF"))
    img.set(8, 8, hex_color("#5FA8FF"))
    img.line(8, 9, 8, 11, hex_color("#3E8A3E"))
    img.outline_shape(OUTLINE)
    return img


def sigil_icon(color, shape):
    img = icon()
    c = hex_color(color)
    if shape == "blade":
        img.rect(7, 2, 9, 12, hex_color("#C8CCD8"))
        img.rect(4, 11, 12, 12, c)
        img.rect(7, 13, 9, 14, c)
    elif shape == "eye":
        img.circle(8, 8, 5, c)
        img.circle(8, 8, 2, hex_color("#1B1426"))
        img.set(7, 7, (255, 255, 255, 255))
    else:
        for y in range(2, 15):
            w = 4 - abs(y - 8) // 2
            img.rect(8 - w, y, 8 + w // 2, y, c)
        img.set(7, 4, (255, 255, 255, 255))
    img.outline_shape(OUTLINE)
    return img


def armor_icon(kind, cloth, trim):
    img = icon()
    c, t = hex_color(cloth), hex_color(trim)
    if kind == "hat":
        for y in range(2, 11):
            half = (y - 2) // 2 + 1
            img.rect(8 - half, y, 8 + half, y, c)
        img.rect(2, 11, 13, 12, shade(c, 0.85))
        img.rect(4, 10, 11, 10, t)
    elif kind == "coat":
        img.rect(4, 2, 11, 13, c)
        img.rect(2, 3, 3, 9, c)
        img.rect(12, 3, 13, 9, c)
        img.rect(7, 2, 8, 13, t)
        img.rect(6, 2, 9, 3, shade(c, 0.7))
    elif kind == "trousers":
        img.rect(4, 2, 11, 5, c)
        img.rect(4, 6, 7, 13, c)
        img.rect(8, 6, 11, 13, c)
        img.rect(4, 2, 11, 2, t)
    else:
        img.rect(3, 8, 7, 13, c)
        img.rect(9, 8, 13, 13, c)
        img.rect(3, 12, 7, 13, shade(c, 0.6))
        img.rect(9, 12, 13, 13, shade(c, 0.6))
        img.rect(3, 8, 7, 8, t)
        img.rect(9, 8, 13, 8, t)
    img.outline_shape(OUTLINE)
    return img


def egg_icon(base, spots, seed):
    img = icon()
    b, s = hex_color(base), hex_color(spots)
    for y in range(2, 15):
        for x in range(3, 14):
            if ((x - 8) / 4.6) ** 2 + ((y - 9) / (6.4 if y < 9 else 5.6)) ** 2 <= 1.0:
                img.set(x, y, b)
    rng = random.Random(seed)
    for _ in range(9):
        x, y = rng.randint(5, 11), rng.randint(4, 13)
        if img.get(x, y)[3]:
            img.set(x, y, s)
    img.set(6, 5, shade(b, 1.3))
    img.outline_shape(OUTLINE)
    return img


def shard_icon():
    img = icon()
    pts = [(9, 1), (13, 6), (10, 14), (4, 9)]
    for y in range(1, 15):
        for x in range(3, 14):
            if (x - 4) * 0.9 >= (9 - y) * 0.6 and x <= 13 - (y - 6) * 0.4 and y <= 14 and x >= 4 + (y - 9) * 1.2:
                img.set(x, y, hex_color("#D8E6F0") if (x + y) % 3 else hex_color("#F8FCFF"))
    img.outline_shape(OUTLINE)
    return img


ITEM_ICONS = {
    "common_grimoire": lambda: grimoire_icon("#7A5230", "#C9A65A", "#EFE6CF"),
    "uncommon_grimoire": lambda: grimoire_icon("#2F6B3A", "#C9A65A", "#B8F0A0"),
    "rare_grimoire": lambda: grimoire_icon("#235A8C", "#D4AF37", "#9FD8FF"),
    "ancient_grimoire": lambda: grimoire_icon("#8A6A1E", "#F3D27A", "#FFF2C0"),
    "legendary_grimoire": lambda: grimoire_icon("#6A2C70", "#F3D27A", "#FFB7F0"),
    "demon_grimoire": lambda: grimoire_icon("#5A1020", "#8E8E9A", "#E0405A"),
    "forbidden_grimoire": lambda: grimoire_icon("#1E1030", "#9B2D6F", "#C9A0FF"),
    "sealed_demon_grimoire": lambda: grimoire_icon("#5A1020", "#8E8E9A", "#E0405A", chains=True),
    "sealed_exam_grimoire": lambda: grimoire_icon("#1F3A6B", "#D04050", "#F3D27A", chains=True),
    "apprentice_staff": lambda: staff_icon("#8B5A2B", "#9FD8FF", "#C9A65A"),
    "mage_staff": lambda: staff_icon("#5A3A20", "#7FB8FF", "#D4AF37"),
    "sages_staff": lambda: staff_icon("#E8E8F0", "#E0405A", "#D4AF37"),
    "guillotine_staff": lambda: staff_icon("#2A1A2A", "#B0263A", "#C8CCD8"),
    "mage_hat": lambda: armor_icon("hat", "#3A3F6B", "#D4AF37"),
    "mage_coat": lambda: armor_icon("coat", "#E8E8F0", "#D4AF37"),
    "mage_trousers": lambda: armor_icon("trousers", "#2E2E44", "#D4AF37"),
    "mage_boots": lambda: armor_icon("boots", "#5A3A20", "#D4AF37"),
    "mana_crystal": lambda: gem_icon("#4FA8FF", "#E8F6FF"),
    "demon_horn": horn_icon,
    "demonic_mana_core": lambda: orb_icon("#6A2C70", "#E0405A"),
    "guillotine_blade": blade_icon,
    "mana_potion": lambda: potion_icon("#4FA8FF"),
    "heiter_flask": lambda: potion_icon("#C0392B", flask=True),
    "mirror_lotus_ring": ring_icon,
    "concealment_charm": charm_icon,
    "demon_hunter_insignia": insignia_icon,
    "exam_seal": seal_icon,
    "stille_cage": cage_icon,
    "journey_journal": journal_icon,
    "sigil_of_the_guillotine": lambda: sigil_icon("#B0263A", "blade"),
    "seal_of_corruption": lambda: sigil_icon("#9B2D6F", "eye"),
    "mirror_shard": shard_icon,
}

SPAWN_EGGS = {
    "demon_soldier": ("#3A2232", "#B0263A"),
    "demon_mage": ("#4A2050", "#E0405A"),
    "aura_thrall": ("#6E7480", "#7A1E2C"),
    "mimic": ("#8B5A2B", "#F3D27A"),
    "stille": ("#CFE7F2", "#4FA8FF"),
    "wandering_mage": ("#6B5030", "#3A3F6B"),
    "examiner": ("#1F3A6B", "#D4AF37"),
    "priest": ("#F0F0F0", "#5FA8FF"),
    "frieren": ("#F4F4FA", "#3E8A3E"),
}

HANDHELD = {"apprentice_staff", "mage_staff", "sages_staff", "guillotine_staff", "guillotine_blade"}

# Block id -> kind
BLOCKS = {
    "mana_crystal_ore": "cube",
    "deepslate_mana_crystal_ore": "cube",
    "mana_crystal_block": "cube",
    "cursed_gold_block": "cube",
    "spell_research_desk": "bottom_top",
    "summoning_altar": "bottom_top",
    "blue_moon_weed": "cross",
    "mana_light": "orb",
}


# =============================================================================================
# Block textures
# =============================================================================================
def ore_texture(base, seed):
    img = Image(16, 16)
    img.noise_fill(0, 0, 15, 15, hex_color(base), 0.16, seed)
    rng = random.Random(seed + 7)
    crystal, glow = hex_color("#4FA8FF"), hex_color("#CFF0FF")
    for _ in range(4):
        cx, cy = rng.randint(2, 13), rng.randint(2, 13)
        img.set(cx, cy, glow)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            img.set(cx + dx, cy + dy, crystal)
        img.set(cx + 1, cy + 1, shade(crystal, 0.7))
    return img


def crystal_block_texture():
    img = Image(16, 16)
    img.noise_fill(0, 0, 15, 15, hex_color("#3A86D8"), 0.12, 99)
    for i in range(0, 16, 4):
        img.line(i, 0, 15, 15 - i, hex_color("#7FC4FF"))
        img.line(0, i, 15 - i, 15, hex_color("#2A66B0"))
    img.outline_rect(0, 0, 15, 15, hex_color("#1E4E8C"))
    return img


def cursed_gold_texture():
    img = Image(16, 16)
    img.noise_fill(0, 0, 15, 15, hex_color("#C9A030"), 0.18, 55)
    rng = random.Random(56)
    for _ in range(10):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        img.set(x, y, hex_color("#6A4A10"))
    img.outline_rect(0, 0, 15, 15, hex_color("#8A6A1E"))
    return img


def desk_textures():
    top = Image(16, 16)
    top.noise_fill(0, 0, 15, 15, hex_color("#8E6238"), 0.08, 11)
    top.rect(3, 3, 9, 10, hex_color("#EFE6CF"))
    top.line(4, 5, 8, 5, hex_color("#7A6A5A"))
    top.line(4, 7, 8, 7, hex_color("#7A6A5A"))
    top.circle(12, 5, 1.5, hex_color("#4FA8FF"))
    top.rect(11, 9, 13, 12, hex_color("#3A2232"))
    side = Image(16, 16)
    side.noise_fill(0, 0, 15, 15, hex_color("#7A5230"), 0.08, 12)
    side.rect(0, 0, 15, 2, hex_color("#5A3A20"))
    side.rect(1, 3, 2, 15, hex_color("#5A3A20"))
    side.rect(13, 3, 14, 15, hex_color("#5A3A20"))
    side.rect(4, 6, 11, 9, hex_color("#6B4528"))
    bottom = Image(16, 16)
    bottom.noise_fill(0, 0, 15, 15, hex_color("#5A3A20"), 0.08, 13)
    return top, side, bottom


def altar_textures():
    top = Image(16, 16)
    top.noise_fill(0, 0, 15, 15, hex_color("#2A2030"), 0.1, 21)
    top.circle(7.5, 7.5, 6, hex_color("#B0263A"), fill=False)
    top.circle(7.5, 7.5, 3, hex_color("#E0405A"), fill=False)
    for a in range(6):
        ang = a * math.pi / 3
        top.set(int(7.5 + math.cos(ang) * 5), int(7.5 + math.sin(ang) * 5), hex_color("#F3D27A"))
    side = Image(16, 16)
    side.noise_fill(0, 0, 15, 15, hex_color("#241A2A"), 0.12, 22)
    for y in (0, 15):
        side.rect(0, y, 15, y, hex_color("#3A2A40"))
    side.rect(6, 4, 9, 11, hex_color("#5A1020"))
    side.rect(7, 5, 8, 10, hex_color("#B0263A"))
    bottom = Image(16, 16)
    bottom.noise_fill(0, 0, 15, 15, hex_color("#1E1624"), 0.1, 23)
    return top, side, bottom


def flower_texture():
    img = Image(16, 16)
    stem, leaf = hex_color("#3E8A3E"), hex_color("#5FAF4F")
    img.line(8, 15, 8, 7, stem)
    img.line(8, 12, 5, 10, leaf)
    img.line(8, 11, 11, 9, leaf)
    petal, center = hex_color("#4F78FF"), hex_color("#CFE7FF")
    for (x, y) in ((8, 3), (6, 4), (10, 4), (7, 6), (9, 6), (8, 5), (6, 5), (10, 5), (8, 2)):
        img.set(x, y, petal)
    img.set(8, 4, center)
    img.set(7, 5, shade(petal, 1.25))
    return img


def light_texture():
    img = Image(16, 16)
    img.circle(7.5, 7.5, 7, hex_color("#FFF6C8"))
    img.circle(7.5, 7.5, 4.5, hex_color("#FFFFFF"))
    return img


# =============================================================================================
# Entity textures (64x64 humanoid skin layout)
# =============================================================================================
def humanoid_skin(p, seed=0, headless=False, horns=None, hat=None, glow_eyes=False):
    """
    p: palette dict with keys skin, hair, eyes, top, bottom, shoes, accent, (cape optional).
    Head (0,0) 8x8x8, hat overlay (32,0), body (16,16) 8x12x4, right arm (40,16) 4x12x4,
    left arm (32,48), right leg (0,16) 4x12x4, left leg (16,48).
    """
    img = Image(64, 64)
    skin, hair, eyes = hex_color(p["skin"]), hex_color(p["hair"]), hex_color(p["eyes"])
    top, bottom, shoes, accent = hex_color(p["top"]), hex_color(p["bottom"]), hex_color(p["shoes"]), hex_color(p["accent"])

    if not headless:
        def head_front(i, x, y, w, h):
            i.noise_fill(x, y, x + w - 1, y + h - 1, skin, 0.04, seed)
            i.rect(x, y, x + w - 1, y + 2, hair)          # fringe
            i.set(x, y + 3, hair)
            i.set(x + w - 1, y + 3, hair)
            ey = y + 4
            i.set(x + 2, ey, eyes)
            i.set(x + 5, ey, eyes)
            if glow_eyes:
                i.set(x + 1, ey, shade(eyes, 0.6))
                i.set(x + 6, ey, shade(eyes, 0.6))
            else:
                i.set(x + 2, ey - 1, (255, 255, 255, 255))
                i.set(x + 5, ey - 1, (255, 255, 255, 255))
            i.set(x + 3, y + 6, shade(skin, 0.85))
            i.set(x + 4, y + 6, shade(skin, 0.85))

        def head_side(i, x, y, w, h):
            i.noise_fill(x, y, x + w - 1, y + h - 1, hair, 0.06, seed + 1)
            i.rect(x + 3, y + 4, x + 5, y + 7, skin)

        paint_box(img, 0, 0, 8, 8, 8, {"front": head_front, "top": solid(hair, 0.06, seed),
                                       "back": solid(hair, 0.06, seed + 2), "right": head_side, "left": head_side,
                                       "bottom": solid(skin, 0.03, seed)})

        # Overlay layer: long hair at the back/sides, optional hat, optional horns.
        def overlay_back(i, x, y, w, h):
            i.noise_fill(x, y, x + w - 1, y + h - 1, hair, 0.08, seed + 3)

        def overlay_side(i, x, y, w, h):
            i.noise_fill(x, y + 3, x + w - 1, y + h - 1, hair, 0.08, seed + 4)
            if horns:
                hc = hex_color(horns)
                i.rect(x + 2, y, x + 4, y + 2, hc)

        def overlay_front(i, x, y, w, h):
            if horns:
                hc = hex_color(horns)
                i.rect(x, y, x + 1, y + 1, hc)
                i.rect(x + w - 2, y, x + w - 1, y + 1, hc)
            if hat:
                hc = hex_color(hat)
                i.rect(x, y, x + w - 1, y + 1, hc)

        def overlay_top(i, x, y, w, h):
            if hat:
                i.noise_fill(x, y, x + w - 1, y + h - 1, hex_color(hat), 0.06, seed + 5)

        paint_box(img, 32, 0, 8, 8, 8, {"back": overlay_back, "right": overlay_side, "left": overlay_side,
                                        "front": overlay_front, "top": overlay_top})

    # Body
    def body_front(i, x, y, w, h):
        i.noise_fill(x, y, x + w - 1, y + h - 1, top, 0.06, seed + 10)
        if headless:
            i.rect(x + 2, y, x + w - 3, y, hex_color("#7A1E2C"))
        i.rect(x + 3, y, x + 4, y + h - 1, accent)
        i.rect(x, y + 8, x + w - 1, y + 8, shade(accent, 0.8))

    paint_box(img, 16, 16, 8, 12, 4, {"front": body_front, "back": solid(top, 0.06, seed + 11),
                                      "right": solid(shade(top, 0.9), 0.06, seed + 12), "left": solid(shade(top, 0.9), 0.06, seed + 13),
                                      "top": solid(hex_color("#7A1E2C") if headless else top, 0.05, seed + 14),
                                      "bottom": solid(top, 0.05, seed + 15)})

    # Arms: sleeve + hand.
    def arm(i, x, y, w, h):
        i.noise_fill(x, y, x + w - 1, y + h - 4, top, 0.06, seed + 20 + x)
        i.rect(x, y + h - 4, x + w - 1, y + h - 4, accent)
        i.noise_fill(x, y + h - 3, x + w - 1, y + h - 1, skin if not headless else shade(top, 0.7), 0.04, seed + 21)

    for (u, v) in ((40, 16), (32, 48)):
        paint_box(img, u, v, 4, 12, 4, {"all": arm, "top": solid(top, 0.05, seed + 22),
                                        "bottom": solid(skin if not headless else top, 0.03, seed + 23)})

    # Legs: trousers/skirt + shoes.
    def leg(i, x, y, w, h):
        i.noise_fill(x, y, x + w - 1, y + h - 3, bottom, 0.06, seed + 30 + x)
        i.noise_fill(x, y + h - 2, x + w - 1, y + h - 1, shoes, 0.05, seed + 31)

    for (u, v) in ((0, 16), (16, 48)):
        paint_box(img, u, v, 4, 12, 4, {"all": leg, "top": solid(bottom, 0.05, seed + 32), "bottom": solid(shoes, 0.03, seed + 33)})
    return img


CHARACTERS = {
    "frieren": dict(skin="#F6E3D6", hair="#ECEEF4", eyes="#3E8A3E", top="#F4F4FA", bottom="#F4F4FA", shoes="#5A3A20", accent="#D4AF37"),
    "fern": dict(skin="#F3DCCB", hair="#6A4A8C", eyes="#5B3A7A", top="#1E1A2A", bottom="#1E1A2A", shoes="#2A2030", accent="#E8E8F0"),
    "stark": dict(skin="#EFC9A8", hair="#B5432A", eyes="#5A3A20", top="#2E3A5A", bottom="#3A3030", shoes="#4A3020", accent="#C9A65A"),
    "serie": dict(skin="#F6E3D6", hair="#F3D27A", eyes="#7A5A2A", top="#F0E6D0", bottom="#F0E6D0", shoes="#8A6A3A", accent="#D4AF37"),
    "examiner": dict(skin="#EAC8AA", hair="#2A2A3A", eyes="#2A3A5A", top="#1F3A6B", bottom="#1A2A4A", shoes="#151520", accent="#D4AF37"),
    "priest": dict(skin="#EFCFB5", hair="#8B6B4A", eyes="#3A5A7A", top="#F0F0F0", bottom="#E0E0E8", shoes="#5A4030", accent="#5FA8FF"),
    "wandering_mage": dict(skin="#E8C4A0", hair="#5A3A20", eyes="#3A2A20", top="#6B5030", bottom="#4A3A2A", shoes="#3A2A1A", accent="#3A3F6B"),
    "demon_soldier": dict(skin="#D8C0C8", hair="#2A1A2A", eyes="#E0405A", top="#3A2232", bottom="#2A1A22", shoes="#1A1016", accent="#B0263A"),
    "demon_mage": dict(skin="#E0CCD8", hair="#3A2A5A", eyes="#FF5070", top="#4A2050", bottom="#2E1A36", shoes="#1A1016", accent="#E0405A"),
    "aura": dict(skin="#F2DCE4", hair="#F0A0C8", eyes="#B0263A", top="#2A1A2A", bottom="#3A2240", shoes="#1A1016", accent="#D4AF37"),
    "qual": dict(skin="#D0B8C8", hair="#1E1030", eyes="#C060A0", top="#3A1A40", bottom="#2A1430", shoes="#140A16", accent="#9B2D6F"),
    "aura_thrall": dict(skin="#7A7E88", hair="#000000", eyes="#000000", top="#6E7480", bottom="#565A66", shoes="#3A3E48", accent="#8E949E"),
    "spiegel": dict(skin="#D8E6F0", hair="#B8C8D8", eyes="#7FD4FF", top="#C8D4E0", bottom="#A8B8C8", shoes="#8898A8", accent="#F8FCFF"),
    "mirror_replica": dict(skin="#CCDDEE", hair="#AABBDD", eyes="#E8F6FF", top="#B8CCE0", bottom="#98ACC4", shoes="#7890A8", accent="#E8F6FF"),
    "mana_decoy": dict(skin="#BFE6FF", hair="#7FC4FF", eyes="#FFFFFF", top="#9FD8FF", bottom="#7FC4FF", shoes="#5FA8E8", accent="#E8F6FF"),
}

CHARACTER_EXTRAS = {
    "demon_soldier": dict(horns="#1A1016", glow_eyes=True),
    "demon_mage": dict(horns="#2A1A2A", glow_eyes=True),
    "aura": dict(horns="#3A2232", glow_eyes=True),
    "qual": dict(horns="#140A16", glow_eyes=True),
    "aura_thrall": dict(headless=True),
    "wandering_mage": dict(hat="#3A3F6B"),
    "examiner": dict(hat="#1A2A4A"),
    "spiegel": dict(glow_eyes=True),
    "mirror_replica": dict(glow_eyes=True),
}


def mimic_texture():
    img = Image(64, 64)
    wood, dark, metal = hex_color("#8B5A2B"), hex_color("#5A3A20"), hex_color("#C9A65A")
    teeth, tongue, mouth = hex_color("#F4F0E0"), hex_color("#C0392B"), hex_color("#2A0A10")

    def wood_face(i, x, y, w, h):
        i.noise_fill(x, y, x + w - 1, y + h - 1, wood, 0.1, x * 7 + y)
        i.outline_rect(x, y, x + w - 1, y + h - 1, dark)

    def lid_bottom(i, x, y, w, h):
        i.noise_fill(x, y, x + w - 1, y + h - 1, mouth, 0.05, 3)
        for tx in range(x, x + w, 2):
            i.set(tx, y, teeth)
            i.set(tx, y + h - 1, teeth)

    def base_top(i, x, y, w, h):
        i.noise_fill(x, y, x + w - 1, y + h - 1, mouth, 0.05, 4)
        i.rect(x + 3, y + 3, x + w - 4, y + h - 4, tongue)
        for tx in range(x, x + w, 2):
            i.set(tx, y, teeth)
            i.set(tx, y + h - 1, teeth)

    paint_box(img, 0, 0, 14, 5, 14, {"all": wood_face, "bottom": lid_bottom})
    paint_box(img, 0, 19, 14, 10, 14, {"all": wood_face, "top": base_top})
    # Latch (second box on the lid uses texOffs(0,0) as well; keep a metal patch there).
    img.rect(14, 14, 17, 18, metal)
    return img


def stille_texture():
    img = Image(32, 32)
    body, belly, beak = hex_color("#4FA8FF"), hex_color("#CFE7F2"), hex_color("#F3D27A")
    paint_box(img, 0, 0, 4, 4, 5, {"all": solid(body, 0.08, 1), "bottom": solid(belly, 0.05, 2)})
    paint_box(img, 0, 10, 1, 1, 1, {"all": solid(beak, 0.02, 3)})
    paint_box(img, 18, 0, 2, 1, 3, {"all": solid(shade(body, 0.8), 0.05, 4)})
    paint_box(img, 0, 16, 5, 1, 4, {"all": solid(shade(body, 1.15), 0.06, 5)})
    paint_box(img, 0, 22, 5, 1, 4, {"all": solid(shade(body, 1.15), 0.06, 6)})
    # Eyes on the body front.
    img.set(5, 6, hex_color("#101018"))
    img.set(8, 6, hex_color("#101018"))
    return img


def equipment_textures():
    """Mage's Attire in the 64x32 armor layout (humanoid and humanoid_leggings layers)."""
    cloth, trim, hat = hex_color("#E8E8F0"), hex_color("#D4AF37"), hex_color("#3A3F6B")
    outer = Image(64, 32)
    paint_box(outer, 0, 0, 8, 8, 8, {"all": solid(hat, 0.06, 1), "bottom": None})
    for face in ("front",):
        pass
    paint_box(outer, 16, 16, 8, 12, 4, {"all": solid(cloth, 0.05, 2)})
    outer.rect(23, 20, 24, 31, trim)
    paint_box(outer, 40, 16, 4, 12, 4, {"all": solid(cloth, 0.05, 3)})
    paint_box(outer, 0, 16, 4, 12, 4, {"all": solid(hex_color("#5A3A20"), 0.05, 4)})
    inner = Image(64, 32)
    paint_box(inner, 16, 16, 8, 12, 4, {"all": solid(hex_color("#2E2E44"), 0.05, 5)})
    paint_box(inner, 0, 16, 4, 12, 4, {"all": solid(hex_color("#2E2E44"), 0.05, 6)})
    inner.rect(16, 20, 39, 20, trim)
    return outer, inner


# =============================================================================================
# JSON assets
# =============================================================================================
SOUNDS = {
    "spell.cast": "minecraft:entity.evoker.prepare_attack",
    "spell.fail": "minecraft:block.fire.extinguish",
    "spell.zoltraak": "minecraft:entity.illusioner.cast_spell",
    "spell.barrier_hit": "minecraft:item.shield.block",
    "spell.barrier_break": "minecraft:block.glass.break",
    "spell.heal": "minecraft:block.amethyst_block.chime",
    "spell.flower_bloom": "minecraft:item.bone_meal.use",
    "mana.conceal": "minecraft:block.beacon.deactivate",
    "mana.release": "minecraft:block.beacon.activate",
    "mana.detect": "minecraft:block.amethyst_block.resonate",
    "grimoire.learn": "minecraft:block.enchantment_table.use",
    "rank.up": "minecraft:ui.toast.challenge_complete",
    "demon.speak": "minecraft:entity.evoker.ambient",
    "aura.scales": "minecraft:block.bell.resonate",
    "mimic.chomp": "minecraft:entity.evoker_fangs.attack",
    "magic.teleport": "minecraft:entity.enderman.teleport",
    "spell.hellfire": "minecraft:entity.blaze.shoot",
    "spell.wind": "minecraft:entity.breeze.wind_burst",
    "spell.light_rain": "minecraft:block.beacon.activate",
    "spell.slash": "minecraft:entity.player.attack.sweep",
    "spell.curse": "minecraft:entity.elder_guardian.curse",
    "spell.barrier_up": "minecraft:block.respawn_anchor.charge",
    "spell.flight": "minecraft:entity.phantom.flap",
    "spell.utility": "minecraft:block.amethyst_block.chime",
    "demon.bolt": "minecraft:entity.shulker.shoot",
    "boss.roar": "minecraft:entity.ravager.roar",
}


def gen_models():
    for item_id in ITEM_ICONS:
        parent = "minecraft:item/handheld" if item_id in HANDHELD else "minecraft:item/generated"
        write_json(f"models/item/{item_id}.json", {"parent": parent, "textures": {"layer0": f"{NS}:item/{item_id}"}})
        write_json(f"items/{item_id}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{item_id}"}})
    for mob in SPAWN_EGGS:
        item_id = f"{mob}_spawn_egg"
        write_json(f"models/item/{item_id}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{item_id}"}})
        write_json(f"items/{item_id}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{item_id}"}})

    for block, kind in BLOCKS.items():
        if kind == "cube":
            write_json(f"models/block/{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{block}"}})
        elif kind == "bottom_top":
            write_json(f"models/block/{block}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
                "top": f"{NS}:block/{block}_top", "side": f"{NS}:block/{block}_side", "bottom": f"{NS}:block/{block}_bottom"}})
        elif kind == "cross":
            write_json(f"models/block/{block}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                      "textures": {"cross": f"{NS}:block/{block}"}})
        elif kind == "orb":
            tex = f"{NS}:block/{block}"
            face = {"texture": "#orb", "uv": [0, 0, 16, 16]}
            write_json(f"models/block/{block}.json", {
                "render_type": "minecraft:cutout", "ambientocclusion": False,
                "textures": {"orb": tex, "particle": tex},
                "elements": [{"from": [5, 5, 5], "to": [11, 11, 11], "shade": False,
                              "faces": {d: dict(face) for d in ("north", "south", "east", "west", "up", "down")}}]})
        write_json(f"blockstates/{block}.json", {"variants": {"": {"model": f"{NS}:block/{block}"}}})
        if block == "mana_light":
            continue
        if kind == "cross":
            write_json(f"models/item/{block}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/{block}"}})
            write_json(f"items/{block}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{block}"}})
        else:
            write_json(f"items/{block}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{block}"}})

    write_json("equipment/mage_attire.json", {"layers": {
        "humanoid": [{"texture": f"{NS}:mage_attire"}],
        "humanoid_leggings": [{"texture": f"{NS}:mage_attire"}]}})

    write_json("sounds.json", {name: {"sounds": [{"name": target, "type": "event"}], "subtitle": f"subtitles.{NS}.{name}"}
                               for name, target in SOUNDS.items()})


def gen_textures():
    for item_id, painter in ITEM_ICONS.items():
        save(painter(), f"item/{item_id}.png")
    for i, (mob, (base, spots)) in enumerate(SPAWN_EGGS.items()):
        save(egg_icon(base, spots, i), f"item/{mob}_spawn_egg.png")

    save(ore_texture("#7F7F7F", 1), "block/mana_crystal_ore.png")
    save(ore_texture("#4A4A52", 2), "block/deepslate_mana_crystal_ore.png")
    save(crystal_block_texture(), "block/mana_crystal_block.png")
    save(cursed_gold_texture(), "block/cursed_gold_block.png")
    top, side, bottom = desk_textures()
    save(top, "block/spell_research_desk_top.png")
    save(side, "block/spell_research_desk_side.png")
    save(bottom, "block/spell_research_desk_bottom.png")
    top, side, bottom = altar_textures()
    save(top, "block/summoning_altar_top.png")
    save(side, "block/summoning_altar_side.png")
    save(bottom, "block/summoning_altar_bottom.png")
    save(flower_texture(), "block/blue_moon_weed.png")
    save(light_texture(), "block/mana_light.png")

    for index, (name, palette) in enumerate(CHARACTERS.items()):
        save(humanoid_skin(palette, seed=index * 101, **CHARACTER_EXTRAS.get(name, {})), f"entity/{name}.png")
    save(mimic_texture(), "entity/mimic.png")
    save(stille_texture(), "entity/stille.png")
    outer, inner = equipment_textures()
    save(outer, "entity/equipment/humanoid/mage_attire.png")
    save(inner, "entity/equipment/humanoid_leggings/mage_attire.png")


def main():
    gen_models()
    gen_textures()
    print("assets generated in", os.path.normpath(ASSETS))


if __name__ == "__main__":
    main()
