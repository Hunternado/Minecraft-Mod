"""
Spell icons for the grimoire and the HUD: one 16x16 picture per spell, drawn from simple shapes.

Shapes are rasterised at 4x and reduced by majority colour, so curves and diagonals come out as clean pixel art
rather than jagged lines; a dark outline is added last, like vanilla item icons, so every icon reads on both
parchment and the HUD's dark glass. Coordinates are in icon pixels (0..16, y down).
"""
import math

from pixelart import Image, hex_color, mix

SS = 4
N = 16
OUTLINE = (28, 22, 38, 255)
WHITE = (255, 255, 255, 255)
GOLD = hex_color("#F0C850")
GOLD_D = hex_color("#B08428")
GOLD_L = hex_color("#FFF0B0")
BROWN = hex_color("#7A5230")
BROWN_D = hex_color("#55361C")
WOOD = hex_color("#A8784A")
STONE = hex_color("#9A9AA4")
STONE_D = hex_color("#6A6A78")
STONE_L = hex_color("#C8C8D2")
IRON = hex_color("#D8DCE4")
GREEN = hex_color("#5FB040")
GREEN_D = hex_color("#3A7A28")
GREEN_L = hex_color("#A8E070")
RED = hex_color("#D83A30")
SKY = hex_color("#7FC8FF")
INK = hex_color("#2A1E14")


class Icon:
    def __init__(self):
        self.buf = [[None] * (N * SS) for _ in range(N * SS)]

    def _each(self, x0, y0, x1, y1):
        for sy in range(max(0, int(y0 * SS) - 1), min(N * SS, int(math.ceil(y1 * SS)) + 1)):
            for sx in range(max(0, int(x0 * SS) - 1), min(N * SS, int(math.ceil(x1 * SS)) + 1)):
                yield sx, sy, (sx + 0.5) / SS, (sy + 0.5) / SS

    def disc(self, cx, cy, r, c):
        for sx, sy, x, y in self._each(cx - r, cy - r, cx + r, cy + r):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                self.buf[sy][sx] = c
        return self

    def ellipse(self, cx, cy, rx, ry, c):
        for sx, sy, x, y in self._each(cx - rx, cy - ry, cx + rx, cy + ry):
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                self.buf[sy][sx] = c
        return self

    def ring(self, cx, cy, r, w, c, a0=None, a1=None):
        """A circular band of width w centred on radius r; optionally only between angles a0..a1 (degrees,
        0 = +x, counter-clockwise on screen means negative y)."""
        for sx, sy, x, y in self._each(cx - r - w, cy - r - w, cx + r + w, cy + r + w):
            d = math.hypot(x - cx, y - cy)
            if abs(d - r) <= w / 2:
                if a0 is not None:
                    ang = math.degrees(math.atan2(cy - y, x - cx)) % 360
                    lo, hi = a0 % 360, a1 % 360
                    inside = lo <= ang <= hi if lo <= hi else (ang >= lo or ang <= hi)
                    if not inside:
                        continue
                self.buf[sy][sx] = c
        return self

    def seg(self, x0, y0, x1, y1, w, c):
        """A capsule: line from (x0, y0) to (x1, y1) with round ends, w wide."""
        dx, dy = x1 - x0, y1 - y0
        ll = dx * dx + dy * dy or 1e-9
        r = w / 2
        for sx, sy, x, y in self._each(min(x0, x1) - r, min(y0, y1) - r, max(x0, x1) + r, max(y0, y1) + r):
            t = max(0.0, min(1.0, ((x - x0) * dx + (y - y0) * dy) / ll))
            if (x - x0 - t * dx) ** 2 + (y - y0 - t * dy) ** 2 <= r * r:
                self.buf[sy][sx] = c
        return self

    def poly(self, pts, c):
        xs, ys = [p[0] for p in pts], [p[1] for p in pts]
        for sx, sy, x, y in self._each(min(xs), min(ys), max(xs), max(ys)):
            inside = False
            j = len(pts) - 1
            for i in range(len(pts)):
                xi, yi = pts[i]
                xj, yj = pts[j]
                if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
                    inside = not inside
                j = i
            if inside:
                self.buf[sy][sx] = c
        return self

    def rect(self, x0, y0, x1, y1, c):
        return self.poly([(x0, y0), (x1, y0), (x1, y1), (x0, y1)], c)

    def curve(self, fn, t0, t1, w, c, steps=24):
        """A thick polyline through fn(t) for t in t0..t1."""
        pts = [fn(t0 + (t1 - t0) * i / steps) for i in range(steps + 1)]
        for (ax, ay), (bx, by) in zip(pts, pts[1:]):
            self.seg(ax, ay, bx, by, w, c)
        return self

    def sparkle(self, cx, cy, r, c=WHITE):
        self.seg(cx - r, cy, cx + r, cy, 0.9, c)
        self.seg(cx, cy - r, cx, cy + r, 0.9, c)
        return self

    def clear(self, cx, cy, r):
        return self.disc(cx, cy, r, None)

    def finish(self, outline=OUTLINE):
        img = Image(N, N)
        for y in range(N):
            for x in range(N):
                counts = {}
                for sy in range(y * SS, y * SS + SS):
                    for sx in range(x * SS, x * SS + SS):
                        c = self.buf[sy][sx]
                        if c is not None:
                            counts[c] = counts.get(c, 0) + 1
                if counts and sum(counts.values()) >= SS * SS * 7 // 16:
                    img.pixels[y][x] = max(counts.items(), key=lambda kv: kv[1])[0]
        if outline:
            img.outline_shape(outline)
        return img


