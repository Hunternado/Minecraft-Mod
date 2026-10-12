"""
The cast. Each function builds a Rig (geometry + painters). Designs follow the anime's silhouettes and colour
schemes at Minecraft scale; textures are original pixel art painted procedurally.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from characters import Rig, hand_prop_pivot, humanoid  # noqa: E402
from skinpaint import (bangs, border, buttons, emblem_diamond, face, fill, gem, hair, hexc, hline, metal, mix,  # noqa: E402
                       shade, side_locks, sub, trim, vline, wood)

WHITE_GOLD = hexc("#D9B44A")


# ---------------------------------------------------------------------------------------------
# Reusable painters
# ---------------------------------------------------------------------------------------------
def p_fill(c, noise=0.04, gradient=0.08, streaks=0.0, seed=1):
    def painter(cv, f):
        for i, (name, r) in enumerate(f.items()):
            fill(cv, r, c, noise=noise, seed=seed + i, gradient=gradient, vertical_streaks=streaks)
    return painter


def p_hair_all(c, seed=3):
    def painter(cv, f):
        for i, r in enumerate(f.values()):
            hair(cv, r, c, seed=seed + i, highlight_row=2)
    return painter


def p_wood(c, seed=5):
    def painter(cv, f):
        for i, r in enumerate(f.values()):
            wood(cv, r, c, seed=seed + i)
    return painter


def p_metal(c, seed=7):
    def painter(cv, f):
        for i, r in enumerate(f.values()):
            metal(cv, r, c, seed=seed + i)
    return painter


def p_gem(c):
    def painter(cv, f):
        for r in f.values():
            fill(cv, r, c, noise=0.03)
            cv.px(r[0], r[1], shade(c, 1.6))
            if r[2] > 1:
                cv.px(r[0] + 1, r[1], shade(c, 1.3))
            cv.px(r[0] + r[2] - 1, r[1] + r[3] - 1, shade(c, 0.6))
    return painter


def head_painter(skin, hair_c, eye, fringe=5, part=None, locks=(2, 9), face_kw=None, side_skin=True, seed=11,
                 back_detail=None):
    """Head: hair on top/back/sides, a fringe and side locks over the face, facial features, skin jaw on sides."""
    face_kw = face_kw or {}

    def painter(cv, f):
        hair(cv, f["top"], hair_c, seed=seed, highlight_row=None)
        # parting line on top
        if part is not None:
            vline(cv, f["top"], part, shade(hair_c, 0.8), y_from=f["top"][3] // 2)
        fill(cv, f["front"], skin, noise=0.015, gradient=0.06)
        face(cv, f["front"], skin, eye, seed=seed, **face_kw)
        bangs(cv, f["front"], hair_c, fringe, seed=seed, part=part)
        if locks:
            side_locks(cv, f["front"], hair_c, locks[0], locks[1], top=fringe - 1, seed=seed)
        for side in ("right", "left"):
            r = f[side]
            hair(cv, r, hair_c, seed=seed + 7, highlight_row=3)
            if side_skin:
                # jaw/cheek toward the front edge
                fx = r[2] - 6 if side == "right" else 0
                for yy in range(9, 16):
                    for xx in range(6):
                        if yy - 9 + (xx if side == "right" else 5 - xx) > 2:
                            cv.px(r[0] + fx + xx, r[1] + yy, shade(skin, 0.95))
        hair(cv, f["back"], hair_c, seed=seed + 9, highlight_row=3)
        if back_detail:
            back_detail(cv, f["back"])
        fill(cv, f["bottom"], shade(skin, 0.85), noise=0.02)
    return painter


def hat_hair_painter(hair_c, fringe=3, seed=13, side_rows=9, back_rows=16, top=True):
    """Hair volume on the inflated hat layer: full top/back, partial sides, a short jagged fringe on the front."""
    def painter(cv, f):
        if top:
            hair(cv, f["top"], shade(hair_c, 1.04), seed=seed, highlight_row=6)
        r = f["back"]
        hair(cv, sub(r, 0, 0, r[2], back_rows), shade(hair_c, 1.02), seed=seed + 1, highlight_row=3)
        for side in ("right", "left"):
            r = f[side]
            hair(cv, sub(r, 0, 0, r[2], side_rows), shade(hair_c, 1.02), seed=seed + 2, highlight_row=3)
            # strands reaching lower toward the back edge
            for xx in range(r[2] // 2):
                col = xx if side == "right" else r[2] - 1 - xx
                for yy in range(side_rows, side_rows + 4 - xx // 2):
                    cv.px(r[0] + col, r[1] + yy, shade(hair_c, 0.95))
        bangs(cv, f["front"], shade(hair_c, 1.05), fringe, seed=seed + 3, jagged=1)
    return painter


def legs_painter(tights, boot, boot_top=14, cuff=None, seed=21):
    def painter(cv, f):
        for i, (name, r) in enumerate(f.items()):
            if name in ("top", "bottom"):
                fill(cv, r, boot if name == "bottom" else tights, noise=0.03)
                continue
            fill(cv, sub(r, 0, 0, r[2], boot_top), tights, noise=0.03, seed=seed + i, gradient=0.1)
            fill(cv, sub(r, 0, boot_top, r[2], r[3] - boot_top), boot, noise=0.05, seed=seed + 10 + i, gradient=0.15)
            if cuff:
                hline(cv, r, boot_top, cuff)
            hline(cv, r, r[3] - 1, shade(boot, 0.6))
    return painter


def sleeve_painter(cloth, cuff, cuff_rows=2, hand_rows=6, seed=31, trim_c=None):
    """Overlay sleeve that stops above the hand, with a cuff band."""
    def painter(cv, f):
        for i, (name, r) in enumerate(f.items()):
            if name == "bottom":
                continue
            if name == "top":
                fill(cv, r, cloth, noise=0.03)
                continue
            body_rows = r[3] - hand_rows
            fill(cv, sub(r, 0, 0, r[2], body_rows - cuff_rows), cloth, noise=0.035, seed=seed + i, gradient=0.08)
            fill(cv, sub(r, 0, body_rows - cuff_rows, r[2], cuff_rows), cuff, noise=0.02)
            if trim_c:
                hline(cv, r, body_rows - cuff_rows - 1, trim_c)
    return painter


def arm_skin_painter(skin, sleeve=None, hand_rows=6, seed=41):
    def painter(cv, f):
        for i, (name, r) in enumerate(f.items()):
            fill(cv, r, skin, noise=0.015, seed=seed + i)
            if sleeve and name not in ("bottom",):
                rows = r[3] - hand_rows if name != "top" else r[3]
                fill(cv, sub(r, 0, 0, r[2], rows), sleeve, noise=0.03, seed=seed + 5 + i, gradient=0.06)
    return painter


# ---------------------------------------------------------------------------------------------
# Rig helpers
# ---------------------------------------------------------------------------------------------
def add_long_hair(rig, hair_c, length=14, width=8, seed=70, painter=None):
    """Flat panel of long hair hanging from the back of the head; sways with movement in Java."""
    rig.pose("hair_back", offset=(0, -6.5, 4.0), rotation=(0.08, 0, 0))
    rig.box("hair_back", -width / 2, 0, 0, width, length, 1, painter or p_hair_all(hair_c, seed=seed))


def add_braid(rig, part, hair_c, offset, length=12, rotation=(0.1, 0, 0), tie=None, seed=80):
    rig.pose(part, offset=offset, rotation=rotation)
    if tie:
        rig.box(part, -1, -1, -1, 2, 1, 2, p_fill(tie, noise=0.02))
    for i in range(0, length, 3):
        w = 2 if (i // 3) % 2 == 0 else 3
        rig.box(part, -w / 2, i, -w / 2 + 0.0, w, 3, 2 if w == 2 else 2, p_hair_all(shade(hair_c, 1.0 - 0.03 * (i // 3)), seed=seed + i))


def add_skirt(rig, painter, length=6, width=9, depth=5, top=10.5, lower=None):
    """Robe/dress skirt hanging from the waist; an optional wider lower tier for long robes."""
    rig.pose("skirt", offset=(0, top, 0))
    rig.box("skirt", -width / 2, 0, -depth / 2, width, length, depth, painter)
    if lower:
        lw, ll, ld, lp = lower
        rig.box("skirt", -lw / 2, length, -ld / 2, lw, ll, ld, lp)


def add_cape(rig, painter, length=18, width=10):
    rig.pose("cape", offset=(0, 0.2, 2.6), rotation=(0.08, 0, 0))
    rig.box("cape", -width / 2, 0, 0, width, length, 1, painter)


def add_elf_ears(rig, painter, earring=None):
    """
    Frieren-style elf ears: long, flat and pointed, sticking out sideways just above eye level with a slight
    backward sweep and lift (tips end near brow height, not above the head). Optional dangling earring.
    """
    for part, sx in (("ear_right", -1), ("ear_left", 1)):
        rig.pose(part, offset=(4 * sx, -3.3, 0.4), rotation=(0, -0.32 * sx, -0.14 * sx))
        rig.box(part, -2 if sx < 0 else 0, -1, -0.5, 2, 2, 1, painter)
        rig.box(part, -4 if sx < 0 else 2, -1.3, -0.5, 2, 1, 1, painter)
        rig.box(part, -6 if sx < 0 else 4, -1.5, -0.5, 2, 1, 1, painter)
        if earring:
            rig.box(part, -1.3 if sx < 0 else 0.3, 1, -0.5, 1, 1, 1, earring)


def add_horns(rig, color, tip=None, size="medium"):
    """
    Demon horns: a tapering staircase of segments that sweeps up, out and back from the temples and hooks at the tip
    (an antelope-like curve instead of straight antennae).
    """
    tip = tip or shade(color, 1.35)
    # (thickness, length) per segment, base to tip
    profile = {
        "small": [(2, 2), (1, 2), (1, 1)],
        "medium": [(2, 2), (2, 2), (1, 2), (1, 1)],
        "large": [(2, 3), (2, 2), (2, 2), (1, 2), (1, 2)],
    }[size]
    n = len(profile)
    for part, sx in (("horn_right", -1), ("horn_left", 1)):
        rig.pose(part, offset=(3.2 * sx, -7.2, -0.5), rotation=(-0.18, 0.0, 0.62 * sx))
        y = 0.0
        body = p_fill(color, noise=0.05, gradient=-0.12)
        for i, (thick, length) in enumerate(profile):
            t = i / max(1, n - 1)
            # sweep outward then curl back up: the part's tilt does the outward lean, the offsets bend it inward
            # again near the tip and push it backward, giving a crescent instead of a straight spike
            inward = -2.2 * t ** 1.6
            back = 3.0 * t ** 1.3
            x = inward * sx - thick / 2.0
            rig.box(part, x, y - length + 0.5, back - thick / 2.0, thick, length, thick,
                    p_fill(tip, noise=0.03) if i == n - 1 else body)
            y -= length - 0.5


def robe_body(base, trim_c, collar=None, sash=None, emblem=None, seed=90, collar_rows=3):
    """Body painter for robes/dresses: base cloth, trims on the edges, optional collar, sash and chest emblem."""
    def painter(cv, f):
        for i, (name, r) in enumerate(f.items()):
            fill(cv, r, base, noise=0.03, seed=seed + i, gradient=0.1, vertical_streaks=0.04)
            if name in ("front", "back", "left", "right"):
                hline(cv, r, r[3] - 1, trim_c)
        r = f["front"]
        vline(cv, r, r[2] // 2 - 1, trim_c, y_from=collar_rows)
        if collar:
            for i in range(collar_rows):
                hline(cv, r, i, collar, x_from=4 - i, x_to=r[2] - 4 + i)
        if sash:
            for name in ("front", "back", "left", "right"):
                fill(cv, sub(f[name], 0, 17, f[name][2], 3), sash, noise=0.03)
        if emblem:
            emblem_diamond(cv, r[0] + r[2] // 2 + 3, r[1] + 7, emblem, radius=2)
    return painter


def skirt_painter(base, trim_c, pleats=True, slit=False):
    def painter(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            fill(cv, r, base, noise=0.03, gradient=0.14)
            if name == "top":
                continue
            if pleats:
                for col in range(2, r[2], 4):
                    vline(cv, r, col, shade(base, 0.85), y_from=1)
            hline(cv, r, r[3] - 2, trim_c)
            hline(cv, r, r[3] - 1, shade(trim_c, 0.8))
            if slit and name == "front":
                vline(cv, r, r[2] // 2, shade(base, 0.55), y_from=r[3] // 3)
    return painter


def armor_painter(plate, cloth, trim_c, seed=100, belt=None):
    def painter(cv, f):
        for i, (name, r) in enumerate(f.items()):
            fill(cv, r, plate, noise=0.05, seed=seed + i, gradient=0.12)
            if name in ("front", "back"):
                for row in range(6, r[3], 7):
                    hline(cv, r, row, shade(plate, 0.7))
                border(cv, r, trim_c, top=False, bottom=False)
                fill(cv, sub(r, 0, r[3] - 5, r[2], 5), cloth, noise=0.04)
            if belt and name in ("front", "back", "left", "right"):
                fill(cv, sub(r, 0, 16, r[2], 2), belt, noise=0.03)
    return painter


def p_blade(edge, core, seed=110):
    def painter(cv, f):
        for i, (name, r) in enumerate(f.items()):
            fill(cv, r, core, noise=0.03, seed=seed + i)
            if r[2] > 2:
                vline(cv, r, 0, edge)
                vline(cv, r, r[2] - 1, shade(edge, 0.85))
    return painter


# ---------------------------------------------------------------------------------------------
# Frieren
# ---------------------------------------------------------------------------------------------
def frieren():
    skin = hexc("#F7E4D6")
    hair_c = hexc("#E6E8F1")
    eye = hexc("#52B98C")
    white = hexc("#F3F1EB")
    gold = hexc("#D6AE45")
    black = hexc("#22222C")
    boot = hexc("#6E4A30")
    red = hexc("#C9212F")
    rig = Rig("frieren", render_scale=0.9)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, white, noise=0.025, gradient=0.08)
        r = f["front"]
        # collar V and gold trim down the centre
        for i in range(5):
            cv.px(r[0] + 7 - i // 2, r[1] + i, gold)
            cv.px(r[0] + 8 + i // 2, r[1] + i, gold)
        vline(cv, r, 7, shade(white, 0.9), y_from=5, y_to=19)
        vline(cv, r, 8, shade(white, 0.86), y_from=5, y_to=19)
        # dark inner shirt peeking at the collar
        cv.rect(r[0] + 7, r[1], 2, 3, black)
        # belt
        fill(cv, sub(r, 0, 18, r[2], 2), hexc("#5A3B26"), noise=0.03)
        cv.rect(r[0] + 7, r[1] + 18, 2, 2, gold)
        for side in ("left", "right", "back"):
            fill(cv, sub(f[side], 0, 18, f[side][2], 2), hexc("#5A3B26"), noise=0.03)

    def capelet(cv, f):
        rows = 9
        for name in ("front", "back", "left", "right"):
            r = f[name]
            fill(cv, sub(r, 0, 0, r[2], rows), shade(white, 1.02), noise=0.02, gradient=0.05)
            hline(cv, r, rows - 1, gold)
            hline(cv, r, rows - 2, shade(gold, 0.85))
        fill(cv, f["top"], white, noise=0.02)
        r = f["front"]
        # brooch with red jewel at the collar
        cv.rect(r[0] + 6, r[1] + 1, 4, 3, gold)
        cv.rect(r[0] + 7, r[1] + 1, 2, 2, red)
        cv.px(r[0] + 7, r[1] + 1, shade(red, 1.5))
        # opening line of the capelet
        vline(cv, r, 7, shade(gold, 0.9), y_from=4, y_to=rows - 2)

    def skirt(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            fill(cv, r, white, noise=0.025, gradient=0.12)
            if name != "top":
                for col in range(1, r[2], 3):
                    vline(cv, r, col, shade(white, 0.88), y_from=2)
                hline(cv, r, r[3] - 2, gold)
                hline(cv, r, r[3] - 1, shade(gold, 0.8))

    def ear(cv, f):
        for name, r in f.items():
            fill(cv, r, skin, noise=0.02)
        r = f["front"]
        hline(cv, r, r[3] - 1, shade(skin, 0.85))
        cv.px(r[0] + r[2] // 2, r[1] + 1, mix(skin, hexc("#E7A0A0"), 0.4))

    def back_detail(cv, r):
        # strands gathering toward the twin tails
        for col in (2, 3, r[2] - 4, r[2] - 3):
            vline(cv, r, col, shade(hair_c, 0.82), y_from=4)

    def tie(cv, f):
        for r in f.values():
            fill(cv, r, gold, noise=0.03)
            hline(cv, r, 0, shade(gold, 1.25))

    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=5, part=9, locks=(2, 10),
                             face_kw=dict(sleepy=True, eye_y=7, gap=4, mouth="small", brow=shade(hair_c, 0.86),
                                          lash=hexc("#5E6274")),
                             back_detail=back_detail),
        "hat": hat_hair_painter(hair_c, fringe=3, side_rows=8, back_rows=12),
        "body": body,
        "jacket": capelet,
        "right_arm": arm_skin_painter(skin),
        "left_arm": arm_skin_painter(skin),
        "right_sleeve": sleeve_painter(white, gold),
        "left_sleeve": sleeve_painter(white, gold),
        "right_leg": legs_painter(black, boot, boot_top=14, cuff=shade(boot, 1.2)),
        "left_leg": legs_painter(black, boot, boot_top=14, cuff=shade(boot, 1.2)),
    }
    humanoid(rig, painters, slim=True)

    add_elf_ears(rig, ear, earring=p_gem(red))

    # Twin tails from the back corners of the head.
    for side, sx in (("tail_right", -1), ("tail_left", 1)):
        rig.pose(side, offset=(3.0 * sx, -4.8, 3.3), rotation=(0.14, 0.0, -0.12 * sx))
        rig.box(side, -1.5, -1.5, -1.5, 3, 2, 3, tie)
        rig.box(side, -1.5, 0.5, -1.5, 3, 14, 3, p_hair_all(hair_c, seed=50 + sx))
        rig.box(side, -1, 14.5, -1, 2, 2, 2, p_hair_all(shade(hair_c, 0.95), seed=60 + sx))

    rig.pose("skirt", offset=(0, 10.5, 0))
    rig.box("skirt", -4.5, 0, -2.5, 9, 6, 5, skirt)

    # Frieren's staff: wooden shaft, gold ring head holding a red orb.
    hand_prop_pivot(rig, slim=True)
    rig.box("prop", -0.5, -18, -0.5, 1, 26, 1, p_wood(hexc("#8A5A36")))
    rig.box("prop", -1, -22.5, -1, 2, 2, 2, p_gem(red))
    rig.box("prop", -2.5, -24, -0.5, 1, 5, 1, p_metal(gold))
    rig.box("prop", 1.5, -24, -0.5, 1, 5, 1, p_metal(gold))
    rig.box("prop", -1.5, -25, -0.5, 3, 1, 1, p_metal(gold))
    rig.box("prop", -1.5, -19, -0.5, 3, 1, 1, p_metal(gold))
    return rig


# ---------------------------------------------------------------------------------------------
# Fern
# ---------------------------------------------------------------------------------------------
def fern():
    skin = hexc("#F8E6DA")
    hair_c = hexc("#5C3A7C")
    eye = hexc("#9A63C8")
    black = hexc("#1D1B24")
    white = hexc("#F2F0F4")
    rig = Rig("fern", render_scale=0.95)
    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=6, part=None, locks=(2, 12),
                             face_kw=dict(eye_y=7, gap=4, mouth="small", brow=shade(hair_c, 1.1),
                                          lash=hexc("#2A1C38"))),
        "hat": hat_hair_painter(hair_c, fringe=4, side_rows=12, back_rows=16),
        "body": robe_body(black, shade(black, 1.6), collar=white, collar_rows=3),
        "right_arm": arm_skin_painter(skin, sleeve=black), "left_arm": arm_skin_painter(skin, sleeve=black),
        "right_sleeve": sleeve_painter(black, white), "left_sleeve": sleeve_painter(black, white),
        "right_leg": legs_painter(black, hexc("#2B2420"), boot_top=12),
        "left_leg": legs_painter(black, hexc("#2B2420"), boot_top=12),
    }
    humanoid(rig, painters, slim=True)
    add_long_hair(rig, hair_c, length=16, width=8)
    add_skirt(rig, skirt_painter(black, shade(black, 1.7), pleats=True), length=7, width=9, depth=5,
              lower=(10, 6, 6, skirt_painter(black, shade(black, 1.7))))
    hand_prop_pivot(rig, slim=True)
    rig.box("prop", -0.5, -17, -0.5, 1, 25, 1, p_wood(hexc("#7B5536")))
    rig.box("prop", -1, -19, -1, 2, 2, 2, p_metal(hexc("#C9CCD6")))
    rig.box("prop", -0.5, -20.5, -0.5, 1, 1, 1, p_gem(hexc("#BFE3FF")))
    return rig


# ---------------------------------------------------------------------------------------------
# Stark
# ---------------------------------------------------------------------------------------------
def stark():
    skin = hexc("#F2D7C2")
    hair_c = hexc("#C2382C")
    eye = hexc("#A0402F")
    navy = hexc("#2A3048")
    shirt = hexc("#EDEBE6")
    brown = hexc("#6A4630")
    rig = Rig("stark", render_scale=1.0)

    def body(cv, f):
        for i, (name, r) in enumerate(f.items()):
            fill(cv, r, navy, noise=0.035, seed=120 + i, gradient=0.1)
        r = f["front"]
        fill(cv, sub(r, 5, 0, 6, 16), shirt, noise=0.02)
        vline(cv, r, 4, shade(navy, 1.4), y_to=17)
        vline(cv, r, 11, shade(navy, 1.4), y_to=17)
        for name in ("front", "back", "left", "right"):
            fill(cv, sub(f[name], 0, 16, f[name][2], 2), brown, noise=0.03)
        cv.rect(r[0] + 7, r[1] + 16, 2, 2, hexc("#C9A43A"))

    def spikes(cv, f):
        for i, r in enumerate(f.values()):
            hair(cv, r, hair_c, seed=130 + i, highlight_row=0)

    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=4, part=None, locks=(1, 6),
                             face_kw=dict(eye_y=7, gap=4, mouth="small", brow_angle=-1, brow=shade(hair_c, 0.7),
                                          lash=hexc("#3A1A14"))),
        "hat": hat_hair_painter(hair_c, fringe=3, side_rows=6, back_rows=9),
        "body": body,
        "right_arm": arm_skin_painter(skin, sleeve=navy), "left_arm": arm_skin_painter(skin, sleeve=navy),
        "right_sleeve": sleeve_painter(navy, brown), "left_sleeve": sleeve_painter(navy, brown),
        "right_leg": legs_painter(hexc("#4A4A55"), brown, boot_top=13, cuff=shade(brown, 1.2)),
        "left_leg": legs_painter(hexc("#4A4A55"), brown, boot_top=13, cuff=shade(brown, 1.2)),
    }
    humanoid(rig, painters, slim=False)
    # spiky hair tufts
    rig.pose("crown", offset=(0, -8, 0))
    for x, z, h in ((-3, -2, 2), (0, -3, 3), (2, -1, 2), (-2, 1, 2), (1, 2, 3)):
        rig.box("crown", x, -h, z, 2, h, 2, spikes)
    # Stark's axe: long haft, broad blade and a back spike
    hand_prop_pivot(rig, slim=False)
    steel = hexc("#B9BEC8")
    rig.box("prop", -0.5, -16, -0.5, 1, 26, 1, p_wood(hexc("#5C3E28")))
    rig.box("prop", -5.5, -17, -0.5, 5, 7, 1, p_blade(shade(steel, 1.25), steel))
    rig.box("prop", -6.5, -18, -0.5, 1, 9, 1, p_blade(shade(steel, 1.35), shade(steel, 1.15)))
    rig.box("prop", 0.5, -15, -0.5, 2, 2, 1, p_metal(steel))
    rig.box("prop", -1, -17.5, -1, 2, 2, 2, p_metal(shade(steel, 0.8)))
    return rig


# ---------------------------------------------------------------------------------------------
# Serie
# ---------------------------------------------------------------------------------------------
def serie():
    skin = hexc("#F7E6D8")
    hair_c = hexc("#E9C86E")
    eye = hexc("#D79A2B")
    robe = hexc("#1E2236")
    gold = hexc("#D8B04A")
    rig = Rig("serie", render_scale=0.88)

    def ear(cv, f):
        for r in f.values():
            fill(cv, r, skin, noise=0.02)

    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=5, part=8, locks=(2, 11),
                             face_kw=dict(sleepy=True, eye_y=7, gap=4, mouth="stern", brow_angle=1,
                                          brow=shade(hair_c, 0.78), lash=hexc("#5A4320"))),
        "hat": hat_hair_painter(hair_c, fringe=3, side_rows=11, back_rows=16),
        "body": robe_body(robe, gold, collar=gold, emblem=gold, collar_rows=2),
        "right_arm": arm_skin_painter(skin, sleeve=robe), "left_arm": arm_skin_painter(skin, sleeve=robe),
        "right_sleeve": sleeve_painter(robe, gold, trim_c=gold), "left_sleeve": sleeve_painter(robe, gold, trim_c=gold),
        "right_leg": legs_painter(robe, hexc("#2E2A24"), boot_top=14),
        "left_leg": legs_painter(robe, hexc("#2E2A24"), boot_top=14),
    }
    humanoid(rig, painters, slim=True)
    add_long_hair(rig, hair_c, length=15, width=8)
    add_braid(rig, "tail_right", hair_c, offset=(0, -1, 4.5), length=15, rotation=(0.12, 0, 0), tie=gold)
    add_elf_ears(rig, ear)
    # gold circlet
    rig.pose("crown", offset=(0, -6.6, 0))
    rig.box("crown", -4.5, -0.5, -4.5, 9, 1, 9, p_metal(gold), inflate=0.0)
    add_skirt(rig, skirt_painter(robe, gold), length=7, width=9, depth=5,
              lower=(10, 6, 6, skirt_painter(robe, gold)))
    return rig


# ---------------------------------------------------------------------------------------------
# Himmel the Hero
# ---------------------------------------------------------------------------------------------
def himmel():
    skin = hexc("#F5DCC8")
    hair_c = hexc("#8FC3EA")
    eye = hexc("#4D8FD1")
    white = hexc("#F1F0EC")
    blue = hexc("#2F5FA8")
    gold = hexc("#D8B04A")
    rig = Rig("himmel", render_scale=1.0)

    def body(cv, f):
        for i, (name, r) in enumerate(f.items()):
            fill(cv, r, white, noise=0.025, seed=140 + i, gradient=0.08)
        r = f["front"]
        fill(cv, sub(r, 0, 0, r[2], 3), blue, noise=0.02)
        vline(cv, r, 7, gold, y_from=3, y_to=18)
        vline(cv, r, 8, shade(gold, 0.85), y_from=3, y_to=18)
        for name in ("front", "back", "left", "right"):
            fill(cv, sub(f[name], 0, 17, f[name][2], 2), hexc("#4A3426"), noise=0.03)
        cv.rect(r[0] + 6, r[1] + 17, 4, 2, gold)

    def cape(cv, f):
        for name, r in f.items():
            fill(cv, r, blue if name != "front" else hexc("#E9E6DF"), noise=0.03, gradient=0.15)
        r = f["back"]
        hline(cv, r, r[3] - 1, gold)
        hline(cv, r, 0, gold)

    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=5, part=10, locks=(2, 7),
                             face_kw=dict(eye_y=7, gap=4, mouth="smile", brow=shade(hair_c, 0.7), lash=hexc("#1F3A5E"))),
        "hat": hat_hair_painter(hair_c, fringe=3, side_rows=7, back_rows=10),
        "body": body,
        "right_arm": arm_skin_painter(skin, sleeve=white), "left_arm": arm_skin_painter(skin, sleeve=white),
        "right_sleeve": sleeve_painter(white, blue, trim_c=gold), "left_sleeve": sleeve_painter(white, blue, trim_c=gold),
        "right_leg": legs_painter(hexc("#2C3550"), hexc("#5B3E2B"), boot_top=11, cuff=gold),
        "left_leg": legs_painter(hexc("#2C3550"), hexc("#5B3E2B"), boot_top=11, cuff=gold),
    }
    humanoid(rig, painters, slim=False)
    add_cape(rig, cape, length=19, width=10)
    hand_prop_pivot(rig, slim=False)
    steel = hexc("#D9DEE6")
    rig.box("prop", -0.5, 2, -0.5, 1, 4, 1, p_wood(hexc("#3D2A4A")))
    rig.box("prop", -2.5, 1, -0.5, 5, 1, 1, p_metal(gold))
    rig.box("prop", -1, -15, -0.5, 2, 16, 1, p_blade(shade(steel, 1.2), steel))
    rig.box("prop", -0.5, 6, -0.5, 1, 1, 1, p_gem(hexc("#3C7BD6")))
    return rig


# ---------------------------------------------------------------------------------------------
# Eisen (dwarf warrior)
# ---------------------------------------------------------------------------------------------
def eisen():
    skin = hexc("#E9C9AE")
    hair_c = hexc("#B8B8BE")
    eye = hexc("#6A5A3A")
    tunic = hexc("#7A3A2A")
    leather = hexc("#4E3424")
    rig = Rig("eisen", render_scale=0.95)

    def beard(cv, f):
        for i, r in enumerate(f.values()):
            hair(cv, r, hair_c, seed=150 + i, highlight_row=1)

    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=4, part=None, locks=(1, 5),
                             face_kw=dict(eye_y=7, gap=4, mouth="stern", brow_angle=1, brow=shade(hair_c, 0.7))),
        "hat": hat_hair_painter(hair_c, fringe=2, side_rows=7, back_rows=10),
        "body": armor_painter(tunic, leather, hexc("#B48A3A"), belt=leather),
        "right_arm": arm_skin_painter(skin, sleeve=tunic), "left_arm": arm_skin_painter(skin, sleeve=tunic),
        "right_leg": legs_painter(leather, hexc("#3A2A1E"), boot_top=8),
        "left_leg": legs_painter(leather, hexc("#3A2A1E"), boot_top=8),
    }
    humanoid(rig, painters, slim=False, legs=8, body_h=10, arm_len=10)
    rig.pose("crown", offset=(0, -2, -4.2))
    rig.box("crown", -3.5, 0, -1, 7, 6, 1, beard)
    rig.box("crown", -2.5, 6, -1, 5, 3, 1, beard)
    rig.box("crown", -4, -1.2, -1.3, 3, 1, 1, beard)
    rig.box("crown", 1, -1.2, -1.3, 3, 1, 1, beard)
    hand_prop_pivot(rig, slim=False, arm_len=10)
    steel = hexc("#A9AEB8")
    rig.box("prop", -0.5, -12, -0.5, 1, 18, 1, p_wood(hexc("#4A3020")))
    rig.box("prop", -4.5, -13, -0.5, 4, 6, 1, p_blade(shade(steel, 1.25), steel))
    rig.box("prop", 0.5, -13, -0.5, 4, 6, 1, p_blade(shade(steel, 1.25), steel))
    return rig


# ---------------------------------------------------------------------------------------------
# Association examiner, wandering mage, priest
# ---------------------------------------------------------------------------------------------
def examiner():
    skin = hexc("#EBCDB4")
    hair_c = hexc("#9A9AA2")
    eye = hexc("#4E6A86")
    robe = hexc("#1F3A3A")
    gold = hexc("#C9A43A")
    rig = Rig("examiner", render_scale=1.0)

    def hat(cv, f):
        for i, r in enumerate(f.values()):
            fill(cv, r, shade(robe, 0.8), noise=0.04, seed=160 + i, gradient=0.1)
        hline(cv, f["front"], f["front"][3] - 1, gold)

    def monocle(cv, r):
        pass

    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=3, part=6, locks=(1, 6),
                             face_kw=dict(eye_y=7, gap=4, mouth="stern", brow=shade(hair_c, 0.6), lash=hexc("#2A2A30"))),
        "hat": hat_hair_painter(hair_c, fringe=2, side_rows=8, back_rows=10, top=False),
        "body": robe_body(robe, gold, collar=gold, emblem=gold, sash=shade(robe, 0.7), collar_rows=2),
        "right_arm": arm_skin_painter(skin, sleeve=robe), "left_arm": arm_skin_painter(skin, sleeve=robe),
        "right_sleeve": sleeve_painter(robe, gold), "left_sleeve": sleeve_painter(robe, gold),
        "right_leg": legs_painter(robe, hexc("#2A2420"), boot_top=14),
        "left_leg": legs_painter(robe, hexc("#2A2420"), boot_top=14),
    }
    humanoid(rig, painters, slim=False)
    rig.pose("crown", offset=(0, -8, 0))
    rig.box("crown", -5.5, -0.5, -5.5, 11, 1, 11, hat)
    rig.box("crown", -3.5, -4.5, -3.5, 7, 4, 7, hat)
    rig.box("crown", -2, -7.5, -2, 4, 3, 4, hat)
    add_skirt(rig, skirt_painter(robe, gold, pleats=False), length=7, width=9, depth=5,
              lower=(10, 6, 6, skirt_painter(robe, gold, pleats=False)))
    return rig


def wandering_mage():
    skin = hexc("#E8C8AC")
    hair_c = hexc("#5A3E2A")
    eye = hexc("#4F7A3A")
    cloak = hexc("#56603A")
    rig = Rig("wandering_mage", render_scale=1.0)

    def hood(cv, f):
        for i, (name, r) in enumerate(f.items()):
            if name == "front":
                hline(cv, r, 0, cloak)
                hline(cv, r, 1, cloak)
                vline(cv, r, 0, cloak)
                vline(cv, r, r[2] - 1, cloak)
                continue
            fill(cv, r, cloak, noise=0.05, seed=170 + i, gradient=0.12)

    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=3, part=None, locks=(1, 4),
                             face_kw=dict(eye_y=7, gap=4, mouth="smile")),
        "hat": hood,
        "body": robe_body(cloak, hexc("#A88B4A"), sash=hexc("#7A4A2A")),
        "right_arm": arm_skin_painter(skin, sleeve=cloak), "left_arm": arm_skin_painter(skin, sleeve=cloak),
        "right_leg": legs_painter(hexc("#4A3A2A"), hexc("#3A2A1E"), boot_top=12),
        "left_leg": legs_painter(hexc("#4A3A2A"), hexc("#3A2A1E"), boot_top=12),
    }
    humanoid(rig, painters, slim=False)
    add_skirt(rig, skirt_painter(cloak, hexc("#A88B4A"), pleats=False), length=8, width=9, depth=5)
    hand_prop_pivot(rig, slim=False)
    rig.box("prop", -0.5, -16, -0.5, 1, 24, 1, p_wood(hexc("#6B4A2E")))
    rig.box("prop", -1, -18, -1, 2, 2, 2, p_gem(hexc("#5FCF6A")))
    return rig


def priest():
    skin = hexc("#EFD3BE")
    hair_c = hexc("#8A6A48")
    eye = hexc("#5A7A9A")
    white = hexc("#EEEEF0")
    blue = hexc("#3456A0")
    gold = hexc("#D2AE48")
    rig = Rig("priest", render_scale=1.0)

    def body(cv, f):
        robe_body(white, gold)(cv, f)
        r = f["front"]
        # blue stole and the Goddess's sigil
        fill(cv, sub(r, 3, 0, 3, 20), blue, noise=0.02)
        fill(cv, sub(r, 10, 0, 3, 20), blue, noise=0.02)
        vline(cv, r, 7, gold, y_from=5, y_to=11)
        hline(cv, r, 7, gold, x_from=5, x_to=10)

    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=4, part=9, locks=(1, 5),
                             face_kw=dict(eye_y=7, gap=4, mouth="smile", brow=shade(hair_c, 0.7))),
        "hat": hat_hair_painter(hair_c, fringe=2, side_rows=6, back_rows=9),
        "body": body,
        "right_arm": arm_skin_painter(skin, sleeve=white), "left_arm": arm_skin_painter(skin, sleeve=white),
        "right_sleeve": sleeve_painter(white, blue), "left_sleeve": sleeve_painter(white, blue),
        "right_leg": legs_painter(white, hexc("#3A3030"), boot_top=14),
        "left_leg": legs_painter(white, hexc("#3A3030"), boot_top=14),
    }
    humanoid(rig, painters, slim=False)
    add_skirt(rig, skirt_painter(white, gold, pleats=False), length=7, width=9, depth=5,
              lower=(10, 6, 6, skirt_painter(white, blue, pleats=False)))
    return rig


# ---------------------------------------------------------------------------------------------
# Demons
# ---------------------------------------------------------------------------------------------
def scale_prop(rig, gold):
    """Aura's Scale of Obedience, held up in her left hand."""
    rig.pose("prop_left", offset=(0.5, 9, 0))
    rig.box("prop_left", -0.5, -11, -0.5, 1, 12, 1, p_metal(gold))
    rig.box("prop_left", -5, -12, -0.5, 10, 1, 1, p_metal(gold))
    rig.box("prop_left", -1, -13.5, -1, 2, 2, 2, p_gem(hexc("#B04CD8")))
    for x in (-4.5, 3.5):
        rig.box("prop_left", x + 0.25, -11, -0.25, 1, 3, 1, p_metal(shade(gold, 0.8)))
        rig.box("prop_left", x - 1, -8, -1.5, 3, 1, 3, p_metal(gold))


