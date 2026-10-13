"""
Textures for the Frieren-style building set (16x16): plaster and half-timbering, roof tiles, town cobbles,
Association stone, elven ruins, the Demon King's stone, leaded glass, lanterns, window boxes, grimoire shelves and
the Association carpet. Light comes from the top left; tiling edges are kept seamless.
"""
import math
import random

from pixelart import Image, hex_color, mix, shade

CLEAR = (0, 0, 0, 0)


def img(fill=None):
    im = Image(16, 16)
    if fill:
        im.rect(0, 0, 15, 15, fill)
    return im


def speckle(im, base, amount, seed, light=1.06, dark=0.94):
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            if rng.random() < amount:
                im.set(x, y, shade(base, light if rng.random() < 0.5 else dark))


# ---------------------------------------------------------------------------------------------
# Plaster and timber
# ---------------------------------------------------------------------------------------------
PLASTER = hex_color("#EDE7D8")
TIMBER = hex_color("#4E3626")


def plaster():
    im = img(PLASTER)
    speckle(im, PLASTER, 0.18, 1, 1.03, 0.95)
    return im


def beam(im, x0, y0, x1, y1, horizontal):
    """A timber beam with a lit edge, a shaded edge and grain along its length."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            along = x if horizontal else y
            across = (y - y0) if horizontal else (x - x0)
            width = (y1 - y0) if horizontal else (x1 - x0)
            c = TIMBER
            if across == 0:
                c = shade(TIMBER, 1.25)
            elif across == width:
                c = shade(TIMBER, 0.72)
            elif (along * 7 + across * 3) % 11 == 0:
                c = shade(TIMBER, 0.85)
            im.set(x, y, c)


def diagonal(im, flip=False, width=2):
    for i in range(16):
        for k in range(width):
            x = i if not flip else 15 - i
            y = i + k - width // 2
            if 0 <= y < 16:
                im.set(x, y, shade(TIMBER, 1.15 if k == 0 else 0.85))


def timber_frame():
    im = plaster()
    beam(im, 0, 0, 15, 1, True)
    beam(im, 0, 14, 15, 15, True)
    beam(im, 0, 0, 1, 15, False)
    beam(im, 14, 0, 15, 15, False)
    return im


def timber_cross():
    im = timber_frame()
    diagonal(im)
    diagonal(im, flip=True)
    return im


def timber_diagonal():
    im = timber_frame()
    diagonal(im)
    return im


def timber_posts():
    im = plaster()
    beam(im, 0, 0, 2, 15, False)
    beam(im, 13, 0, 15, 15, False)
    beam(im, 0, 7, 15, 8, True)
    return im


def timber_side():
    im = img(TIMBER)
    for y in range(16):
        for x in range(16):
            c = TIMBER
            if x % 4 == 0:
                c = shade(TIMBER, 0.78)
            elif (x * 5 + y * 3) % 13 == 0:
                c = shade(TIMBER, 1.15)
            im.set(x, y, c)
    return im


def timber_end():
    im = img(shade(TIMBER, 1.15))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if int(d) % 3 == 0:
                im.set(x, y, shade(TIMBER, 0.95))
            if d > 7.2:
                im.set(x, y, shade(TIMBER, 0.75))
    return im


# ---------------------------------------------------------------------------------------------
# Roof tiles: overlapping rows of rounded tiles, offset every row
# ---------------------------------------------------------------------------------------------
def roof_tiles(color):
    base = hex_color(color)
    im = img(shade(base, 0.6))
    for row in range(4):
        y0 = row * 4
        offset = 2 if row % 2 else 0
        for t in range(-1, 5):
            x0 = t * 4 + offset
            for dy in range(4):
                for dx in range(4):
                    x = x0 + dx
                    if not 0 <= x < 16:
                        continue
                    # rounded lower corners, lit top, shaded bottom edge
                    if dy == 3 and dx in (0, 3):
                        continue
                    c = base
                    if dy == 0:
                        c = shade(base, 1.2)
                    elif dy == 3 or dx == 3:
                        c = shade(base, 0.78)
                    elif dx == 0:
                        c = shade(base, 1.08)
                    im.set(x, y0 + dy, c)
    return im


# ---------------------------------------------------------------------------------------------
# Stone
# ---------------------------------------------------------------------------------------------
def cobbles(color="#9C978E", seed=3):
    base = hex_color(color)
    mortar = shade(base, 0.62)
    im = img(mortar)
    rng = random.Random(seed)
    # a fixed, tile-safe layout of rounded stones
    stones = [(0, 0, 5, 4), (6, 0, 4, 3), (11, 0, 5, 4), (0, 5, 4, 5), (5, 4, 6, 4), (12, 5, 4, 4), (0, 11, 6, 5),
              (7, 9, 4, 4), (12, 10, 4, 6), (7, 14, 4, 2), (5, 10, 1, 1)]
    for (x0, y0, w, h) in stones:
        tint = shade(base, 0.9 + rng.random() * 0.22)
        for y in range(h):
            for x in range(w):
                if (x in (0, w - 1)) and (y in (0, h - 1)) and w > 2 and h > 2:
                    continue
                c = tint
                if y == 0 or x == 0:
                    c = shade(tint, 1.14)
                elif y == h - 1 or x == w - 1:
                    c = shade(tint, 0.82)
                im.set((x0 + x) % 16, (y0 + y) % 16, c)
    return im


def bricks(color, mortar=None, seed=5, rows=4, speck=0.12, height=4):
    base = hex_color(color)
    mortar = hex_color(mortar) if mortar else shade(base, 0.72)
    im = img(mortar)
    rng = random.Random(seed)
    for row in range(16 // height):
        y0 = row * height
        offset = 4 if row % 2 else 0
        for b in range(-1, 3):
            x0 = b * 8 + offset
            tint = shade(base, 0.94 + rng.random() * 0.1)
            for y in range(height - 1):
                for x in range(7):
                    xx = x0 + x
                    if not 0 <= xx < 16:
                        continue
                    c = tint
                    if y == 0:
                        c = shade(tint, 1.1)
                    elif y == height - 2:
                        c = shade(tint, 0.88)
                    elif rng.random() < speck:
                        c = shade(tint, 0.95)
                    im.set(xx, y0 + y, c)
    return im


ASSOC = "#E9E2CF"
GOLD = hex_color("#D4AF37")


def association_bricks():
    return bricks(ASSOC, "#CBC2AA", seed=7, height=8)


def association_gilded():
    im = association_bricks()
    for x in range(16):
        im.set(x, 0, shade(GOLD, 1.15))
        im.set(x, 1, GOLD)
        im.set(x, 14, GOLD)
        im.set(x, 15, shade(GOLD, 0.75))
    return im


def association_chiseled():
    base = hex_color(ASSOC)
    im = img(base)
    carve = shade(base, 0.78)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 6.3 < d < 7.3 or 3.6 < d < 4.4:
                im.set(x, y, carve)
    for k in range(5):                                      # a five-pointed star inside the circle
        a1 = -math.pi / 2 + k * 4 * math.pi / 5
        a2 = -math.pi / 2 + (k + 1) * 4 * math.pi / 5
        for t in range(12):
            u = t / 11
            x = 7.5 + 4 * ((1 - u) * math.cos(a1) + u * math.cos(a2))
            y = 7.5 + 4 * ((1 - u) * math.sin(a1) + u * math.sin(a2))
            im.set(int(round(x)), int(round(y)), shade(GOLD, 0.95))
    for x in range(16):
        im.set(x, 0, shade(base, 1.06))
        im.set(x, 15, shade(base, 0.85))
    return im


def association_pillar_side():
    base = hex_color(ASSOC)
    im = img(base)
    for y in range(16):
        for x in range(16):
            if x % 4 == 0:
                im.set(x, y, shade(base, 0.82))
            elif x % 4 == 1:
                im.set(x, y, shade(base, 1.06))
    for x in range(16):
        im.set(x, 0, GOLD)
        im.set(x, 15, shade(GOLD, 0.8))
    return im


def association_pillar_top():
    base = hex_color(ASSOC)
    im = img(base)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 5.5 < d < 6.5:
                im.set(x, y, GOLD)
            elif d >= 7:
                im.set(x, y, shade(base, 0.85))
    return im


def elven_bricks(cracked=False):
    im = bricks("#A8B3A2", "#7E8A79", seed=11, height=8)
    rng = random.Random(13)
    moss = hex_color("#6E9A55")
    for _ in range(14):                                    # moss creeping along the mortar
        x, y = rng.randrange(16), rng.choice((7, 15, 6, 14))
        im.set(x, y, moss)
        if rng.random() < 0.5:
            im.set(x, (y + 1) % 16, shade(moss, 0.85))
    for y in range(16):                                    # a vine down one side
        x = 2 + int(1.5 * math.sin(y * 0.8))
        im.set(x, y, hex_color("#4E7A3A"))
        if y % 4 == 1:
            im.set(x + 1, y, hex_color("#7FB060"))
    if cracked:
        x, y = 9, 0
        for _ in range(22):
            im.set(x % 16, y % 16, hex_color("#5A6456"))
            y += 1
            x += rng.choice((-1, 0, 1))
    return im


def demon_bricks():
    im = bricks("#2D2434", "#18121D", seed=17, height=4, speck=0.18)
    vein = hex_color("#B0263A")
    rng = random.Random(19)
    x, y = 3, 0
    for _ in range(26):                                    # a crimson vein wandering through the stone
        im.set(x % 16, y % 16, vein if rng.random() < 0.7 else shade(vein, 0.7))
        y += 1
        x += rng.choice((-1, 0, 1, 1))
    return im


def demon_chiseled():
    base = hex_color("#2D2434")
    im = img(base)
    for x in range(16):
        im.set(x, 0, shade(base, 1.2))
        im.set(x, 15, shade(base, 0.6))
    eye = hex_color("#B0263A")
    for y in range(16):                                    # an eye sigil
        for x in range(16):
            dy = abs(y - 7.5)
            half = 6.5 * math.sqrt(max(0.0, 1 - (dy / 4.2) ** 2))
            if abs(x - 7.5) <= half and dy <= 4.2:
                edge = abs(abs(x - 7.5) - half) < 1.0 or abs(dy - 4.2) < 0.7
                im.set(x, y, shade(base, 0.6) if edge else shade(base, 1.25))
    for y in range(5, 11):
        for x in range(6, 10):
            im.set(x, y, eye)
    im.set(7, 6, shade(eye, 1.6))
    return im


# ---------------------------------------------------------------------------------------------
# Glass, lantern, decor
# ---------------------------------------------------------------------------------------------
LEAD = hex_color("#45454E")


def leaded_glass():
    im = img()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15) or (x + y) % 8 == 0 or (x - y) % 8 == 0:
                im.set(x, y, LEAD if not (x in (0, 15) or y in (0, 15)) else shade(LEAD, 0.85))
    for (x, y) in ((3, 2), (4, 3), (11, 2), (12, 3), (3, 10), (4, 11), (11, 10), (12, 11)):
        im.set(x, y, hex_color("#EAF4F8"))
    return im


def leaded_glass_edge():
    im = img(shade(LEAD, 0.9))
    for y in range(16):
        im.set(7, y, shade(LEAD, 1.2))
    return im


def lantern():
    """Atlas for the mana lantern model: frame (0-7, 0-7), glowing glass (8-15, 0-7), cap (0-7, 8-15)."""
    im = img()
    iron = hex_color("#3A3A44")
    for y in range(8):
        for x in range(8):
            im.set(x, y, shade(iron, 1.2) if x == 0 or y == 0 else (shade(iron, 0.8) if x == 7 or y == 7 else iron))
    glow = hex_color("#8FD8FF")
    for y in range(8):
        for x in range(8, 16):
            d = math.hypot(x - 11.5, y - 3.5)
            im.set(x, y, mix(hex_color("#FFFFFF"), glow, min(1.0, d / 4.0)))
            if x in (8, 15) or y in (0, 7):
                im.set(x, y, iron)
    for y in range(8, 16):
        for x in range(8):
            im.set(x, y, shade(iron, 1.1 - (y - 8) * 0.04))
    gold = hex_color("#C9A040")
    for y in range(8, 16):
        for x in range(8, 16):
            im.set(x, y, shade(gold, 1.15) if y == 8 else gold)
    return im


def planter_side():
    wood = hex_color("#7A5232")
    im = img(wood)
    for y in range(16):
        for x in range(16):
            if y % 5 == 0:
                im.set(x, y, shade(wood, 0.75))
            elif (x * 3 + y) % 9 == 0:
                im.set(x, y, shade(wood, 1.15))
    return im


def soil():
    base = hex_color("#4A3424")
    im = img(base)
    speckle(im, base, 0.3, 23, 1.2, 0.8)
    return im


def flowers():
    """Cross sprite for the window box: blue moon weed among green leaves (transparent background)."""
    im = img()
    leaf, petal, core = hex_color("#4E8A3A"), hex_color("#5B8FE8"), hex_color("#F3E27A")
    for x in (3, 7, 11):
        for y in range(8, 16):
            im.set(x, y, leaf)
        im.set(x - 1, 11, shade(leaf, 1.2))
        im.set(x + 1, 13, shade(leaf, 1.2))
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if abs(dx) + abs(dy) == 1:
                    im.set(x + dx, 6 + dy, petal)
        im.set(x, 6, core)
    return im


def grimoire_shelf():
    wood = hex_color("#6B4A2E")
    im = img(wood)
    covers = ["#7A2A3A", "#2F5A8C", "#3A6B3A", "#8A6A1E", "#5A2C70", "#1F3A6B", "#9B2D6F", "#2E6B5E"]
    rng = random.Random(29)
    for shelf in (1, 9):
        x = 1
        while x < 15:
            w = rng.choice((1, 2, 2))
            h = rng.choice((5, 6, 6))
            c = hex_color(rng.choice(covers))
            for xx in range(x, min(15, x + w)):
                for yy in range(shelf + 6 - h, shelf + 6):
                    im.set(xx, yy, c if xx > x or w == 1 else shade(c, 1.2))
                im.set(xx, shelf + 6 - h + 1, GOLD)
            x += w
        for x in range(16):
            im.set(x, shelf + 6, shade(wood, 0.7))
    for x in range(16):
        im.set(x, 0, shade(wood, 1.2))
        im.set(x, 15, shade(wood, 0.7))
    return im


def planks_top():
    wood = hex_color("#6B4A2E")
    im = img(wood)
    for y in range(16):
        for x in range(16):
            if y % 4 == 3:
                im.set(x, y, shade(wood, 0.72))
            elif (x + y * 5) % 7 == 0:
                im.set(x, y, shade(wood, 1.12))
    return im


def association_carpet():
    blue = hex_color("#2E4A8C")
    im = img(blue)
    for i in range(16):
        for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
            im.set(x, y, GOLD)
    for y in range(16):
        for x in range(16):
            if abs(x - 7.5) + abs(y - 7.5) < 3.2:
                im.set(x, y, shade(GOLD, 0.9))
    im.set(7, 7, shade(GOLD, 1.3))
    return im


def lantern_icon():
    """Inventory sprite for the mana lantern."""
    im = img()
    iron, glow, gold = hex_color("#3A3A44"), hex_color("#8FD8FF"), hex_color("#C9A040")
    im.rect(7, 1, 8, 2, iron)
    im.rect(5, 3, 10, 4, gold)
    for y in range(5, 13):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (5, 12)
            im.set(x, y, iron if edge else mix(hex_color("#FFFFFF"), glow, min(1.0, abs(x - 7.5) / 3 + abs(y - 8.5) / 5)))
    im.rect(5, 13, 10, 13, gold)
    im.outline_shape(hex_color("#1B1426"))
    return im