def tones(color):
    c = hex_color(color) if isinstance(color, str) else color
    return c, mix(c, WHITE, 0.55), mix(c, (0, 0, 0, 255), 0.35), mix(c, (0, 0, 0, 255), 0.6)


# ---------------------------------------------------------------------------------------------
# Shared motifs
# ---------------------------------------------------------------------------------------------
def beam(ic, c, l, d, width=2.6, origin=(4.5, 11.5), circle=3.2):
    ox, oy = origin
    ic.ring(ox, oy, circle, 1.1, d).disc(ox, oy, 1.3, l)
    ic.seg(ox + 0.6, oy - 0.6, 15.2, 0.8, width, c)
    ic.seg(ox + 0.8, oy - 0.8, 15.2, 0.8, max(0.9, width * 0.4), WHITE)


def flame(ic, cx, by, h, w, outer, inner, core=GOLD_L):
    ic.disc(cx, by - w * 0.55, w * 0.55, outer).poly([(cx - w * 0.5, by - w * 0.6), (cx, by - h), (cx + w * 0.5, by - w * 0.6)], outer)
    ic.disc(cx, by - w * 0.42, w * 0.36, inner).poly([(cx - w * 0.3, by - w * 0.45), (cx, by - h * 0.62), (cx + w * 0.3, by - w * 0.45)], inner)
    ic.disc(cx, by - w * 0.32, w * 0.16, core)


def hexagon(ic, cx, cy, r, c):
    ic.poly([(cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a))) for a in range(30, 390, 60)], c)


def shield(ic, cx, top, w, h, c):
    ic.poly([(cx - w / 2, top), (cx + w / 2, top), (cx + w / 2, top + h * 0.45), (cx, top + h), (cx - w / 2, top + h * 0.45)], c)


def arrow(ic, x0, y0, x1, y1, w, head, c):
    ic.seg(x0, y0, x1, y1, w, c)
    ang = math.atan2(y1 - y0, x1 - x0)
    lx, ly = x1 - head * math.cos(ang - 0.6), y1 - head * math.sin(ang - 0.6)
    rx, ry = x1 - head * math.cos(ang + 0.6), y1 - head * math.sin(ang + 0.6)
    ic.poly([(x1 + 0.6 * math.cos(ang), y1 + 0.6 * math.sin(ang)), (lx, ly), (rx, ry)], c)


def figure(ic, cx, top, c, scale=1.0):
    s = scale
    ic.disc(cx, top + 2.2 * s, 2.0 * s, c)
    ic.poly([(cx - 3.4 * s, top + 10.5 * s), (cx - 2.6 * s, top + 5.0 * s), (cx, top + 4.2 * s), (cx + 2.6 * s, top + 5.0 * s),
             (cx + 3.4 * s, top + 10.5 * s)], c)


# ---------------------------------------------------------------------------------------------
# One painter per spell
# ---------------------------------------------------------------------------------------------
def zoltraak(c, l, d, dd):
    ic = Icon()
    beam(ic, c, l, d)
    return ic


def zoltraak_barrage(c, l, d, dd):
    ic = Icon()
    for ox, oy in ((2.5, 13.5), (6.5, 14.5), (2.0, 9.0)):
        ic.disc(ox, oy, 1.4, d)
    for (x0, y0, x1, y1) in ((3, 13, 9, 7), (7, 14, 14, 7), (2.5, 8.5, 9, 2)):
        ic.seg(x0, y0, x1, y1, 2.0, c).seg(x0 + 0.5, y0 - 0.5, x1, y1, 0.8, WHITE)
    return ic