def aura():
    skin = hexc("#F6E2DA")
    hair_c = hexc("#F19FC7")
    eye = hexc("#B65CD9")
    dress = hexc("#3A2348")
    gold = hexc("#D7B24C")
    horn = hexc("#3B2A40")
    rig = Rig("aura", render_scale=0.95)
    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=5, part=7, locks=(2, 12),
                             face_kw=dict(eye_y=7, gap=4, mouth="smirk", brow_angle=1, brow=shade(hair_c, 0.7),
                                          lash=hexc("#3A1A30"), blush=True)),
        "hat": hat_hair_painter(hair_c, fringe=3, side_rows=12, back_rows=16),
        "body": robe_body(dress, gold, collar=gold, collar_rows=2),
        "right_arm": arm_skin_painter(skin, sleeve=dress), "left_arm": arm_skin_painter(skin, sleeve=dress),
        "right_sleeve": sleeve_painter(dress, gold, trim_c=gold), "left_sleeve": sleeve_painter(dress, gold, trim_c=gold),
        "right_leg": legs_painter(hexc("#1E1622"), hexc("#2A1A2E"), boot_top=12),
        "left_leg": legs_painter(hexc("#1E1622"), hexc("#2A1A2E"), boot_top=12),
    }
    humanoid(rig, painters, slim=True)
    add_long_hair(rig, hair_c, length=17, width=8)
    add_horns(rig, horn, tip=hexc("#CFA6C8"), size="large")
    add_skirt(rig, skirt_painter(dress, gold, slit=True), length=7, width=9, depth=5,
              lower=(10, 6, 6, skirt_painter(dress, gold, slit=True)))
    scale_prop(rig, gold)
    return rig


