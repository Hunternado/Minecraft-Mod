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


def soft_head_compat(rig, skin, hair_c, eye, fringe=5, part=None, locks=(2, 9), face_kw=None, side_skin=True, seed=11,
                     back_detail=None, side_rows=11, back_rows=14, shell=True, crown=True):
    """soft_head for rigs written against head_painter/hat_hair_painter: same arguments, softer result. The shell
    painter is left on rig.soft_shell for the "hat" entry of the painters dict (evaluated right after "head")."""
    kw = dict(face_kw or {})
    style = kw.pop("style", None) or ("calm" if kw.pop("sleepy", False) else
                                      ("fierce" if kw.get("brow_angle", 0) > 0 else "soft"))
    face = dict(style=style, eye_y=kw.pop("eye_y", 7) + 1, fringe=fringe, part=part, locks=locks)
    for key in ("eye_w", "gap", "mouth", "brow", "lash", "brow_angle", "blush", "glow", "depth"):
        if key in kw:
            face[key] = kw[key]
    head, shell_paint = soft_head(rig, skin, hair_c, eye, face=face, side_rows=side_rows, back_rows=back_rows,
                                  side_tips=3, back_tips=3, fringe_tips=4, fringe_rows=max(2, min(5, fringe)),
                                  part_col=part, crown=crown, shell=shell, seed=seed, back_detail=back_detail,
                                  side_skin=side_skin)
    rig.soft_shell = shell_paint
    return head


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
def back_loop(rig, color):
    """
    The leather loop a back-carried axe hangs in (CarriedItemLayer BACK): sewn onto the back where the haft crosses
    mid-back (x -0.54 at y 6 for the 15-degree lean), deep enough to wrap the haft, so the weapon reads as strapped
    on rather than floating. Keep in step with tools/models/held.py BACK.
    """
    loop = cloth_painter(color, trim=shade(color, 1.35))
    rig.box("body", -2.05, 5.2, 2.0, 3, 1.6, 1.8, loop)
    rig.box("body", -1.1, 9.8, 2.0, 3.5, 1.2, 1.4, loop)       # a second loop lower down (haft at x +0.64 there)


