"""
Item icons (16x16), drawn as pixel art: hand-placed templates for most items, a shaded-band helper for the
diagonal tools. Light comes from the top left; every icon gets the dark outline the vanilla items have.
tools/gen_assets.py saves these as textures/item/<id>.png.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixelart import Image, hex_color, mix, shade  # noqa: E402

OUTLINE = hex_color("#1B1426")


def ramp(base, *factors):
    """Tones of one material from light to dark (multiplicative, slightly warmer when lighter)."""
    c = hex_color(base)
    out = []
    for f in factors:
        t = shade(c, f)
        if f > 1.0:
            t = mix(t, (255, 248, 230, 255), min(0.35, (f - 1.0) * 0.6))
        out.append(t)
    return out


def tones(base):
    """Four tones: highlight, light, base, shadow."""
    return ramp(base, 1.45, 1.18, 1.0, 0.72)


def sprite(rows, palette, outline=True):
    img = Image(16, 16)
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != ".":
                img.set(x, y, palette[key])
    if outline:
        img.outline_shape(OUTLINE)
    return img


def band(img, x0, y0, length, colors, step=1):
    """A diagonal band rising to the right: colors run across the band (first = upper-left edge, lit)."""
    for i in range(length):
        for k, c in enumerate(colors):
            img.set(x0 + i * step + k, y0 - i * step, c)


def pal(**materials):
    """Palette from material letters: pal(w=('#8A5A36', 'wW')) maps w/W to tones of that colour (light->dark)."""
    out = {}
    for _, (base, keys) in materials.items():
        t = tones(base)
        picks = {1: [2], 2: [1, 3], 3: [1, 2, 3], 4: [0, 1, 2, 3]}[len(keys)]
        for key, idx in zip(keys, picks):
            out[key] = t[idx]
    return out


# ---------------------------------------------------------------------------------------------
# Staffs (shaft rising from the bottom left to an ornament at the top right)
# ---------------------------------------------------------------------------------------------
def shaft(img, wood, start=(2, 14), length=9):
    light, mid, dark = ramp(wood, 1.25, 1.0, 0.7)
    band(img, start[0], start[1], length, (light, dark))


def staff_sage():
    """Frieren's staff: gold crescent cradling a red orb."""
    img = sprite([
        "................",
        "..........h..h..",
        ".........gh..hg.",
        ".........grRrrg.",
        ".........grrrkg.",
        "..........grkg..",
        "...........gG...",
    ] + ["." * 16] * 9, pal(g=("#D8B04A", "hgG"), r=("#C8283A", "Rrk")), outline=False)
    shaft(img, "#8A5A36", start=(3, 14), length=8)
    img.set(2, 15, tones("#D8B04A")[2])
    img.outline_shape(OUTLINE)
    return img


def staff_mage():
    """Association staff: silver prongs around a blue crystal."""
    img = sprite([
        "................",
        "...........L....",
        "..........LcS...",
        ".........sLckS..",
        ".........sccks..",
        "..........sSS...",
    ] + ["." * 16] * 10, {**pal(c=("#7FC4FF", "Lck")), **pal(s=("#C9CCD6", "sS"))}, outline=False)
    shaft(img, "#5C3D28", start=(2, 14), length=9)
    img.outline_shape(OUTLINE)
    return img


def staff_apprentice():
    """A first staff: knotted wood, leather band, a small pale crystal."""
    img = sprite([
        "................",
        "................",
        "...........c....",
        "..........kKk...",
        "..........lLl...",
        "..........KkK...",
    ] + ["." * 16] * 10, {**pal(c=("#BFE6FF", "c")), **pal(k=("#8A5E36", "kK")), **pal(l=("#5A3A22", "lL"))},
        outline=False)
    shaft(img, "#9A6A3C", start=(2, 14), length=9)
    img.outline_shape(OUTLINE)
    return img


def staff_guillotine():
    """Aura's guillotine staff: black shaft, silver falling blade, crimson gem."""
    img = sprite([
        "................",
        "............sss.",
        "...........s.SS.",
        "..........rRs...",
        "..........rrS...",
        "...........d....",
    ] + ["." * 16] * 10, {**pal(s=("#D8DCE6", "sS")), **pal(r=("#B0263A", "Rr")), **pal(d=("#4A3A4A", "d"))},
        outline=False)
    shaft(img, "#3A2A3A", start=(2, 14), length=9)
    img.outline_shape(OUTLINE)
    return img


