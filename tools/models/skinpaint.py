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
                cv.px(x0 + col, y0 + yy, c if i == 0 else tone(c, 0.86))


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


# ---------------------------------------------------------------------------------------------
# Anime style (character skins): flat cel shading with hue-shifted shadows instead of value noise, clean hair
# locks instead of random strands, and big expressive eyes. roster.py imports these under the classic names.
# ---------------------------------------------------------------------------------------------
SHADOW_TINT = (0.90, 0.88, 1.10)   # shadows lean cool/violet, like the anime's palette
LIGHT_TINT = (1.00, 0.985, 0.94)   # highlights lean warm


def tone(c, f):
    """Shade or light a colour like a cel painter: darker shades shift toward violet, lighter ones toward warm white."""
    if f == 1.0:
        return c
    r, g, b = c[0], c[1], c[2]
    if f < 1.0:
        k = min(1.0, (1.0 - f) * 2.5)
        tint = [1.0 + (t - 1.0) * k for t in SHADOW_TINT]
        out = (r * f * tint[0], g * f * tint[1], b * f * tint[2])
    else:
        k = f - 1.0
        out = tuple(v + (255 - v) * k * 0.55 * lt + v * k * 0.45 for v, lt in zip((r, g, b), LIGHT_TINT))
    return (max(0, min(255, int(out[0]))), max(0, min(255, int(out[1]))), max(0, min(255, int(out[2]))), c[3])