def belt_frog(rig, color):
    """
    The belt frog a hip-carried sword hangs from (CarriedItemLayer HIP): from the back-left corner of the waist to the
    scabbard's mouth, behind the arm and in front of any cape.
    """
    rig.box("body", 3.4, 8.4, 2.0, 3.5, 1.4, 0.95, cloth_painter(color, trim=shade(color, 0.8)))


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

    def streak_crown(cv, f):
        # a lock, not a stripe: it starts at the hairline and tapers out across the front of the crown
        t = f["top"]
        reach = max(2, t[3] * 2 // 5)
        for yy in range(reach):
            half = 2 if yy < reach // 2 else 1
            for xx in range(t[2] // 2 - half, t[2] // 2 + half):
                cv.px(t[0] + xx, t[1] + t[3] - 1 - yy, streak if half == 1 or abs(xx - t[2] // 2 + 0.5) < 1 else shade(streak, 1.5))
        r = f["front"]
        for xx in range(r[2] // 2 - 1, r[2] // 2 + 1):
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
    back_loop(rig, strap)
    rig.held = ("frieren:warriors_axe", None)
    rig.carry = "back"
    return rig


# ---------------------------------------------------------------------------------------------
def serie():
    """
    Serie, from her character sheet: golden hair falling straight to her knees with a light fringe, long side locks
    and a thin ringed lock in front of each shoulder; long elf ears; calm amber eyes and the faintest smile. A cream
    wrap top crossed at the chest, edged in dusty mauve, pinned with a teal brooch on a cord, ruffled below; an open
    over-robe with wide hanging sleeves banded in mauve and side panels to mid-thigh; a mauve sash; cream shorts;
    bare legs and feet with brown ankle bands.
    """
    skin = hexc("#F8D9C4")
    hair_c = hexc("#F6CB58")
    eye = hexc("#B9813A")
    cream = hexc("#FAF6E8")
    mauve = hexc("#AE8A84")
    mauve_d = shade(mauve, 0.8)
    teal = hexc("#2E9C8E")
    gold = hexc("#C9A048")
    band = hexc("#8A6438")
    rig = Rig("serie", render_scale=0.88)

    # A light, slightly uneven fringe at the brows; side locks to the chest.
    depth = [16, 15, 6, 7, 6, 5, 6, 5, 4, 5, 6, 5, 6, 7, 15, 16]
    head, shell = soft_head(rig, skin, hair_c, eye,
                            face=dict(style="calm", eye_y=8, depth=depth, locks=(2, 13), lash=hexc("#6A4A24"),
                                      mouth="smile", brow=shade(hair_c, 0.78)),
                            side_rows=16, side_tips=0, back_rows=16, back_tips=0, fringe_tips=4, fringe_period=3,
                            fringe_rows=4, seed=91)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, cream, gradient=0.05)
        r = f["front"]
        w = r[2]
        for y in range(10):                                    # the crossed V neckline, edged in mauve
            half = max(0, 4 - (y * 4) // 10)
            for x in range(w // 2 - half, w // 2 + half):
                cv.px(r[0] + x, r[1] + y, skin)
            for k in (1, 2):
                cv.px(r[0] + w // 2 - half - k, r[1] + y, mauve if k == 1 else mauve_d)
                cv.px(r[0] + w // 2 + half - 1 + k, r[1] + y, mauve if k == 1 else mauve_d)
        for k in range(6):                                     # the cord from the brooch up to each side
            cv.px(r[0] + w // 2 - 3 - k // 2, r[1] + 11 - k, gold)
            cv.px(r[0] + w // 2 + 2 + k // 2, r[1] + 11 - k, gold)
        cv.rect(r[0] + w // 2 - 2, r[1] + 10, 4, 3, gold)      # the teal brooch in a gold frame
        cv.rect(r[0] + w // 2 - 1, r[1] + 10, 2, 3, teal)
        cv.px(r[0] + w // 2 - 1, r[1] + 10, shade(teal, 1.5))
        for x in (3, 6, 9, 12):                                # the ruffle below the brooch
            vline(cv, r, x, shade(cream, 0.9), y_from=13, y_to=19)
        for name in ("front", "back", "left", "right"):        # mauve sash and the shorts' waistband
            fill(cv, sub(f[name], 0, 19, f[name][2], 3), mauve)
            hline(cv, f[name], 21, mauve_d)
            fill(cv, sub(f[name], 0, 22, f[name][2], 2), cream)

    def robe(cv, f):
        # the open over-robe on the jacket layer: cream sides edged in mauve, open down the front
        for name, r in f.items():
            if name in ("top", "bottom"):
                continue
            fill(cv, r, cream, gradient=0.06)
        r = f["front"]
        for y in range(r[3]):
            for x in range(4, r[2] - 4):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
            for x, c in ((3, mauve), (2, mauve_d), (r[2] - 4, mauve), (r[2] - 3, mauve_d)):
                cv.px(r[0] + x, r[1] + y, c)

    def robe_panels(cv, f):
        # the robe below the sash: side and back panels to mid-thigh, open in front, banded in mauve
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, shade(cream, 0.75))
                continue
            fill(cv, r, cream, gradient=0.08)
            if name != "top":
                for y, c in ((r[3] - 4, mauve), (r[3] - 3, mauve), (r[3] - 2, mauve_d), (r[3] - 1, mauve)):
                    hline(cv, r, y, c)
        r = f["front"]
        for y in range(r[3]):
            for x in range(4, r[2] - 4):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
            for x in (3, r[2] - 4):
                cv.px(r[0] + x, r[1] + y, mauve)
        for face in ("top", "bottom"):
            b = f[face]
            for x in range(4, b[2] - 4):
                for y in range(b[3]):
                    cv.px(b[0] + x, b[1] + y, (0, 0, 0, 0))

    def wide_sleeve(cv, f):
        # a hanging kimono sleeve, open at the bottom so the hand shows; a broad mauve band at the hem
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, (0, 0, 0, 0))
                continue
            fill(cv, r, cream, gradient=0.08)
            if name != "top":
                for y, c in ((r[3] - 6, mauve_d), (r[3] - 5, mauve), (r[3] - 4, mauve), (r[3] - 3, mauve),
                             (r[3] - 2, mauve_d), (r[3] - 1, mauve)):
                    hline(cv, r, y, c)

    def legs(cv, f):
        for name, r in f.items():
            if name == "top":
                fill(cv, r, cream)
                continue
            if name == "bottom":
                fill(cv, r, shade(skin, 0.86))
                continue
            fill(cv, r, skin, gradient=0.06)
            fill(cv, sub(r, 0, 0, r[2], 5), cream)                  # the shorts
            hline(cv, r, 5, shade(cream, 0.85))
            hline(cv, r, r[3] - 5, band)                            # ankle bands
            hline(cv, r, r[3] - 4, shade(band, 1.2))
            hline(cv, r, r[3] - 1, shade(skin, 0.86))               # toes

    def lock_ringed(cv, f):
        for name, r in f.items():
            hair(cv, r, hair_c, seed=95, highlight_row=1, strands=False)
            if name in ("top", "bottom"):
                continue
            for y in (2, 7):
                if y < r[3]:
                    hline(cv, r, y, band)

    painters = {"head": head, "hat": shell, "body": body, "jacket": robe,
                "right_arm": arm_skin_painter(skin, cream), "left_arm": arm_skin_painter(skin, cream),
                "right_leg": legs, "left_leg": legs}
    humanoid(rig, painters, slim=True)

    # Long elf ears like Frieren's, swept slightly up.
    def ear(cv, f):
        for r in f.values():
            fill(cv, r, skin)
        r = f["front"]
        hline(cv, r, r[3] - 1, shade(skin, 0.84))
    for part, sx in (("ear_right", -1), ("ear_left", 1)):
        rig.pose(part, offset=(4 * sx, -3.0, 0.6), rotation=(0, -0.3 * sx, -0.25 * sx))
        rig.box(part, -2 if sx < 0 else 0, -1, -0.5, 2, 2, 1, ear)
        rig.box(part, -4 if sx < 0 else 2, -1.2, -0.5, 2, 1, 1, ear)
        rig.box(part, -6 if sx < 0 else 4, -1.4, -0.5, 2, 1, 1, ear)

    # Hair to the knees in two links, a little wider than her back; ringed locks in front of the shoulders.
    chain(rig, ("hair_back", "hair_back_2"), ((0, -6.8, 4.1), (0, 13, 0)), ((0.05, 0, 0), (0.03, 0, 0)))
    rig.box("hair_back", -4.5, 0, -0.5, 9, 13, 1, hair_painter(hair_c, seed=96, highlight_row=2))
    rig.box("hair_back_2", -4.5, 0, -0.5, 9, 12, 1, hair_painter(shade(hair_c, 0.96), seed=97, highlight_row=None, tips=5))
    for part, sx in (("lock_right", -1), ("lock_left", 1)):
        chain(rig, (part, part + "_2"), ((3.6 * sx, -4.4, -3.6), (0, 7, 0)), ((-0.06, 0, -0.04 * sx), (0.04, 0, 0.03 * sx)))
        rig.box(part, -0.5, 0, -0.5, 1, 7, 1, lock_ringed)
        rig.box(part + "_2", -0.5, 0, -0.5, 1, 6, 1, hair_painter(hair_c, seed=98, highlight_row=None, tips=2))

    # Wide sleeves hanging from the shoulders to just above the wrist; the robe's panels below the sash.
    for part, x0 in (("right_sleeve", -3.5), ("left_sleeve", -1.5)):
        rig.box(part, x0, -2.4, -3, 5, 10, 6, wide_sleeve)
    rig.pose("skirt", offset=(0, 11.5, 0))
    rig.box("skirt", -4.75, 0, -2.75, 9.5, 5, 5.5, robe_panels)
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
    belt_frog(rig, hexc("#F2F0EA"))
    rig.held = ("frieren:heros_sword", None)
    rig.carry = "hip"
    return rig


# ---------------------------------------------------------------------------------------------
def eisen():
    """
    Eisen, from his character sheet: a dwarf wrapped from shoulders to ground in a rust-red cloak, round steel
    pauldrons on his shoulders, a steel helmet with a nose guard and two pale horns curving up from its sides, tired
    eyes peering out under the rim, and a straight, wheat-blond beard that hides his face and falls to his belly.
    """
    skin = hexc("#EBC3A6")
    beard_c = hexc("#CDB57C")
    steel = hexc("#C9C9C2")
    horn = hexc("#ECE7DA")
    cloak = hexc("#B0472E")
    cloak_d = shade(cloak, 0.78)
    rig = Rig("eisen", render_scale=0.95)

    pal = {"S": skin, "n": shade(skin, 0.85), "L": hexc("#3A2A20"), "W": hexc("#FFFFFF"), "p": hexc("#2A2420"),
           "H": steel, "B": beard_c, "b": shade(beard_c, 0.85)}

    def face_rows():
        grid = [["S"] * 16 for _ in range(16)]
        for y in range(9):                                     # under the helmet (the helmet box covers it)
            for x in range(16):
                grid[y][x] = "H"
        for x0 in (3, 9):                                      # heavy-lidded eyes right under the rim
            for i in range(4):
                grid[9][x0 + i] = "L"
            grid[10][x0], grid[10][x0 + 1], grid[10][x0 + 2], grid[10][x0 + 3] = "S", "W", "p", "S"
        for y in (9, 10, 11):                                  # the big nose between them
            grid[y][7] = grid[y][8] = "n" if y < 11 else "S"
        for y in range(11, 16):                                # beard below (the beard box covers it)
            for x in range(16):
                grid[y][x] = "B" if (x + y) % 3 else "b"
        return ["".join(r) for r in grid]

    def head(cv, f):
        pixel_art(cv, f["front"], face_rows(), pal)
        for side in ("right", "left", "back"):
            r = f[side]
            fill(cv, r, beard_c)
            fill(cv, sub(r, 0, 0, r[2], 9), steel)
        fill(cv, f["top"], steel)
        fill(cv, f["bottom"], shade(beard_c, 0.8))

    def helmet(cv, f):
        for name, r in f.items():
            fill(cv, r, steel if name != "bottom" else shade(steel, 0.7), gradient=0.12)
            if name in ("front", "back", "left", "right"):
                hline(cv, r, r[3] - 1, shade(steel, 0.72))         # the rim
                hline(cv, r, r[3] - 2, shade(steel, 1.12))
            if name in ("front", "back"):
                vline(cv, r, r[2] // 2, shade(steel, 0.8), y_from=0, y_to=r[3] - 2)   # the centre ridge
        b = f["bottom"]
        for x in range(1, b[2] - 1):                               # open underneath where the head goes in
            for y in range(1, b[3] - 1):
                cv.px(b[0] + x, b[1] + y, (0, 0, 0, 0))

    def nose_guard(cv, f):
        for r in f.values():
            fill(cv, r, shade(steel, 0.95))

    def beard(cv, f):
        for name, r in f.items():
            fill(cv, r, beard_c, gradient=0.06)
            if name in ("front", "back"):
                for x in range(0, r[2], 2):                         # long straight strands
                    vline(cv, r, x, shade(beard_c, 0.88), y_from=2)
                for x in (0, r[2] - 1):                             # rounded bottom corners
                    cv.px(r[0] + x, r[1] + r[3] - 1, (0, 0, 0, 0))
        r = f["front"]
        for x in range(r[2]):                                       # the moustache sweeping out from under the nose
            cv.px(r[0] + x, r[1] + 1, shade(beard_c, 0.8) if abs(x - r[2] / 2 + 0.5) > 1 else beard_c)
        cv.rect(r[0] + r[2] // 2 - 1, r[1], 2, 1, shade(skin, 0.85))   # tip of the nose above it

    def pauldron(cv, f):
        for name, r in f.items():
            fill(cv, r, steel if name != "bottom" else shade(steel, 0.65), gradient=0.18)
            if name not in ("top", "bottom"):
                hline(cv, r, r[3] - 1, shade(steel, 0.7))

    def cloak_upper(cv, f):
        for name, r in f.items():
            if name in ("top", "bottom"):
                fill(cv, r, cloak_d)
                continue
            fill(cv, r, cloak, gradient=0.08)
            for x in range(2, r[2], 5):                             # folds
                vline(cv, r, x, cloak_d, y_from=1)

    def cloak_lower(cv, f):
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, shade(cloak, 0.6))
                continue
            fill(cv, r, cloak, gradient=0.1)
            if name != "top":
                for x in range(1, r[2], 4):
                    vline(cv, r, x, cloak_d)
                hline(cv, r, r[3] - 1, shade(cloak, 0.66))

    painters = {"head": head, "body": cloak_upper,
                "right_arm": p_fill(cloak), "left_arm": p_fill(cloak),
                "right_leg": p_fill(cloak_d), "left_leg": p_fill(cloak_d)}
    humanoid(rig, painters, slim=False, legs=8, body_h=10, arm_len=10)

    # Helmet over the top of the head down to the brow, a nose guard, and horns curving out and up from its sides.
    rig.box("hair_top", -4.6, -8.8, -4.6, 9.2, 4.9, 9.2, helmet)
    rig.box("hair_top", -3.6, -9.5, -3.6, 7.2, 0.8, 7.2, helmet)
    rig.box("bangs", -0.5, -4.2, -4.95, 1, 2.2, 0.5, nose_guard)
    for sx in (-1, 1):
        horn_chain(rig, sx, (4.3, -6.8, 0.0),
                   [(None, (0.0, 0.0, 1.15), (2, 2, 2)), ((0, -1.8, 0), (0.0, 0.0, -0.8), (2, 2, 2)),
                    ((0, -1.8, 0), (0.0, 0.0, -0.55), (1, 2, 1))], horn, ribs=False)

    # The beard hangs from under the eyes over the chest to the belly; the moustache sits on top of it.
    rig.box("crown", -4.0, -2.7, -5.2, 8, 12.5, 1.2, beard)

    # Round pauldrons on the shoulders, and the cloak from shoulders to ground, wide enough to swallow the arms.
    rig.box("collar", -8.6, -1.6, -2.8, 4.4, 3.2, 5.6, pauldron)
    rig.box("collar", 4.2, -1.6, -2.8, 4.4, 3.2, 5.6, pauldron)
    rig.pose("skirt", offset=(0, 0, 0))
    rig.box("skirt", -7.6, 0.4, -3.4, 15.2, 9, 6.8, cloak_upper)
    rig.box("skirt", -8.6, 9.4, -3.9, 17.2, 8.6, 7.8, cloak_lower)
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
        "head": soft_head_compat(rig, skin, hair_c, eye, fringe=3, part=6, locks=(1, 6),
                             face_kw=dict(eye_y=7, gap=4, mouth="stern", brow=shade(hair_c, 0.6), lash=hexc("#2A2A30")), side_rows=10, back_rows=10),
        "hat": rig.soft_shell,
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
        "head": soft_head_compat(rig, skin, hair_c, eye, fringe=3, part=None, locks=(1, 4),
                             face_kw=dict(eye_y=7, gap=4, mouth="smile"), shell=False, crown=False),
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
        "head": soft_head_compat(rig, skin, hair_c, eye, fringe=4, part=9, locks=(1, 5),
                             face_kw=dict(eye_y=7, gap=4, mouth="smile", brow=shade(hair_c, 0.7)), side_rows=8, back_rows=9),
        "hat": rig.soft_shell,
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
    """
    Qual the Corrupt, from his character sheet: a dark horned mask for a face, slit eyes and a wide grin of white
    teeth, two pale horns curling out from its brow; long pale-blond hair down his back and over his chest; a white
    fur mantle on bare grey-brown shoulders; a scarred, stitched torso; dark bands on the upper arms, fur cuffs at
    the wrists, clawed hands with red nails; a dark sash, a chain and a belt of fangs over wide, floor-length dark
    hakama with a chain hanging at the hip.
    """
    skin = hexc("#6E6158")
    skin_d = shade(skin, 0.8)
    mask = hexc("#66564C")
    hair_c = hexc("#EFE6C4")
    horn = hexc("#D9CBAA")
    fur = hexc("#F1ECDF")
    cloth = hexc("#4A4A50")
    cloth_d = shade(cloth, 0.78)
    sash = hexc("#3A3A40")
    chain_c = hexc("#8E8E96")
    tooth = hexc("#EDE6D2")
    nail = hexc("#B0303A")
    rig = Rig("qual", render_scale=1.1)

    def head(cv, f):
        r = f["front"]
        fill(cv, r, mask, gradient=0.12)
        for x in range(16):                                    # pale hair above the mask
            for y in range(0, 2 if 4 <= x <= 11 else 3):
                cv.px(r[0] + x, r[1] + y, hair_c)
        for x0, sgn in ((2, 1), (13, -1)):                     # heavy brow ridges sloping down to the slit eyes
            for i in range(4):
                cv.px(r[0] + x0 + sgn * i, r[1] + 5 + i // 2, shade(mask, 0.7))
            for i in range(3):
                cv.px(r[0] + x0 + sgn * (i + 1), r[1] + 7, (24, 18, 20, 255))
            cv.px(r[0] + x0 + sgn * 2, r[1] + 7, hexc("#E8C860"))   # a glint in each slit
        for x in (7, 8):                                       # nostrils
            cv.px(r[0] + x, r[1] + 9, shade(mask, 0.6))
        for x in range(2, 14):                                 # the grin: two rows of teeth
            top = (x % 2 == 0)
            cv.px(r[0] + x, r[1] + 11, (24, 18, 20, 255))
            cv.px(r[0] + x, r[1] + 12, tooth if top else shade(tooth, 0.85))
            cv.px(r[0] + x, r[1] + 13, shade(tooth, 0.85) if top else tooth)
            cv.px(r[0] + x, r[1] + 14, (24, 18, 20, 255))
        for x in (1, 14):
            cv.px(r[0] + x, r[1] + 12, (24, 18, 20, 255))
        for side in ("right", "left", "back"):
            hair(cv, f[side], hair_c, seed=201, highlight_row=3)
        hair(cv, f["top"], hair_c, seed=202, highlight_row=None)
        fill(cv, f["bottom"], skin_d)

    def shell(cv, f):
        hair(cv, f["back"], shade(hair_c, 1.02), seed=203, highlight_row=3)
        zigzag(cv, f["back"], 3, seed=203)
        for side in ("right", "left"):
            r = sub(f[side], 0, 0, f[side][2], 13)
            hair(cv, r, shade(hair_c, 1.02), seed=204, highlight_row=3)
        hair(cv, sub(f["front"], 0, 0, f["front"][2], 2), shade(hair_c, 1.04), seed=205, highlight_row=None)

    def torso(cv, f):
        for name, r in f.items():
            fill(cv, r, skin, gradient=0.1)
        r = f["front"]
        hline(cv, r, 7, skin_d, x_from=2, x_to=7)               # chest
        hline(cv, r, 7, skin_d, x_from=9, x_to=14)
        vline(cv, r, 8, skin_d, y_from=8, y_to=17)
        for y in (11, 14):                                       # abdomen
            hline(cv, r, y, skin_d, x_from=4, x_to=12)
        for y in range(3, 16):                                   # a stitched scar down his left side
            cv.px(r[0] + 11, r[1] + y, hexc("#2E2620"))
            if y % 3 == 0:
                cv.px(r[0] + 10, r[1] + y, hexc("#2E2620"))
                cv.px(r[0] + 12, r[1] + y, hexc("#2E2620"))
        for x in range(3, 8):                                     # and across his right ribs
            cv.px(r[0] + x, r[1] + 13, hexc("#2E2620"))
            if x % 2:
                cv.px(r[0] + x, r[1] + 12, hexc("#2E2620"))
        for name in ("front", "back", "left", "right"):           # the sash knotted at the waist
            fill(cv, sub(f[name], 0, 18, f[name][2], 6), sash, gradient=0.1)
            for x in range(0, f[name][2], 3):
                cv.px(f[name][0] + x, f[name][1] + 20, shade(sash, 1.25))

    def arm(cv, f):
        for name, r in f.items():
            fill(cv, r, skin, gradient=0.08)
            if name in ("top", "bottom"):
                if name == "bottom":
                    fill(cv, r, skin_d)
                continue
            for y in (5, 7):                                      # dark bands around the upper arm
                hline(cv, r, y, hexc("#2A2422"))
            hline(cv, r, 9, hexc("#B49456"))                      # a braided band
            fill(cv, sub(r, 0, 15, r[2], 3), fur)                 # fur cuffs
            fill(cv, sub(r, 0, 18, r[2], 6), skin_d)              # clawed hands
            for x in range(0, r[2], 2):
                cv.px(r[0] + x, r[1] + r[3] - 1, nail)

    def mantle(cv, f):
        for name, r in f.items():
            fill(cv, r, fur if name != "bottom" else shade(fur, 0.82), gradient=0.08)
            if name not in ("top", "bottom"):
                zigzag(cv, r, 2, seed=207, period=3)
                for x in range(1, r[2], 3):
                    vline(cv, r, x, shade(fur, 0.9), y_from=0, y_to=r[3] - 3)
        r = f["front"]
        for y in range(r[3]):                                     # open at the throat, wider toward the chest
            half = 3 + y // 2
            for x in range(r[2] // 2 - half, r[2] // 2 + half):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
        for face in ("top", "bottom"):
            b = f[face]
            for x in range(b[2] // 2 - 4, b[2] // 2 + 4):
                for y in range(b[3] // 2 + 1):
                    cv.px(b[0] + x, b[1] + y, (0, 0, 0, 0))

    def hakama(cv, f):
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, cloth_d)
                continue
            fill(cv, r, cloth, gradient=0.08)
            if name != "top":
                for x in range(2, r[2], 4):
                    vline(cv, r, x, cloth_d, y_from=2)

    def hakama_top(cv, f):
        hakama(cv, f)
        for name in ("front", "left", "right", "back"):
            r = f[name]
            for x in range(0, r[2], 3):                            # chain belt
                cv.px(r[0] + x, r[1] + 0, chain_c)
                cv.px(r[0] + x + 1, r[1] + 1, chain_c)
        r = f["front"]
        for x in range(1, r[2] - 1, 2):                            # the belt of fangs across the hips
            cv.px(r[0] + x, r[1] + 3, tooth)
            cv.px(r[0] + x, r[1] + 4, tooth)
            cv.px(r[0] + x, r[1] + 5, shade(tooth, 0.85))
        rr = f["left"]
        for y in range(2, rr[3]):                                  # a chain hanging at his hip
            cv.px(rr[0] + rr[2] // 2, rr[1] + y, chain_c if y % 2 else shade(chain_c, 0.7))

    painters = {"head": head, "hat": shell, "body": torso, "right_arm": arm, "left_arm": arm,
                "right_leg": p_fill(cloth_d), "left_leg": p_fill(cloth_d)}
    humanoid(rig, painters, slim=False)

    # Horns curling out and up from the mask's brow.
    for sx in (-1, 1):
        horn_chain(rig, sx, (3.2, -6.4, -3.0),
                   [(None, (0.2, 0.0, 1.05), (2, 2, 2)), ((0, -1.8, 0), (0.0, 0.0, -0.95), (2, 2, 2)),
                    ((0, -1.8, 0), (0.0, 0.0, -0.75), (1, 2, 1))], horn, ribs=True)

    # Long pale hair down his back and in two heavy locks over his chest.
    chain(rig, ("hair_back", "hair_back_2"), ((0, -7.0, 4.1), (0, 11, 0)), ((0.06, 0, 0), (0.04, 0, 0)))
    rig.box("hair_back", -4.5, 0, -0.5, 9, 11, 1, hair_painter(hair_c, seed=208, highlight_row=2))
    rig.box("hair_back_2", -4.0, 0, -0.5, 8, 9, 1, hair_painter(shade(hair_c, 0.96), seed=209, highlight_row=None, tips=4))
    for part, sx in (("lock_right", -1), ("lock_left", 1)):
        chain(rig, (part, part + "_2"), ((4.6 * sx, -3.0, -2.6), (0, 7, 0)), ((-0.1, 0, -0.04 * sx), (0.08, 0, 0.03 * sx)))
        rig.box(part, -1.5, 0, -0.5, 3, 7, 1, hair_painter(hair_c, seed=210, highlight_row=1))
        rig.box(part + "_2", -1.25, 0, -0.5, 2.5, 6, 1, hair_painter(shade(hair_c, 0.95), seed=211, highlight_row=None, tips=3))

    # The fur mantle, and wide hakama from the sash to the ground.
    rig.box("collar", -7.5, -1.2, -3.2, 15, 4.5, 6.4, mantle)
    rig.pose("skirt", offset=(0, 11, 0))
    rig.box("skirt", -5.2, 0, -3.0, 10.4, 6, 6.0, hakama_top)
    rig.box("skirt", -6.0, 6, -3.5, 12.0, 7, 7.0, hakama)
    return rig


def demon_soldier():
    skin = hexc("#D8C2B4")
    hair_c = hexc("#3A2C34")
    eye = hexc("#D63A3A")
    plate = hexc("#4A4650")
    red = hexc("#7A1E26")
    rig = Rig("demon_soldier", render_scale=1.0)
    painters = {
        "head": soft_head_compat(rig, skin, hair_c, eye, fringe=4, part=None, locks=(1, 6),
                             face_kw=dict(eye_y=7, gap=4, mouth="fangs", brow_angle=1, glow=True), side_rows=9, back_rows=10),
        "hat": rig.soft_shell,
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
        "head": soft_head_compat(rig, skin, hair_c, eye, fringe=5, part=6, locks=(2, 10),
                             face_kw=dict(eye_y=7, gap=4, mouth="smirk", glow=True), side_rows=12, back_rows=14),
        "hat": rig.soft_shell,
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
    """
    Linie, from her character sheet: peach-pink hair with a soft fringe, shoulder-length twin tails tied with dark
    brown ribbons and flicking outward, small pale horns, violet eyes and no expression at all; a lilac-white blouse
    with puffed shoulders, long bell cuffs and a frilled collar with a brown ribbon; a dark brown dress with a laced
    red-brown corset over a front split showing white ruffled tiers; banded stockings and cross-laced brown boots.
    She carries a great axe on her back, the way Eisen did.
    """
    skin = hexc("#FCE7DA")
    hair_c = hexc("#F3A898")
    eye = hexc("#6A64B4")
    blouse = hexc("#ECE8F3")
    blouse_d = shade(blouse, 0.86)
    dress = hexc("#6E4A3C")
    dress_d = shade(dress, 0.8)
    corset = hexc("#8C3E30")
    ribbon = hexc("#4A2E22")
    frill = hexc("#F7F4EF")
    stocking = hexc("#B99A92")
    boot = hexc("#5A3A2A")
    horn = hexc("#DDCB98")
    rig = Rig("linie", render_scale=0.92)

    head, shell = soft_head(rig, skin, hair_c, eye,
                            face=dict(style="soft", eye_y=8, fringe=6, part=None, locks=(2, 6), lash=hexc("#3A2A3A"),
                                      mouth="small", brow=shade(hair_c, 0.72)),
                            side_rows=12, side_tips=3, back_rows=12, back_tips=3, fringe_tips=4, fringe_period=3,
                            fringe_rows=4, seed=131)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, dress, gradient=0.06)
        r = f["front"]
        w = r[2]
        for y in range(7):                                     # blouse above the sweetheart neckline
            for x in range(w):
                dip = 1 if abs(x - w / 2 + 0.5) < 1.5 else 0
                if y < 6 + dip:
                    cv.px(r[0] + x, r[1] + y, blouse)
        fill(cv, sub(r, w // 2 - 3, 0, 6, 2), frill)          # frilled collar
        hline(cv, r, 2, blouse_d, x_from=w // 2 - 3, x_to=w // 2 + 3)
        cv.rect(r[0] + w // 2 - 2, r[1] + 2, 4, 2, ribbon)     # the ribbon bow and its tails
        cv.px(r[0] + w // 2 - 1, r[1] + 4, ribbon)
        cv.px(r[0] + w // 2, r[1] + 4, ribbon)
        cv.px(r[0] + w // 2 - 2, r[1] + 5, ribbon)
        cv.px(r[0] + w // 2 + 1, r[1] + 5, ribbon)
        for name in ("front", "back", "left", "right"):        # the laced corset at the waist
            fill(cv, sub(f[name], 0, 13, f[name][2], 9), corset, gradient=0.08)
        for y in range(14, 21, 2):                              # lacing
            cv.px(r[0] + w // 2 - 1, r[1] + y, frill)
            cv.px(r[0] + w // 2, r[1] + y + 1, frill)
            cv.px(r[0] + w // 2, r[1] + y, shade(corset, 0.7))
            cv.px(r[0] + w // 2 - 1, r[1] + y + 1, shade(corset, 0.7))
        fill(cv, sub(r, 0, 22, w, 2), dress)

    def sleeve(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            fill(cv, r, blouse, gradient=0.06)
            if name != "top":
                for x in range(0, r[2], 2):                     # gathered folds
                    vline(cv, r, x, blouse_d, y_from=0, y_to=r[3] - 7)
                for yy in range(r[3] - 6, r[3]):
                    for xx in range(r[2]):
                        cv.px(r[0] + xx, r[1] + yy, (0, 0, 0, 0))

    def puff(cv, f):
        for name, r in f.items():
            fill(cv, r, blouse if name != "bottom" else blouse_d, gradient=0.12)
            if name not in ("top", "bottom"):
                hline(cv, r, r[3] - 1, blouse_d)

    def bell(cv, f):
        # the long bell cuff, flaring around the wrist and open at the bottom so the hand shows
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, (0, 0, 0, 0))
                continue
            fill(cv, r, blouse, gradient=0.1)
            if name != "top":
                for x in range(1, r[2], 3):
                    vline(cv, r, x, blouse_d, y_from=1)
                hline(cv, r, r[3] - 1, frill)

    def skirt(cv, f):
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, dress_d)
                continue
            fill(cv, r, dress, gradient=0.08)
            if name != "top":
                for x in range(2, r[2], 4):
                    vline(cv, r, x, dress_d, y_from=2)
        r = f["front"]
        for y in range(r[3]):                                    # the split widening toward the hem, white tiers inside
            half = 2 + (y * 3) // r[3]
            for x in range(r[2] // 2 - half, r[2] // 2 + half):
                cv.px(r[0] + x, r[1] + y, frill if (y % 4) else shade(frill, 0.85))

    def legs(cv, f):
        for name, r in f.items():
            if name == "top":
                fill(cv, r, stocking)
                continue
            if name == "bottom":
                fill(cv, r, shade(boot, 0.6))
                continue
            fill(cv, r, stocking, gradient=0.05)
            for y in range(8, r[3] - 3):                         # straps crossing up the shin
                x = (y * 2) % r[2]
                cv.px(r[0] + x, r[1] + y, boot)
                cv.px(r[0] + (r[2] - 1 - x), r[1] + y, boot)
            fill(cv, sub(r, 0, r[3] - 3, r[2], 3), boot)
            hline(cv, r, r[3] - 1, shade(boot, 0.6))

    painters = {"head": head, "hat": shell, "body": body,
                "right_arm": arm_skin_painter(skin, blouse), "left_arm": arm_skin_painter(skin, blouse),
                "right_sleeve": sleeve, "left_sleeve": sleeve, "right_leg": legs, "left_leg": legs}
    humanoid(rig, painters, slim=True)

    # Puffed shoulders and bell cuffs on both arms (slim arms are 3 wide).
    for part, x0 in (("right_sleeve", -2.75), ("left_sleeve", -1.25)):
        rig.box(part, x0, -2.6, -2.5, 4, 4, 5, puff)
        rig.box(part, x0 - 0.25, 5.0, -2.75, 4.5, 4.4, 5.5, bell)

    # Twin tails tied high at the sides, flicking outward at shoulder height; dark ribbons at the ties.
    tie = cloth_painter(ribbon)
    for side, sx in (("tail_right", -1), ("tail_left", 1)):
        chain(rig, (side, side + "_2"), ((4.6 * sx, -6.2, 0.2), (0, 5, 0)), ((0.06, 0, -0.32 * sx), (0.0, 0, -0.55 * sx)))
        rig.box(side, -1.5, -0.5, -1.5, 3, 5.5, 3, hair_painter(hair_c, seed=132, highlight_row=2, soft=True))
        rig.box(side + "_2", -1.25, 0, -1.25, 2.5, 3.5, 2.5,
                hair_painter(shade(hair_c, 0.95), seed=133, highlight_row=None, tips=3, bottom=False, soft=True))
        rig.box(side, -1.25, -1.6, -1.6, 2.5, 1.2, 3.2, tie)
        rig.box(side, (-2.6 if sx < 0 else 0.6), -2.4, -0.4, 2.0, 1.4, 0.8, tie)      # ribbon loop

    # Small pale horns rising from the crown and curving back.
    for sx in (-1, 1):
        horn_chain(rig, sx, (2.4, -8.2, 0.4),
                   [(None, (-0.25, 0.0, 0.18), (1, 2, 1)), ((0, -1.9, 0), (-0.35, 0.0, 0.0), (1, 2, 1))],
                   horn, ribs=False)

    # The dress skirt to below the knee, split in front over the ruffles.
    rig.pose("skirt", offset=(0, 11, 0))
    rig.box("skirt", -4.75, 0, -2.75, 9.5, 5, 5.5, skirt)
    rig.box("skirt", -5.25, 5, -3.25, 10.5, 4, 6.5, skirt)
    back_loop(rig, ribbon)
    rig.held = ("frieren:warriors_axe", None)
    rig.carry = "back"
    return rig


def draht():
    """
    Draht, from his character sheet: ash-blond hair with a long side-swept fringe hiding his right eye, ram horns
    curling at the crown, a calm dark-blue eye; a white ruffled shirt with puffed sleeves and wide cuffs, a striped
    navy ribbon tie with long tails, brown braces with steel clips, slim dark-brown trousers, navy socks and brown
    lace-up shoes. His weapon is wire, carried on two spools.
    """
    skin = hexc("#F9E4D6")
    hair_c = hexc("#B9AC7E")
    eye = hexc("#3E4268")
    shirt = hexc("#EDEBF2")
    shirt_d = shade(shirt, 0.86)
    navy = hexc("#3A3E62")
    brace = hexc("#7A5132")
    steel = hexc("#A8ACB6")
    trousers = hexc("#4A3E36")
    sock = hexc("#4A4E72")
    shoe = hexc("#8C5A30")
    horn = hexc("#9E9890")
    rig = Rig("draht", render_scale=1.0)

    # The fringe sweeps across from his left to hide his right eye (the viewer's left): long over columns 1-7.
    depth = [16, 13, 12, 12, 12, 11, 11, 10, 6, 4, 4, 3, 3, 4, 9, 12]
    face_head, shell = soft_head(rig, skin, hair_c, eye,
                                 face=dict(style="sharp", eye_y=8, depth=depth, locks=(1, 9), lash=hexc("#2A2A3A"),
                                           mouth="small", brow=shade(hair_c, 0.72)),
                                 side_rows=12, side_tips=3, back_rows=12, back_tips=3, fringe_rows=0, seed=141)

    def head(cv, f):
        face_head(cv, f)
        r = f["front"]
        for x in range(0, 8):                                  # the hidden eye stays hidden between the fringe's tips
            for y in range(6, 12):
                cv.px(r[0] + x, r[1] + y, shade(hair_c, 0.9 if (x + y) % 3 else 0.8))

    def fringe(cv, f):
        for name, r in f.items():
            hair(cv, r, shade(hair_c, 1.05), seed=142, highlight_row=1 if name == "front" else None,
                 strands=name == "front")
        zigzag(cv, f["front"], 2, seed=143, period=3)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, shirt, gradient=0.05)
        r = f["front"]
        w = r[2]
        fill(cv, sub(r, w // 2 - 3, 0, 6, 2), shirt)           # collar
        hline(cv, r, 2, shirt_d, x_from=w // 2 - 3, x_to=w // 2 + 3)
        for y in range(4, 15):                                  # the ruffled jabot down the front
            for x in (w // 2 - 2, w // 2 + 1):
                cv.px(r[0] + x, r[1] + y, shirt_d if y % 2 else shirt)
        cv.rect(r[0] + w // 2 - 2, r[1] + 2, 4, 2, navy)        # the ribbon tie, striped, with long tails
        for k in range(6):
            cv.px(r[0] + w // 2 - 2 - k // 3, r[1] + 4 + k, navy if k % 2 else shade(navy, 1.4))
            cv.px(r[0] + w // 2 + 1 + k // 3, r[1] + 4 + k, navy if k % 2 else shade(navy, 1.4))
        for name, cols in (("front", (2, w - 3)), ("back", (3, w - 4))):     # braces
            rr = f[name]
            for x in cols:
                vline(cv, rr, x, brace, y_from=0, y_to=21)
                vline(cv, rr, x + 1, brace, y_from=0, y_to=21)
        for x in (2, w - 3):                                     # steel clips at the waistband
            cv.rect(r[0] + x, r[1] + 19, 2, 2, steel)
        for name in ("front", "back", "left", "right"):          # trousers' waistband
            fill(cv, sub(f[name], 0, 21, f[name][2], 3), trousers)
        cv.rect(r[0] + w // 2 - 1, r[1] + 21, 2, 2, steel)        # button

    def sleeve(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            fill(cv, r, shirt, gradient=0.06)
            if name != "top":
                for x in range(0, r[2], 2):
                    vline(cv, r, x, shirt_d, y_from=2, y_to=r[3] - 10)
                fill(cv, sub(r, 0, r[3] - 10, r[2], 4), shirt)    # wide cuff
                hline(cv, r, r[3] - 10, shirt_d)
                hline(cv, r, r[3] - 7, shirt_d)
                for yy in range(r[3] - 6, r[3]):
                    for xx in range(r[2]):
                        cv.px(r[0] + xx, r[1] + yy, (0, 0, 0, 0))

    def legs(cv, f):
        for name, r in f.items():
            if name == "top":
                fill(cv, r, trousers)
                continue
            if name == "bottom":
                fill(cv, r, shade(shoe, 0.6))
                continue
            fill(cv, r, trousers, gradient=0.06)
            fill(cv, sub(r, 0, r[3] - 6, r[2], 3), sock)
            fill(cv, sub(r, 0, r[3] - 3, r[2], 3), shoe)
            hline(cv, r, r[3] - 1, shade(shoe, 0.6))

    def spool(cv, f):
        for name, r in f.items():
            fill(cv, r, shade(steel, 0.9))
            if name in ("front", "back", "left", "right"):
                for y in range(1, r[3] - 1):
                    hline(cv, r, y, hexc("#E6E6EE") if y % 2 else hexc("#C8C8D2"))

    painters = {"head": head, "hat": shell, "body": body,
                "right_arm": arm_skin_painter(skin, shirt), "left_arm": arm_skin_painter(skin, shirt),
                "right_sleeve": sleeve, "left_sleeve": sleeve, "right_leg": legs, "left_leg": legs}
    humanoid(rig, painters, slim=False)

    # The long side-swept fringe plate, over his right eye only.
    rig.box("bangs", -4.6, -8.3, -4.75, 5.6, 6.6, 1, fringe)
    rig.box("bangs", 1.0, -8.3, -4.75, 3.6, 3.2, 1, fringe)

    # Ram horns curling out of the crown: up, back, then down and forward around a tight spiral.
    for sx in (-1, 1):
        horn_chain(rig, sx, (2.9, -7.8, 0.4),
                   [(None, (-0.2, 0.0, 0.95), (2, 3, 2)), ((0, -2.7, 0), (-1.1, 0.0, 0.55), (2, 2, 2)),
                    ((0, -1.8, 0), (-1.4, 0.0, 0.35), (1, 2, 1))], horn, ribs=True)

    # Wire spools in both hands.
    hand_prop_pivot(rig, slim=False)
    rig.box("prop", -1.5, -1, -1.5, 3, 3, 3, spool)
    rig.pose("prop_left", offset=(1.0, 9, 0))
    rig.box("prop_left", -1.5, -1, -1.5, 3, 3, 3, spool)
    return rig


def heiter():
    """
    Heiter the priest, from his character sheet: sage-green hair combed back with a few loose curls at the temples,
    thin glasses, a mild smile; a black priest's robe to the ankles, open down the front and edged in gold, a broad
    gold-edged collar over the shoulders, a high black collar with gold bands over a white neckcloth with a gold
    pendant strip, double gold bands at the cuffs, a woven tan belt, dark trousers and black boots.
    """
    skin = hexc("#F6DBC6")
    hair_c = hexc("#7FA46A")
    eye = hexc("#6C7A5A")
    robe = hexc("#2E2E31")
    robe_d = shade(robe, 0.8)
    gold = hexc("#CDA95F")
    white = hexc("#F2F0EA")
    belt = hexc("#B88A52")
    rim = hexc("#8C8476")
    rig = Rig("heiter", render_scale=1.0)

    # Combed back: a high hairline with a slight widow's peak, short sides.
    depth = [7, 6, 4, 3, 3, 2, 2, 3, 3, 2, 2, 3, 3, 4, 6, 7]
    base_head, shell = soft_head(rig, skin, hair_c, eye,
                                 face=dict(style="sharp", eye_y=9, depth=depth, locks=(1, 3), lash=hexc("#3A3A2E"),
                                           mouth="smile", brow=shade(hair_c, 0.72)),
                                 side_rows=7, side_tips=2, back_rows=11, back_tips=2, fringe_rows=0, seed=121)

    def head(cv, f):
        base_head(cv, f)
        r = f["front"]
        for x0, x1 in ((1, 6), (9, 14)):                       # thin rims around the eyes, the lower edge fainter
            for xx in range(x0 + 1, x1):
                cv.px(r[0] + xx, r[1] + 8, rim)
                cv.px(r[0] + xx, r[1] + 12, mix(rim, skin, 0.5))
            for yy in range(9, 12):
                cv.px(r[0] + x0, r[1] + yy, rim)
                cv.px(r[0] + x1, r[1] + yy, rim)
        cv.px(r[0] + 7, r[1] + 9, rim)
        cv.px(r[0] + 8, r[1] + 9, rim)
        for x, y in ((2, 3), (13, 3), (1, 4), (14, 4)):          # loose curls at the temples
            cv.px(r[0] + x, r[1] + y, shade(hair_c, 0.9))

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, robe, gradient=0.06)
        r = f["front"]
        w = r[2]
        fill(cv, sub(r, w // 2 - 3, 0, 6, 3), robe)            # high collar with gold bands
        hline(cv, r, 0, gold, x_from=w // 2 - 3, x_to=w // 2 + 3)
        hline(cv, r, 2, gold, x_from=w // 2 - 3, x_to=w // 2 + 3)
        for y in range(3, 9):                                   # white neckcloth in a V, gold strip down it
            half = max(1, 4 - (y - 3) // 2)
            for x in range(w // 2 - half, w // 2 + half):
                cv.px(r[0] + x, r[1] + y, white)
            cv.px(r[0] + w // 2 - half - 1, r[1] + y, gold)
            cv.px(r[0] + w // 2 + half, r[1] + y, gold)
        vline(cv, r, w // 2 - 1, gold, y_from=3, y_to=10)
        vline(cv, r, w // 2, gold, y_from=3, y_to=10)
        for y in range(9, r[3]):                                 # the robe's front edges, gold, with the opening
            vline(cv, r, w // 2 - 3, gold, y_from=y, y_to=y + 1)
            vline(cv, r, w // 2 + 2, gold, y_from=y, y_to=y + 1)
            for x in range(w // 2 - 2, w // 2 + 2):
                cv.px(r[0] + x, r[1] + y, robe_d)
        for k in range(3):                                       # the gold cord-loop clasp at the chest
            cv.px(r[0] + w // 2 - 3 + k * 3, r[1] + 10, gold)
        for name in ("front", "back", "left", "right"):          # woven tan belt
            fill(cv, sub(f[name], 0, 19, f[name][2], 3), belt)
            for x in range(0, f[name][2], 2):
                cv.px(f[name][0] + x, f[name][1] + 20, shade(belt, 0.8))
        for y in range(19, 22):                                   # the robe edges cross over the belt
            cv.px(r[0] + w // 2 - 3, r[1] + y, gold)
            cv.px(r[0] + w // 2 + 2, r[1] + y, gold)

    def collar(cv, f):
        # broad collar over the shoulders, gold along its edge, open at the front
        for name, r in f.items():
            fill(cv, r, robe if name != "bottom" else robe_d, gradient=0.06)
            if name not in ("top", "bottom"):
                hline(cv, r, r[3] - 1, gold)
        r = f["front"]
        for y in range(r[3]):
            for x in range(r[2] // 2 - 4, r[2] // 2 + 4):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
            cv.px(r[0] + r[2] // 2 - 5, r[1] + y, gold)
            cv.px(r[0] + r[2] // 2 + 4, r[1] + y, gold)
        for face in ("top", "bottom"):
            b = f[face]
            for x in range(b[2] // 2 - 4, b[2] // 2 + 4):
                for y in range(b[3] // 2 + 1):
                    cv.px(b[0] + x, b[1] + y, (0, 0, 0, 0))

    def sleeve(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            fill(cv, r, robe, gradient=0.06)
            if name != "top":
                for y in (r[3] - 9, r[3] - 7):                    # double gold bands at the cuff
                    hline(cv, r, y, gold)
                for x in range(0, r[2], 2):
                    cv.px(r[0] + x, r[1] + r[3] - 8, gold)
                for yy in range(r[3] - 6, r[3]):
                    for xx in range(r[2]):
                        cv.px(r[0] + xx, r[1] + yy, (0, 0, 0, 0))

    def robe_skirt(cv, f):
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, robe_d)
                continue
            fill(cv, r, robe, gradient=0.08)
            if name != "top":
                hline(cv, r, r[3] - 1, gold)
                for x in range(3, r[2], 5):
                    vline(cv, r, x, robe_d, y_from=2, y_to=r[3] - 1)
        r = f["front"]
        for y in range(r[3]):                                     # open down the front over the trousers
            for x in range(r[2] // 2 - 2, r[2] // 2 + 2):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
            cv.px(r[0] + r[2] // 2 - 3, r[1] + y, gold)
            cv.px(r[0] + r[2] // 2 + 2, r[1] + y, gold)

    painters = {"head": head, "hat": shell, "body": body,
                "right_arm": arm_skin_painter(skin, robe), "left_arm": arm_skin_painter(skin, robe),
                "right_sleeve": sleeve, "left_sleeve": sleeve,
                "right_leg": legs_painter(robe_d, hexc("#18181C"), boot_top=15, cuff=hexc("#26262C")),
                "left_leg": legs_painter(robe_d, hexc("#18181C"), boot_top=15, cuff=hexc("#26262C"))}
    humanoid(rig, painters, slim=False)
    rig.box("collar", -6.0, -0.6, -2.8, 12, 3, 5.6, collar)
    rig.pose("skirt", offset=(0, 11, 0))
    rig.box("skirt", -4.6, 0, -2.6, 9.2, 10.5, 5.2, robe_skirt)
    return rig


def lugner():
    """
    Lügner, as he came to Graf Granat's domain posing as an envoy of peace: shoulder-length ash-blond hair parted in
    the middle with two long strands framing his face, medium horns rising from the crown, bent at the base and near
    the tip, pointing up and forward; a long ruffled white cravat pinned with a gem over a dark doublet; a long dark
    coat with gold trim; grey trousers and white knee boots.
    """
    skin = hexc("#F5E1D5")
    hair_c = hexc("#C9C0A2")
    eye = hexc("#B53242")
    doublet = hexc("#2C2A38")
    coat = hexc("#1F1F2A")
    coat_d = shade(coat, 0.8)
    gold = hexc("#C9A352")
    white = hexc("#F4F2EE")
    white_d = shade(white, 0.86)
    gem = hexc("#B0182C")
    trousers = hexc("#8E8E96")
    boot = hexc("#ECE9E3")
    horn = hexc("#3F3B46")
    rig = Rig("lugner", render_scale=1.0)

    # Parted in the middle; the fringe sweeps down to both sides; long locks framing the face.
    depth = [16, 15, 9, 8, 7, 6, 4, 2, 2, 4, 6, 7, 8, 9, 15, 16]
    head, shell = soft_head(rig, skin, hair_c, eye,
                            face=dict(style="sharp", eye_y=8, depth=depth, locks=(2, 14), lash=hexc("#2A2430"),
                                      mouth="small", brow=shade(hair_c, 0.7)),
                            side_rows=16, side_tips=3, back_rows=16, back_tips=3, fringe_rows=0, seed=151)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, doublet, gradient=0.06)
        r = f["front"]
        w = r[2]
        for y in range(0, 17):                                  # the long ruffled cravat: tiers of frills widening as it falls
            half = 2 + min(2, y // 5)
            tier_edge = y % 4 == 3
            for x in range(w // 2 - half, w // 2 + half):
                cv.px(r[0] + x, r[1] + y, white_d if tier_edge or x in (w // 2 - half, w // 2 + half - 1) else white)
        cv.rect(r[0] + w // 2 - 1, r[1] + 3, 2, 2, gem)          # the gem pinning it
        cv.px(r[0] + w // 2 - 1, r[1] + 3, shade(gem, 1.6))
        for y in (8, 12, 16, 19):                                 # doublet buttons below
            cv.px(r[0] + w // 2 - 4, r[1] + y, gold)
            cv.px(r[0] + w // 2 + 3, r[1] + y, gold)

    def coat_layer(cv, f):
        for name, r in f.items():
            if name in ("top", "bottom"):
                continue
            fill(cv, r, coat, gradient=0.06)
        r = f["front"]
        for y in range(r[3]):                                     # open down the front, edged in gold
            for x in range(3, r[2] - 3):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
            cv.px(r[0] + 2, r[1] + y, gold)
            cv.px(r[0] + r[2] - 3, r[1] + y, gold)

    def sleeve(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            fill(cv, r, coat, gradient=0.06)
            if name != "top":
                fill(cv, sub(r, 0, r[3] - 9, r[2], 3), coat_d)
                hline(cv, r, r[3] - 9, gold)
                hline(cv, r, r[3] - 7, gold)
                for yy in range(r[3] - 6, r[3]):
                    for xx in range(r[2]):
                        cv.px(r[0] + xx, r[1] + yy, (0, 0, 0, 0))

    def coat_skirt(cv, f):
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, coat_d)
                continue
            fill(cv, r, coat, gradient=0.08)
            if name != "top":
                hline(cv, r, r[3] - 1, gold)
                for x in range(3, r[2], 5):
                    vline(cv, r, x, coat_d, y_from=1, y_to=r[3] - 1)
        r = f["front"]
        for y in range(r[3]):
            for x in range(r[2] // 2 - 3, r[2] // 2 + 3):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
            cv.px(r[0] + r[2] // 2 - 4, r[1] + y, gold)
            cv.px(r[0] + r[2] // 2 + 3, r[1] + y, gold)

    def collar(cv, f):
        for name, r in f.items():
            fill(cv, r, coat if name != "bottom" else coat_d)
            if name not in ("top", "bottom"):
                hline(cv, r, 0, gold)
        r = f["front"]
        for y in range(r[3]):
            for x in range(3, r[2] - 3):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))

    painters = {"head": head, "hat": shell, "body": body, "jacket": coat_layer,
                "right_arm": arm_skin_painter(skin, coat), "left_arm": arm_skin_painter(skin, coat),
                "right_sleeve": sleeve, "left_sleeve": sleeve,
                "right_leg": legs_painter(trousers, boot, boot_top=11, cuff=shade(boot, 0.9)),
                "left_leg": legs_painter(trousers, boot, boot_top=11, cuff=shade(boot, 0.9))}
    humanoid(rig, painters, slim=False)

    # Two long strands framing the face, and hair to the shoulders behind.
    for part, sx in (("lock_right", -1), ("lock_left", 1)):
        chain(rig, (part, part + "_2"), ((3.7 * sx, -5.0, -3.6), (0, 5, 0)), ((-0.04, 0, -0.03 * sx), (0.03, 0, 0.03 * sx)))
        rig.box(part, -0.75, 0, -0.5, 1.5, 5, 1, hair_painter(hair_c, seed=152, highlight_row=1))
        rig.box(part + "_2", -0.75, 0, -0.5, 1.5, 4, 1, hair_painter(shade(hair_c, 0.95), seed=153, highlight_row=None, tips=2))
    chain(rig, ("hair_back",), ((0, -6.5, 4.1),), ((0.08, 0, 0),))
    rig.box("hair_back", -4.5, 0, -0.5, 9, 9, 1, hair_painter(hair_c, seed=154, highlight_row=2, tips=4))

    # Horns from the crown, bent at the base and again near the tip, up and forward.
    for sx in (-1, 1):
        horn_chain(rig, sx, (1.7, -8.2, -0.4),
                   [(None, (0.05, 0.0, 0.25), (2, 2, 2)), ((0, -1.9, 0), (0.6, 0.0, -0.15), (2, 2, 2)),
                    ((0, -1.9, 0), (0.55, 0.0, 0.0), (1, 2, 1))], horn, tip=shade(horn, 1.6), ribs=False)

    # A stand collar behind the neck, and the coat's long skirts to the calf, open in front.
    rig.box("collar", -4.5, -1.2, -2.6, 9, 2, 5.2, collar)
    rig.pose("skirt", offset=(0, 11, 0))
    rig.box("skirt", -4.75, 0, -2.75, 9.5, 8, 5.5, coat_skirt)
    return rig


def macht():
    """
    Macht of the Golden Land, from his portrait: long magenta-red hair swept back from the forehead and falling past
    the shoulders, great ribbed ivory ram horns, pointed ears, blue eyes; a dark fur stole over a long dark coat, a
    deep teal high-collared coat with a gold inner collar and a burgundy cravat, and a striped gold vest with a
    fringed hem.
    """
    skin = hexc("#F3DDCF")
    hair_c = hexc("#A8365F")
    eye = hexc("#5374C4")
    horn = hexc("#DDD3BF")
    fur = hexc("#2D3248")
    teal = hexc("#1F3B37")
    gold = hexc("#D2AC48")
    vest = hexc("#B9962F")
    cravat = hexc("#7A1E30")
    trousers = hexc("#1E2030")
    boot = hexc("#16161C")
    rig = Rig("macht", render_scale=1.12)

    # Swept back: almost no fringe, a high hairline and two loose strands; long side locks.
    depth = [16, 13, 3, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 3, 13, 16]
    head, shell = soft_head(rig, skin, hair_c, eye,
                            face=dict(style="sharp", eye_y=8, depth=depth, locks=(2, 12), lash=hexc("#2A2030"),
                                      mouth="small", brow=shade(hair_c, 0.7)),
                            side_rows=16, side_tips=0, back_rows=16, back_tips=0, fringe_rows=0, seed=71)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, teal, gradient=0.06)
        r = f["front"]
        fill(cv, sub(r, 4, 3, 8, r[3] - 3), vest, vertical_streaks=0.0)    # the gold vest
        for x in range(5, 11, 2):
            vline(cv, r, x, shade(vest, 0.75), y_from=4)
        fill(cv, sub(r, 5, 0, 6, 3), gold)                                 # gold inner collar
        cv.rect(r[0] + 6, r[1] + 1, 4, 4, cravat)                          # burgundy cravat
        cv.rect(r[0] + 7, r[1] + 5, 2, 3, cravat)
        cv.px(r[0] + 6, r[1] + 1, shade(cravat, 1.4))

    def stole(cv, f):
        for name, r in f.items():
            fill(cv, r, fur, gradient=-0.15)
            if name not in ("top", "bottom"):
                for x in range(0, r[2], 2):
                    cv.px(r[0] + x, r[1] + (x * 7) % 3, shade(fur, 1.3))     # glints in the fur
                zigzag(cv, r, 3, seed=len(name), period=3)
        r = f["front"]
        for y in range(r[3]):                                              # open in front
            for x in range(r[2] // 2 - 3, r[2] // 2 + 3):
                cv.px(r[0] + x, r[1] + y, (0, 0, 0, 0))
        for face in ("top", "bottom"):
            b = f[face]
            for x in range(b[2] // 2 - 4, b[2] // 2 + 4):
                for y in range(b[3] // 2 + 1):
                    cv.px(b[0] + x, b[1] + y, (0, 0, 0, 0))

    def coat(cv, f):
        for name, r in f.items():
            fill(cv, r, fur if name != "front" else shade(fur, 0.85), gradient=0.1, vertical_streaks=0.05)

    def fringe_hem(cv, f):
        for name, r in f.items():
            fill(cv, r, vest if name != "bottom" else shade(vest, 0.7))
            if name not in ("top", "bottom"):
                for x in range(0, r[2], 2):
                    vline(cv, r, x, shade(vest, 0.78), y_from=1)
                zigzag(cv, r, 3, seed=3, period=2)

    def sleeve(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            fill(cv, r, teal, gradient=0.06)
            if name != "top":
                hline(cv, r, r[3] - 7, gold)
                hline(cv, r, r[3] - 6, shade(gold, 0.85))
                for yy in range(r[3] - 5, r[3]):
                    for xx in range(r[2]):
                        cv.px(r[0] + xx, r[1] + yy, (0, 0, 0, 0))

    def ear(cv, f):
        for name, r in f.items():
            fill(cv, r, skin)
        hline(cv, f["front"], f["front"][3] - 1, shade(skin, 0.84))

    painters = {"head": head, "hat": shell, "body": body,
                "right_arm": arm_skin_painter(skin, teal), "left_arm": arm_skin_painter(skin, teal),
                "right_sleeve": sleeve, "left_sleeve": sleeve,
                "right_leg": legs_painter(trousers, boot, boot_top=14, cuff=shade(boot, 1.4)),
                "left_leg": legs_painter(trousers, boot, boot_top=14, cuff=shade(boot, 1.4))}
    humanoid(rig, painters, slim=False)

    # Pointed ears, shorter than an elf's.
    for part, sx in (("ear_right", -1), ("ear_left", 1)):
        rig.pose(part, offset=(4 * sx, -3.2, 0.6), rotation=(0, -0.35 * sx, -0.35 * sx))
        rig.box(part, -2 if sx < 0 else 0, -1, -0.5, 2, 2, 1, ear)
        rig.box(part, -3 if sx < 0 else 2, -1.4, -0.5, 1, 1, 1, ear)

    # Ram horns: up and back from the temples, curling out and forward.
    for sx in (-1, 1):
        horn_chain(rig, sx, (3.2, -7.6, 0.0),
                   [(None, (-0.3, 0.0, 0.3), (3, 4, 3)), ((0, -3.7, 0), (-0.75, 0.0, 0.45), (3, 4, 3)),
                    ((0, -3.7, 0), (-0.95, 0.0, 0.55), (2, 4, 2))], horn)

    # Long hair down the back in two links, strands over the shoulders.
    chain(rig, ("hair_back", "hair_back_2"), ((0, -7.2, 4.0), (0, 10, 0)), ((0.08, 0, 0), (0.05, 0, 0)))
    rig.box("hair_back", -4.5, 0, -0.5, 9, 10, 2, hair_painter(hair_c, seed=73, highlight_row=2))
    rig.box("hair_back_2", -4, 0, -0.5, 8, 7, 2, hair_painter(shade(hair_c, 0.95), seed=74, highlight_row=None, tips=4))
    for part, sx in (("lock_right", -1), ("lock_left", 1)):
        chain(rig, (part, part + "_2"), ((4.0 * sx, -4.5, -1.5), (0, 6, 0)), ((-0.05, 0, -0.1 * sx), (0.05, 0, 0.05 * sx)))
        rig.box(part, -1, 0, -1, 2, 6, 2, hair_painter(hair_c, seed=75, highlight_row=1))
        rig.box(part + "_2", -1, 0, -1, 2, 5, 2, hair_painter(shade(hair_c, 0.95), seed=76, highlight_row=None, tips=3))

    # The fur stole and the long coat behind; the vest's fringed hem below the waist.
    rig.box("collar", -7, -1, -3.2, 14, 4, 7, stole)
    chain(rig, ("cape", "cape_2"), ((0, 1.5, 3.0), (0, 11, 0)), ((0.05, 0, 0), (0.04, 0, 0)))
    rig.box("cape", -6.5, 0, 0, 13, 11, 1, coat)
    rig.box("cape_2", -6.5, 0, 0, 13, 10, 1, coat)
    rig.pose("skirt", offset=(0, 11, 0))
    rig.box("skirt", -3, 0, -2.4, 6, 3, 1, fringe_hem)
    return rig


def demon_king():
    """
    The Demon King, designed for the mod (the anime keeps him in shadow): a tall, regal demon with ashen skin, long
    black hair and crimson eyes, great black horns sweeping back from a gold circlet; a high stiff collar, a black
    robe trimmed in gold and split over crimson, gold-edged pauldrons, dark gauntlets and a floor-length cape lined
    in crimson. He wields the Blade of the Demon King.
    """
    skin = hexc("#D6D0D8")
    hair_c = hexc("#2A2532")
    eye = hexc("#E8323A")
    robe = hexc("#1C1820")
    gold = hexc("#C9A040")
    crimson = hexc("#8E1A2A")
    horn = hexc("#2C2830")
    rig = Rig("demon_king", render_scale=1.45)

    head, shell = soft_head(rig, skin, hair_c, eye,
                            face=dict(style="sharp", eye_y=8, fringe=4, part=7, locks=(2, 12), lash=hexc("#141018"),
                                      mouth="stern", brow=shade(hair_c, 1.3), brow_angle=1, glow=True),
                            side_rows=16, side_tips=3, back_rows=16, back_tips=0, fringe_tips=4, fringe_period=3,
                            fringe_rows=3, part_col=8, seed=81)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, robe, gradient=0.06)
        r = f["front"]
        fill(cv, sub(r, 6, 0, 4, r[3]), crimson)               # crimson down the centre
        for x in (5, 10):
            vline(cv, r, x, gold)
        for y in (4, 9):                                      # gold clasps
            hline(cv, r, y, gold, x_from=5, x_to=11)
        for name in ("front", "back", "left", "right"):       # crimson sash
            fill(cv, sub(f[name], 0, 16, f[name][2], 2), crimson)
            hline(cv, f[name], 16, gold)

    def collar(cv, f):
        # a tall, stiff collar standing behind the head, black outside, crimson inside, edged in gold
        for name, r in f.items():
            fill(cv, r, robe if name != "front" else crimson, gradient=-0.1)
            if name not in ("bottom",):
                for x in range(r[2]):
                    cv.px(r[0] + x, r[1], gold)
                if name in ("back", "front"):
                    vline(cv, r, 0, gold)
                    vline(cv, r, r[2] - 1, gold)

    def pauldron(cv, f):
        for name, r in f.items():
            fill(cv, r, robe, gradient=0.12)
            if name not in ("top", "bottom"):
                hline(cv, r, r[3] - 1, gold)
                hline(cv, r, r[3] - 2, shade(gold, 0.7))
        t = f["top"]
        for x in range(1, t[2] - 1, 3):
            cv.px(t[0] + x, t[1] + t[3] // 2, shade(gold, 0.85))

    def robe_skirt(cv, f):
        for name, r in f.items():
            fill(cv, r, robe if name != "bottom" else shade(robe, 0.7), gradient=0.08, vertical_streaks=0.05)
            if name not in ("top", "bottom"):
                hline(cv, r, r[3] - 1, gold)
                hline(cv, r, r[3] - 2, shade(gold, 0.7))
        r = f["front"]
        fill(cv, sub(r, r[2] // 2 - 2, 0, 4, r[3] - 2), crimson)  # the split over crimson
        vline(cv, r, r[2] // 2 - 3, gold)
        vline(cv, r, r[2] // 2 + 2, gold)

    def cape_panel(cv, f):
        for name, r in f.items():
            fill(cv, r, robe if name != "front" else crimson, gradient=0.1)
            if name == "back":
                for x in range(3, r[2], 6):
                    vline(cv, r, x, shade(robe, 1.3), y_from=2)
                hline(cv, r, r[3] - 1, gold)

    def sleeve(cv, f):
        for name, r in f.items():
            if name == "bottom":
                continue
            fill(cv, r, robe, gradient=0.06)
            if name != "top":
                hline(cv, r, r[3] - 7, gold)

    def gauntlet(cv, f):
        for name, r in f.items():
            fill(cv, r, hexc("#2E2A34"), gradient=0.08)
            if name not in ("top", "bottom"):
                hline(cv, r, r[3] - 6, gold)

    def crown_band(cv, f):
        for name, r in f.items():
            fill(cv, r, gold, gradient=-0.15)
        r = f["front"]
        if r[2] >= 8:
            cv.rect(r[0] + r[2] // 2 - 1, r[1], 2, r[3], crimson)

    painters = {"head": head, "hat": shell, "body": body, "right_arm": gauntlet, "left_arm": gauntlet,
                "right_sleeve": sleeve, "left_sleeve": sleeve,
                "right_leg": legs_painter(robe, hexc("#141016"), boot_top=14, cuff=gold),
                "left_leg": legs_painter(robe, hexc("#141016"), boot_top=14, cuff=gold)}
    humanoid(rig, painters, slim=False)

    # Long black hair down the back; locks over the shoulders.
    chain(rig, ("hair_back", "hair_back_2"), ((0, -7.0, 4.0), (0, 10, 0)), ((0.06, 0, 0), (0.04, 0, 0)))
    rig.box("hair_back", -4.5, 0, -0.5, 9, 10, 2, hair_painter(hair_c, seed=83, highlight_row=2))
    rig.box("hair_back_2", -4, 0, -0.5, 8, 8, 2, hair_painter(shade(hair_c, 0.95), seed=84, highlight_row=None, tips=4))
    for part, sx in (("lock_right", -1), ("lock_left", 1)):
        chain(rig, (part, part + "_2"), ((3.9 * sx, -4.4, -2.0), (0, 6, 0)), ((-0.04, 0, -0.08 * sx), (0.04, 0, 0.04 * sx)))
        rig.box(part, -1, 0, -1, 2, 6, 2, hair_painter(hair_c, seed=85, highlight_row=1))
        rig.box(part + "_2", -1, 0, -1, 2, 5, 2, hair_painter(shade(hair_c, 0.95), seed=86, highlight_row=None, tips=3))

    # Horns sweeping back from the temples and rising again, crimson at the points; a gold circlet.
    for sx in (-1, 1):
        horn_chain(rig, sx, (3.2, -7.2, -0.6),
                   [(None, (-0.35, 0.0, 0.85), (3, 4, 3)), ((0, -3.7, 0), (-0.55, 0.0, -0.55), (2, 4, 2)),
                    ((0, -3.7, 0), (-0.2, 0.0, -0.45), (2, 4, 2))], horn, tip=hexc("#7A1E2C"))
    rig.box("crown", -4.6, -7.6, -4.7, 9, 1, 1, crown_band)
    rig.box("crown", -4.7, -7.6, -4.6, 1, 1, 8, crown_band)
    rig.box("crown", 3.7, -7.6, -4.6, 1, 1, 8, crown_band)
    rig.box("crown", -1, -8.8, -4.75, 2, 2, 1, crown_band)

    # High collar, pauldrons, robe to the floor and a cape lined in crimson.
    rig.box("collar", -5, -5.5, 2.2, 10, 6, 1, collar)
    rig.box("right_sleeve", -4.5, -2.8, -2.5, 6, 3, 5, pauldron)
    rig.box("left_sleeve", -1.5, -2.8, -2.5, 6, 3, 5, pauldron)
    rig.pose("skirt", offset=(0, 11, 0))
    rig.box("skirt", -5, 0, -3, 10, 12, 6, robe_skirt)
    chain(rig, ("cape", "cape_2"), ((0, 0.2, 3.2), (0, 12, 0)), ((0.05, 0, 0), (0.04, 0, 0)))
    rig.box("cape", -7, 0, 0, 14, 12, 1, cape_panel)
    rig.box("cape_2", -7, 0, 0, 14, 12, 1, cape_panel)
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
    rig.carry = "hand"   # the copper sword is baked into the statue
    himmel_sword(rig)
    rig.render_scale = 2.8
    rig.post_paint = copper_cast("hero_statue")
    rig.variants = {name: copper_cast(name) for name in COPPER_STAGES if name != "hero_statue"}
    return rig


def ubel():
    """
    Übel, First-Class exam candidate: dark green hair with a fringe split into three locks and one thick side
    ponytail tied low on her left and falling over the shoulder; droopy purple eyes with heavy lashes and a lazy smile.
    A snug black corset dress held up by four thin straps that meet at a silver ring on her choker, a front-clasped
    belt and a pleated skirt; bare arms with a black band on the left arm and a long black glove on the right;
    knee-high black boots. Her black spear-staff (frieren:ubel_staff) has twin curved blades under a lightning tip.
    """
    skin = hexc("#F7E1D3")
    hair_c = hexc("#33493C")
    eye = hexc("#8A5CB8")
    black = hexc("#23212A")
    black_l = shade(black, 1.35)
    silver = hexc("#C6CAD4")
    boot = hexc("#1C1B22")
    rig = Rig("ubel", render_scale=0.94)

    # The fringe in three locks: a short centre lock and two longer ones either side, parted at columns 5 and 10.
    depth = [12, 11, 6, 7, 7, 3, 6, 7, 7, 6, 3, 7, 7, 6, 11, 12]
    head, shell = soft_head(rig, skin, hair_c, eye,
                            face=dict(style="calm", eye_y=8, depth=depth, lash=hexc("#1E1A24"), mouth="smile",
                                      brow=shade(hair_c, 0.8)),
                            side_rows=12, side_tips=4, back_rows=13, back_tips=4, fringe_tips=3, fringe_period=5,
                            fringe_rows=4, part_col=None, seed=151)

    def body(cv, f):
        for name, r in f.items():
            fill(cv, r, black, gradient=0.06)
        r = f["front"]
        w = r[2]
        # bare shoulders and upper chest above the corset's straight neckline, the choker on top
        for name in ("front", "back", "left", "right"):
            q = f[name]
            fill(cv, sub(q, 0, 0, q[2], 6), skin)
            hline(cv, q, 0, black)
            hline(cv, q, 1, black)
        cv.rect(r[0] + w // 2 - 1, r[1] + 1, 2, 2, silver)                  # the ring on the choker
        cv.px(r[0] + w // 2 - 1, r[1] + 1, shade(silver, 1.2))

        def strap(x0, y0, x1, y1):
            steps = max(abs(x1 - x0), abs(y1 - y0))
            for k in range(steps + 1):
                cv.px(r[0] + round(x0 + (x1 - x0) * k / steps), r[1] + round(y0 + (y1 - y0) * k / steps), black)
        for side in (0, 1):                                                 # four straps from the neckline up to the ring
            mx = (lambda x: x) if side == 0 else (lambda x: w - 1 - x)
            strap(mx(w // 2 - 5), 5, mx(w // 2 - 2), 2)
            strap(mx(w // 2 - 2), 5, mx(w // 2 - 1), 3)
        b = f["back"]
        for x0 in (3, b[2] - 4):                                            # the straps cross the back to the corset
            for y in range(2, 6):
                cv.px(b[0] + x0, b[1] + y, black)
        hline(cv, r, 6, black_l)                                            # neckline edge
        for name in ("front", "back", "left", "right"):                     # the belt
            fill(cv, sub(f[name], 0, 15, f[name][2], 2), shade(black, 0.8))
        cv.rect(r[0] + w // 2 - 1, r[1] + 15, 2, 2, silver)                 # front clasp
        for y in range(8, 15, 2):                                           # corset seams
            cv.px(r[0] + 4, r[1] + y, black_l)
            cv.px(r[0] + w - 5, r[1] + y, black_l)

    def skirt(cv, f):
        for name, r in f.items():
            if name == "bottom":
                fill(cv, r, shade(black, 0.7))
                continue
            fill(cv, r, black, gradient=0.08)
            if name != "top":
                for x in range(1, r[2], 3):                                 # pleats
                    vline(cv, r, x, black_l, y_from=1)
                hline(cv, r, r[3] - 1, shade(black, 0.75))

    def legs(cv, f):
        for name, r in f.items():
            if name == "top":
                fill(cv, r, skin)
                continue
            if name == "bottom":
                fill(cv, r, shade(boot, 0.6))
                continue
            fill(cv, r, skin, noise=0.015)
            fill(cv, sub(r, 0, 9, r[2], r[3] - 9), boot, gradient=0.06)     # knee-high boots
            hline(cv, r, 9, shade(boot, 1.6))
            hline(cv, r, r[3] - 1, shade(boot, 0.6))

    def right_arm(cv, f):
        # the long black glove reaching above the elbow
        for i, (name, r) in enumerate(f.items()):
            fill(cv, r, skin, noise=0.015, seed=161 + i)
            if name != "top":
                fill(cv, sub(r, 0, 4, r[2], r[3] - 4), black, gradient=0.05)
                hline(cv, r, 4, black_l)

    def left_arm(cv, f):
        for i, (name, r) in enumerate(f.items()):
            fill(cv, r, skin, noise=0.015, seed=171 + i)
            if name not in ("top", "bottom"):
                fill(cv, sub(r, 0, 3, r[2], 2), black)                      # the band on her upper arm
                hline(cv, r, 3, silver)

    painters = {"head": head, "hat": shell, "body": body, "right_arm": right_arm, "left_arm": left_arm,
                "right_leg": legs, "left_leg": legs}
    humanoid(rig, painters, slim=True)

    # One thick side ponytail tied low on her left, falling forward over the shoulder and down to the chest.
    tail = hair_painter(hair_c, seed=152, highlight_row=2, soft=True)
    chain(rig, ("tail_left", "tail_left_2", "tail_left_3"),
          ((4.4, -3.2, 0.6), (0, 4.5, 0), (0, 4.0, 0)), ((-0.25, 0, -0.18), (-0.2, 0, 0.12), (-0.1, 0, 0.08)))
    rig.box("tail_left", -1.75, -0.5, -1.75, 3.5, 5.0, 3.5, tail)
    rig.box("tail_left_2", -1.6, 0, -1.6, 3.2, 4.5, 3.2, hair_painter(shade(hair_c, 0.97), seed=153, highlight_row=None, soft=True))
    rig.box("tail_left_3", -1.25, 0, -1.25, 2.5, 3.5, 2.5,
            hair_painter(shade(hair_c, 0.94), seed=154, highlight_row=None, tips=3, bottom=False, soft=True))
    rig.box("tail_left", -1.4, -1.4, -1.4, 2.8, 1.0, 2.8, cloth_painter(black))          # the tie

    # The pleated skirt to mid-thigh.
    rig.pose("skirt", offset=(0, 11, 0))
    rig.box("skirt", -4.5, 0, -2.5, 9, 4.5, 5, skirt)
    rig.held = ("frieren:ubel_staff", None)
    return rig


ROSTER = {
    "frieren": frieren,
    "fern": fern,
    "stark": stark,
    "serie": serie,
    "himmel": himmel,
    "eisen": eisen,
    "heiter": heiter,
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
    "ubel": ubel,
}