def qual():
    skin = hexc("#D9C3B8")
    hair_c = hexc("#C9C6CC")
    eye = hexc("#D23A3A")
    robe = hexc("#2A1D33")
    red = hexc("#8E2430")
    rig = Rig("qual", render_scale=1.05)

    def beard(cv, f):
        for i, r in enumerate(f.values()):
            hair(cv, r, hair_c, seed=200 + i, highlight_row=1)

    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=2, part=8, locks=(1, 9),
                             face_kw=dict(eye_y=7, gap=4, mouth="stern", brow_angle=1, glow=True,
                                          brow=shade(hair_c, 0.8), lash=hexc("#2A1A1A"))),
        "hat": hat_hair_painter(hair_c, fringe=1, side_rows=12, back_rows=16),
        "body": robe_body(robe, red, collar=red, sash=red),
        "right_arm": arm_skin_painter(skin, sleeve=robe), "left_arm": arm_skin_painter(skin, sleeve=robe),
        "right_sleeve": sleeve_painter(robe, red), "left_sleeve": sleeve_painter(robe, red),
        "right_leg": legs_painter(robe, hexc("#1A1218"), boot_top=14),
        "left_leg": legs_painter(robe, hexc("#1A1218"), boot_top=14),
    }
    humanoid(rig, painters, slim=False)
    add_long_hair(rig, hair_c, length=12, width=8)
    add_horns(rig, hexc("#2E2626"), tip=hexc("#8A7A70"), size="large")
    rig.pose("crown", offset=(0, -1.5, -4.2))
    rig.box("crown", -2.5, 0, -0.5, 5, 6, 1, beard)
    rig.box("crown", -1.5, 6, -0.5, 3, 3, 1, beard)
    add_skirt(rig, skirt_painter(robe, red, pleats=False), length=7, width=9, depth=5,
              lower=(10, 6, 6, skirt_painter(robe, red, pleats=False)))
    return rig