def qual_zoltraak(c, l, d, dd):
    ic = Icon()
    ic.ring(4.5, 11.5, 3.4, 1.2, dd).disc(4.5, 11.5, 1.4, hex_color("#E04848"))
    ic.seg(5.0, 11.0, 15.2, 0.8, 2.8, c).seg(5.2, 10.8, 15.2, 0.8, 1.0, hex_color("#FFB0D8"))
    return ic


def zoltraak_full_power(c, l, d, dd):
    ic = Icon()
    ic.ring(4.0, 12.0, 3.6, 1.2, d).ring(4.0, 12.0, 1.8, 0.8, l)
    ic.seg(4.5, 11.5, 15.0, 1.0, 5.2, l).seg(4.5, 11.5, 15.0, 1.0, 3.4, c).seg(4.8, 11.2, 15.0, 1.0, 1.6, WHITE)
    return ic


def vollzanbel(c, l, d, dd):
    ic = Icon()
    flame(ic, 8, 15.2, 14.5, 10.5, c, hex_color("#FFC040"))
    return ic


def judradjim(c, l, d, dd):
    ic = Icon()
    ic.poly([(10.5, 0.5), (3.5, 8.8), (7.6, 8.8), (4.5, 15.5), (13.0, 6.4), (8.8, 6.4), (12.5, 0.5)], c)
    ic.poly([(10.6, 1.6), (5.6, 8.0), (8.4, 8.0), (6.6, 12.6), (11.2, 7.2), (8.0, 7.2), (11.4, 1.6)], WHITE)
    return ic


def waldgose(c, l, d, dd):
    ic = Icon()
    ic.curve(lambda t: (8 + (1.2 + t * 5.6) * math.cos(t * 5.2), 8 + (1.2 + t * 5.6) * math.sin(t * 5.2)), 0, 1, 1.7, c, 40)
    ic.curve(lambda t: (8 + (0.8 + t * 3.6) * math.cos(t * 5.2 + 3.1), 8 + (0.8 + t * 3.6) * math.sin(t * 5.2 + 3.1)), 0, 1, 1.2, l, 30)
    return ic


def catastravia(c, l, d, dd):
    ic = Icon()
    for x, y0 in ((3.5, 0.8), (8.0, 2.8), (12.5, 0.4)):
        arrow(ic, x, y0, x, y0 + 11.0, 1.4, 3.0, c)
        ic.seg(x, y0, x, y0 + 7.5, 0.6, WHITE)
    return ic


def reelseiden(c, l, d, dd):
    ic = Icon()
    ic.disc(7.0, 9.0, 7.4, c).disc(4.4, 6.4, 7.0, None)
    ic.disc(7.0, 9.0, 6.6, WHITE).disc(4.0, 6.0, 6.9, None)
    ic.sparkle(13.6, 2.6, 1.8)
    return ic


def diagolze(c, l, d, dd):
    ic = Icon()
    figure(ic, 8, 2.0, c, 1.15)
    ic.poly([(4.2, 9.0), (8.0, 7.2), (11.8, 9.0), (11.0, 14.5), (5.0, 14.5)], d)
    ic.rect(3.0, 14.0, 13.0, 15.6, GOLD_D)
    ic.sparkle(12.6, 3.0, 1.6, GOLD_L).disc(6.6, 3.6, 0.6, GOLD_L)
    return ic


def defensive_magic(c, l, d, dd):
    ic = Icon()
    hexagon(ic, 8, 8, 7.2, d)
    hexagon(ic, 8, 8, 6.0, c)
    hexagon(ic, 8, 8, 3.4, l)
    ic.disc(6.6, 6.4, 0.9, WHITE)
    return ic


def barrier_dome(c, l, d, dd):
    ic = Icon()
    ic.ring(8, 13, 6.8, 1.6, c, 0, 180).ring(8, 13, 3.6, 1.0, l, 0, 180)
    ic.seg(8, 6.2, 8, 13, 0.9, l).seg(3.2, 8.6, 8, 13, 0.9, l).seg(12.8, 8.6, 8, 13, 0.9, l)
    ic.rect(0.6, 13.2, 15.4, 15.0, d)
    return ic


