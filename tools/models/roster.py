"""
The cast. Each function builds a Rig (geometry + painters). Designs follow the anime's silhouettes and colour
schemes at Minecraft scale; textures are original pixel art painted procedurally.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from characters import Rig, hand_prop_pivot, humanoid  # noqa: E402
from skinpaint import (border, buttons, emblem_diamond, face_template, gem, hexc, hline, metal, mix,  # noqa: E402
                       pixel_art, side_locks, sub, trim, vline, wood)
# Character skins use the anime style under the classic names: flat cel fills with hue-shifted shadows, clean hair
# locks, pointed fringes and big eyes (the mimic and other generators keep the noisy originals).
from skinpaint import anime_bangs as bangs, anime_face as face, anime_hair as hair, cel_fill as fill  # noqa: E402
from skinpaint import tone as shade  # noqa: E402
from parts import chain, cloth_painter, hair_painter, plus_box, zigzag  # noqa: E402

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
    """
    Head: anime face from skinpaint.face_template on the front (fringe of pointed locks, side locks, eyes, brows),
    clean hair locks on top/sides/back, and the jaw on the sides for short-haired characters.
    face_kw: eye_y, eye_w, gap, mouth, brow, lash, brow_angle, blush, glow, sleepy, style, depth.
    """
    kw = dict(face_kw or {})
    style = kw.pop("style", None)
    if style is None:
        style = "calm" if kw.get("sleepy") else ("fierce" if kw.get("brow_angle", 0) > 0 else "soft")
    rows, palette = face_template(
        skin, hair_c, eye, fringe=fringe, part=part, locks=locks, eye_y=kw.get("eye_y", 7) + 1,
        eye_w=kw.get("eye_w", 4), gap=kw.get("gap", 4), style=style, mouth=kw.get("mouth", "small"),
        brow=kw.get("brow"), lash=kw.get("lash"), brow_angle=max(-1, min(1, kw.get("brow_angle", 0))),
        blush=kw.get("blush", False), glow=kw.get("glow", False), depth=kw.get("depth"), seed=seed)

    def painter(cv, f):
        hair(cv, f["top"], hair_c, seed=seed, highlight_row=None)
        if part is not None:
            vline(cv, f["top"], part, shade(hair_c, 0.84), y_from=f["top"][3] // 2)
        pixel_art(cv, f["front"], rows, palette)
        for side in ("right", "left"):
            r = f[side]
            hair(cv, r, hair_c, seed=seed + 7, highlight_row=4)
            if side_skin:
                # jaw and cheek toward the front edge
                fx = r[2] - 6 if side == "right" else 0
                for yy in range(9, 16):
                    for xx in range(6):
                        if yy - 9 + (xx if side == "right" else 5 - xx) > 2:
                            cv.px(r[0] + fx + xx, r[1] + yy, shade(skin, 0.95))
        hair(cv, f["back"], hair_c, seed=seed + 9, highlight_row=4)
        if back_detail:
            back_detail(cv, f["back"])
        fill(cv, f["bottom"], shade(skin, 0.86))
    return painter


def soft_head(rig, skin, hair_c, eye, face=None, rows=None, palette=None, side_rows=11, back_rows=16, back_tips=4,
              side_tips=4, fringe=5, fringe_tips=5, fringe_period=4, fringe_rows=5, part_col=None, crown=True,
              shell=True, seed=11, back_detail=None, side_skin=False, fringe_extra=None, crown_extra=None):
    """
    A head that does not read as a cube: the face (a face_template dict, or hand-made rows + palette) on the head
    box, a thin hair shell (open front, pointed lock tips along the sides and back), a fringe plate standing off
    the forehead, and an inset crown cap that chamfers the top edges.
    """
    if rows is None:
        kw = dict(face or {})
        kw.setdefault("fringe", fringe)
        rows, palette = face_template(skin, hair_c, eye, seed=seed, **kw)

    def head(cv, f):
        hair(cv, f["top"], hair_c, seed=seed, highlight_row=None)
        pixel_art(cv, f["front"], rows, palette)
        for side in ("right", "left"):
            r = f[side]
            hair(cv, r, hair_c, seed=seed + 7, highlight_row=4)
            if side_skin:
                fx = r[2] - 6 if side == "right" else 0
                for yy in range(9, 16):
                    for xx in range(6):
                        if yy - 9 + (xx if side == "right" else 5 - xx) > 2:
                            cv.px(r[0] + fx + xx, r[1] + yy, shade(skin, 0.95))
        hair(cv, f["back"], hair_c, seed=seed + 9, highlight_row=4)
        if back_detail:
            back_detail(cv, f["back"])
        fill(cv, f["bottom"], shade(skin, 0.86))

    def shell_paint(cv, f):
        hair(cv, sub(f["back"], 0, 0, f["back"][2], back_rows), shade(hair_c, 1.02), seed=seed + 1, highlight_row=4)
        if back_tips:
            zigzag(cv, sub(f["back"], 0, 0, f["back"][2], back_rows), back_tips, seed=seed + 1)
        for side in ("right", "left"):
            r = sub(f[side], 0, 0, f[side][2], side_rows)
            hair(cv, r, shade(hair_c, 1.02), seed=seed + 2, highlight_row=4)
            if side_tips:
                zigzag(cv, r, side_tips, seed=seed + 2)
        hair(cv, sub(f["front"], 0, 0, f["front"][2], 2), shade(hair_c, 1.04), seed=seed + 3, highlight_row=None)

    def fringe_paint(cv, f):
        for name, r in f.items():
            hair(cv, r, shade(hair_c, 1.05), seed=seed + 4, highlight_row=1 if name == "front" else None,
                 strands=name == "front")
        r = f["front"]
        if fringe_tips:
            zigzag(cv, r, fringe_tips, seed=seed + 5, period=fringe_period)
        if part_col is not None:
            for yy in range(0, 3):
                cv.px(r[0] + part_col, r[1] + yy, shade(hair_c, 0.85))
        if fringe_extra:
            fringe_extra(cv, f)

    if fringe_rows:
        rig.box("bangs", -4.5, -8.3, -4.75, 9, fringe_rows, 1, fringe_paint)
    if crown:
        cap = hair_painter(shade(hair_c, 1.04), seed=seed + 6, highlight_row=None)

        def cap_paint(cv, f):
            cap(cv, f)
            if crown_extra:
                crown_extra(cv, f)
        rig.box("hair_top", -4, -9.1, -4, 8, 1, 8, cap_paint)
        rig.box("hair_top", -4.5, -8.6, -4.5, 9, 1, 9, hair_painter(shade(hair_c, 1.02), seed=seed + 7, highlight_row=None))
    return head, (shell_paint if shell else None)


def horn_chain(rig, sx, base, links, color, tip=None, ribs=True):
    """
    Curving horns as a chain of links (horn_left -> horn_left_2 -> horn_left_3): base is the first link's pivot on
    the head, links are (pivot, rotation, (w, h, d)) with later pivots relative to the previous link (the first
    link's pivot entry is ignored); each link grows upward (-y) from its pivot. Everything is given for the left
    horn (sx = +1) and mirrored for the right one (sx = -1).
    """
    names = ("horn_right", "horn_right_2", "horn_right_3") if sx < 0 else ("horn_left", "horn_left_2", "horn_left_3")
    tip = tip or shade(color, 1.15)
    for i, (name, (pivot, rot, size)) in enumerate(zip(names, links)):
        px, py, pz = base if i == 0 else pivot     # the first link hangs off the head, the rest off their parent
        rx, ry, rz = rot
        rig.pose(name, offset=(px * sx, py, pz), rotation=(rx, ry * sx, rz * sx))
        w, h, d = size
        last = i == len(links) - 1

        def paint(cv, f, c=(tip if last else color), last=last):
            for name_, r in f.items():
                fill(cv, r, c, gradient=-0.08)
                if ribs and name_ not in ("top", "bottom"):
                    for yy in range(1, r[3], 3):
                        hline(cv, r, yy, shade(c, 0.82))
        rig.box(name, -w / 2, -h, -d / 2, w, h, d, paint)


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
    """
    Frieren as drawn in the anime: pale silver twin tails hanging past her waist, long elf ears with red drop
    earrings, calm half-lidded green eyes, a white capelet with gold trim and red gems over a black-and-white
    striped high-collared shirt, a white dress with a gold hem, gold cuffs, dark tights and brown boots.
    She holds her staff as a real item (frieren:sages_staff), not a baked prop.
    """
    skin = hexc("#FCEADF")
    hair_c = hexc("#EEECF6")
    eye = hexc("#44AA8A")
    white = hexc("#F6F4EF")
    gold = hexc("#CFAE52")
    stripe = hexc("#3A3946")
    tights = hexc("#3E4254")
    boot = hexc("#8C6648")
    red = hexc("#C8283A")
    belt = hexc("#3B3747")
    rig = Rig("frieren", render_scale=0.9)

    pal = {
        "H": hair_c, "h": shade(hair_c, 0.88), "d": shade(hair_c, 0.74),
        "S": skin, "s": shade(skin, 0.9), "n": shade(skin, 0.84), "m": hexc("#E2A8A0"),
        "L": hexc("#3B3350"), "l": hexc("#8A7E9E"),
        "I": hexc("#2B7461"), "i": eye, "j": hexc("#86DDBF"), "p": hexc("#1D4F43"), "W": hexc("#FFFFFF"),
    }

    def face_rows():
        # fringe: pointed locks (first skin row per column), a parting at column 7, side locks framing the face
        depth = [16, 14, 6, 7, 6, 6, 5, 4, 5, 6, 6, 7, 6, 6, 14, 16]
        grid = [["S"] * 16 for _ in range(16)]
        for x, d in enumerate(depth):
            for y in range(min(16, d)):
                grid[y][x] = "H"
            if 0 < d <= 15:
                grid[d - 1][x] = "d" if 1 < x < 14 else "h"
                if d < 16:
                    grid[d][x] = "s"
        for x in (1, 14):  # side locks are shaded toward the cheek
            for y in range(6, 14):
                grid[y][x] = "h"
        grid[3][6], grid[4][5], grid[3][10], grid[4][11] = "h", "h", "h", "h"   # lock lines in the fringe
        eye_rows = ["LLLL", "LIWI", "Iipi", "ljjW"]
        for y, row in enumerate(eye_rows):
            for i, key in enumerate(row):
                grid[8 + y][2 + i] = key        # her right eye (viewer's left): outer corner at column 2
                grid[8 + y][13 - i] = key       # mirrored: outer corner at column 13
        grid[8][1] = grid[8][14] = "L"          # lash wings
        grid[14][7] = grid[14][8] = "m"
        return ["".join(r) for r in grid]

    def head(cv, f):
        hair(cv, f["top"], hair_c, seed=11, highlight_row=None)
        pixel_art(cv, f["front"], face_rows(), pal)
        for side in ("right", "left"):
            hair(cv, f[side], hair_c, seed=17, highlight_row=4)
        hair(cv, f["back"], hair_c, seed=19, highlight_row=4)
        r = f["back"]
        for col in (3, r[2] - 4):  # strands gathering toward the twin tails
            vline(cv, r, col, shade(hair_c, 0.78), y_from=5)
        fill(cv, f["bottom"], shade(skin, 0.86))

    def hat(cv, f):
        # A thin shell of hair: full top and back, sides down to the ears, all ending in pointed locks; the front
        # stays open (the fringe is its own plate) so the head's outline is hair, not a cube.
        hair(cv, f["back"], shade(hair_c, 1.02), seed=14, highlight_row=4)
        zigzag(cv, f["back"], 4, seed=14)
        for side in ("right", "left"):
            r = f[side]
            hair(cv, sub(r, 0, 0, r[2], 11), shade(hair_c, 1.02), seed=15, highlight_row=4)
            zigzag(cv, sub(r, 0, 0, r[2], 11), 4, seed=15)
        hair(cv, sub(f["front"], 0, 0, f["front"][2], 2), shade(hair_c, 1.04), seed=16, highlight_row=None)

    def fringe(cv, f):
        # the fringe plate: pointed locks, a parting just off centre, lighter where the hair catches the light
        for name, r in f.items():
            hair(cv, r, shade(hair_c, 1.05), seed=17, highlight_row=1 if name == "front" else None,
                 strands=name in ("front",))
        r = f["front"]
        zigzag(cv, r, 5, seed=18, period=4)
        for yy in range(0, 3):
            cv.px(r[0] + 9, r[1] + yy, shade(hair_c, 0.85))

    def lock(cv, f):
        for r in f.values():
            hair(cv, r, hair_c, seed=21, highlight_row=1, strands=False)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, white)
            if name in ("top", "bottom"):
                continue
            # striped shirt under the capelet (rows 0-13), then the belt and the white dress
            for y in range(0, 14):
                if (y // 2) % 2 == 0:
                    hline(cv, r, y, stripe)
            fill(cv, sub(r, 0, 14, r[2], 2), belt)
            fill(cv, sub(r, 0, 16, r[2], r[3] - 16), white, gradient=0.1)
        r = f["front"]
        fill(cv, sub(r, 4, 0, 8, 3), stripe)            # high collar
        hline(cv, r, 0, gold, x_from=4, x_to=12)
        cv.rect(r[0] + 7, r[1] + 1, 2, 2, red)          # collar brooch
        cv.px(r[0] + 7, r[1] + 1, shade(red, 1.5))
        cv.rect(r[0] + 7, r[1] + 14, 2, 2, gold)        # buckle
        vline(cv, r, 7, shade(white, 0.9), y_from=17)    # dress fold

    def capelet(cv, f):
        # 15x7x6 shell over the shoulders; its front opening (transparent) shows the striped shirt beneath
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, shade(white, 0.78))
                continue
            fill(cv, r, white, gradient=0.1)
            if name != "top":
                hline(cv, r, r[3] - 1, shade(gold, 0.85))
                hline(cv, r, r[3] - 2, gold)
        r = f["front"]
        w, h = r[2], r[3]
        cx = w // 2
        for y in range(h):                               # the opening widens toward the hem, edged in gold
            half = 4 + (y * 2) // h
            for x in range(cx - half, cx + half):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
            cv.px(r[0] + cx - half - 1, r[1] + y, gold)
            cv.px(r[0] + cx + half, r[1] + y, gold)
        for gx in (5, w - 7):                            # the two red gems on the capelet front
            cv.rect(r[0] + gx - 1, r[1] + 1, 4, 4, gold)
            cv.rect(r[0] + gx, r[1] + 2, 2, 2, red)
            cv.px(r[0] + gx, r[1] + 2, shade(red, 1.6))
        for face, rows in (("bottom", 6), ("top", 6)):   # matching gaps where the body passes through
            b = f[face]
            for x in range(b[2] // 2 - 5, b[2] // 2 + 5):
                for y in range(0, rows):
                    cv.px(b[0] + x, b[1] + y, (0, 0, 0, 0))

    def cape_shoulders(cv, f):
        for name, r in f.items():
            fill(cv, r, white if name != "bottom" else shade(white, 0.8))
        r = f["front"]
        cx = r[2] // 2
        for y in range(r[3]):
            for x in range(cx - 4, cx + 4):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
        b = f["bottom"]
        for x in range(b[2] // 2 - 4, b[2] // 2 + 4):
            for y in range(b[3] // 2):
                cv.px(b[0] + x, b[1] + y, (0, 0, 0, 0))
        t = f["top"]
        for x in range(t[2] // 2 - 4, t[2] // 2 + 4):
            for y in range(t[3] // 2):
                cv.px(t[0] + x, t[1] + y, (0, 0, 0, 0))

    def skirt(cv, f):
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, shade(white, 0.75))
                continue
            fill(cv, r, white, gradient=0.08)
            if name != "top":
                for col in range(2, r[2], 4):
                    vline(cv, r, col, shade(white, 0.92), y_from=2)

    def hem(cv, f):
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, shade(white, 0.75))
                continue
            fill(cv, r, white)
            if name != "top":
                hline(cv, r, 0, gold)
                hline(cv, r, r[3] - 1, shade(gold, 0.85))

    def sleeve(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            if name == "top":
                fill(cv, r, white)
                continue
            body_rows = r[3] - 5
            fill(cv, sub(r, 0, 0, r[2], body_rows), white, gradient=0.06)
            for y, c in ((body_rows - 6, gold), (body_rows - 5, gold), (body_rows - 2, gold), (body_rows - 1, shade(gold, 0.85))):
                hline(cv, r, y, c)

    def legs(cv, f):
        for name, r in f.items():
            if name == "top":
                fill(cv, r, tights)
                continue
            if name == "bottom":
                fill(cv, r, shade(boot, 0.6))
                continue
            fill(cv, sub(r, 0, 0, r[2], 13), tights)
            fill(cv, sub(r, 0, 13, r[2], r[3] - 13), boot, gradient=0.12)
            hline(cv, r, 13, shade(boot, 1.25))
            hline(cv, r, 14, shade(boot, 1.12))
            hline(cv, r, r[3] - 1, shade(boot, 0.55))

    def ear(cv, f):
        for name, r in f.items():
            fill(cv, r, skin)
        r = f["front"]
        hline(cv, r, r[3] - 1, shade(skin, 0.84))
        if r[2] >= 3:
            hline(cv, r, max(0, r[3] // 2), mix(skin, hexc("#E8A2A0"), 0.45), x_from=1, x_to=r[2] - 1)

    def earring(cv, f):
        for r in f.values():
            fill(cv, r, red)
        for r in (f["front"], f["back"], f["left"], f["right"]):
            hline(cv, r, 0, gold)
            cv.px(r[0], r[1] + 1, shade(red, 1.5))

    painters = {
        "head": head,
        "hat": hat,
        "body": body,
        "right_arm": arm_skin_painter(skin),
        "left_arm": arm_skin_painter(skin),
        "right_sleeve": sleeve,
        "left_sleeve": sleeve,
        "right_leg": legs,
        "left_leg": legs,
    }
    humanoid(rig, painters, slim=True)

    # Long elf ears, tips swept up toward brow height, red drop earrings hanging from the lobes.
    for part, sx in (("ear_right", -1), ("ear_left", 1)):
        rig.pose(part, offset=(4 * sx, -3.0, 0.6), rotation=(0, -0.3 * sx, -0.3 * sx))
        rig.box(part, -2 if sx < 0 else 0, -1, -0.5, 2, 2, 1, ear)
        rig.box(part, -4 if sx < 0 else 2, -1.2, -0.5, 2, 1, 1, ear)
        rig.box(part, -6 if sx < 0 else 4, -1.4, -0.5, 2, 1, 1, ear)
        rig.box(part, -1.2 if sx < 0 else 0.2, 1, -0.5, 1, 2, 1, earring)

    # The fringe stands just off the forehead; side locks hang in two swaying links past the chin.
    rig.box("bangs", -4.5, -8.3, -4.75, 9, 5, 1, fringe)
    # crown: an inset cap above the open-topped shell chamfers the head's top edges
    rig.box("hair_top", -4, -9.1, -4, 8, 1, 8, hair_painter(shade(hair_c, 1.04), seed=12, highlight_row=None))
    rig.box("hair_top", -4.5, -8.6, -4.5, 9, 1, 9, hair_painter(shade(hair_c, 1.02), seed=11, highlight_row=None))
    lock_tip = hair_painter(shade(hair_c, 1.02), seed=21, highlight_row=1, tips=3, tip_faces=("front", "back"))
    for part, sx in (("lock_right", -1), ("lock_left", 1)):
        chain(rig, (part, part + "_2"), ((3.8 * sx, -4.8, -3.8), (0, 5, 0)), ((-0.05, 0, -0.06 * sx), (0.04, 0, 0.05 * sx)))
        rig.box(part, -1, 0, -0.5, 2, 5, 1, hair_painter(hair_c, seed=22, highlight_row=1))
        rig.box(part + "_2", -1, 0, -0.5, 2, 4, 1, lock_tip)

    # Capelet over the shoulders, bell-shaped: a narrower shoulder tier over a wider hem (the jacket overlay
    # itself stays unpainted).
    rig.box("collar", -6.5, -0.6, -2.75, 13, 2, 6, cape_shoulders)
    rig.box("jacket", -7.5, 1.4, -3, 15, 5, 6, capelet)

    # Twin tails: three links each with a bevelled (rounded) cross-section, puffy at the tie, tapering to pointed
    # tips, curving out past the shoulders and back in, hanging behind the capelet down to her thighs.
    for side, sx in (("tail_right", -1), ("tail_left", 1)):
        chain(rig, (side, side + "_2", side + "_3"),
              ((4.6 * sx, -6.6, 4.2), (0, 8.5, 0), (0, 7.5, 0)),
              ((0.12, 0, -0.28 * sx), (0.05, 0, 0.13 * sx), (0.04, 0, 0.1 * sx)))
        rig.box(side, -1.5, -1, -1.5, 3, 10, 3, hair_painter(hair_c, seed=50 + sx, highlight_row=3, soft=True))
        rig.box(side + "_2", -1.5, 0, -1.5, 3, 8, 3, hair_painter(shade(hair_c, 0.97), seed=55 + sx,
                                                                   highlight_row=None, soft=True))
        rig.box(side + "_3", -1, 0, -1, 2, 7, 2, hair_painter(shade(hair_c, 0.93), seed=60 + sx, highlight_row=None,
                                                               tips=6, bottom=False, soft=True))
        rig.box(side, -1.5, -1.6, -1.5, 3, 1, 3, cloth_painter(shade(hair_c, 0.82)))   # where the tail is tied

    # A-line dress: white skirt with a flared gold-trimmed hem.
    rig.pose("skirt", offset=(0, 10.5, 0))
    rig.box("skirt", -4.5, 0, -2.5, 9, 4, 5, skirt)
    rig.box("skirt", -5, 4, -3, 10, 2, 6, hem)
    rig.held = ("frieren:sages_staff", None)
    return rig


# ---------------------------------------------------------------------------------------------
# Fern
# ---------------------------------------------------------------------------------------------
def fern():
    """
    Fern, from her character sheet: long straight violet hair in a hime cut (blunt fringe, side locks to the chest,
    hair to the hips), violet eyes; a long open charcoal coat with its hood bunched behind the neck, over a white
    ankle-length dress with a frilled high collar, pintucks and a row of buttons; dark boots.
    """
    skin = hexc("#FCEADF")
    hair_c = hexc("#8D4FBA")
    eye = hexc("#9A62D0")
    coat = hexc("#45434A")
    white = hexc("#F4F2EE")
    tuck = hexc("#D6D3D6")
    boot = hexc("#34333A")
    rig = Rig("fern", render_scale=0.95)

    # Hime cut: an even, blunt fringe at the brows; long straight side locks.
    depth = [16, 15] + [6] * 12 + [15, 16]
    head, shell = soft_head(rig, skin, hair_c, eye, face=dict(style="soft", eye_y=8, depth=depth, locks=(2, 13),
                                                             lash=hexc("#3A2A4A"), mouth="small"),
                            side_rows=16, side_tips=0, back_tips=0, fringe_tips=1, fringe_period=8, fringe_rows=4)

    def dress_body(cv, f):
        for name, r in f.items():
            fill(cv, r, white)
        r = f["front"]
        fill(cv, sub(r, 4, 0, 8, 3), white)                   # frilled stand collar
        hline(cv, r, 3, tuck, x_from=4, x_to=12)
        for x in (5, 7, 8, 10):                                # frill folds
            vline(cv, r, x, tuck, y_from=0, y_to=3)
        for x in (5, 6, 9, 10):                                # pintucks down the bodice
            vline(cv, r, x, tuck, y_from=4, y_to=14)
        for y in (5, 8, 11):                                   # buttons
            cv.px(r[0] + 7, r[1] + y, shade(tuck, 0.8))
            cv.px(r[0] + 8, r[1] + y, shade(tuck, 0.8))
        for name in ("front", "back", "left", "right"):        # waistband
            hline(cv, f[name], 14, tuck)
            hline(cv, f[name], 15, shade(white, 0.95))

    def coat_body(cv, f):
        # open coat over the dress: the front shows only its edges, the white dress stays visible in the middle
        for name, r in f.items():
            if name in ("top", "bottom"):
                continue
            fill(cv, r, coat, gradient=0.08, vertical_streaks=0.05)
        r = f["front"]
        for y in range(r[3]):
            open_half = 3 if y < 4 else 4
            for x in range(r[2] // 2 - open_half, r[2] // 2 + open_half):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
            cv.px(r[0] + r[2] // 2 - open_half - 1, r[1] + y, shade(coat, 1.25))
            cv.px(r[0] + r[2] // 2 + open_half, r[1] + y, shade(coat, 1.25))

    def hood(cv, f):
        for name, r in f.items():
            fill(cv, r, coat, gradient=-0.12)
            if name not in ("top", "bottom"):
                for x in range(2, r[2], 4):
                    vline(cv, r, x, shade(coat, 0.82))
        for face in ("front", "top", "bottom"):                # open where the head and dress pass through
            r = f[face]
            for x in range(r[2] // 2 - 4, r[2] // 2 + 4):
                for y in range(r[3] if face == "front" else r[3] // 2 + 1):
                    cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))

    def sleeve(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            fill(cv, r, coat, gradient=0.08)
            if name != "top":
                hline(cv, r, r[3] - 5, shade(coat, 1.2))       # wide cuff edge
                for yy in range(r[3] - 4, r[3]):
                    for xx in range(r[2]):
                        cv.px(r[0] + xx, r[1] + yy, (0, 0, 0, 0))

    def skirt(cv, f):
        for name, r in f.items():
            fill(cv, r, white if name != "bottom" else shade(white, 0.75), gradient=0.06 if name != "bottom" else 0)
            if name not in ("top", "bottom"):
                for col in range(3, r[2], 5):
                    vline(cv, r, col, shade(white, 0.92), y_from=3)

    def coat_tail(cv, f):
        for name, r in f.items():
            fill(cv, r, coat if name != "bottom" else shade(coat, 0.7), gradient=0.06, vertical_streaks=0.05)
            if name not in ("top", "bottom"):
                hline(cv, r, r[3] - 1, shade(coat, 0.75))

    def legs(cv, f):
        for name, r in f.items():
            fill(cv, r, boot if name != "top" else white)
            if name not in ("top", "bottom"):
                hline(cv, r, r[3] - 1, shade(boot, 0.6))
                hline(cv, r, r[3] - 7, shade(boot, 1.2))

    painters = {"head": head, "hat": shell, "body": dress_body, "jacket": coat_body,
                "right_arm": arm_skin_painter(skin, coat), "left_arm": arm_skin_painter(skin, coat),
                "right_sleeve": sleeve, "left_sleeve": sleeve, "right_leg": legs, "left_leg": legs}
    humanoid(rig, painters, slim=True)

    # Hair: long straight side locks to the chest, and a heavy curtain down the back to the hips.
    lock = hair_painter(hair_c, seed=31, highlight_row=1)
    lock_end = hair_painter(shade(hair_c, 0.97), seed=32, highlight_row=None, tips=1, tip_faces=("front", "back"))
    for part, sx in (("lock_right", -1), ("lock_left", 1)):
        chain(rig, (part, part + "_2"), ((3.7 * sx, -4.6, -3.7), (0, 6, 0)), ((-0.04, 0, -0.04 * sx), (0.03, 0, 0.03 * sx)))
        rig.box(part, -1, 0, -0.5, 2, 6, 1, lock)
        rig.box(part + "_2", -1, 0, -0.5, 2, 6, 1, lock_end)
    chain(rig, ("hair_back", "hair_back_2"), ((0, -7.0, 4.0), (0, 11, 0)), ((0.06, 0, 0), (0.03, 0, 0)))
    rig.box("hair_back", -4.5, 0, -0.5, 9, 11, 2, hair_painter(hair_c, seed=33, highlight_row=2))
    rig.box("hair_back_2", -4.5, 0, -0.5, 9, 9, 2, hair_painter(shade(hair_c, 0.96), seed=34, highlight_row=None,
                                                                  tips=1, tip_faces=("back", "front", "left", "right")))

    # Coat: the hood bunched behind the neck, the open front panels and the long back to the ankles.
    rig.box("collar", -5, -0.8, -2.6, 10, 3, 7, hood)
    rig.pose("skirt", offset=(0, 11, 0))
    rig.box("skirt", -4.5, 0, -2.5, 9, 12, 5, skirt)
    for part, sx in (("coat_right", -1), ("coat_left", 1)):
        rig.pose(part, offset=(0, 11, 0))
        x = -5.2 if sx < 0 else 2.6
        rig.box(part, x, 0, -3.2, 3, 12, 1, coat_tail)                      # front panel
        rig.box(part, -5.6 if sx < 0 else 4.6, 0, -2.6, 1, 12, 5, coat_tail)  # side panel
    rig.pose("cape", offset=(0, 0.5, 2.6), rotation=(0.04, 0, 0))
    rig.box("cape", -5, 0, 0, 10, 23, 1, coat_tail)
    rig.held = ("frieren:mage_staff", None)
    return rig


# ---------------------------------------------------------------------------------------------
def stark():
    """
    Stark, from the anime: spiky red-orange hair with a black streak at the crown, grey-blue eyes; a red jacket with
    white lapels over a black stand-collar shirt fastened with white frog toggles, a dark strap across the chest
    for the huge axe he carries on his back; dark trousers and brown boots.
    """
    skin = hexc("#F6DCC8")
    hair_c = hexc("#C9472E")
    streak = hexc("#2A2026")
    eye = hexc("#8CA0B6")
    red = hexc("#B3352D")
    lapel = hexc("#ECE7DE")
    shirt = hexc("#25232B")
    strap = hexc("#3A2A22")
    trousers = hexc("#3A3432")
    boot = hexc("#5C3F2C")
    rig = Rig("stark", render_scale=1.0)

    def streak_fringe(cv, f):
        r = f["front"]
        for yy in range(r[3] - 1):                               # the black lock falling into his fringe
            for xx in (r[2] // 2 - 1, r[2] // 2):
                if cv.get(r[0] + xx, r[1] + yy)[3]:
                    cv.px(r[0] + xx, r[1] + yy, streak if yy < r[3] - 3 else shade(streak, 1.6))
        t = f["top"]
        for yy in range(t[3]):
            cv.px(t[0] + t[2] // 2 - 1, t[1] + yy, streak)
            cv.px(t[0] + t[2] // 2, t[1] + yy, streak)

    def streak_crown(cv, f):
        t = f["top"]
        for yy in range(t[3]):
            for xx in range(t[2] // 2 - 2, t[2] // 2 + 2):
                cv.px(t[0] + xx, t[1] + yy, streak if abs(xx - t[2] // 2 + 0.5) < 1.6 else shade(streak, 1.5))
        for name in ("front", "back"):
            r = f[name]
            for xx in range(r[2] // 2 - 2, r[2] // 2 + 2):
                for yy in range(r[3]):
                    cv.px(r[0] + xx, r[1] + yy, streak)

    head, shell = soft_head(rig, skin, hair_c, eye,
                            face=dict(style="sharp", eye_y=8, fringe=6, part=None, locks=(1, 6), lash=hexc("#3A2A2A"),
                                      mouth="small", brow=shade(hair_c, 0.72)),
                            side_rows=12, side_tips=6, back_rows=16, back_tips=6, fringe_tips=5, fringe_period=3,
                            fringe_rows=4, side_skin=True, seed=41, fringe_extra=streak_fringe,
                            crown_extra=streak_crown)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, shirt, gradient=0.06)
        r = f["front"]
        vline(cv, r, 7, lapel, y_from=0)
        vline(cv, r, 8, lapel, y_from=0)
        for y in (4, 8, 12, 16):                              # frog toggles
            hline(cv, r, y, lapel, x_from=5, x_to=11)
        hline(cv, r, 0, lapel, x_from=5, x_to=11)              # collar edge

    def jacket(cv, f):
        for name, r in f.items():
            if name in ("top", "bottom"):
                continue
            fill(cv, r, red, gradient=0.08, vertical_streaks=0.04)
        r = f["front"]
        for y in range(r[3]):                                  # open front with white lapels near the collar
            half = 3 if y < 10 else 2
            for x in range(r[2] // 2 - half, r[2] // 2 + half):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
            if y < 9:
                lap = max(0, 3 - y // 3)
                for k in range(lap):
                    cv.px(r[0] + r[2] // 2 - half - 1 - k, r[1] + y, lapel)
                    cv.px(r[0] + r[2] // 2 + half + k, r[1] + y, lapel)
        for k in range(r[3] - 4):                               # strap from his left shoulder across to the right hip
            x = r[2] - 3 - (k * (r[2] - 4)) // (r[3] - 4)
            for t in (0, 1):
                if 0 <= x + t < r[2]:
                    cv.px(r[0] + x + t, r[1] + 2 + k, strap)
        b = f["back"]
        for k in range(b[3] - 4):
            x = 2 + (k * (b[2] - 4)) // (b[3] - 4)
            cv.px(b[0] + x, b[1] + 2 + k, strap)
            cv.px(b[0] + x + 1, b[1] + 2 + k, strap)

    def hem(cv, f):
        for name, r in f.items():
            fill(cv, r, red if name != "bottom" else shade(red, 0.7), gradient=0.1)
        r = f["front"]
        for y in range(r[3]):
            for x in range(r[2] // 2 - 2, r[2] // 2 + 2):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))

    def sleeve(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            fill(cv, r, red, gradient=0.08)
            if name != "top":
                hline(cv, r, r[3] - 6, shade(red, 0.75))
                for yy in range(r[3] - 5, r[3]):
                    for xx in range(r[2]):
                        cv.px(r[0] + xx, r[1] + yy, (0, 0, 0, 0))

    painters = {"head": head, "hat": shell, "body": body, "jacket": jacket,
                "right_arm": arm_skin_painter(skin, red), "left_arm": arm_skin_painter(skin, red),
                "right_sleeve": sleeve, "left_sleeve": sleeve,
                "right_leg": legs_painter(trousers, boot, boot_top=14, cuff=shade(boot, 1.2)),
                "left_leg": legs_painter(trousers, boot, boot_top=14, cuff=shade(boot, 1.2))}
    humanoid(rig, painters, slim=False)

    # Spikes: locks flicking out a little over the ears.
    spike = hair_painter(hair_c, seed=43, highlight_row=1, tips=3)
    for part, sx in (("lock_right", -1), ("lock_left", 1)):
        chain(rig, (part,), ((4.3 * sx, -5.5, -0.5),), ((0.15, 0, -0.25 * sx),))
        rig.box(part, -0.5, 0, -1.5, 1, 4, 3, spike)
    chain(rig, ("hair_back",), ((0, -3.0, 4.1),), ((0.35, 0, 0),))
    rig.box("hair_back", -3.5, 0, -0.5, 7, 3, 1, hair_painter(hair_c, seed=45, highlight_row=None, tips=3))

    # Jacket hem below the waist (open in front), and the axe rides on his back until he fights.
    rig.pose("skirt", offset=(0, 10.5, 0))
    rig.box("skirt", -4.5, 0, -2.5, 9, 3, 5, hem)
    rig.held = ("frieren:warriors_axe", None)
    rig.back_carry = True
    return rig


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
    """
    Himmel the Hero, from his character sheet: neat light-blue hair with a fringe, blue eyes and an easy smile; a
    blue knee-length tunic with a white-edged stand collar, five white toggles, white cuffs, a white belt with a
    grey buckle and a white sword strap; a large cream cloak over both shoulders, clasped at the throat; white
    trousers and dark knee boots banded in white.
    """
    skin = hexc("#F8E2D2")
    hair_c = hexc("#9DC3EC")
    eye = hexc("#7192C0")
    blue = hexc("#3F5FB0")
    white = hexc("#F0F1F4")
    cloak = hexc("#E6DEC9")
    metal = hexc("#8C9098")
    trousers = hexc("#E4E7EE")
    boot = hexc("#30343F")
    rig = Rig("himmel", render_scale=1.0)

    head, shell = soft_head(rig, skin, hair_c, eye,
                            face=dict(style="soft", eye_y=8, fringe=6, part=None, locks=(1, 7), lash=hexc("#33384A"),
                                      mouth="smile", brow=shade(hair_c, 0.72)),
                            side_rows=12, side_tips=3, back_rows=13, back_tips=3, fringe_tips=3, fringe_period=4,
                            fringe_rows=4, side_skin=True, seed=51)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, blue, gradient=0.08)
        r = f["front"]
        fill(cv, sub(r, 5, 0, 6, 2), blue)                    # stand collar edged in white
        hline(cv, r, 0, white, x_from=5, x_to=11)
        vline(cv, r, 5, white, y_from=0, y_to=2)
        vline(cv, r, 10, white, y_from=0, y_to=2)
        for y in (3, 5, 7, 9, 11):                            # toggles
            hline(cv, r, y, white, x_from=5, x_to=11)
        for name in ("front", "back", "left", "right"):       # white belt
            fill(cv, sub(f[name], 0, 14, f[name][2], 2), white)
        cv.rect(r[0] + 6, r[1] + 14, 4, 2, metal)             # buckle
        cv.rect(r[0] + 7, r[1] + 14, 2, 1, shade(metal, 1.3))
        for k in range(8):                                    # sword strap across the hip
            cv.px(r[0] + 2 + k, r[1] + 17 + k // 3, white)

    def tunic_skirt(cv, f):
        for name, r in f.items():
            fill(cv, r, blue if name != "bottom" else shade(blue, 0.7), gradient=0.1)
            if name not in ("top", "bottom"):
                hline(cv, r, r[3] - 1, white)
        r = f["front"]
        vline(cv, r, r[2] // 2, white, y_from=2)              # the front split
        vline(cv, r, r[2] // 2 - 1, shade(blue, 0.7), y_from=2)

    def mantle(cv, f):
        for name, r in f.items():
            fill(cv, r, cloak if name != "bottom" else shade(cloak, 0.78), gradient=0.08)
            if name not in ("top", "bottom"):
                for x in range(2, r[2], 5):
                    vline(cv, r, x, shade(cloak, 0.9), y_from=1)
        r = f["front"]
        for y in range(r[3]):                                  # open in front over the tunic
            half = 4 + y // 2
            for x in range(r[2] // 2 - half, r[2] // 2 + half):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
        cv.rect(r[0] + r[2] // 2 - 2, r[1], 4, 2, metal)      # the clasp at his throat
        cv.rect(r[0] + r[2] // 2 - 1, r[1], 2, 1, shade(metal, 1.4))
        for face in ("top", "bottom"):
            b = f[face]
            for x in range(b[2] // 2 - 4, b[2] // 2 + 4):
                for y in range(b[3] // 2 + 1):
                    cv.px(b[0] + x, b[1] + y, (0, 0, 0, 0))

    def cloak_panel(cv, f):
        for name, r in f.items():
            fill(cv, r, cloak if name != "front" else shade(cloak, 0.86), gradient=0.08)
            if name in ("back", "front"):
                for x in range(3, r[2], 5):
                    vline(cv, r, x, shade(cloak, 0.88), y_from=2)

    def sleeve(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            fill(cv, r, blue, gradient=0.06)
            if name != "top":
                hline(cv, r, r[3] - 6, white)
                hline(cv, r, r[3] - 7, white)
                for yy in range(r[3] - 5, r[3]):
                    for xx in range(r[2]):
                        cv.px(r[0] + xx, r[1] + yy, (0, 0, 0, 0))

    def legs(cv, f):
        for name, r in f.items():
            fill(cv, r, trousers if name != "bottom" else shade(boot, 0.6), gradient=0.05)
            if name in ("top", "bottom"):
                continue
            fill(cv, sub(r, 0, 12, r[2], r[3] - 12), boot, gradient=0.12)
            hline(cv, r, 12, white)
            hline(cv, r, 13, white)
            hline(cv, r, 14, metal)
            hline(cv, r, r[3] - 1, shade(boot, 0.55))

    painters = {"head": head, "hat": shell, "body": body,
                "right_arm": arm_skin_painter(skin, blue), "left_arm": arm_skin_painter(skin, blue),
                "right_sleeve": sleeve, "left_sleeve": sleeve, "right_leg": legs, "left_leg": legs}
    humanoid(rig, painters, slim=False)

    # Tunic to mid-thigh; the cloak: a mantle over both shoulders and a long back falling in two links to the boots.
    rig.pose("skirt", offset=(0, 11, 0))
    rig.box("skirt", -4.5, 0, -2.5, 9, 5, 5, tunic_skirt)
    rig.box("collar", -6.5, -0.8, -3, 13, 4, 6, mantle)
    chain(rig, ("cape", "cape_2"), ((0, 0.4, 3.0), (0, 12, 0)), ((0.06, 0, 0), (0.04, 0, 0)))
    rig.box("cape", -7, 0, 0, 14, 12, 1, cloak_panel)
    rig.box("cape_2", -7, 0, 0, 14, 11, 1, cloak_panel)
    rig.held = ("frieren:heros_sword", None)
    return rig


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
    rig.held = ("frieren:warriors_axe", None)
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
    rig.held = ("frieren:mage_staff", None)
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
    rig.held = ("frieren:apprentice_staff", None)
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


def aura():
    """
    Aura the Guillotine, from her character sheet: a pink bob with a blunt fringe and two braids falling in front of
    the shoulders, ivory horns curving out and up, blue-violet eyes and a smug little smile; a navy corset with
    diamond cut-outs and a gold choker, navy opera gloves, a white pleated skirt under a magenta drape knotted at
    the waist and trailing to the floor; navy thigh-highs banded in white. She carries the Scale of Obedience.
    """
    skin = hexc("#FBE6DC")
    hair_c = hexc("#D68BC8")
    eye = hexc("#6E70C4")
    navy = hexc("#272844")
    gold = hexc("#D8B04A")
    white = hexc("#F0EEF2")
    drape = hexc("#B43F74")
    horn = hexc("#E6DCB0")
    rig = Rig("aura", render_scale=0.95)

    depth = [16, 14, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5, 14, 16]
    head, shell = soft_head(rig, skin, hair_c, eye,
                            face=dict(style="calm", eye_y=8, depth=depth, locks=(2, 11), lash=hexc("#2E2440"),
                                      mouth="smile", brow=shade(hair_c, 0.75)),
                            side_rows=14, side_tips=2, back_rows=14, back_tips=2, fringe_tips=1, fringe_period=8,
                            fringe_rows=4, seed=61)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, navy, gradient=0.06)
        r = f["front"]
        for y in range(0, 3):                                  # bare shoulders above the sweetheart neckline
            hline(cv, r, y, skin)
        for x, y in ((1, 3), (2, 3), (13, 3), (14, 3)):
            cv.px(r[0] + x, r[1] + y, skin)
        hline(cv, r, 0, gold, x_from=5, x_to=11)               # gold choker and pendant
        cv.rect(r[0] + 7, r[1] + 1, 2, 2, gold)
        for dy in range(4):                                    # diamond window at the chest
            for dx in range(-dy // 2 - 1, dy // 2 + 1):
                cv.px(r[0] + 8 + dx, r[1] + 3 + dy, skin)
        for dy in range(-2, 3):                                # and at the waist
            for dx in range(-(2 - abs(dy)), 2 - abs(dy)):
                cv.px(r[0] + 8 + dx, r[1] + 14 + dy, skin)
        for x in (3, 12):                                      # gold seams
            vline(cv, r, x, shade(gold, 0.8), y_from=5, y_to=20)
        for name in ("left", "right", "back"):
            for y in range(0, 3):
                hline(cv, f[name], y, skin)

    def gloves(cv, f):
        for name, r in f.items():
            fill(cv, r, navy if name != "top" else skin, gradient=0.05)
            if name not in ("top", "bottom"):
                for y in range(0, 6):
                    hline(cv, r, y, skin)
                hline(cv, r, 6, shade(navy, 1.4))

    def skirt(cv, f):
        for name, r in f.items():
            fill(cv, r, white if name != "bottom" else shade(white, 0.72), gradient=0.08)
            if name not in ("top", "bottom"):
                for col in range(1, r[2], 3):
                    vline(cv, r, col, shade(white, 0.86), y_from=1)

    def drape_paint(cv, f):
        for name, r in f.items():
            fill(cv, r, drape if name != "front" else shade(drape, 0.8), gradient=0.12)
            if name in ("back", "front", "left", "right"):
                for col in range(2, r[2], 4):
                    vline(cv, r, col, shade(drape, 0.82), y_from=2)
                zigzag(cv, r, 2, seed=len(name), period=5)

    def bow(cv, f):
        for name, r in f.items():
            fill(cv, r, shade(drape, 1.08), gradient=-0.1)
        r = f["back"]
        vline(cv, r, r[2] // 2, shade(drape, 0.75))

    def legs(cv, f):
        for name, r in f.items():
            if name in ("top", "bottom"):
                fill(cv, r, skin if name == "top" else shade(navy, 0.6))
                continue
            fill(cv, sub(r, 0, 0, r[2], 4), skin)
            fill(cv, sub(r, 0, 4, r[2], r[3] - 4), navy, gradient=0.06)
            for y in (5, 6, 12, 13):                           # white bands at the thigh and knee
                hline(cv, r, y, white)
            hline(cv, r, r[3] - 1, shade(navy, 0.55))
        r = f["front"]
        cv.rect(r[0] + 3, r[1] + 19, 2, 2, gold)               # the diamond on each boot

    def braid(cv, f):
        for name, r in f.items():
            fill(cv, r, hair_c, gradient=0.05)
            if name not in ("top", "bottom"):
                for y in range(0, r[3], 3):                    # crossing plaits
                    for x in range(r[2]):
                        if (x + y) % 3 == 0:
                            cv.px(r[0] + x, r[1] + y, shade(hair_c, 0.78))
                        if (x + y) % 3 == 1:
                            cv.px(r[0] + x, r[1] + y + 1, shade(hair_c, 1.15))

    painters = {"head": head, "hat": shell, "body": body, "right_arm": gloves, "left_arm": gloves,
                "right_leg": legs, "left_leg": legs}
    humanoid(rig, painters, slim=True)

    # Two braids in front of the shoulders, tied off just above the hem of the chest.
    for part, sx in (("lock_right", -1), ("lock_left", 1)):
        chain(rig, (part, part + "_2"), ((4.1 * sx, -3.0, -2.6), (0, 5, 0)), ((-0.08, 0, -0.05 * sx), (0.05, 0, 0.04 * sx)))
        rig.box(part, -1, 0, -1, 2, 5, 2, braid)
        rig.box(part + "_2", -1, 0, -1, 2, 4, 2, braid)
        rig.box(part + "_2", -1, 4, -1, 2, 1, 2, cloth_painter(shade(hair_c, 0.85)))

    # Horns: out from the crown, sweeping up into points.
    for sx in (-1, 1):
        horn_chain(rig, sx, (3.0, -7.8, 0.5),
                   [(None, (0.0, 0.0, 1.1), (2, 3, 2)), ((0, -2.6, 0), (0.0, 0.0, -0.75), (2, 3, 2)),
                    ((0, -2.6, 0), (0.0, 0.0, -0.6), (1, 3, 1))], horn)

    # White pleated skirt; the magenta drape knotted at the back with a bow, trailing to the floor, open in front.
    rig.pose("skirt", offset=(0, 10.5, 0))
    rig.box("skirt", -4.5, 0, -2.5, 9, 3, 5, skirt)
    rig.box("skirt", -4, -0.5, 2.5, 3, 3, 2, bow)
    rig.box("skirt", 1, -0.5, 2.5, 3, 3, 2, bow)
    rig.box("skirt", -1, 0, 2.6, 2, 2, 1, bow)
    rig.pose("skirt_2", offset=(0, 1.5, 3.2), rotation=(0.1, 0, 0))
    rig.box("skirt_2", -6, 0, 0, 12, 12, 1, drape_paint)
    for part, sx in (("coat_right", -1), ("coat_left", 1)):
        rig.pose(part, offset=(5.0 * sx, 10.5, 0), rotation=(0, 0, -0.12 * sx))
        rig.box(part, -0.5, 0, -2, 1, 13, 5, drape_paint)
    rig.held = (None, "frieren:scale_of_obedience")
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
    rig.held = ("minecraft:iron_sword", None)
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
    rig.held = ("frieren:guillotine_staff", None)
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
    rig.held = ("minecraft:iron_sword", None)
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
    rig.held = ("frieren:warriors_axe", None)
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
    rig.held = ("frieren:demon_kings_blade", None)
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
    "hero_statue_exposed": (hexc("#5A3B30"), hexc("#A0705A"), hexc("#CFA28A")),
    # freshly polished: saturated like a vanilla copper block, so a white shirt reads as bright metal, not skin
    "hero_statue_polished": (hexc("#6B2C17"), hexc("#C0592F"), hexc("#F29265")),
}
VERDIGRIS = hexc("#5FA88E")
GLINT = hexc("#FFD9BE")


def copper_cast(stage):
    """Post-paint turning the painted skin into a metal casting: luminance mapped onto the stage's palette."""
    lo, mid, hi = COPPER_STAGES[stage]
    oxidized = stage == "hero_statue"
    exposed = stage == "hero_statue_exposed"
    polished = stage == "hero_statue_polished"

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
        if exposed:
            # the first verdigris specks, like vanilla exposed copper
            rng = np.random.default_rng(11)
            specks = rng.random(out.shape[:2]) < 0.07
            out[specks] = out[specks] * 0.45 + np.array(VERDIGRIS[:3], dtype=float) * 0.55
        if polished:
            # sparse specular glints on the lit surfaces, which is what makes metal read as metal at a distance
            rng = np.random.default_rng(5)
            glints = (rng.random(out.shape[:2]) < 0.025) & (lum > 0.45)
            out[glints] = np.array(GLINT[:3], dtype=float)
        a[..., :3] = np.clip(out, 0, 255).astype("uint8")
    return paint



def himmel_sword(rig):
    """The replica Hero's Sword as a baked prop: only the copper statue uses it (living characters hold items)."""
    gold = hexc("#D8B04A")
    hand_prop_pivot(rig, slim=False)
    steel = hexc("#D9DEE6")
    rig.box("prop", -0.5, 2, -0.5, 1, 4, 1, p_wood(hexc("#3D2A4A")))
    rig.box("prop", -2.5, 1, -0.5, 5, 1, 1, p_metal(gold))
    rig.box("prop", -1, -15, -0.5, 2, 16, 1, p_blade(shade(steel, 1.2), steel))
    rig.box("prop", -0.5, 6, -0.5, 1, 1, 1, p_gem(hexc("#3C7BD6")))


def hero_statue():
    rig = himmel()
    rig.name = "hero_statue"
    rig.held = (None, None)
    himmel_sword(rig)
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
