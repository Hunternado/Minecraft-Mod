"""
Generates magic visual-effect textures (white/greyscale so the game tints them per spell):
  * textures/particle/<family>_<n>.png and particles/<family>.json for the custom particle types
  * textures/effect/circle_<style>.png  magic circles (arcane, holy, demon, flora)
  * textures/effect/beam.png, hex_shield.png, glow.png, crystal.png
Drawn at 4x and downsampled for clean anti-aliased edges.
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

REPO = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(REPO, "src/main/resources/assets/frieren")
PARTICLE_TEX = os.path.join(ASSETS, "textures/particle")
EFFECT_TEX = os.path.join(ASSETS, "textures/effect")
PARTICLE_DEFS = os.path.join(ASSETS, "particles")
SS = 4  # supersampling


def canvas(size):
    return Image.new("RGBA", (size * SS, size * SS), (0, 0, 0, 0))


def finish(img, size):
    return img.resize((size, size), Image.LANCZOS)


def glow_dot(size, radius, falloff=2.2, core=0.35):
    img = Image.new("RGBA", (size * SS, size * SS), (0, 0, 0, 0))
    px = img.load()
    c = size * SS / 2
    r = radius * SS
    for y in range(size * SS):
        for x in range(size * SS):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c) / r
            if d < 1:
                a = (1 - d) ** falloff
                if d < core:
                    a = 1.0
                px[x, y] = (255, 255, 255, int(255 * min(1, a)))
    return finish(img, size)


# ---------------------------------------------------------------------------------------------
# Particles
# ---------------------------------------------------------------------------------------------
def spark_frames():
    return [glow_dot(16, r, falloff=1.8, core=0.25) for r in (7.5, 6.0, 4.5, 3.0)]


RUNE_STROKES = [
    [((0.2, 0.15), (0.8, 0.85)), ((0.8, 0.15), (0.2, 0.85)), ((0.5, 0.1), (0.5, 0.9))],
    [((0.5, 0.1), (0.5, 0.9)), ((0.5, 0.35), (0.8, 0.15)), ((0.5, 0.6), (0.2, 0.4))],
    [((0.2, 0.2), (0.8, 0.2)), ((0.8, 0.2), (0.5, 0.85)), ((0.5, 0.85), (0.2, 0.2))],
    [((0.3, 0.1), (0.3, 0.9)), ((0.3, 0.3), (0.75, 0.5)), ((0.75, 0.5), (0.3, 0.7))],
    [((0.15, 0.5), (0.85, 0.5)), ((0.5, 0.15), (0.5, 0.85)), ((0.25, 0.25), (0.75, 0.75))],
    [((0.2, 0.85), (0.5, 0.15)), ((0.5, 0.15), (0.8, 0.85)), ((0.32, 0.6), (0.68, 0.6))],
    [((0.5, 0.1), (0.85, 0.5)), ((0.85, 0.5), (0.5, 0.9)), ((0.5, 0.9), (0.15, 0.5)), ((0.15, 0.5), (0.5, 0.1))],
    [((0.25, 0.15), (0.25, 0.85)), ((0.75, 0.15), (0.75, 0.85)), ((0.25, 0.5), (0.75, 0.5)), ((0.5, 0.2), (0.5, 0.35))],
]


def rune_frames():
    out = []
    for strokes in RUNE_STROKES:
        img = canvas(16)
        d = ImageDraw.Draw(img)
        s = 16 * SS
        for (a, b) in strokes:
            d.line([(a[0] * s, a[1] * s), (b[0] * s, b[1] * s)], fill=(255, 255, 255, 255), width=int(1.6 * SS))
        glow = img.filter(ImageFilter.GaussianBlur(SS * 1.2))
        base = Image.alpha_composite(glow, img)
        out.append(finish(base, 16))
    return out


def petal_frames():
    out = []
    for i in range(4):
        img = canvas(16)
        d = ImageDraw.Draw(img)
        s = 16 * SS
        angle = i * 45
        petal = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        pd = ImageDraw.Draw(petal)
        pd.ellipse([s * 0.3, s * 0.15, s * 0.7, s * 0.85], fill=(255, 255, 255, 255))
        pd.line([(s * 0.5, s * 0.2), (s * 0.5, s * 0.8)], fill=(225, 225, 230, 255), width=SS)
        petal = petal.rotate(angle, resample=Image.BICUBIC)
        img.alpha_composite(petal)
        out.append(finish(img, 16))
    return out


def ring_frames():
    out = []
    for r in (2.5, 3.8, 5.0, 6.0, 6.8, 7.4):
        img = canvas(16)
        d = ImageDraw.Draw(img)
        c = 8 * SS
        w = max(1, int((1.8 - r / 8) * SS))
        a = int(255 * (1.15 - r / 8.0))
        d.ellipse([c - r * SS, c - r * SS, c + r * SS, c + r * SS], outline=(255, 255, 255, min(255, a)), width=w)
        out.append(finish(img, 16))
    return out


def hex_frames():
    out = []
    for fill_a in (90, 30):
        img = canvas(16)
        d = ImageDraw.Draw(img)
        c = 8 * SS
        r = 7 * SS
        pts = [(c + r * math.cos(math.radians(60 * k + 30)), c + r * math.sin(math.radians(60 * k + 30))) for k in range(6)]
        d.polygon(pts, fill=(255, 255, 255, fill_a), outline=(255, 255, 255, 255))
        d.line(pts + [pts[0]], fill=(255, 255, 255, 255), width=int(1.2 * SS))
        out.append(finish(img, 16))
    return out


def streak_frames():
    out = []
    for length in (14, 10):
        img = canvas(16)
        px = img.load()
        s = 16 * SS
        for y in range(s):
            for x in range(s):
                dx = abs(x + 0.5 - s / 2) / (1.6 * SS)
                dy = abs(y + 0.5 - s / 2) / (length / 2 * SS)
                v = max(0.0, 1 - dx) * max(0.0, 1 - dy ** 2)
                if v > 0:
                    px[x, y] = (255, 255, 255, int(255 * min(1, v * 1.4)))
        out.append(finish(img, 16))
    return out


def ember_frames():
    out = []
    rng = random.Random(4)
    for i in range(4):
        img = canvas(16)
        d = ImageDraw.Draw(img)
        s = 16 * SS
        size = (6 - i) * SS
        for k in range(5):
            ox = rng.uniform(-1.5, 1.5) * SS
            oy = rng.uniform(-2, 1) * SS
            d.ellipse([s / 2 - size / 2 + ox, s / 2 - size / 2 + oy, s / 2 + size / 2 + ox, s / 2 + size / 2 + oy],
                      fill=(255, 255, 255, 140))
        img = img.filter(ImageFilter.GaussianBlur(SS * 0.8))
        d = ImageDraw.Draw(img)
        d.ellipse([s / 2 - size / 4, s / 2 - size / 4, s / 2 + size / 4, s / 2 + size / 4], fill=(255, 255, 255, 255))
        out.append(finish(img, 16))
    return out


def frost_frames():
    out = []
    for arms in (6, 6, 4):
        img = canvas(16)
        d = ImageDraw.Draw(img)
        c = 8 * SS
        r = (7 if arms == 6 else 5) * SS
        for k in range(arms):
            a = math.radians(360 / arms * k + (15 if len(out) == 1 else 0))
            x, y = c + r * math.cos(a), c + r * math.sin(a)
            d.line([(c, c), (x, y)], fill=(255, 255, 255, 255), width=int(1.3 * SS))
            for t in (0.5, 0.75):
                bx, by = c + r * t * math.cos(a), c + r * t * math.sin(a)
                for side in (-1, 1):
                    aa = a + side * math.radians(40)
                    d.line([(bx, by), (bx + r * 0.25 * math.cos(aa), by + r * 0.25 * math.sin(aa))],
                           fill=(255, 255, 255, 230), width=SS)
        out.append(finish(img, 16))
    return out


def wisp_frames():
    out = []
    rng = random.Random(9)
    for i in range(4):
        img = canvas(16)
        d = ImageDraw.Draw(img)
        s = 16 * SS
        for k in range(7):
            r = rng.uniform(2, 5) * SS * (1 - i * 0.12)
            x = s / 2 + rng.uniform(-3, 3) * SS
            y = s / 2 + rng.uniform(-3, 3) * SS
            d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, 90 - i * 15))
        out.append(finish(img.filter(ImageFilter.GaussianBlur(SS * 1.4)), 16))
    return out


PARTICLES = {
    "mana_spark": spark_frames,
    "rune": rune_frames,
    "petal": petal_frames,
    "magic_ring": ring_frames,
    "hex": hex_frames,
    "light_streak": streak_frames,
    "ember": ember_frames,
    "frost": frost_frames,
    "demon_wisp": wisp_frames,
}


# ---------------------------------------------------------------------------------------------
# Magic circles (256x256, white; tinted in game)
# ---------------------------------------------------------------------------------------------
def polar(c, r, deg):
    a = math.radians(deg)
    return c + r * math.cos(a), c + r * math.sin(a)


def rune_text_ring(d, c, r, size, count, rng, width):
    for k in range(count):
        ang = 360 / count * k
        strokes = RUNE_STROKES[rng.randrange(len(RUNE_STROKES))]
        # local frame: tangent and radial axes
        t = math.radians(ang + 90)
        n = math.radians(ang)
        cx, cy = polar(c, r, ang)
        for (a, b) in strokes:
            pts = []
            for (u, v) in (a, b):
                du, dv = (u - 0.5) * size, (v - 0.5) * size
                pts.append((cx + du * math.cos(t) + dv * math.cos(n), cy + du * math.sin(t) + dv * math.sin(n)))
            d.line(pts, fill=(255, 255, 255, 255), width=width)


def circle_arcane():
    s = 256 * SS
    c = s / 2
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    w = int(2.2 * SS)
    rng = random.Random(11)
    for r, width in ((0.48, w), (0.44, int(w * 0.7)), (0.31, w), (0.27, int(w * 0.6)), (0.12, w)):
        rr = r * s
        d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=(255, 255, 255, 255), width=width)
    rune_text_ring(d, c, 0.375 * s, 0.07 * s, 18, rng, int(1.6 * SS))
    # hexagram
    for offset in (90, 270):
        pts = [polar(c, 0.27 * s, offset + 120 * k) for k in range(3)]
        d.polygon(pts, outline=(255, 255, 255, 255), width=int(1.8 * SS))
    # spokes
    for k in range(12):
        a, b = polar(c, 0.12 * s, 30 * k), polar(c, 0.27 * s, 30 * k)
        d.line([a, b], fill=(255, 255, 255, 140), width=SS)
    # ticks on the outer ring
    for k in range(72):
        a, b = polar(c, 0.44 * s, 5 * k), polar(c, (0.46 if k % 3 else 0.48) * s, 5 * k)
        d.line([a, b], fill=(255, 255, 255, 200), width=SS)
    return finish(add_glow(img), 256)


def circle_holy():
    s = 256 * SS
    c = s / 2
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    w = int(2.2 * SS)
    for r in (0.47, 0.42, 0.2):
        rr = r * s
        d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=(255, 255, 255, 255), width=w)
    # eight-pointed star of the Goddess
    for k in range(8):
        tip = polar(c, 0.42 * s, 45 * k - 90)
        l, r = polar(c, 0.13 * s, 45 * k - 90 - 22.5), polar(c, 0.13 * s, 45 * k - 90 + 22.5)
        d.polygon([tip, l, (c, c), r], outline=(255, 255, 255, 255), fill=(255, 255, 255, 40), width=int(1.6 * SS))
    rune_text_ring(d, c, 0.445 * s, 0.035 * s, 36, random.Random(5), SS)
    return finish(add_glow(img), 256)


def circle_demon():
    s = 256 * SS
    c = s / 2
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    w = int(2.4 * SS)
    rng = random.Random(23)
    # jagged outer ring
    pts = [polar(c, (0.47 if k % 2 else 0.43) * s, k * 7.5) for k in range(48)]
    d.polygon(pts, outline=(255, 255, 255, 255), width=w)
    rr = 0.36 * s
    d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=(255, 255, 255, 255), width=int(w * 0.8))
    # inverted pentagram
    star = [polar(c, 0.36 * s, 90 + 144 * k) for k in range(5)]
    d.line(star + [star[0]], fill=(255, 255, 255, 255), width=int(1.8 * SS))
    # horned eye in the centre
    d.ellipse([c - 0.07 * s, c - 0.04 * s, c + 0.07 * s, c + 0.04 * s], outline=(255, 255, 255, 255), width=int(1.6 * SS))
    d.ellipse([c - 0.02 * s, c - 0.02 * s, c + 0.02 * s, c + 0.02 * s], fill=(255, 255, 255, 255))
    rune_text_ring(d, c, 0.4 * s, 0.05 * s, 14, rng, int(1.4 * SS))
    return finish(add_glow(img), 256)


def circle_flora():
    s = 256 * SS
    c = s / 2
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    w = int(2 * SS)
    for r in (0.46, 0.4):
        rr = r * s
        d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=(255, 255, 255, 255), width=w)
    # petals
    for k in range(10):
        ang = 36 * k
        petal = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        pd = ImageDraw.Draw(petal)
        pd.ellipse([c - 0.05 * s, c - 0.38 * s, c + 0.05 * s, c - 0.1 * s], outline=(255, 255, 255, 255),
                   fill=(255, 255, 255, 50), width=int(1.6 * SS))
        img.alpha_composite(petal.rotate(ang, center=(c, c), resample=Image.BICUBIC))
    rr = 0.08 * s
    d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=(255, 255, 255, 255), fill=(255, 255, 255, 80), width=w)
    # vine dots
    for k in range(40):
        x, y = polar(c, 0.43 * s, 9 * k)
        d.ellipse([x - SS * 1.5, y - SS * 1.5, x + SS * 1.5, y + SS * 1.5], fill=(255, 255, 255, 220))
    return finish(add_glow(img), 256)


def add_glow(img):
    glow = img.filter(ImageFilter.GaussianBlur(SS * 3))
    glow2 = glow.point(lambda v: v)
    out = Image.alpha_composite(glow2, img)
    return out


def beam_texture():
    """32x32: solid bright core across the middle third, a soft halo to transparent edges across U; tiles along V."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for x in range(32):
        d = abs(x + 0.5 - 16) / 16
        core = 1.0 if d < 0.3 else max(0.0, 1 - (d - 0.3) / 0.25)
        halo = max(0.0, 1 - d) ** 1.5 * 0.75
        for y in range(32):
            ripple = 0.88 + 0.12 * math.sin((y / 32) * math.tau * 2 + x * 0.3)
            a = min(1.0, core + halo * ripple)
            px[x, y] = (255, 255, 255, int(255 * a))
    return img