def goddess_healing(c, l, d, dd):
    ic = Icon()
    ic.rect(5.6, 1.8, 10.4, 14.2, c).rect(1.8, 5.6, 14.2, 10.4, c)
    ic.rect(6.8, 3.0, 9.2, 13.0, l).rect(3.0, 6.8, 13.0, 9.2, l)
    ic.rect(7.4, 7.4, 8.6, 8.6, WHITE)
    return ic


def curse_purification(c, l, d, dd):
    ic = Icon()
    ic.disc(8, 10.2, 4.8, c).poly([(3.4, 9.2), (8, 1.0), (12.6, 9.2)], c)
    ic.disc(8, 10.6, 3.0, l).disc(6.4, 9.0, 1.0, WHITE)
    ic.sparkle(13.4, 3.0, 1.5, GOLD_L)
    return ic


def flight_magic(c, l, d, dd):
    ic = Icon()
    for (x1, y1, w, col) in ((14.5, 2.0, 2.4, c), (14.6, 5.6, 2.2, l), (13.6, 9.0, 2.0, c), (11.6, 12.0, 1.8, l)):
        ic.seg(3.0, 12.5, x1, y1, w, col)
    ic.disc(3.0, 12.6, 2.2, d)
    return ic


def flower_field(c, l, d, dd):
    ic = Icon()
    ic.seg(8, 9, 8.6, 15.5, 1.2, GREEN).ellipse(10.6, 12.6, 2.2, 1.0, GREEN_L)
    for k in range(5):
        a = math.radians(-90 + k * 72)
        ic.disc(8 + 3.1 * math.cos(a), 6.6 + 3.1 * math.sin(a), 2.4, c)
    ic.disc(8, 6.6, 1.7, GOLD).disc(7.6, 6.2, 0.6, GOLD_L)
    return ic


def mimic_detection(c, l, d, dd):
    ic = Icon()
    ic.rect(1.5, 7.0, 11.5, 14.5, WOOD).rect(1.5, 5.0, 11.5, 8.0, BROWN).rect(5.6, 7.0, 7.4, 9.6, GOLD)
    ic.ring(10.8, 6.0, 3.4, 1.4, c).disc(10.8, 6.0, 2.6, mix(c, WHITE, 0.4)).seg(13.2, 8.4, 15.2, 10.6, 1.6, d)
    return ic


def clothes_cleaning(c, l, d, dd):
    ic = Icon()
    ic.poly([(4.8, 2.0), (6.6, 2.0), (8.0, 3.6), (9.4, 2.0), (11.2, 2.0), (15.0, 5.4), (13.0, 7.8), (11.6, 6.6), (11.6, 14.5),
             (4.4, 14.5), (4.4, 6.6), (3.0, 7.8), (1.0, 5.4)], c)
    ic.rect(5.6, 8.0, 10.4, 13.4, l)
    ic.sparkle(12.8, 11.6, 1.6).sparkle(3.4, 11.0, 1.1)
    return ic


def rust_removal(c, l, d, dd):
    ic = Icon()
    ic.seg(2.6, 13.4, 12.6, 3.4, 2.8, IRON).seg(3.2, 12.8, 12.6, 3.4, 0.9, WHITE)
    for x, y in ((4.2, 11.0), (5.8, 10.6), (3.6, 12.6)):
        ic.disc(x, y, 0.9, c)
    ic.seg(1.0, 15.0, 3.4, 12.6, 1.6, BROWN).seg(1.6, 11.6, 4.4, 14.4, 1.2, GOLD_D)
    ic.sparkle(13.6, 2.4, 1.8)
    return ic


def seeking_magic(c, l, d, dd):
    ic = Icon()
    ic.disc(8, 8, 6.8, d).disc(8, 8, 5.6, mix(c, WHITE, 0.6))
    ic.poly([(8, 2.6), (9.4, 8), (6.6, 8)], RED).poly([(8, 13.4), (9.4, 8), (6.6, 8)], c)
    ic.disc(8, 8, 0.9, GOLD)
    return ic


def light_orb(c, l, d, dd):
    ic = Icon()
    ic.ring(8, 8, 6.6, 0.8, l)
    ic.disc(8, 8, 4.8, c).disc(8, 8, 3.0, mix(c, WHITE, 0.6)).disc(6.8, 6.8, 1.3, WHITE)
    return ic


