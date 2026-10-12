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

    # Long elf ears, angled back and up, tapering to a point, with small dangling red earrings.
    rig.pose("ear_right", offset=(-4, -3.6, 0.6), rotation=(0, 0.5, 0.34))
    rig.box("ear_right", -2, -1, -0.5, 2, 2, 1, ear)
    rig.box("ear_right", -4, -1.5, -0.5, 2, 1, 1, ear)
    rig.box("ear_right", -5.5, -2.2, -0.5, 2, 1, 1, ear)
    rig.box("ear_right", -1.2, 1, -0.5, 1, 1, 1, p_gem(red))
    rig.pose("ear_left", offset=(4, -3.6, 0.6), rotation=(0, -0.5, -0.34))
    rig.box("ear_left", 0, -1, -0.5, 2, 2, 1, ear)
    rig.box("ear_left", 2, -1.5, -0.5, 2, 1, 1, ear)
    rig.box("ear_left", 3.5, -2.2, -0.5, 2, 1, 1, ear)
    rig.box("ear_left", 0.2, 1, -0.5, 1, 1, 1, p_gem(red))

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


ROSTER = {
    "frieren": frieren,
}