# ---------------------------------------------------------------------------------------------
# Weapons
# ---------------------------------------------------------------------------------------------
def serie_letter():
    """Serie's letter: a cream envelope sealed with her golden wax and a sprig of blue moonweed."""
    img = Image(16, 16)
    paper, paper_d, fold = hex_color("#F4ECD6"), hex_color("#D8CBA8"), hex_color("#B8A880")
    gold, gold_d = hex_color("#E8B840"), hex_color("#A87A1C")
    img.rect(1, 4, 14, 12, paper)
    img.rect(1, 12, 14, 12, paper_d)
    for i in range(7):                                    # the flap folds down to a point
        img.set(1 + i, 4 + i // 2 + (i // 2), fold)
        img.set(14 - i, 4 + i // 2 + (i // 2), fold)
    img.rect(6, 8, 9, 10, gold)
    img.set(6, 10, gold_d)
    img.set(9, 10, gold_d)
    img.set(7, 9, hex_color("#FFF2B0"))
    img.set(12, 3, hex_color("#5FA8FF"))
    img.set(13, 2, hex_color("#5FA8FF"))
    img.set(12, 2, hex_color("#3A7A28"))
    img.outline_shape(OUTLINE)
    return img


def staff_ubel():
    """Übel's black spear-staff: twin curved blades and a lightning-bolt tip, drawn on the diagonal."""
    img = Image(16, 16)
    shaft, shaft_l = hex_color("#1E1C24"), hex_color("#4A4652")
    steel, edge = hex_color("#B8BEC8"), hex_color("#EEF2F8")
    for i in range(10):                                   # the shaft, lower-left to the collar
        img.set(2 + i, 13 - i, shaft)
        img.set(2 + i, 14 - i, shaft_l if i % 3 == 0 else shaft)
    for (x, y) in ((10, 6), (9, 4), (9, 3), (10, 2)):     # left blade
        img.set(x, y, steel)
    for (x, y) in ((12, 5), (13, 4), (14, 4), (14, 3)):   # right blade
        img.set(x, y, steel)
    img.set(10, 1, edge)
    img.set(15, 2, edge)
    for (x, y) in ((12, 3), (13, 2), (12, 2), (13, 1), (14, 0)):   # the zigzag tip
        img.set(x, y, edge if (x + y) % 2 else steel)
    img.outline_shape(OUTLINE)
    return img


def sword(blade, edge, guard, grip, pommel, long=True):
    img = Image(16, 16)
    e, b, d = ramp(edge, 1.15, 1.0, 1.0)[0], ramp(blade, 1.0)[0], ramp(blade, 0.72)[0]
    band(img, 5, 10, 9 if long else 7, (e, b, d))           # blade: lit edge, face, shaded edge
    img.set(14, 1, e)
    g1, g2 = ramp(guard, 1.25, 0.8)
    for x, y in ((3, 9), (4, 10), (5, 11), (6, 12)):         # crossguard across the blade
        img.set(x, y, g1 if (x + y) % 2 else g2)
    gr1, gr2 = ramp(grip, 1.2, 0.8)
    band(img, 2, 13, 2, (gr1, gr2))                          # grip
    p1, p2 = ramp(pommel, 1.4, 0.85)
    img.set(1, 14, p1)
    img.set(1, 15, p2)
    img.set(0, 15, p2)
    img.outline_shape(OUTLINE)
    return img


def heros_sword():
    return sword("#C9D1DC", "#F4F7FB", "#D4AF37", "#3D2A4A", "#3C7BD6")


def demon_kings_blade():
    img = sword("#2A2230", "#C8283A", "#B8902E", "#2A1A20", "#C8283A")
    return img


def warriors_axe():
    """Stark's axe: a crescent bit facing up-left (like the vanilla axes), long haft, leather grip."""
    return sprite([
        "................",
        "........ee......",
        ".......eEEs.....",
        "......eEEEss....",
        "......eEEsSSk...",
        ".......eEssSSk..",
        "........eesSSk..",
        "...........hk...",
        "..........hk....",
        ".........hk.....",
        "........lL......",
        ".......lL.......",
        "......hk........",
        ".....hk.........",
        "....hk..........",
        "...hk...........",
    ], {"e": hex_color("#F4F6FA"), "E": hex_color("#D8DDE4"), **pal(s=("#9AA0AA", "sS")), "k": hex_color("#4A3020"),
        "h": hex_color("#8A6040"), **pal(l=("#3E2A1C", "lL"))})


def guillotine_blade():
    """Aura's guillotine: a heavy weighted bar over a blade with a slanted, crimson-stained edge."""
    return sprite([
        "................",
        "..kkkkkkkkkkkk..",
        "..kKKKKKKKKKKk..",
        "..kkkkkkkkkkkk..",
        "...SssssssssS...",
        "...Sssssssssr...",
        "...Ssssssssre...",
        "...Sssssssre....",
        "...Sssssssre....",
        "...Ssssssre.....",
        "...Sssssre......",
        "...Ssssre.......",
        "...Sssre........",
        "...Ssre.........",
        "...See..........",
        "................",
    ], {**pal(s=("#AEB4C0", "Ss")), **pal(k=("#4A4A56", "Kk")), "e": hex_color("#F4F6FA"),
        "r": hex_color("#B0263A")})


def scale_of_obedience():
    return sprite([
        ".......vv.......",
        "......vVVv......",
        ".......gg.......",
        ".hgggggggggggh..",
        ".h.....g.....h..",
        ".c.....g.....c..",
        ".c.....g.....c..",
        "gGGg...g...gGGg.",
        ".gg....g....gg..",
        ".......g........",
        ".......g........",
        "......hgh.......",
        ".....gggGg......",
        "................",
        "................",
        "................",
    ], {**pal(g=("#D8B04A", "hgG")), **pal(v=("#B04CD8", "Vv")), **pal(c=("#A88A3A", "c"))})


# ---------------------------------------------------------------------------------------------
# Grimoires (one template, recoloured; sealed ones are bound with chains)
# ---------------------------------------------------------------------------------------------
BOOK = [
    "................",
    "..gaaaaaaaaag...",
    "..daabbbbbbbbp..",
    "..dabbbxxbbbbp..",
    "..dabbxbbxbbbp..",
    "..dabxbyybxbbp..",
    "..dabxbyybxbcq..",
    "..dabbxbbxbbcq..",
    "..dabbbxxbbbcp..",
    "..dabbbbbbbbcp..",
    "..dabbbbbbbbcq..",
    "..dabbbbbbbbcp..",
    "..gbbbbbbbbbgq..",
    "..dccccccccccpp.",
    "...ppppppppppp..",
    "................",
]


def grimoire(cover, accent, glyph, chains=False):
    c = tones(cover)
    p = {"a": c[1], "b": c[2], "c": c[3], "d": shade(c[3], 0.8),
         "p": hex_color("#EFE6CF"), "q": hex_color("#D2C4A6"),
         "g": tones(accent)[1], "x": hex_color(glyph), "y": tones(glyph)[0]}
    img = sprite(BOOK, p, outline=False)
    if chains:
        ch1, ch2 = ramp("#9A9AA8", 1.3, 0.7)
        for i in range(1, 15):
            img.set(i, i, ch1 if i % 2 else ch2)
            img.set(15 - i, i, ch1 if i % 2 else ch2)
    img.outline_shape(OUTLINE)
    return img


# ---------------------------------------------------------------------------------------------
# Mage attire
# ---------------------------------------------------------------------------------------------
def mage_hat():
    return sprite([
        "................",
        "........hh......",
        ".......hbbd.....",
        "......hbbbd.....",
        "......hbbbbd....",
        ".....hbbbbbd....",
        ".....hbbbbbbd...",
        "....hbbbbbbbd...",
        "....gGgGgGgGg...",
        "..hbbbbbbbbbbbd.",
        ".hbbbbbbbbbbbbbd",
        "..dddddddddddd..",
        "................",
        "................",
        "................",
        "................",
    ], {**pal(b=("#3A3F6B", "hbd")), **pal(g=("#D4AF37", "gG"))})


def mage_coat():
    return sprite([
        "................",
        "...hwwg..gwwd...",
        "..hwwwwggwwwwd..",
        ".hwwwwwggwwwwwd.",
        ".hwwwwwggwwwwwd.",
        ".hwwhwwggwwdwwd.",
        ".gg.hwwggwwd.gg.",
        "....hwwggwwd....",
        "....hwwggwwd....",
        "....hwwggwwd....",
        "....hwwggwwd....",
        "....gggggggg....",
        "................",
        "................",
        "................",
        "................",
    ], {**pal(w=("#F0EEE8", "hwd")), **pal(g=("#D4AF37", "g"))})


def mage_trousers():
    return sprite([
        "................",
        "....gggggggg....",
        "....hbbbbbbd....",
        "....hbbbbbbd....",
        "....hbbddbbd....",
        "....hbd..hbd....",
        "....hbd..hbd....",
        "....hbd..hbd....",
        "....hbd..hbd....",
        "....hbd..hbd....",
        "....hbd..hbd....",
        "....ggg..ggg....",
        "................",
        "................",
        "................",
        "................",
    ], {**pal(b=("#2E2E44", "hbd")), **pal(g=("#D4AF37", "g"))})


def mage_boots():
    return sprite([
        "................",
        "................",
        "................",
        "..gggg....gggg..",
        "..hbbd....hbbd..",
        "..hbbd....hbbd..",
        "..hbbd....hbbd..",
        "..hbbd....hbbd..",
        "..hbbbbd..hbbbbd",
        "..hbbbbd..hbbbbd",
        "..ssssss..ssssss",
        "................",
        "................",
        "................",
        "................",
        "................",
    ], {**pal(b=("#6B4A30", "hbd")), **pal(g=("#D4AF37", "g")), "s": hex_color("#2A1E16")})


# ---------------------------------------------------------------------------------------------
# Materials, potions, artifacts
# ---------------------------------------------------------------------------------------------
def mana_crystal():
    return sprite([
        "................",
        ".......h........",
        "......hLb.......",
        ".....hLLbd......",
        ".....hLbbd......",
        "....hLLbbbd.....",
        "....hLbbbbd.....",
        "...hLLbbbbbd....",
        "...hLbbbbbbd....",
        "....hbbbbbd.....",
        "....hbbbbbd.....",
        ".....hbbbd......",
        ".....hbbbd......",
        "......hbd.......",
        ".......d........",
        "................",
    ], {**pal(b=("#4FA8FF", "hbd")), "L": hex_color("#E8F6FF")})


def demon_horn():
    return sprite([
        "................",
        "............t...",
        "...........tT...",
        "...........hb...",
        "..........hbd...",
        "..........hbd...",
        ".........hbbd...",
        "........hbbd....",
        ".......hbbbd....",
        "......hbbbd.....",
        ".....hbbbd......",
        "....hbbbdd......",
        "...hbbbdd.......",
        "...rrrrr........",
        "................",
        "................",
    ], {**pal(b=("#3A2232", "hbd")), **pal(t=("#D8C8D0", "tT")), **pal(r=("#7A1E2C", "r"))})


def demonic_mana_core():
    return sprite([
        "................",
        "......dddd......",
        "....ddbbbbdd....",
        "...dbbrrrrbbd...",
        "...dbrRRrrrbd...",
        "..dbrRWRrrrrbd..",
        "..dbrRRrrrrrbd..",
        "..dbrrrrrrrrbd..",
        "..dbrrrrrrrkbd..",
        "...dbrrrrrkbd...",
        "...dbbrrkkbbd...",
        "....ddbbbbdd....",
        "......dddd......",
        "................",
        "................",
        "................",
    ], {**pal(b=("#6A2C70", "bd")), **pal(r=("#E0405A", "Rrk")), "W": (255, 255, 255, 255)})


def potion(liquid, flask=False):
    rows = [
        "................",
        "......cccc......",
        "......kCCk......",
        ".......gg.......",
        "......g..g......",
        ".....gLLLLg.....",
        "....gLlllllg....",
        "....glllllll....",
        "...glllllllldg..",
        "...gllllllllldg.",
        "...gllllllllldg.",
        "...gllllllllldg.",
        "....gllllllldg..",
        ".....gdddddgg...",
        "......ggggg.....",
        "................",
    ] if not flask else [
        "................",
        ".......cc.......",
        "......kCCk......",
        "......gggg......",
        ".......gg.......",
        "......gLLg......",
        ".....gllllg.....",
        "....gLllllldg...".replace("gLllllldg...", "gLllllldg..."),
        "....glllllldg...",
        "....glllllldg...",
        "....glllllldg...",
        "....glllllldg...",
        "....glllllldg...",
        "....gddddddgg...",
        ".....gggggg.....",
        "................",
    ]
    lq = tones(liquid)
    p = {"c": hex_color("#8B5A2B"), "C": hex_color("#A8743F"), "k": hex_color("#6B4520"),
         "g": hex_color("#CFE7F2"), "L": lq[0], "l": lq[2], "d": lq[3]}
    return sprite(rows, p)


def sweet_grapes():
    return sprite([
        "................",
        "........ss......",
        ".......sLL......",
        "......sLLL......",
        ".....pPpPp......",
        "....pPpPpPp.....",
        "....PpPpPpP.....",
        ".....pPpPpPp....",
        ".....PpPpPp.....",
        "......pPpPp.....",
        "......PpPp......",
        ".......pP.......",
        "................",
        "................",
        "................",
        "................",
    ], {**pal(p=("#7A3C9E", "Pp")), **pal(s=("#5A3A22", "s")), **pal(L=("#5FB04A", "L"))})


def mirror_lotus_ring():
    return sprite([
        "................",
        "......pPPp......",
        ".....pWPPWp.....",
        "......pPPp......",
        ".....gGGGGg.....",
        "....gG....gG....",
        "...gG......gG...",
        "...gG......gg...",
        "...gG......gg...",
        "...gG......gg...",
        "....gg....gg....",
        ".....gggggg.....",
        "................",
        "................",
        "................",
        "................",
    ], {**pal(g=("#D8B04A", "Gg")), **pal(p=("#F4B8D8", "Pp")), "W": hex_color("#FFFFFF")})


def concealment_charm():
    return sprite([
        "................",
        ".......cc.......",
        "......c..c......",
        ".......cc.......",
        "......hssd......",
        ".....hsvvsd.....",
        "....hsvVVvsd....",
        "....hsvVVvsd....",
        "....hsvvvvsd....",
        ".....hsvvsd.....",
        "......hssd......",
        ".......sd.......",
        "................",
        "................",
        "................",
        "................",
    ], {**pal(s=("#C9CCD6", "hsd")), **pal(v=("#5A3A8C", "Vv")), "c": hex_color("#8A8A96")})


def demon_hunter_insignia():
    return sprite([
        "................",
        "..gggggggggggg..",
        "..gRRRRRRRRRRg..",
        "..gRwRRRRRRwRg..",
        "..gRRwRRRRwRRg..",
        "..gRRRwRRwRRRg..",
        "..gRRRRwwRRRRg..",
        "..gRRRRwwRRRRg..",
        "...gRRwRRwRRg...",
        "...gRwRRRRwRg...",
        "....gRRRRRRg....",
        ".....gRRRRg.....",
        "......gRRg......",
        ".......gg.......",
        "................",
        "................",
    ], {**pal(R=("#A01E2C", "R")), **pal(g=("#D8B04A", "g")), "w": hex_color("#E8E8F0")})


def exam_seal():
    return sprite([
        "................",
        ".....bbbbbb.....",
        "...bbBBBBBBbb...",
        "..bBBggggggBBb..",
        "..bBgGGGGGGgBb..",
        ".bBgGGwGGwGGgBb.",
        ".bBgGGGwwGGGgBb.",
        ".bBgGGGwwGGGgBb.",
        ".bBgGGwGGwGGgBb.",
        "..bBgGGGGGGgBb..",
        "..bBBggggggBBb..",
        "...bbBBBBBBbb...",
        ".....bbbbbb.....",
        ".....rr..rr.....",
        "....rr....rr....",
        "................",
    ], {**pal(b=("#1F3A6B", "Bb")), **pal(g=("#D4AF37", "Gg")), "w": hex_color("#FFFFFF"),
        **pal(r=("#C02838", "r"))})


def stille_cage():
    return sprite([
        "................",
        ".......gg.......",
        "......g..g......",
        "....gggggggg....",
        "....b.b..b.b....",
        "....b.b..b.b....",
        "....b.bccb.b....",
        "....b.cCCc.b....",
        "....b.cCCc.b....",
        "....b.bccb.b....",
        "....b.b..b.b....",
        "....gggggggg....",
        "....dddddddd....",
        "................",
        "................",
        "................",
    ], {**pal(g=("#B8902E", "g")), **pal(b=("#8A8A96", "b")), **pal(c=("#8FD8FF", "Cc")),
        **pal(d=("#5A4A2E", "d"))})


def journey_journal():
    return sprite([
        "................",
        "..hbbbbbbbbbp...",
        "..hbbbbbbbbbbp..",
        "..hbbggggggbbp..",
        "..hbbgffffgbbq..",
        "..hbbgfFFfgbbp..",
        "..hbbgffffgbbq..",
        "..hbbggggggbbp..",
        "..hbbbbbbbbbbp..",
        "..hbbbbbbbbbbq..",
        "..hbbbbbbbbbbp..",
        "..hbbbbbbbbbbq..",
        "..hbbbbbbbbbbp..",
        "..dddddddddddpp.",
        "...pppppppppppp.",
        "................",
    ], {**pal(b=("#7A5230", "hbd")), "p": hex_color("#EFE6CF"), "q": hex_color("#D2C4A6"),
        **pal(g=("#D4AF37", "g")), **pal(f=("#5FA8E8", "Ff"))})


def sigil(color, motif):
    c = tones(color)
    p = {"o": c[3], "r": c[2], "R": c[1], "H": c[0], "m": hex_color("#F3D27A"), "M": hex_color("#FFF2C0"),
         "k": hex_color("#1B1426")}
    rows = [
        "................",
        ".....oooooo.....",
        "...oorRRRRroo...",
        "..orRRRRRRRRro..",
        "..oRRHHrrrrrRo..",
        ".orRHrrrrrrrrro.",
        ".orRrrrrrrrrrro.",
        ".orRrrrrrrrrrro.",
        ".orrrrrrrrrrrro.",
        ".orrrrrrrrrrrro.",
        "..orrrrrrrrrro..",
        "..oorrrrrrrroo..",
        "...oorrrrrroo...",
        ".....oooooo.....",
        "................",
        "................",
    ]
    img = sprite(rows, p, outline=False)
    motifs = {
        "blade": [(7, 3), (8, 3), (7, 4), (8, 4), (7, 5), (8, 5), (7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8),
                  (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9), (7, 10), (8, 10), (7, 11), (8, 11)],
        "eye": [(5, 7), (6, 6), (7, 6), (8, 6), (9, 6), (10, 7), (9, 8), (8, 8), (7, 8), (6, 8), (7, 7), (8, 7)],
        "crown": [(4, 9), (4, 8), (4, 7), (5, 8), (6, 7), (6, 6), (7, 8), (8, 8), (9, 7), (9, 6), (10, 8), (11, 7),
                  (11, 8), (11, 9), (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9)],
    }
    for (x, y) in motifs[motif]:
        img.set(x, y, p["m"])
    if motif == "eye":
        img.set(7, 7, p["k"])
        img.set(8, 7, p["k"])
    img.outline_shape(OUTLINE)
    return img


def mirror_shard():
    return sprite([
        "................",
        ".........w......",
        "........wWs.....",
        ".......wWWs.....",
        "......wWWsss....",
        "......wWssss....",
        ".....wWWssss....",
        ".....wWsssssd...",
        "....wWWsssssd...",
        "....wWssssssd...",
        "...wWsssssdd....",
        "...wssssdd......",
        "...ssdd.........",
        "................",
        "................",
        "................",
    ], {"w": hex_color("#FFFFFF"), "W": hex_color("#E4F4FA"), "s": hex_color("#BFE0EC"), "d": hex_color("#8FB8C8")})