def demon_soldier():
    skin = hexc("#D8C2B4")
    hair_c = hexc("#3A2C34")
    eye = hexc("#D63A3A")
    plate = hexc("#4A4650")
    red = hexc("#7A1E26")
    rig = Rig("demon_soldier", render_scale=1.0)
    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=4, part=None, locks=(1, 6),
                             face_kw=dict(eye_y=7, gap=4, mouth="fangs", brow_angle=1, glow=True)),
        "hat": hat_hair_painter(hair_c, fringe=2, side_rows=7, back_rows=10),
        "body": armor_painter(plate, red, shade(plate, 1.4), belt=hexc("#3A2A22")),
        "right_arm": armor_painter(plate, plate, shade(plate, 1.3)),
        "left_arm": armor_painter(plate, plate, shade(plate, 1.3)),
        "right_leg": legs_painter(hexc("#2E2A30"), plate, boot_top=10, cuff=shade(plate, 1.3)),
        "left_leg": legs_painter(hexc("#2E2A30"), plate, boot_top=10, cuff=shade(plate, 1.3)),
    }
    humanoid(rig, painters, slim=False)
    add_horns(rig, hexc("#2A2024"), size="medium")
    hand_prop_pivot(rig, slim=False)
    steel = hexc("#9AA0AA")
    rig.box("prop", -0.5, 0, -0.5, 1, 4, 1, p_wood(hexc("#3A2A22")))
    rig.box("prop", -2, -0.5, -0.5, 4, 1, 1, p_metal(shade(steel, 0.8)))
    rig.box("prop", -1, -13, -0.5, 2, 13, 1, p_blade(shade(steel, 1.2), steel))
    return rig