def hex_shield_texture():
    """64x64 tileable hexagon lattice for barrier shells."""
    s = 64 * SS
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = s / 4 / math.sqrt(3) * 2
    w = r * math.sqrt(3)
    for row in range(-1, 6):
        for col in range(-1, 6):
            cx = col * w + (w / 2 if row % 2 else 0)
            cy = row * r * 1.5
            pts = [(cx + r * math.cos(math.radians(60 * k + 30)), cy + r * math.sin(math.radians(60 * k + 30))) for k in range(6)]
            d.polygon(pts, outline=(255, 255, 255, 255), fill=(255, 255, 255, 38), width=int(1.4 * SS))
    return add_glow(img).resize((64, 64), Image.LANCZOS)


def crystal_texture():
    """Faceted crystal face: bright core fading to the edges, a diagonal glint and faint inner facet lines."""
    n = 32
    img = Image.new("RGBA", (n, n))
    px = img.load()
    for y in range(n):
        for x in range(n):
            u, v = x / (n - 1), y / (n - 1)
            edge = min(u, 1 - u, v, 1 - v) * 2
            base = 150 + 105 * min(1.0, edge * 1.6)
            glint = max(0.0, 1.0 - abs((u + v) - 0.8) * 8) * 90
            facet = 30 if abs(u - 0.5) < 0.02 or abs(v - 0.62) < 0.02 else 0
            val = int(min(255, base + glint - facet))
            px[x, y] = (val, val, 255 if val > 200 else val + 40, 235)
    return img