def gathering_magic(c, l, d, dd):
    ic = Icon()
    for a in range(45, 405, 90):
        r = math.radians(a)
        arrow(ic, 8 + 7.4 * math.cos(r), 8 + 7.4 * math.sin(r), 8 + 4.2 * math.cos(r), 8 + 4.2 * math.sin(r), 1.4, 3.2, c)
    ic.disc(8, 8, 2.6, l).disc(7.4, 7.4, 0.9, WHITE)
    return ic


def azeryuze(c, l, d, dd):
    ic = Icon()
    ic.seg(8, 1.8, 8, 13.6, 1.4, c).rect(4.8, 13.4, 11.2, 15.2, d)
    ic.seg(2.4, 4.0, 13.6, 4.0, 1.2, c).disc(8, 2.0, 1.2, l)
    for x in (3.0, 13.0):
        ic.seg(x, 4.0, x - 1.6, 8.6, 0.6, d).seg(x, 4.0, x + 1.6, 8.6, 0.6, d)
        ic.ring(x, 8.6, 2.4, 1.2, c, 180, 360)
    return ic


def lands_doppelganger(c, l, d, dd):
    ic = Icon()
    figure(ic, 10.4, 3.0, mix(c, WHITE, 0.45), 1.05)
    figure(ic, 5.6, 4.0, d, 1.05)
    return ic


def frost_lance(c, l, d, dd):
    ic = Icon()
    ic.poly([(0.8, 15.2), (2.6, 11.0), (12.6, 2.0), (15.4, 0.6), (14.0, 3.4), (5.0, 13.4)], c)
    ic.poly([(2.0, 13.8), (12.8, 2.6), (14.6, 1.4), (4.0, 12.6)], WHITE)
    ic.poly([(1.4, 4.0), (3.4, 1.4), (5.4, 3.8), (3.2, 6.2)], l).poly([(10.4, 12.0), (12.8, 10.2), (14.2, 12.6), (11.8, 14.6)], l)
    return ic


def earth_spikes(c, l, d, dd):
    ic = Icon()
    for (x, w, h) in ((4.0, 5.0, 10.0), (11.0, 5.2, 13.0), (7.6, 4.4, 7.0)):
        ic.poly([(x - w / 2, 14.2), (x, 14.2 - h), (x + w / 2, 14.2)], c)
        ic.poly([(x - w / 2 + 0.6, 14.2), (x - 0.2, 14.2 - h + 1.4), (x, 14.2)], l)
    ic.rect(0.4, 13.6, 15.6, 15.6, d)
    return ic


def water_whip(c, l, d, dd):
    ic = Icon()
    ic.curve(lambda t: (1.6 + t * 13.0, 9.0 + 4.2 * math.sin(t * 7.0) * (1.0 - 0.5 * t)), 0, 1, 2.4, c, 40)
    ic.curve(lambda t: (1.8 + t * 12.8, 8.4 + 4.2 * math.sin(t * 7.0) * (1.0 - 0.5 * t)), 0, 1, 0.8, l, 40)
    ic.disc(14.6, 6.6, 1.0, l).disc(12.6, 3.4, 0.7, c)
    return ic


def mana_release(c, l, d, dd):
    ic = Icon()
    for k in range(8):
        a = math.radians(k * 45 + 22.5)
        ic.seg(8 + 3.0 * math.cos(a), 8 + 3.0 * math.sin(a), 8 + 7.2 * math.cos(a), 8 + 7.2 * math.sin(a), 1.3, c)
    ic.disc(8, 8, 3.4, l).disc(8, 8, 1.8, WHITE)
    return ic


def blood_blades(c, l, d, dd):
    ic = Icon()
    edge = hex_color("#FF8090")
    ic.disc(9.0, 9.4, 6.6, c).disc(11.4, 7.0, 6.4, None)
    ic.disc(9.0, 9.4, 5.8, edge).disc(11.2, 6.8, 6.4, None)
    ic.disc(11.6, 5.4, 4.2, c).disc(13.2, 3.8, 4.0, None)
    ic.disc(2.6, 14.2, 0.9, c).disc(5.0, 15.2, 0.6, c)
    return ic


def wire_snare(c, l, d, dd):
    ic = Icon()
    for (x0, y0, x1, y1) in ((0.8, 3.0, 15.2, 12.0), (0.8, 13.0, 15.2, 4.0), (8.0, 0.8, 8.0, 15.2), (0.8, 8.0, 15.2, 8.6)):
        ic.seg(x0, y0, x1, y1, 0.8, c)
    ic.disc(8, 8.2, 2.0, dd).disc(8, 8.2, 1.0, l)
    for x, y in ((3.2, 4.6), (12.6, 11.0), (12.8, 5.4), (3.4, 11.6)):
        ic.disc(x, y, 0.8, l)
    return ic