def demon_mage():
    skin = hexc("#DCC6BC")
    hair_c = hexc("#5A3A6A")
    eye = hexc("#E04A6A")
    robe = hexc("#34203E")
    trim_c = hexc("#9A3A5A")
    rig = Rig("demon_mage", render_scale=1.0)
    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=5, part=6, locks=(2, 10),
                             face_kw=dict(eye_y=7, gap=4, mouth="smirk", glow=True)),
        "hat": hat_hair_painter(hair_c, fringe=3, side_rows=10, back_rows=14),
        "body": robe_body(robe, trim_c, collar=trim_c, sash=shade(robe, 0.7)),
        "right_arm": arm_skin_painter(skin, sleeve=robe), "left_arm": arm_skin_painter(skin, sleeve=robe),
        "right_sleeve": sleeve_painter(robe, trim_c), "left_sleeve": sleeve_painter(robe, trim_c),
        "right_leg": legs_painter(robe, hexc("#1E1420"), boot_top=14),
        "left_leg": legs_painter(robe, hexc("#1E1420"), boot_top=14),
    }
    humanoid(rig, painters, slim=False)
    add_long_hair(rig, hair_c, length=10, width=8)
    add_horns(rig, hexc("#241A28"), size="medium")
    add_skirt(rig, skirt_painter(robe, trim_c, pleats=False), length=8, width=9, depth=5)
    hand_prop_pivot(rig, slim=False)
    rig.box("prop", -0.5, -16, -0.5, 1, 24, 1, p_wood(hexc("#2A1E24")))
    rig.box("prop", -1, -18.5, -1, 2, 2, 2, p_gem(hexc("#C23AD8")))
    rig.box("prop", -1.5, -17, -0.5, 1, 2, 1, p_metal(hexc("#5A4A5A")))
    rig.box("prop", 0.5, -17, -0.5, 1, 2, 1, p_metal(hexc("#5A4A5A")))
    return rig