def cel_fill(cv, r, c, noise=0.0, seed=0, gradient=0.0, vertical_streaks=0.0):
    """
    Flat fill with one shadow band instead of noise: a positive gradient darkens the lowest rows one tone (cloth
    folding under), a negative one darkens the top rows. vertical_streaks draws soft, evenly spaced folds.
    noise and seed are accepted for drop-in compatibility and ignored: anime colour fields are flat.
    """
    x0, y0, w, h = r
    cv.rect(x0, y0, w, h, c)
    if gradient and h >= 4:
        rows = max(1, round(h * 0.2))
        dark = tone(c, 1.0 - min(0.22, max(0.07, abs(gradient) * 0.8)))
        band = range(h - rows, h) if gradient > 0 else range(0, rows)
        for yy in band:
            for xx in range(w):
                cv.px(x0 + xx, y0 + yy, dark)
    if vertical_streaks and w >= 6 and h >= 6:
        fold = tone(c, 0.93)
        for xx in range(3, w - 2, 5):
            for yy in range(h // 3, h):
                cv.px(x0 + xx, y0 + yy, fold)


def anime_hair(cv, r, c, seed=0, highlight_row=None, strands=True, sheen=0.18):
    """
    Hair as clean locks: flat base, evenly spaced lock lines that end in points, a crisp highlight band (the
    'angel ring') broken where the locks part, and a darker tip row.
    """
    x0, y0, w, h = r
    cv.rect(x0, y0, w, h, c)
    line = tone(c, 0.85)
    soft = tone(c, 0.94)
    rng = random.Random(seed)
    lock_cols = set()
    if strands and w >= 3:
        spacing = 3 if w <= 6 else (4 if w <= 10 else 5)
        start = rng.randint(0, spacing - 1)
        for xx in range(start, w, spacing):
            lock_cols.add(xx)
            top = rng.randint(h // 6, max(h // 6, h // 3))
            for yy in range(top, h):
                cv.px(x0 + xx, y0 + yy, line)
            # soft shadow beside the line so locks read as rounded
            if xx + 1 < w:
                for yy in range(top + h // 3, h):
                    cv.px(x0 + xx + 1, y0 + yy, soft)
    if highlight_row is not None and 0 <= highlight_row < h:
        hi = tone(c, 1.0 + sheen * 1.4)
        for xx in range(w):
            if xx in lock_cols:
                continue
            cv.px(x0 + xx, y0 + highlight_row, hi)
    if h >= 6:
        for xx in range(w):
            cv.px(x0 + xx, y0 + h - 1, tone(c, 0.84))


def anime_bangs(cv, r, c, depth, seed=0, part=None, jagged=2):
    """Fringe over the face: locks that come to points, a darker lower edge and a soft shadow on the skin below."""
    x0, y0, w, h = r
    rng = random.Random(seed)
    # pointed locks: each 3-4 px lock is longest in its middle
    xx = 0
    while xx < w:
        lw = 3 if rng.random() < 0.5 else 4
        extra = rng.randint(0, jagged) if jagged else 0
        for i in range(lw):
            if xx + i >= w:
                break
            mid = abs(i - (lw - 1) / 2.0)
            d = depth + extra - int(mid * 1.2)
            if part is not None and abs(xx + i - part) <= 0:
                d = max(1, depth - 3)
            col = xx + i
            shadow = cv.get(x0 + col, y0 + min(h - 1, max(0, d))) if d < h else None
            for yy in range(max(0, d)):
                cv.px(x0 + col, y0 + yy, c if yy < d - 1 else tone(c, 0.82))
            if shadow is not None and 0 <= d < h and shadow[3] > 0:
                cv.px(x0 + col, y0 + d, tone(shadow, 0.9))
        if xx + lw < w:
            cv.px(x0 + xx + lw - 1, y0 + max(0, depth - 2), tone(c, 0.86))
        xx += lw


def anime_face(cv, r, skin, eye, eye_y=7, eye_w=4, gap=4, lash=None, brow=None, mouth="small", blush=False,
               sleepy=False, glow=False, pupil=None, lip=None, brow_angle=0, lower_lash=True, seed=0):
    """
    Anime eyes on a 16-wide face (2x density). Each eye is four rows: a heavy upper lash with a wing at the outer
    corner, a dark iris top with a white catch-light, the iris colour, and a bright lower iris with a short lower
    lash. A white sclera pixel sits at the inner corner. sleepy lowers the outer lid over the iris (Frieren's calm
    half-closed look) instead of shading the skin. Brows are thin and high.
    """
    x0, y0, w, h = r
    lash = lash or tone(skin, 0.3)
    brow = brow or tone(skin, 0.62)
    pupil = pupil or tone(eye, 0.45)
    total = eye_w * 2 + gap
    left = (w - total) // 2
    white = (250, 250, 252, 255)
    for idx, ex in enumerate((left, left + eye_w + gap)):
        outward = -1 if idx == 0 else 1        # toward the face edge
        outer = 0 if idx == 0 else eye_w - 1
        inner = eye_w - 1 if idx == 0 else 0
        # brow: short and high, tilted by brow_angle at the inner end
        for i in range(1, eye_w):
            bx = ex + (i if idx == 0 else eye_w - 1 - i)
            tilt = brow_angle if i >= eye_w - 1 else 0
            cv.px(x0 + bx, y0 + eye_y - 3 + tilt, brow)
        # upper lash, with a wing past the outer corner
        for i in range(eye_w):
            cv.px(x0 + ex + i, y0 + eye_y, lash)
        cv.px(x0 + ex + outer + outward, y0 + eye_y, lash)
        cv.px(x0 + ex + outer + outward, y0 + eye_y + 1, tone(lash, 1.4))
        # iris: dark top, colour, bright bottom
        dark, mid, bright = tone(eye, 0.55), eye, tone(eye, 1.35)
        if glow:
            dark, mid, bright = tone(eye, 1.1), tone(eye, 1.35), tone(eye, 1.7)
        for i in range(eye_w):
            cv.px(x0 + ex + i, y0 + eye_y + 1, dark)
            cv.px(x0 + ex + i, y0 + eye_y + 2, mid)
            cv.px(x0 + ex + i, y0 + eye_y + 3, bright)
        centre = ex + (eye_w - 1) // 2 + (1 if idx == 0 else 0)
        cv.px(x0 + centre, y0 + eye_y + 2, pupil)
        cv.px(x0 + ex + inner, y0 + eye_y + 2, white)
        cv.px(x0 + ex + inner, y0 + eye_y + 3, white)
        # catch-light
        cv.px(x0 + ex + (outer - outward), y0 + eye_y + 1, white if not glow else tone(eye, 1.8))
        if sleepy:
            # heavy, slightly lowered lid over the outer half of the iris
            for i in range(eye_w // 2 + 1):
                cv.px(x0 + ex + (outer - outward * i), y0 + eye_y + 1, lash)
        if lower_lash:
            cv.px(x0 + ex + outer, y0 + eye_y + 4, tone(lash, 1.7))
    # nose: one soft shadow
    cv.px(x0 + w // 2, y0 + eye_y + 5, tone(skin, 0.86))
    my = y0 + min(h - 2, eye_y + 7)
    lip = lip or mix(tone(skin, 0.78), (200, 96, 100, 255), 0.35)
    cx = x0 + w // 2
    if mouth == "small":
        cv.rect(cx - 1, my, 2, 1, lip)
    elif mouth == "smile":
        cv.px(cx - 2, my - 1, lip)
        cv.rect(cx - 1, my, 2, 1, lip)
        cv.px(cx + 1, my - 1, lip)
    elif mouth == "smirk":
        cv.rect(cx - 1, my, 2, 1, lip)
        cv.px(cx + 1, my - 1, lip)
    elif mouth == "fangs":
        cv.rect(cx - 2, my, 4, 1, tone(lip, 0.6))
        cv.px(cx - 2, my + 1, white)
        cv.px(cx + 1, my + 1, white)
    elif mouth == "stern":
        cv.rect(cx - 2, my, 4, 1, tone(skin, 0.62))
    elif mouth == "open":
        cv.rect(cx - 1, my, 2, 2, tone(lip, 0.55))
    if blush:
        for ex in (left, left + eye_w + gap):
            cv.rect(x0 + ex, y0 + eye_y + 5, 3, 1, mix(skin, (245, 140, 150, 255), 0.4))


def pixel_art(cv, r, rows, palette, flip=False):
    """
    Paints hand-drawn pixel art onto a face rectangle: rows are strings of palette keys ('.' leaves the texel
    untouched, ' ' clears it). Rows/columns beyond the rectangle are ignored, so templates can be reused on
    smaller faces. flip mirrors horizontally (left/right side faces).
    """
    x0, y0, w, h = r
    for yy, row in enumerate(rows[:h]):
        cells = row[::-1] if flip else row
        for xx, key in enumerate(cells[:w]):
            if key == ".":
                continue
            cv.px(x0 + xx, y0 + yy, (0, 0, 0, 0) if key == " " else palette[key])


EYE_STYLES = {
    # rows outer corner -> inner corner; L lash, l soft lash, I dark iris, i iris, j bright iris, p pupil, W white
    "calm": ["LLLL", "LIWI", "Iipi", "ljjW"],    # Frieren: heavy outer lid, half-closed and unbothered
    "soft": ["LLLL", "IWII", "iipi", "jjjW"],    # round and open
    "sharp": ["LLLL", "IWpI", "ljjW"],           # narrower, three rows (most men, hunters, generals)
    "fierce": ["LLL.", "LWpI", "lijW"],          # tilted, with an angry lash (pairs with brow_angle=1)
}


def face_template(skin, hair_c, eye, fringe=5, part=None, locks=(2, 10), eye_y=8, eye_w=4, gap=4, style="soft",
                  mouth="small", brow=None, lash=None, brow_angle=0, blush=False, glow=False, depth=None, seed=0):
    """
    Builds a hand-placed anime face for a 16x16 head front: a fringe of pointed locks (or an explicit per-column
    depth list), side locks, eyes from EYE_STYLES, thin brows, a one-pixel nose and a small mouth.
    Returns (rows, palette) for pixel_art().
    """
    rng = random.Random(seed)
    lock_w, lock_len = locks if locks else (0, 0)
    limit = max(2, eye_y - 1)                       # never let the fringe cover the lash row
    if depth is None:
        depth = [0] * 16
        x = lock_w
        while x < 16 - lock_w:
            lw = 3 if rng.random() < 0.6 else 2
            tip = fringe + (1 if rng.random() < 0.6 else 0)
            for i in range(lw):
                if x + i >= 16 - lock_w:
                    break
                edge = (i == 0 or i == lw - 1) and lw == 3
                depth[x + i] = tip - (1 if edge else 0)
            x += lw
        if part is not None and 0 <= part < 16:
            depth[part] = max(1, fringe - 2)
        for x in range(16):
            if x < lock_w or x >= 16 - lock_w:
                depth[x] = min(16, 4 + lock_len) if (x == 0 or x == 15) else min(16, 3 + lock_len)
            else:
                depth[x] = min(depth[x], limit)
    grid = [["S"] * 16 for _ in range(16)]
    for x, d in enumerate(depth):
        for y in range(min(16, d)):
            grid[y][x] = "H"
        side = x < lock_w or x >= 16 - lock_w
        if 0 < d <= 16 and not side:
            grid[d - 1][x] = "d"
        if d < 16:
            grid[d][x] = "s" if not side else "S"
        if side and x in (lock_w - 1, 16 - lock_w):
            for y in range(fringe, min(16, d)):
                grid[y][x] = "h"
    sprite = EYE_STYLES[style]
    left = (16 - (2 * eye_w + gap)) // 2
    for y, row in enumerate(sprite):
        cells = row if eye_w == 4 else row[0] + row[2:]
        for i, key in enumerate(cells):
            if key == ".":
                continue
            grid[eye_y + y][left + i] = key
            grid[eye_y + y][left + 2 * eye_w + gap - 1 - i] = key
    grid[eye_y][left - 1] = grid[eye_y][left + 2 * eye_w + gap] = "L"   # lash wings
    # brows: three pixels over the inner part of each eye, drawn only on skin
    by = eye_y - 2
    for idx, start in enumerate((left + 1, left + eye_w + gap)):
        for i in range(eye_w - 1):
            x = start + i
            inner = (i == eye_w - 2) if idx == 0 else (i == 0)
            y = by + (brow_angle if inner else 0)
            if 0 <= y < 16 and grid[y][x] in ("S", "s"):
                grid[y][x] = "B"
    grid[min(15, eye_y + 5)][8] = "n"
    my = min(15, eye_y + 6)
    if mouth == "small":
        grid[my][7] = grid[my][8] = "m"
    elif mouth == "smile":
        grid[my - 1][6] = grid[my - 1][9] = "m"
        grid[my][7] = grid[my][8] = "m"
    elif mouth == "smirk":
        grid[my][7] = grid[my][8] = "m"
        grid[my - 1][9] = "m"
    elif mouth == "fangs":
        for x in (6, 7, 8, 9):
            grid[my][x] = "M"
        if my + 1 < 16:
            grid[my + 1][6] = grid[my + 1][9] = "W"
    elif mouth == "stern":
        for x in (6, 7, 8, 9):
            grid[my][x] = "M"
    elif mouth == "open":
        grid[my][7] = grid[my][8] = "M"
        if my + 1 < 16:
            grid[my + 1][7] = grid[my + 1][8] = "M"
    if blush:
        for x in (left, left + 1, left + 2 * eye_w + gap - 2, left + 2 * eye_w + gap - 1):
            if grid[min(15, eye_y + 5)][x] == "S":
                grid[min(15, eye_y + 5)][x] = "b"
    lash = lash or tone(skin, 0.28)
    palette = {
        "H": hair_c, "h": tone(hair_c, 0.88), "d": tone(hair_c, 0.74),
        "S": skin, "s": tone(skin, 0.9), "n": tone(skin, 0.84),
        "m": mix(tone(skin, 0.78), (200, 96, 100, 255), 0.35), "M": tone(skin, 0.55),
        "L": lash, "l": mix(lash, skin, 0.5),
        "I": tone(eye, 0.55) if not glow else tone(eye, 1.05), "i": eye if not glow else tone(eye, 1.3),
        "j": tone(eye, 1.35) if not glow else tone(eye, 1.7), "p": tone(eye, 0.4), "W": (255, 255, 255, 255),
        "B": brow or tone(hair_c, 0.7), "b": mix(skin, (245, 140, 150, 255), 0.4),
    }
    return ["".join(r) for r in grid], palette
