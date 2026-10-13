"""
Generates the mod's GUI art as sprites for the vanilla GUI atlas (assets/frieren/textures/gui/sprites/), with
nine-slice .mcmeta where a sprite stretches, plus one icon per spell and the Java list of spells that have one.

The look is a mage's grimoire: an indigo leather cover with gold rules, cream parchment pages with a shaded gutter,
silk bookmark ribbons per school, ink-drawn rows and buttons, and a faint magic circle on the facing page. The HUD
uses the same gold and silver on dark glass so it reads over any world.

Usage: python3 tools/gen_gui.py
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from pixelart import Image, hex_color, mix  # noqa: E402
import gen_data  # noqa: E402
import gui_icons  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
SPRITES = os.path.join(REPO, "src", "main", "resources", "assets", "frieren", "textures", "gui", "sprites")
JAVA = os.path.join(REPO, "src", "main", "java", "com", "hunternado", "frieren", "client", "gui", "SpellIcons.java")

CLEAR = (0, 0, 0, 0)
LEATHER = hex_color("#2C3150")
LEATHER_D = hex_color("#1B1F36")
LEATHER_L = hex_color("#3E4570")
GOLD = hex_color("#D9B45A")
GOLD_D = hex_color("#94722A")
GOLD_L = hex_color("#F6E3A0")
PARCH = hex_color("#F1E6C8")
PARCH_L = hex_color("#F8F0DA")
PARCH_M = hex_color("#E4D3AA")
PARCH_E = hex_color("#CDB888")
PARCH_D = hex_color("#A88E5E")
INK = hex_color("#3A2A1C")
SILVER = hex_color("#AEB4C8")
SILVER_L = hex_color("#E2E6F0")
SILVER_D = hex_color("#6E7488")
GLASS = (14, 16, 30, 200)


def vary(c, rng, amount):
    k = rng.uniform(-amount, amount)
    return (max(0, min(255, int(c[0] + k))), max(0, min(255, int(c[1] + k))), max(0, min(255, int(c[2] + k * 0.9))), c[3])


def canvas(w, h, fill=CLEAR):
    img = Image(w, h)
    for y in range(h):
        for x in range(w):
            img.pixels[y][x] = fill
    return img


def put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.pixels[y][x] = c


def save(img, rel, scaling=None):
    path = os.path.join(SPRITES, rel + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    meta = path + ".mcmeta"
    if scaling:
        with open(meta, "w", encoding="utf-8") as f:
            json.dump({"gui": {"scaling": scaling}}, f, indent=2)
            f.write("\n")
    elif os.path.exists(meta):
        os.remove(meta)


def nine(w, h, border):
    return {"type": "nine_slice", "width": w, "height": h, "border": border}


# ---------------------------------------------------------------------------------------------
# The book
# ---------------------------------------------------------------------------------------------
def cover():
    """48x48 leather with a double gold rule and corner studs; nine-slice with 12px borders."""
    rng = random.Random(7)
    img = canvas(48, 48)
    for y in range(48):
        for x in range(48):
            put(img, x, y, vary(LEATHER, rng, 7))
    for i in range(48):
        for (x, y) in ((i, 0), (i, 47), (0, i), (47, i)):
            put(img, x, y, LEATHER_D)
        if 0 < i < 47:
            put(img, i, 1, mix(LEATHER, LEATHER_L, 0.7))
            put(img, 1, i, mix(LEATHER, LEATHER_L, 0.5))
    for inset, col in ((4, GOLD_D), (5, GOLD), (8, GOLD_D)):
        for i in range(inset, 48 - inset):
            for (x, y) in ((i, inset), (i, 47 - inset), (inset, i), (47 - inset, i)):
                put(img, x, y, col)
    for cx, cy in ((6, 6), (41, 6), (6, 41), (41, 41)):
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if abs(dx) + abs(dy) <= 2:
                    put(img, cx + dx, cy + dy, GOLD if abs(dx) + abs(dy) < 2 else GOLD_D)
        put(img, cx, cy, GOLD_L)
    return img


def page(gutter_right):
    """32x32 parchment: stacked-page edges on the outside, a shadowed gutter toward the spine."""
    rng = random.Random(11 if gutter_right else 12)
    img = canvas(32, 32)
    for y in range(32):
        for x in range(32):
            c = vary(PARCH, rng, 5)
            if rng.random() < 0.04:
                c = mix(c, PARCH_M, 0.6)
            put(img, x, y, c)
    for y in range(32):
        for g in range(8):
            x = 31 - g if gutter_right else g
            put(img, x, y, mix(img.pixels[y][x], PARCH_D, (1 - g / 8) ** 1.6 * 0.55))
    outer = [0, 1, 2] if gutter_right else [31, 30, 29]
    for y in range(32):
        for col, c in zip(outer, (PARCH_D, PARCH_M, PARCH_E)):
            put(img, col, y, c)
    for x in range(32):
        put(img, x, 0, PARCH_E)
        for row, c in ((31, PARCH_D), (30, PARCH_M), (29, PARCH_E)):
            put(img, x, row, mix(img.pixels[row][x], c, 0.85))
    return img


def ribbon():
    """A silk bookmark, near-white so it can be tinted per school: shaded edges and a swallowtail notch."""
    img = canvas(10, 30)
    for y in range(30):
        for x in range(10):
            notch = y >= 25 and abs(x - 4.5) < (y - 24)
            if notch:
                continue
            c = (236, 236, 236, 255)
            if x in (0, 9):
                c = (176, 176, 176, 255)
            elif x in (1, 8):
                c = (212, 212, 212, 255)
            elif x == 3:
                c = (250, 250, 250, 255)
            put(img, x, y, c)
    return img


def row_hover():
    img = canvas(16, 12, (110, 80, 40, 34))
    for x in range(16):
        put(img, x, 11, (110, 80, 40, 80))
    return img


def row_selected():
    img = canvas(16, 12, (214, 170, 80, 70))
    for x in range(16):
        put(img, x, 0, (170, 120, 40, 150))
        put(img, x, 11, (170, 120, 40, 150))
    for y in range(12):
        put(img, 0, y, (150, 100, 30, 230))
        put(img, 1, y, (200, 150, 60, 200))
    return img


def button(state):
    """24x14 ink-bordered stamp for parchment; rounded corners."""
    base = {"normal": PARCH_M, "hover": mix(PARCH_L, GOLD_L, 0.35), "disabled": mix(PARCH, PARCH_M, 0.5)}[state]
    edge = {"normal": INK, "hover": GOLD_D, "disabled": PARCH_E}[state]
    img = canvas(24, 14)
    for y in range(14):
        for x in range(24):
            if (x in (0, 23)) and (y in (0, 13)):
                continue
            border = x in (0, 23) or y in (0, 13)
            c = edge if border else base
            if not border and y == 1:
                c = mix(base, PARCH_L, 0.7)
            if not border and y == 12:
                c = mix(base, PARCH_D, 0.35)
            put(img, x, y, c)
    return img


def circle():
    """96x96 faint ink magic circle: double outer ring, rune ticks, a hexagram and an inner seal."""
    img = canvas(96, 96)
    cx = cy = 47.5
    ink = INK[:3]

    def plot(x, y, a):
        if 0 <= x < 96 and 0 <= y < 96:
            old = img.pixels[y][x][3]
            img.pixels[y][x] = ink + (min(255, max(old, int(a))),)

    rings = ((46.0, 0.8, 110), (43.0, 0.6, 90), (31.0, 0.7, 100), (12.0, 0.6, 90), (8.5, 0.5, 70))
    pts = [(cx + 43.0 * math.cos(math.radians(a - 90)), cy + 43.0 * math.sin(math.radians(a - 90))) for a in range(0, 360, 60)]
    lines = [(pts[i], pts[(i + 2) % 6]) for i in range(6)]
    for y in range(96):
        for x in range(96):
            px, py = x + 0.5, y + 0.5
            d = math.hypot(px - cx, py - cy)
            a = 0.0
            for r, w, alpha in rings:
                a = max(a, alpha * max(0.0, 1.0 - abs(d - r) / (w + 0.5)))
            for (x0, y0), (x1, y1) in lines:
                dx, dy = x1 - x0, y1 - y0
                t = max(0.0, min(1.0, ((px - x0) * dx + (py - y0) * dy) / (dx * dx + dy * dy)))
                dist = math.hypot(px - x0 - t * dx, py - y0 - t * dy)
                a = max(a, 75 * max(0.0, 1.0 - dist / 0.9))
            if 33.0 < d < 41.5:
                ang = (math.degrees(math.atan2(py - cy, px - cx)) + 360) % 15
                if ang < 1.4 or (6.5 < ang < 8.5 and 35.5 < d < 39.0):
                    a = max(a, 85)
            if a > 4:
                plot(x, y, a)
    return img


def slot(selected):
    """22x22 HUD slot (24x24 with a glow when selected): metal frame, studs, dark glass."""
    size = 24 if selected else 22
    o = 1 if selected else 0
    light, mid, dark = (GOLD_L, GOLD, GOLD_D) if selected else (SILVER_L, SILVER, SILVER_D)
    img = canvas(size, size)
    if selected:
        for i in range(size):
            for (x, y) in ((i, 0), (i, size - 1), (0, i), (size - 1, i)):
                put(img, x, y, (255, 214, 110, 110))
    for y in range(22):
        for x in range(22):
            X, Y = x + o, y + o
            if x in (0, 21) or y in (0, 21):
                c = (20, 20, 32, 255)
            elif x == 1 or y == 1:
                c = light
            elif x == 20 or y == 20:
                c = dark
            else:
                c = GLASS if y > 4 else (40, 46, 70, 205)
            put(img, X, Y, c)
    for (x, y) in ((1, 1), (19, 1), (1, 19), (19, 19)):
        for dy in range(2):
            for dx in range(2):
                put(img, x + dx + o, y + dy + o, mid if (dx + dy) else light)
    return img


def mana_frame():
    """40x9 frame for the mana bar with a small gem at each end; nine-slice (6px ends, 3px top/bottom)."""
    img = canvas(40, 9)
    for y in range(9):
        for x in range(40):
            edge = x in (0, 39) or y in (0, 8)
            c = (18, 18, 30, 255) if edge else (8, 10, 22, 210)
            if not edge and y == 1:
                c = SILVER_D
            if not edge and y == 7:
                c = (30, 32, 48, 230)
            put(img, x, y, c)
    for gx in (3, 36):
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if abs(dx) + abs(dy) <= 2:
                    put(img, gx + dx, 4 + dy, GOLD if abs(dx) + abs(dy) < 2 else GOLD_D)
        put(img, gx, 4, hex_color("#8FD8FF"))
    return img


def mana_fill(conceal):
    rows = (("#D6CCFF", "#A090F0", "#6A58C8") if conceal else ("#B8EAFF", "#5FB4FF", "#2F7FE0"))
    img = canvas(16, 3)
    for y, hexc in enumerate(rows):
        for x in range(16):
            put(img, x, y, hex_color(hexc))
    put(img, 3, 0, (255, 255, 255, 255))
    put(img, 11, 1, mix(hex_color(rows[0]), (255, 255, 255, 255), 0.6))
    return img


def gem(color):
    img = canvas(7, 7)
    c = hex_color(color)
    for y in range(7):
        for x in range(7):
            d = abs(x - 3) + abs(y - 3)
            if d <= 3:
                put(img, x, y, (40, 28, 20, 255) if d == 3 else (mix(c, (255, 255, 255, 255), 0.45) if x + y < 6 else c))
    put(img, 2, 2, (255, 255, 255, 255))
    return img


def stat_icon(kind):
    """9x9 ink glyphs for the spell page: mana drop, hourglass, casting sparkle, mastery star."""
    img = canvas(9, 9)
    if kind == "mana":
        blue, light = hex_color("#3F8FE0"), hex_color("#A8DCFF")
        for y in range(9):
            for x in range(9):
                cx, cy = 4, 5.4
                inside = (x - cx) ** 2 + (y - cy) ** 2 <= 8.5 or (y < 5 and abs(x - cx) <= (y - 0.2) * 0.75)
                if inside:
                    put(img, x, y, blue)
        put(img, 3, 4, light)
        put(img, 3, 5, light)
    elif kind == "cooldown":
        for y in range(9):
            w = abs(y - 4)
            for x in range(4 - w, 5 + w):
                put(img, x, y, INK if y in (0, 8) or x in (4 - w, 4 + w) else hex_color("#E8D29A"))
        for (x, y) in ((4, 3), (4, 5), (3, 6), (4, 6), (5, 6), (2, 7), (6, 7)):
            put(img, x, y, hex_color("#C8963C"))
    elif kind == "cast":
        g = hex_color("#C8963C")
        for i in range(9):
            put(img, 4, i, g)
            put(img, i, 4, g)
        for (x, y) in ((2, 2), (6, 2), (2, 6), (6, 6)):
            put(img, x, y, g)
        put(img, 4, 4, hex_color("#FFF0B0"))
    elif kind == "mastery":
        g, gl, gd = hex_color("#D9A030"), hex_color("#FFE7A0"), hex_color("#8A5A10")
        star = ("....#....",
                "...###...",
                "...###...",
                "#########",
                ".#######.",
                "..#####..",
                "..##.##..",
                ".##...##.",
                ".#.....#.")
        for y, row in enumerate(star):
            for x, ch in enumerate(row):
                if ch == "#":
                    put(img, x, y, gl if y < 3 or (y == 3 and 2 < x < 6) else (gd if y > 6 else g))
    return img


RARITY_GEMS = {"common": "#D8D8D8", "uncommon": "#6FD86F", "rare": "#5FC8F0", "ancient": "#F0B040",
               "legendary": "#E070F0", "demon": "#C02040", "forbidden": "#7A3AC0"}


def gen_spell_icons():
    out = []
    for sid, spell in gen_data.SPELLS.items():
        save(gui_icons.icon(sid, spell["color"]), f"spell/{sid}")
        out.append(sid)
    save(gui_icons.icon("generic", "#FFFFFF"), "spell/generic")
    return out


def write_java(ids):
    body = ",\n".join(f'        "{sid}"' for sid in sorted(ids))
    src = f'''package com.hunternado.frieren.client.gui;

// GENERATED by tools/gen_gui.py - do not edit by hand.

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.FrierenMod;
import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * Sprite ids of the spell icons in the GUI atlas (textures/gui/sprites/spell/). Spells added by data packs have
 * no drawn icon and use the generic rune, tinted with the spell's colour.
 */
public final class SpellIcons {{
    private static final Set<String> DRAWN = Set.of(
{body});

    public static final Identifier GENERIC = FrierenIds.id("spell/generic");

    private SpellIcons() {{}}

    /** True when this spell has its own drawn icon (otherwise draw {{@link #GENERIC}} tinted). */
    public static boolean hasIcon(Identifier spell) {{
        return FrierenMod.MODID.equals(spell.getNamespace()) && DRAWN.contains(spell.getPath());
    }}

    public static Identifier sprite(Identifier spell) {{
        return hasIcon(spell) ? FrierenIds.id("spell/" + spell.getPath()) : GENERIC;
    }}
}}
'''
    with open(JAVA, "w", encoding="utf-8") as f:
        f.write(src)


def main():
    save(cover(), "book/cover", nine(48, 48, 12))
    save(page(True), "book/page_left", nine(32, 32, {"left": 3, "top": 3, "right": 8, "bottom": 4}))
    left = page(True)
    right = canvas(32, 32)
    for y in range(32):
        for x in range(32):
            right.pixels[y][x] = left.pixels[y][31 - x]
    save(right, "book/page_right", nine(32, 32, {"left": 8, "top": 3, "right": 3, "bottom": 4}))
    save(ribbon(), "book/ribbon", nine(10, 30, {"left": 0, "top": 2, "right": 0, "bottom": 6}))
    save(row_hover(), "book/row_hover", nine(16, 12, 2))
    save(row_selected(), "book/row_selected", nine(16, 12, 2))
    for state in ("normal", "hover", "disabled"):
        save(button(state), f"book/button_{state}", nine(24, 14, 3))
    save(circle(), "book/circle")
    save(slot(False), "hud/slot")
    save(slot(True), "hud/slot_selected")
    save(mana_frame(), "hud/mana_frame", nine(40, 9, {"left": 6, "top": 3, "right": 6, "bottom": 3}))
    save(mana_fill(False), "hud/mana_fill", {"type": "tile", "width": 16, "height": 3})
    save(mana_fill(True), "hud/mana_fill_conceal", {"type": "tile", "width": 16, "height": 3})
    for rarity, color in RARITY_GEMS.items():
        save(gem(color), f"gem/{rarity}")
    for kind in ("mana", "cooldown", "cast", "mastery"):
        save(stat_icon(kind), f"stat/{kind}")
    ids = gen_spell_icons()
    write_java(ids)
    print(f"wrote GUI sprites and {len(ids)} spell icons")


if __name__ == "__main__":
    main()
