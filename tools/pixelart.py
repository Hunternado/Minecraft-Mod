"""
Tiny dependency-free PNG writer and pixel-art helpers used to generate the mod's original placeholder
textures. Every texture is drawn procedurally from shapes and palettes in gen_assets.py, so the art is
original, reproducible and easy to replace with hand-made art later.
"""
import math
import random
import struct
import zlib

Color = tuple  # (r, g, b, a)


def hex_color(value, alpha=255):
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def shade(color, factor):
    r, g, b, a = color
    return (max(0, min(255, int(r * factor))), max(0, min(255, int(g * factor))), max(0, min(255, int(b * factor))), a)


def mix(c1, c2, t):
    return tuple(int(c1[i] * (1 - t) + c2[i] * t) for i in range(4))


CLEAR = (0, 0, 0, 0)


class Image:
    def __init__(self, width, height, fill=CLEAR):
        self.width = width
        self.height = height
        self.pixels = [[fill for _ in range(width)] for _ in range(height)]

    def set(self, x, y, color):
        if 0 <= x < self.width and 0 <= y < self.height and color is not None:
            if len(color) == 4 and color[3] < 255 and color[3] > 0:
                base = self.pixels[y][x]
                if base[3] == 0:
                    self.pixels[y][x] = color
                else:
                    t = color[3] / 255.0
                    self.pixels[y][x] = (int(base[0] * (1 - t) + color[0] * t), int(base[1] * (1 - t) + color[1] * t),
                                         int(base[2] * (1 - t) + color[2] * t), 255)
            else:
                self.pixels[y][x] = color

    def get(self, x, y):
        return self.pixels[y][x]

    def rect(self, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, color)

    def outline_rect(self, x0, y0, x1, y1, color):
        for x in range(x0, x1 + 1):
            self.set(x, y0, color)
            self.set(x, y1, color)
        for y in range(y0, y1 + 1):
            self.set(x0, y, color)
            self.set(x1, y, color)

    def line(self, x0, y0, x1, y1, color):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.set(x0, y0, color)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def circle(self, cx, cy, r, color, fill=True):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                d = math.hypot(x - cx, y - cy)
                if (fill and d <= r) or (not fill and abs(d - r) < 0.6):
                    self.set(x, y, color)

    def noise_fill(self, x0, y0, x1, y1, base, spread=0.12, seed=0):
        rng = random.Random(seed)
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, shade(base, 1.0 + (rng.random() - 0.5) * 2 * spread))

    def outline_shape(self, color):
        """Adds a dark 1px outline around opaque pixels (classic item-icon look)."""
        opaque = [[self.pixels[y][x][3] > 0 for x in range(self.width)] for y in range(self.height)]
        for y in range(self.height):
            for x in range(self.width):
                if opaque[y][x]:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < self.width and 0 <= ny < self.height and opaque[ny][nx]:
                        self.pixels[y][x] = color
                        break

    def save(self, path):
        raw = bytearray()
        for row in self.pixels:
            raw.append(0)
            for (r, g, b, a) in row:
                raw += bytes((r, g, b, a))

        def chunk(tag, data):
            return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        png += chunk(b"IEND", b"")
        with open(path, "wb") as f:
            f.write(png)


def paint_box(img, u, v, w, h, d, faces):
    """
    Paints the six faces of a model cube using Minecraft's box UV layout.
    `faces` maps 'top','bottom','right','front','left','back' to callables (img, x0, y0, width, height).
    """
    regions = {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }
    for name, (x, y, fw, fh) in regions.items():
        painter = faces.get(name) or faces.get("all")
        if painter:
            painter(img, x, y, fw, fh)


def solid(color, spread=0.06, seed=1):
    def painter(img, x, y, w, h):
        img.noise_fill(x, y, x + w - 1, y + h - 1, color, spread, seed + x * 31 + y)
    return painter
