"""
Pixel-art painting toolkit for character skins. Textures are painted at 2x density (a 16x16 texel face on an
8x8 model face) into RGBA numpy arrays; every helper takes a face rectangle (x, y, w, h) in texels.
"""
import random

import numpy as np

CLEAR = (0, 0, 0, 0)


def hexc(value, alpha=255):
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), c[3])


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(4))


class Canvas:
    def __init__(self, w, h):
        self.a = np.zeros((h, w, 4), dtype=np.uint8)
        self.w, self.h = w, h

    def px(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h and c is not None:
            if len(c) == 4 and 0 < c[3] < 255:
                base = self.a[y, x].astype(float)
                t = c[3] / 255.0
                if base[3] == 0:
                    self.a[y, x] = c
                else:
                    self.a[y, x] = (int(base[0] * (1 - t) + c[0] * t), int(base[1] * (1 - t) + c[1] * t),
                                    int(base[2] * (1 - t) + c[2] * t), 255)
            else:
                self.a[y, x] = c

    def get(self, x, y):
        return tuple(int(v) for v in self.a[y, x])

    def rect(self, x, y, w, h, c):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.px(xx, yy, c)

    def save(self, path):
        from PIL import Image
        Image.fromarray(self.a, "RGBA").save(path)


# ---------------------------------------------------------------------------------------------
# Fills
# ---------------------------------------------------------------------------------------------
def fill(cv, r, c, noise=0.05, seed=0, gradient=0.0, vertical_streaks=0.0):
    """Base fill with value noise, an optional top-to-bottom darkening gradient and soft vertical streaks."""
    x0, y0, w, h = r
    rng = random.Random(seed)
    streak = [1.0 + (rng.random() - 0.5) * 2 * vertical_streaks for _ in range(w)]
    for yy in range(h):
        g = 1.0 - gradient * (yy / max(1, h - 1))
        for xx in range(w):
            f = g * streak[xx] * (1.0 + (rng.random() - 0.5) * 2 * noise)
            cv.px(x0 + xx, y0 + yy, shade(c, f))


def hline(cv, r, row, c, x_from=0, x_to=None):
    x0, y0, w, h = r
    x_to = w if x_to is None else x_to
    for xx in range(x_from, x_to):
        cv.px(x0 + xx, y0 + row, c)


def vline(cv, r, col, c, y_from=0, y_to=None):
    x0, y0, w, h = r
    y_to = h if y_to is None else y_to
    for yy in range(y_from, y_to):
        cv.px(x0 + col, y0 + yy, c)


def border(cv, r, c, top=True, bottom=True, left=True, right=True, thickness=1):
    x0, y0, w, h = r
    for t in range(thickness):
        if top:
            hline(cv, r, t, c)
        if bottom:
            hline(cv, r, h - 1 - t, c)
        if left:
            vline(cv, r, t, c)
        if right:
            vline(cv, r, w - 1 - t, c)


def sub(r, x, y, w, h):
    """Sub-rectangle relative to r."""
    return (r[0] + x, r[1] + y, w, h)


# ---------------------------------------------------------------------------------------------
# Hair
# ---------------------------------------------------------------------------------------------
def hair(cv, r, c, seed=0, highlight_row=None, strands=True, sheen=0.18):
    """Hair: base with darker strand lines, a soft highlight band ('angel ring') and slightly darker ends."""
    x0, y0, w, h = r
    rng = random.Random(seed)
    fill(cv, r, c, noise=0.035, seed=seed, gradient=0.16)
    if strands:
        for xx in range(w):
            if rng.random() < 0.34:
                length = rng.randint(max(2, h // 3), h)
                start = rng.randint(0, max(0, h - length))
                for yy in range(start, start + length):
                    cv.px(x0 + xx, y0 + yy, shade(c, 0.86 - 0.08 * (yy / max(1, h))))
    if highlight_row is not None and 0 <= highlight_row < h:
        for xx in range(w):
            if rng.random() < 0.75:
                cv.px(x0 + xx, y0 + highlight_row, shade(c, 1.0 + sheen))
                if rng.random() < 0.4 and highlight_row + 1 < h:
                    cv.px(x0 + xx, y0 + highlight_row + 1, shade(c, 1.0 + sheen * 0.5))


def bangs(cv, r, c, depth, seed=0, part=None, jagged=2):
    """Hair fringe hanging over the top rows of a face: depth rows with a jagged lower edge and a part line."""
    x0, y0, w, h = r
    rng = random.Random(seed)
    for xx in range(w):
        d = depth + (rng.randint(-jagged, 1) if jagged else 0)
        if part is not None and abs(xx - part) <= 0:
            d = max(1, depth - 3)
        for yy in range(max(0, d)):
            f = 1.0 - 0.18 * (yy / max(1, depth)) + (rng.random() - 0.5) * 0.06
            cv.px(x0 + xx, y0 + yy, shade(c, f))
        if d > 0 and rng.random() < 0.5:
            cv.px(x0 + xx, y0 + d - 1, shade(c, 0.78))


def side_locks(cv, r, c, width, length, top=4, seed=0):
    """Long strands framing the face on both edges of a face rectangle."""
    x0, y0, w, h = r
    rng = random.Random(seed)
    for side in (0, 1):
        for i in range(width):
            col = i if side == 0 else w - 1 - i
            ln = length - (i if i > 0 else 0) + rng.randint(-1, 0)
            for yy in range(top, min(h, top + ln)):
                cv.px(x0 + col, y0 + yy, shade(c, 0.92 - 0.12 * (i / max(1, width)) - 0.1 * (yy / h)))


# ---------------------------------------------------------------------------------------------
# Faces (16x16 at 2x density)
# ---------------------------------------------------------------------------------------------
def face(cv, r, skin, eye, eye_y=7, eye_w=4, gap=4, lash=None, brow=None, mouth="small", blush=False,
         sleepy=False, glow=False, pupil=None, lip=None, brow_angle=0, lower_lash=True, seed=0):
    """
    Anime-style features on a skin-filled 16-wide face (2x density). Each eye: a dark lash row, then two iris rows
    (darker top with a white highlight, lighter bottom) and a short lower lash at the outer corner. sleepy adds a
    lid shadow above the lash (Frieren's calm half-closed look) without hiding the iris. brow_angle tilts brows
    (+1 = stern/angry, -1 = worried).
    """
    x0, y0, w, h = r
    lash = lash or shade(skin, 0.32)
    brow = brow or shade(skin, 0.6)
    pupil = pupil or shade(eye, 0.4)
    total = eye_w * 2 + gap
    left = (w - total) // 2
    for idx, ex in enumerate((left, left + eye_w + gap)):
        outer = 0 if idx == 0 else eye_w - 1
        inner = eye_w - 1 if idx == 0 else 0
        # brow (thin), optionally angled
        for i in range(eye_w - 1):
            bx = ex + i + (1 if idx == 0 else 0)
            tilt = 0
            if brow_angle:
                toward_inner = (i >= (eye_w - 2) // 2) if idx == 0 else (i <= (eye_w - 2) // 2)
                tilt = brow_angle if toward_inner else 0
            cv.px(x0 + bx, y0 + eye_y - 2 + tilt, brow)
        if sleepy:
            for i in range(eye_w):
                cv.px(x0 + ex + i, y0 + eye_y - 1, shade(skin, 0.86))
        # lash row (slightly heavier at the outer corner)
        for i in range(eye_w):
            cv.px(x0 + ex + i, y0 + eye_y, lash)
        cv.px(x0 + ex + outer + (-1 if idx == 0 else 1), y0 + eye_y, lash)
        # iris
        for i in range(eye_w):
            cv.px(x0 + ex + i, y0 + eye_y + 1, shade(eye, 0.72))
            cv.px(x0 + ex + i, y0 + eye_y + 2, eye if not glow else shade(eye, 1.35))
        mid = ex + eye_w // 2 - (1 if idx == 0 else 0)
        cv.px(x0 + mid, y0 + eye_y + 1, pupil)
        cv.px(x0 + mid + (1 if idx == 0 else -1), y0 + eye_y + 1, pupil)
        cv.px(x0 + ex + (1 if idx == 0 else eye_w - 2), y0 + eye_y + 1, (255, 255, 255, 255) if not glow else shade(eye, 1.7))
        if sleepy:
            cv.px(x0 + ex + inner, y0 + eye_y + 1, shade(skin, 0.8))
        if lower_lash:
            cv.px(x0 + ex + outer, y0 + eye_y + 3, shade(lash, 1.6))
    # nose: a single soft shadow
    cv.px(x0 + w // 2 - 1, y0 + eye_y + 4, shade(skin, 0.88))
    my = y0 + eye_y + 6
    lip = lip or mix(skin, (205, 110, 110, 255), 0.45)
    cx = x0 + w // 2
    if mouth == "small":
        cv.rect(cx - 1, my, 2, 1, lip)
    elif mouth == "smile":
        cv.px(cx - 2, my - 1, lip)
        cv.rect(cx - 1, my, 2, 1, shade(lip, 0.85))
        cv.px(cx + 1, my - 1, lip)
    elif mouth == "smirk":
        cv.rect(cx - 1, my, 2, 1, lip)
        cv.px(cx + 1, my - 1, shade(lip, 0.9))
    elif mouth == "fangs":
        cv.rect(cx - 2, my, 4, 1, shade(lip, 0.7))
        cv.px(cx - 2, my + 1, (248, 248, 248, 255))
        cv.px(cx + 1, my + 1, (248, 248, 248, 255))
    elif mouth == "stern":
        cv.rect(cx - 2, my, 4, 1, shade(skin, 0.6))
    elif mouth == "open":
        cv.rect(cx - 1, my, 2, 2, shade(lip, 0.6))
    if blush:
        for ex in (left, left + eye_w + gap):
            cv.rect(x0 + ex, y0 + eye_y + 4, 2, 1, mix(skin, (240, 130, 140, 255), 0.35))


# ---------------------------------------------------------------------------------------------
# Clothing details
# ---------------------------------------------------------------------------------------------
def trim(cv, r, c, rows=(), cols=()):
    for row in rows:
        hline(cv, r, row if row >= 0 else r[3] + row, c)
    for col in cols:
        vline(cv, r, col if col >= 0 else r[2] + col, c)


def buttons(cv, r, col, rows, c):
    for row in rows:
        cv.px(r[0] + col, r[1] + row, c)


def gem(cv, x, y, c, size=2):
    cv.rect(x, y, size, size, c)
    cv.px(x, y, shade(c, 1.5))
    if size > 1:
        cv.px(x + size - 1, y + size - 1, shade(c, 0.6))


def emblem_diamond(cv, cx, cy, c, radius=2):
    for dy in range(-radius, radius + 1):
        span = radius - abs(dy)
        for dx in range(-span, span + 1):
            cv.px(cx + dx, cy + dy, c if abs(dx) + abs(dy) < radius else shade(c, 0.7))


def wood(cv, r, c, seed=0):
    fill(cv, r, c, noise=0.06, seed=seed, vertical_streaks=0.12)


def metal(cv, r, c, seed=0):
    x0, y0, w, h = r
    fill(cv, r, c, noise=0.04, seed=seed)
    for yy in range(h):
        cv.px(x0, y0 + yy, shade(c, 1.25))
        cv.px(x0 + w - 1, y0 + yy, shade(c, 0.7))