def sorganeil(c, l, d, dd):
    ic = Icon()
    ic.poly([(0.8, 8.0)] + [(8 + 7.2 * math.cos(math.radians(a)), 8 - 4.6 * math.sin(math.radians(a))) for a in range(0, 181, 15)], WHITE)
    ic.poly([(0.8, 8.0)] + [(8 + 7.2 * math.cos(math.radians(a)), 8 + 4.6 * math.sin(math.radians(a))) for a in range(0, 181, 15)], WHITE)
    ic.disc(8, 8, 3.4, c).disc(8, 8, 1.6, dd).disc(7.0, 7.0, 0.7, WHITE)
    return ic


def reflection_barrier(c, l, d, dd):
    ic = Icon()
    ic.ellipse(10.0, 8.0, 4.0, 6.8, d).ellipse(10.0, 8.0, 3.0, 5.8, c).seg(9.0, 4.0, 9.0, 8.0, 1.0, WHITE)
    arrow(ic, 1.0, 2.0, 5.6, 7.2, 1.2, 2.2, l)
    arrow(ic, 5.6, 8.8, 1.2, 14.0, 1.2, 2.2, GOLD)
    return ic


def goddess_sanctuary(c, l, d, dd):
    ic = Icon()
    ic.ellipse(8, 13.0, 7.0, 2.4, d).ellipse(8, 13.0, 5.6, 1.6, c)
    ic.rect(5.6, 0.8, 10.4, 12.6, l).rect(6.8, 0.8, 9.2, 12.6, WHITE)
    ic.sparkle(2.8, 5.0, 1.4, GOLD_L).sparkle(13.2, 3.6, 1.4, GOLD_L)
    return ic


def bottled_golem(c, l, d, dd):
    ic = Icon()
    glass = hex_color("#BFE6F0")
    ic.disc(8, 10.4, 5.2, glass).rect(6.2, 2.6, 9.8, 6.4, glass).rect(5.8, 0.8, 10.2, 3.0, BROWN)
    ic.rect(5.6, 8.0, 10.4, 13.6, STONE).rect(6.4, 9.4, 7.6, 10.4, hex_color("#7FE0FF")).rect(8.4, 9.4, 9.6, 10.4, hex_color("#7FE0FF"))
    ic.seg(4.6, 8.0, 4.6, 12.0, 0.9, WHITE)
    return ic


def wind_step(c, l, d, dd):
    ic = Icon()
    for (y, x0, x1, r) in ((3.6, 0.8, 9.6, 2.0), (8.4, 2.6, 12.6, 2.2), (13.0, 0.8, 8.4, 1.6)):
        ic.seg(x0, y, x1, y, 1.3, c if y != 8.4 else l)
        ic.ring(x1, y + r, r, 1.3, c if y != 8.4 else l, 270, 450)
    return ic


def harvest_magic(c, l, d, dd):
    ic = Icon()
    for (x1, y1) in ((4.0, 2.4), (8.0, 1.2), (12.0, 2.4)):
        ic.seg(8, 14.8, x1, y1 + 3.0, 1.0, GOLD_D)
        for k in range(3):
            ic.ellipse(x1 + (8 - x1) * k * 0.12, y1 + k * 1.9, 1.3, 1.0, GOLD)
    ic.rect(6.0, 9.6, 10.0, 11.2, c)
    return ic


def cooking_magic(c, l, d, dd):
    ic = Icon()
    ic.seg(9.6, 6.4, 13.6, 2.4, 1.6, WHITE).disc(14.0, 1.8, 1.2, WHITE).disc(14.6, 3.0, 1.0, WHITE)
    ic.ellipse(6.6, 9.6, 5.0, 4.4, hex_color("#B05A28")).ellipse(6.0, 9.0, 3.4, 2.8, c).disc(4.6, 7.8, 0.9, GOLD_L)
    return ic


def grape_sweetening(c, l, d, dd):
    ic = Icon()
    ic.poly([(8.0, 1.6), (13.4, 1.0), (11.0, 4.6)], GREEN_L).seg(8.0, 1.6, 8.0, 4.0, 0.9, BROWN)
    for (x, y) in ((5.4, 5.6), (8.2, 5.4), (11.0, 5.8), (6.8, 8.4), (9.6, 8.4), (8.2, 11.2), (5.6, 10.8), (10.8, 10.8), (8.2, 14.0)):
        ic.disc(x, y, 1.7, c)
        ic.disc(x - 0.5, y - 0.5, 0.5, l)
    ic.sparkle(2.4, 13.0, 1.4, GOLD_L)
    return ic