def aura_thrall():
    """A headless soldier animated by the Scale of Obedience."""
    plate = hexc("#5A5A62")
    rust = hexc("#6E3A2A")
    rig = Rig("aura_thrall", render_scale=1.05)

    def stump(cv, f):
        for name, r in f.items():
            fill(cv, r, hexc("#4A1E22") if name == "top" else plate, noise=0.05)

    painters = {
        "body": armor_painter(plate, rust, shade(plate, 1.3), belt=hexc("#3A2A22")),
        "jacket": None,
        "right_arm": armor_painter(plate, plate, shade(plate, 1.25)),
        "left_arm": armor_painter(plate, plate, shade(plate, 1.25)),
        "right_leg": legs_painter(hexc("#3A3A40"), plate, boot_top=8, cuff=shade(plate, 1.3)),
        "left_leg": legs_painter(hexc("#3A3A40"), plate, boot_top=8, cuff=shade(plate, 1.3)),
    }
    humanoid(rig, painters, slim=False, headless=True)
    rig.box("head", -3, -1, -2, 6, 1, 4, stump)
    rig.box("head", -4.5, -0.5, -2.5, 9, 1, 5, p_metal(shade(plate, 1.15)))
    hand_prop_pivot(rig, slim=False)
    steel = hexc("#8C929C")
    rig.box("prop", -0.5, 0, -0.5, 1, 4, 1, p_wood(hexc("#2E2420")))
    rig.box("prop", -2, -0.5, -0.5, 4, 1, 1, p_metal(steel))
    rig.box("prop", -1, -14, -0.5, 2, 14, 1, p_blade(shade(steel, 1.15), steel))
    return rig


def linie():
    skin = hexc("#F2DCD0")
    hair_c = hexc("#E06A6A")
    eye = hexc("#F0A030")
    cloth = hexc("#4A1E28")
    trim_c = hexc("#C9A43A")
    rig = Rig("linie", render_scale=0.92)
    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=5, part=None, locks=(1, 6),
                             face_kw=dict(eye_y=7, gap=4, mouth="smile", glow=True, brow=shade(hair_c, 0.7))),
        "hat": hat_hair_painter(hair_c, fringe=3, side_rows=7, back_rows=12),
        "body": robe_body(cloth, trim_c, sash=hexc("#2A1418")),
        "right_arm": arm_skin_painter(skin, sleeve=cloth), "left_arm": arm_skin_painter(skin, sleeve=cloth),
        "right_leg": legs_painter(hexc("#2A1418"), hexc("#3A2420"), boot_top=10),
        "left_leg": legs_painter(hexc("#2A1418"), hexc("#3A2420"), boot_top=10),
    }
    humanoid(rig, painters, slim=True)
    add_horns(rig, hexc("#3A2224"), size="small")
    add_braid(rig, "tail_right", hair_c, offset=(-3, -2, 3), length=12, rotation=(0.1, 0, 0.1), tie=trim_c, seed=210)
    add_braid(rig, "tail_left", hair_c, offset=(3, -2, 3), length=12, rotation=(0.1, 0, -0.1), tie=trim_c, seed=220)
    hand_prop_pivot(rig, slim=True)
    steel = hexc("#B0B4BE")
    rig.box("prop", -0.5, -15, -0.5, 1, 22, 1, p_wood(hexc("#3A2620")))
    rig.box("prop", -4.5, -16, -0.5, 4, 6, 1, p_blade(shade(steel, 1.25), steel))
    rig.box("prop", 0.5, -16, -0.5, 4, 6, 1, p_blade(shade(steel, 1.25), steel))
    return rig