def main():
    os.makedirs(PARTICLE_TEX, exist_ok=True)
    os.makedirs(EFFECT_TEX, exist_ok=True)
    os.makedirs(PARTICLE_DEFS, exist_ok=True)
    for family, frames_fn in PARTICLES.items():
        frames = frames_fn()
        names = []
        for i, frame in enumerate(frames):
            frame.save(os.path.join(PARTICLE_TEX, f"{family}_{i}.png"))
            names.append(f"frieren:{family}_{i}")
        with open(os.path.join(PARTICLE_DEFS, f"{family}.json"), "w") as f:
            json.dump({"textures": names}, f, indent=2)
            f.write("\n")
    circles = {"arcane": circle_arcane, "holy": circle_holy, "demon": circle_demon, "flora": circle_flora}
    for name, fn in circles.items():
        fn().save(os.path.join(EFFECT_TEX, f"circle_{name}.png"))
    beam_texture().save(os.path.join(EFFECT_TEX, "beam.png"))
    hex_shield_texture().save(os.path.join(EFFECT_TEX, "hex_shield.png"))
    glow_dot(64, 31, falloff=2.0, core=0.1).save(os.path.join(EFFECT_TEX, "glow.png"))
    crystal_texture().save(os.path.join(EFFECT_TEX, "crystal.png"))
    print(f"generated {len(PARTICLES)} particle families, {len(circles)} circles and effect textures")


if __name__ == "__main__":
    main()