def clear_skies(c, l, d, dd):
    ic = Icon()
    sun = hex_color("#FFD84A")
    for k in range(8):
        a = math.radians(k * 45)
        ic.seg(6.4 + 4.0 * math.cos(a), 6.4 + 4.0 * math.sin(a), 6.4 + 6.0 * math.cos(a), 6.4 + 6.0 * math.sin(a), 1.1, sun)
    ic.disc(6.4, 6.4, 3.2, sun).disc(5.6, 5.6, 1.0, GOLD_L)
    ic.disc(10.0, 12.2, 2.6, WHITE).disc(13.0, 11.6, 2.2, WHITE).rect(7.6, 12.0, 15.4, 14.8, WHITE).disc(8.0, 13.0, 1.8, WHITE)
    return ic


def tree_felling(c, l, d, dd):
    ic = Icon()
    ic.disc(5.6, 11.0, 4.4, BROWN_D).disc(5.6, 11.0, 3.4, c).ring(5.6, 11.0, 1.8, 0.7, BROWN).disc(5.6, 11.0, 0.6, BROWN)
    ic.seg(8.0, 15.0, 14.0, 2.8, 1.4, WOOD)
    ic.poly([(11.4, 3.8), (15.4, 3.6), (15.2, 8.6), (12.4, 7.4)], IRON).seg(15.0, 4.0, 15.0, 8.2, 0.8, WHITE)
    return ic


def field_blessing(c, l, d, dd):
    ic = Icon()
    ic.rect(1.6, 12.4, 14.4, 15.4, BROWN).rect(1.6, 12.4, 14.4, 13.2, BROWN_D)
    ic.seg(8, 12.8, 8, 6.0, 1.2, GREEN_D)
    ic.ellipse(5.0, 6.8, 3.2, 1.8, c).ellipse(11.0, 5.6, 3.2, 1.8, GREEN_L).ellipse(8.0, 3.8, 1.6, 2.0, c)
    ic.sparkle(2.6, 2.6, 1.4, GOLD_L).sparkle(13.6, 10.0, 1.2, GOLD_L)
    return ic


def ore_finding(c, l, d, dd):
    ic = Icon()
    ic.ring(8, 9, 6.6, 1.0, d, 20, 160).ring(8, 9, 4.6, 1.0, c, 30, 150)
    ic.poly([(8, 5.6), (11.4, 9.0), (8, 15.0), (4.6, 9.0)], c).poly([(8, 5.6), (11.4, 9.0), (8, 9.8), (4.6, 9.0)], l)
    ic.disc(6.8, 8.2, 0.7, WHITE)
    return ic


def tunnel_carving(c, l, d, dd):
    ic = Icon()
    ic.rect(0.8, 1.6, 15.2, 15.2, STONE).rect(0.8, 1.6, 15.2, 3.0, STONE_L)
    for (x, y) in ((2.4, 5.0), (12.6, 4.4), (13.2, 11.6), (2.0, 12.4)):
        ic.rect(x, y, x + 1.4, y + 1.0, STONE_D)
    ic.disc(8, 8.6, 4.4, INK).rect(3.6, 8.6, 12.4, 15.2, INK)
    ic.disc(8, 9.0, 2.6, mix(INK, c, 0.35)).rect(5.4, 9.0, 10.6, 15.2, mix(INK, c, 0.35))
    return ic


def return_home(c, l, d, dd):
    ic = Icon()
    ic.poly([(1.2, 8.0), (8.0, 1.4), (14.8, 8.0)], RED).rect(3.0, 7.6, 13.0, 14.8, hex_color("#F0E2C0"))
    ic.rect(6.6, 10.0, 9.4, 14.8, BROWN).rect(10.4, 9.0, 12.0, 10.8, SKY)
    ic.sparkle(13.6, 2.4, 1.5, c)
    return ic


def light_bridge(c, l, d, dd):
    ic = Icon()
    ic.rect(0.4, 8.8, 3.6, 15.6, BROWN).rect(12.4, 8.8, 15.6, 15.6, BROWN).rect(0.4, 8.8, 3.6, 9.8, GREEN).rect(12.4, 8.8, 15.6, 9.8, GREEN)
    ic.ring(8.0, 16.0, 8.4, 2.0, c, 18, 162).ring(8.0, 16.0, 8.4, 0.7, WHITE, 30, 150)
    for x in (4.6, 8.0, 11.4):
        ic.seg(x, 8.4, x, 10.2, 0.6, l)
    return ic