def draht():
    skin = hexc("#F0DACC")
    hair_c = hexc("#E8D27A")
    eye = hexc("#6AC8D8")
    cloth = hexc("#3A3A4E")
    trim_c = hexc("#9A9AB8")
    rig = Rig("draht", render_scale=1.0)

    def spool(cv, f):
        for name, r in f.items():
            fill(cv, r, hexc("#D8D8E8") if name in ("front", "back", "left", "right") else hexc("#6A4A3A"), noise=0.02)
            if name in ("front", "back", "left", "right"):
                for row in range(0, r[3], 2):
                    hline(cv, r, row, hexc("#BFC3D8"))

    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=5, part=10, locks=(1, 6),
                             face_kw=dict(eye_y=7, gap=4, mouth="smirk", glow=True)),
        "hat": hat_hair_painter(hair_c, fringe=3, side_rows=7, back_rows=10),
        "body": robe_body(cloth, trim_c, collar=trim_c),
        "right_arm": arm_skin_painter(skin, sleeve=cloth), "left_arm": arm_skin_painter(skin, sleeve=cloth),
        "right_leg": legs_painter(cloth, hexc("#22222E"), boot_top=12),
        "left_leg": legs_painter(cloth, hexc("#22222E"), boot_top=12),
    }
    humanoid(rig, painters, slim=False)
    add_horns(rig, hexc("#2E2E3A"), size="medium")
    hand_prop_pivot(rig, slim=False)
    rig.box("prop", -1.5, -1, -1.5, 3, 3, 3, spool)
    rig.pose("prop_left", offset=(1.0, 9, 0))
    rig.box("prop_left", -1.5, -1, -1.5, 3, 3, 3, spool)
    return rig


def lugner():
    skin = hexc("#E8D2C6")
    hair_c = hexc("#2A3458")
    eye = hexc("#D8303A")
    suit = hexc("#22263A")
    red = hexc("#9A1E2A")
    rig = Rig("lugner", render_scale=1.0)

    def head_extra(cv, f):
        r = f["front"]
        # glasses
        for xx in range(2, 14):
            cv.px(r[0] + xx, r[1] + 7, hexc("#B8BCC8"))
        cv.rect(r[0] + 2, r[1] + 7, 1, 3, hexc("#B8BCC8"))
        cv.rect(r[0] + 13, r[1] + 7, 1, 3, hexc("#B8BCC8"))

    base_head = head_painter(skin, hair_c, eye, fringe=3, part=11, locks=(1, 5),
                             face_kw=dict(eye_y=7, gap=4, mouth="stern", glow=True))

    def head(cv, f):
        base_head(cv, f)
        head_extra(cv, f)

    painters = {
        "head": head,
        "hat": hat_hair_painter(hair_c, fringe=2, side_rows=6, back_rows=9),
        "body": robe_body(suit, red, collar=hexc("#E8E8EE"), collar_rows=3),
        "right_arm": arm_skin_painter(skin, sleeve=suit), "left_arm": arm_skin_painter(skin, sleeve=suit),
        "right_leg": legs_painter(suit, hexc("#15151E"), boot_top=14),
        "left_leg": legs_painter(suit, hexc("#15151E"), boot_top=14),
    }
    humanoid(rig, painters, slim=False)
    add_horns(rig, hexc("#1E1E28"), size="medium")
    hand_prop_pivot(rig, slim=False)
    rig.box("prop", -0.5, 0, -0.5, 1, 4, 1, p_metal(hexc("#3A1E22")))
    rig.box("prop", -1, -14, -0.5, 2, 14, 1, p_blade(hexc("#E8404A"), hexc("#A01E28")))
    return rig


def macht():
    skin = hexc("#E6D0C4")
    hair_c = hexc("#BFBCC6")
    eye = hexc("#E0B040")
    coat = hexc("#1E1A16")
    gold = hexc("#E2BC4A")
    rig = Rig("macht", render_scale=1.12)
    painters = {
        "head": head_painter(skin, hair_c, eye, fringe=3, part=7, locks=(2, 11),
                             face_kw=dict(eye_y=7, gap=4, mouth="stern", glow=True, brow=shade(hair_c, 0.7))),
        "hat": hat_hair_painter(hair_c, fringe=2, side_rows=12, back_rows=16),
        "body": robe_body(coat, gold, collar=gold, emblem=gold),
        "right_arm": arm_skin_painter(skin, sleeve=coat), "left_arm": arm_skin_painter(skin, sleeve=coat),
        "right_sleeve": sleeve_painter(coat, gold, trim_c=gold), "left_sleeve": sleeve_painter(coat, gold, trim_c=gold),
        "right_leg": legs_painter(coat, hexc("#3A2E1E"), boot_top=13, cuff=gold),
        "left_leg": legs_painter(coat, hexc("#3A2E1E"), boot_top=13, cuff=gold),
    }
    humanoid(rig, painters, slim=False)
    add_long_hair(rig, hair_c, length=16, width=8)
    add_horns(rig, gold, tip=hexc("#FFF2B0"), size="large")
    add_cape(rig, lambda cv, f: [fill(cv, r, coat if n != "front" else hexc("#5A4A1A"), noise=0.03, gradient=0.15) for n, r in f.items()],
             length=20, width=10)
    return rig