def night_sight(c, l, d, dd):
    ic = Icon()
    ic.disc(7.0, 8.0, 6.0, c).disc(9.8, 6.4, 5.0, None)
    ic.sparkle(12.6, 11.6, 1.8, GOLD_L).disc(12.0, 3.2, 0.7, WHITE)
    return ic


def fish_breath(c, l, d, dd):
    ic = Icon()
    for (x, y, r) in ((6.0, 10.0, 4.0), (11.6, 5.2, 2.6), (12.2, 12.4, 1.8), (4.4, 3.2, 1.4)):
        ic.ring(x, y, r, 1.0, c).disc(x - r * 0.4, y - r * 0.4, max(0.5, r * 0.25), WHITE)
    return ic


def swift_hands(c, l, d, dd):
    ic = Icon()
    ic.seg(2.4, 13.8, 11.4, 4.6, 1.6, WOOD)
    ic.ring(8.2, 7.8, 6.6, 2.0, IRON, -5, 95).ring(8.2, 7.8, 6.6, 0.7, WHITE, 20, 70)
    for y, x0 in ((10.6, 9.0), (13.4, 7.4)):
        ic.seg(x0, y, x0 + 5.4, y, 1.1, c)
    return ic


def feather_fall(c, l, d, dd):
    ic = Icon()
    ic.poly([(13.6, 1.4), (15.0, 3.4), (6.0, 12.6), (3.2, 12.0), (3.6, 9.4)], c)
    ic.poly([(13.6, 1.4), (11.0, 2.0), (3.6, 9.4)], l)
    ic.seg(14.2, 2.0, 2.0, 14.2, 0.8, d)
    return ic


def flame_ward(c, l, d, dd):
    ic = Icon()
    shield(ic, 8, 1.2, 12.4, 14.4, hex_color("#5A6A8A"))
    shield(ic, 8, 2.4, 10.0, 11.8, hex_color("#8EA0C0"))
    flame(ic, 8, 12.4, 9.0, 6.4, c, GOLD)
    return ic


def smelting_magic(c, l, d, dd):
    ic = Icon()
    ic.poly([(2.6, 6.8), (13.4, 6.8), (15.0, 10.4), (1.0, 10.4)], GOLD).poly([(3.2, 6.8), (12.8, 6.8), (13.4, 8.0), (2.6, 8.0)], GOLD_L)
    flame(ic, 5.0, 15.6, 5.6, 4.2, c, GOLD)
    flame(ic, 11.0, 15.6, 5.0, 3.8, c, GOLD)
    ic.sparkle(13.6, 2.6, 1.6, GOLD_L)
    return ic


def pocket_space(c, l, d, dd):
    ic = Icon()
    ic.ellipse(8, 8, 7.0, 5.6, dd).ellipse(8, 8, 5.6, 4.2, c)
    ic.curve(lambda t: (8 + (0.6 + 3.8 * t) * math.cos(t * 6.0), 8 + (0.5 + 2.8 * t) * math.sin(t * 6.0)), 0, 1, 1.0, l, 30)
    ic.disc(8, 8, 1.2, WHITE).sparkle(14.0, 2.2, 1.4, l)
    return ic


def generic(c, l, d, dd):
    ic = Icon()
    ic.ring(8, 8, 6.2, 1.2, WHITE)
    for k in range(6):
        a1, a2 = math.radians(k * 60 - 90), math.radians(k * 60 + 30)
        ic.seg(8 + 5.6 * math.cos(a1), 8 + 5.6 * math.sin(a1), 8 + 5.6 * math.cos(a2), 8 + 5.6 * math.sin(a2), 0.8, WHITE)
    ic.disc(8, 8, 1.4, WHITE)
    return ic


PAINTERS = {name: fn for name, fn in globals().items() if callable(fn) and fn.__module__ == __name__
            and name not in ("tones", "beam", "flame", "hexagon", "shield", "arrow", "figure", "hex_color", "mix")
            and not isinstance(fn, type)}


def icon(spell_id, color):
    """The finished 16x16 icon for a spell, or the generic rune (white, meant to be tinted) when none is drawn."""
    fn = PAINTERS.get(spell_id, generic)
    return fn(*tones(color if fn is not generic else "#FFFFFF")).finish()