def demon_king():
    skin = hexc("#B8A8B0")
    eye = hexc("#FF3A2A")
    armor = hexc("#1E1A22")
    red = hexc("#8A1420")
    gold = hexc("#A8823A")
    rig = Rig("demon_king", render_scale=1.65)

    def helm(cv, f):
        for i, (name, r) in enumerate(f.items()):
            fill(cv, r, armor, noise=0.05, seed=300 + i, gradient=0.1)
        r = f["front"]
        fill(cv, sub(r, 2, 6, 12, 5), hexc("#0A080C"), noise=0.0)
        for xx in (4, 5, 10, 11):
            cv.px(r[0] + xx, r[1] + 8, eye)
            cv.px(r[0] + xx, r[1] + 9, shade(eye, 0.7))
        vline(cv, r, 7, gold, y_to=6)
        vline(cv, r, 8, gold, y_to=6)

    def cape(cv, f):
        for name, r in f.items():
            fill(cv, r, red if name == "front" else hexc("#120E16"), noise=0.04, gradient=0.2)
            if name == "back":
                hline(cv, r, 0, gold)

    def plate_with_glow(base):
        inner = armor_painter(base, hexc("#2A1E26"), gold, belt=hexc("#2A1418"))

        def painter(cv, f):
            inner(cv, f)
            r = f["front"]
            emblem_diamond(cv, r[0] + r[2] // 2, r[1] + 8, eye, radius=2)
        return painter

    painters = {
        "head": helm,
        "body": plate_with_glow(armor),
        "right_arm": armor_painter(armor, armor, gold),
        "left_arm": armor_painter(armor, armor, gold),
        "right_leg": legs_painter(hexc("#14101A"), armor, boot_top=8, cuff=gold),
        "left_leg": legs_painter(hexc("#14101A"), armor, boot_top=8, cuff=gold),
    }
    humanoid(rig, painters, slim=False)
    # crown of horns
    for part, sx in (("horn_right", -1), ("horn_left", 1)):
        rig.pose(part, offset=(3.2 * sx, -7.8, 0), rotation=(-0.2, 0, 0.35 * sx))
        rig.box(part, -1, -4, -1, 2, 4, 2, p_fill(hexc("#2A2028"), noise=0.05))
        rig.box(part, -0.5 + 1.0 * sx, -9, -0.5, 1, 5, 1, p_fill(hexc("#3A2A34"), noise=0.05))
        rig.box(part, 0.0 + 2.0 * sx, -11, -0.5, 1, 3, 1, p_fill(hexc("#7A5A6A"), noise=0.05))
    rig.pose("crown", offset=(0, -8, 0))
    for x in (-3.5, -1, 1.5):
        rig.box("crown", x, -3, -4.6, 2, 3, 1, p_metal(gold))
    add_cape(rig, cape, length=22, width=12)
    hand_prop_pivot(rig, slim=False)
    rig.box("prop", -0.5, 0, -0.5, 1, 5, 1, p_metal(hexc("#2A2028")))
    rig.box("prop", -3, -0.5, -0.5, 6, 1, 1, p_metal(gold))
    rig.box("prop", -1.5, -20, -0.5, 3, 20, 1, p_blade(eye, hexc("#14101A")))
    return rig


def mirror_frieren():
    """Spiegel's reflection of Frieren: the same rig, made of rippling mirror-glass."""
    rig = frieren()
    rig.name = "mirror_frieren"
    glass = hexc("#BFE6F0")

    def mirror_tint(cv):
        a = cv.a
        rgb = a[..., :3].astype(float)
        lum = rgb.mean(axis=2, keepdims=True)
        tinted = lum * 0.55 + rgb * 0.2 + [glass[0] * 0.3, glass[1] * 0.32, glass[2] * 0.36]
        a[..., :3] = tinted.clip(0, 255).astype("uint8")
        for y in range(0, cv.h, 7):
            for x in range(cv.w):
                if a[y, x, 3] and (x + y) % 5 == 0:
                    a[y, x, :3] = (235, 250, 255)
    rig.post_paint = mirror_tint
    return rig


def mirror_replica():
    """A faceless reflection: glassy humanoid with cracks and glowing eyes."""
    glass = hexc("#9FC8D8")
    rig = Rig("mirror_replica", render_scale=1.0)

    def glass_painter(seed):
        def painter(cv, f):
            for i, (name, r) in enumerate(f.items()):
                fill(cv, r, glass, noise=0.08, seed=seed + i, gradient=0.15)
                for k in range(3):
                    x = r[0] + (seed * 7 + k * 5 + i) % max(1, r[2])
                    for y in range(r[1], r[1] + r[3], 2):
                        cv.px(x, y, (235, 250, 255, 255))
        return painter

    def head(cv, f):
        glass_painter(400)(cv, f)
        r = f["front"]
        for xx in (4, 5, 10, 11):
            cv.px(r[0] + xx, r[1] + 8, hexc("#E8FFFF"))
            cv.px(r[0] + xx, r[1] + 9, hexc("#7FE0F0"))

    painters = {"head": head, "body": glass_painter(410), "right_arm": glass_painter(420),
                "left_arm": glass_painter(430), "right_leg": glass_painter(440), "left_leg": glass_painter(450)}
    humanoid(rig, painters, slim=False)
    return rig


def spiegel():
    """
    Spiegel, the mirror of the labyrinth: a pale being of silver and glass shards in a long robe. Its head is a blank
    oval mirror in a silver frame (corners cut away), crowned by a small silver crest; a mirror is set into its chest.
    """
    silver = hexc("#D9DEE8")
    silver_dark = hexc("#9AA3B5")
    glass = hexc("#CFEAF4")
    robe = hexc("#E4EEF4")
    rig = Rig("spiegel", render_scale=1.15)

    def mirror_face(cv, r, frame=2, oval=True):
        x0, y0, w, h = r
        for yy in range(h):
            for xx in range(w):
                # elliptical mask: pixels outside the oval are cleared (character skins render as cutout)
                nx, ny = (xx + 0.5 - w / 2) / (w / 2), (yy + 0.5 - h / 2) / (h / 2)
                d = nx * nx + ny * ny
                if oval and d > 1.0:
                    cv.a[y0 + yy, x0 + xx] = (0, 0, 0, 0)
                elif oval and d > 0.62 or not oval and min(xx, yy, w - 1 - xx, h - 1 - yy) < frame:
                    cv.px(x0 + xx, y0 + yy, silver if (xx + yy) % 5 else silver_dark)
                else:
                    t = (xx + yy) / (w + h)
                    cv.px(x0 + xx, y0 + yy, mix(glass, (250, 254, 255, 255), max(0.0, 0.7 - abs(t - 0.4) * 3)))

    def head(cv, f):
        for name, r in f.items():
            fill(cv, r, silver, noise=0.04, seed=600)
        mirror_face(cv, f["front"])
        mirror_face(cv, f["back"])

    def shards(seed, base):
        def painter(cv, f):
            for i, (name, r) in enumerate(f.items()):
                fill(cv, r, base, noise=0.05, seed=seed + i, gradient=0.12)
                x0, y0, w, h = r
                for k in range(max(1, w // 4)):
                    sx = (seed * 3 + k * 7 + i * 5) % max(1, w)
                    for yy in range(0, h, 1):
                        xx = sx + (yy // 3) % 2
                        if 0 <= xx < w and (yy + k) % 6 < 4:
                            cv.px(x0 + xx, y0 + yy, shade(base, 0.85))
        return painter

    def body(cv, f):
        shards(610, robe)(cv, f)
        r = f["front"]
        mirror_face(cv, sub(r, 3, 3, r[2] - 6, r[3] - 8), oval=False)

    painters = {"head": head, "body": body, "right_arm": shards(620, robe), "left_arm": shards(630, robe),
                "right_leg": shards(640, silver_dark), "left_leg": shards(650, silver_dark)}
    humanoid(rig, painters, slim=True, legs=13, arm_len=13)
    add_skirt(rig, shards(660, robe), length=12, width=9, depth=5)
    # A small silver crest on the head.
    rig.pose("crown", offset=(0, -8, 0))
    rig.box("crown", -1, -3, -0.5, 2, 3, 1, p_metal(silver))
    rig.box("crown", -3, -2, -0.5, 1, 2, 1, p_metal(silver))
    rig.box("crown", 2, -2, -0.5, 1, 2, 1, p_metal(silver))
    return rig


def mana_decoy():
    glow = hexc("#7FB8FF")
    rig = Rig("mana_decoy", render_scale=1.0)

    def painter(cv, f):
        for i, (name, r) in enumerate(f.items()):
            fill(cv, r, glow, noise=0.12, seed=500 + i, gradient=-0.2)
            for y in range(0, r[3], 3):
                hline(cv, r, y, shade(glow, 1.3))

    humanoid(rig, {k: painter for k in ("head", "body", "right_arm", "left_arm", "right_leg", "left_leg")}, slim=False)
    return rig


# ---------------------------------------------------------------------------------------------
# The Hero's statue: Himmel's rig cast in copper, in four patina stages (Rust Removal polishes it)
# ---------------------------------------------------------------------------------------------
COPPER_STAGES = {
    # name: (shadow, mid, highlight) of the metal; the skin's luminance picks between them
    "hero_statue": (hexc("#2E6B5E"), hexc("#4FA58B"), hexc("#8FD8C0")),            # oxidized (verdigris)
    "hero_statue_weathered": (hexc("#3F5E4A"), hexc("#6B9A7A"), hexc("#B5C9A0")),
    "hero_statue_exposed": (hexc("#6B4535"), hexc("#A77B5B"), hexc("#D9B08A")),
    "hero_statue_polished": (hexc("#7A3A22"), hexc("#D9773F"), hexc("#FFD1A0")),   # freshly polished copper
}


def copper_cast(stage):
    """Post-paint turning the painted skin into a metal casting: luminance mapped onto the stage's palette."""
    lo, mid, hi = COPPER_STAGES[stage]
    oxidized = stage == "hero_statue"

    def paint(cv):
        import numpy as np
        a = cv.a
        rgb = a[..., :3].astype(float)
        lum = (rgb[..., 0] * 0.3 + rgb[..., 1] * 0.55 + rgb[..., 2] * 0.15) / 255.0
        lum = np.clip((lum - 0.12) / 0.8, 0.0, 1.0)
        lo_c, mid_c, hi_c = (np.array(c[:3], dtype=float) for c in (lo, mid, hi))
        t = lum[..., None]
        low_mix = lo_c + (mid_c - lo_c) * np.clip(t * 2.0, 0, 1)
        high_mix = mid_c + (hi_c - mid_c) * np.clip(t * 2.0 - 1.0, 0, 1)
        out = np.where(t < 0.5, low_mix, high_mix)
        if oxidized:
            # verdigris streaks running down from edges
            rng = np.random.default_rng(7)
            streaks = rng.random(out.shape[:2]) < 0.06
            out[streaks] = out[streaks] * 0.75 + np.array(hi_c) * 0.25
        a[..., :3] = np.clip(out, 0, 255).astype("uint8")
    return paint


def hero_statue():
    rig = himmel()
    rig.name = "hero_statue"
    rig.render_scale = 2.8
    rig.post_paint = copper_cast("hero_statue")
    rig.variants = {name: copper_cast(name) for name in COPPER_STAGES if name != "hero_statue"}
    return rig


ROSTER = {
    "frieren": frieren,
    "fern": fern,
    "stark": stark,
    "serie": serie,
    "himmel": himmel,
    "eisen": eisen,
    "examiner": examiner,
    "wandering_mage": wandering_mage,
    "priest": priest,
    "aura": aura,
    "qual": qual,
    "demon_soldier": demon_soldier,
    "demon_mage": demon_mage,
    "aura_thrall": aura_thrall,
    "linie": linie,
    "draht": draht,
    "lugner": lugner,
    "macht": macht,
    "demon_king": demon_king,
    "mirror_frieren": mirror_frieren,
    "mirror_replica": mirror_replica,
    "spiegel": spiegel,
    "mana_decoy": mana_decoy,
    "hero_statue": hero_statue,
}
