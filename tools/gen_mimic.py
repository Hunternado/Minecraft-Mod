"""
Generates the mimic's model layer and its 2x-density texture from one description, so UVs always match:
  * src/main/java/.../client/render/MimicLayer.java  (GENERATED: the LayerDefinition)
  * src/main/resources/assets/frieren/textures/entity/mimic.png  (256x256 for a 128x128 UV space)

Disguised, the mimic is an ordinary iron-banded chest. Revealed, its lid gapes to show rows of teeth, two fangs,
a lolling tongue and eyes glowing in its throat (MimicModel animates and toggles those parts).
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "models"))
from PIL import Image  # noqa: E402

from characters import DENSITY, Packer, face_rects  # noqa: E402
from boxmodel import Cube  # noqa: E402
from skinpaint import Canvas, fill, hexc, hline, mix, shade, vline  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
JAVA = os.path.join(REPO, "src/main/java/com/hunternado/frieren/client/render/MimicLayer.java")
TEXTURE = os.path.join(REPO, "src/main/resources/assets/frieren/textures/entity/mimic.png")
TEX = 128

WOOD = hexc("#8B5A2B")
WOOD_DARK = hexc("#5E3B1C")
IRON = hexc("#6E6E78")
GOLD = hexc("#D4AF37")
GUM = hexc("#7A1424")
THROAT = hexc("#1A0508")
TOOTH = hexc("#F2EEDC")
TONGUE = hexc("#C8455A")
EYE = hexc("#FFD24A")


# ---------------------------------------------------------------------------------------------
# Painters (each receives the six face rectangles of its cube)
# ---------------------------------------------------------------------------------------------
def planks(cv, r, seed, vertical=False):
    x0, y0, w, h = r
    fill(cv, r, WOOD, noise=0.06, seed=seed)
    step = 6
    if vertical:
        for x in range(step, w, step):
            vline(cv, r, x, WOOD_DARK)
    else:
        for y in range(step - 1, h, step):
            hline(cv, r, y, WOOD_DARK)


def bands(cv, r, top=True, bottom=True):
    x0, y0, w, h = r
    for x in (0, 1, w - 2, w - 1):
        vline(cv, r, x, shade(IRON, 1.1 if x in (0, w - 1) else 0.9))
    if top:
        hline(cv, r, 0, shade(IRON, 1.15))
        hline(cv, r, 1, IRON)
    if bottom:
        hline(cv, r, h - 2, IRON)
        hline(cv, r, h - 1, shade(IRON, 0.8))
    for (x, y) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        cv.px(x0 + x, y0 + y, shade(IRON, 1.4))


def paint_base(cv, f):
    for i, name in enumerate(("front", "back", "left", "right")):
        planks(cv, f[name], seed=10 + i)
        bands(cv, f[name])
    # bottom: plain dark planks
    planks(cv, f["bottom"], seed=20, vertical=True)
    # top: the mouth, seen when the lid opens
    x0, y0, w, h = f["top"]
    for y in range(h):
        for x in range(w):
            dx, dy = (x - w / 2) / (w / 2), (y - h * 0.55) / (h / 2)
            d = min(1.0, (dx * dx + dy * dy) ** 0.5)
            cv.px(x0 + x, y0 + y, mix(THROAT, GUM, d ** 1.6))
    # glossy wet highlights and the eyes deep in the throat
    for x in range(4, w - 4, 7):
        cv.px(x0 + x, y0 + h - 4, shade(GUM, 1.5))
    for ex in (w // 2 - 6, w // 2 + 3):
        for i in range(4):
            cv.px(x0 + ex + i, y0 + 6, EYE)
            cv.px(x0 + ex + i, y0 + 7, shade(EYE, 0.8))
        cv.px(x0 + ex + 1, y0 + 6, (255, 255, 255, 255))


def paint_lid(cv, f):
    for i, name in enumerate(("front", "back", "left", "right")):
        planks(cv, f[name], seed=30 + i)
        bands(cv, f[name], top=True, bottom=False)
    x0, y0, w, h = f["top"]
    planks(cv, f["top"], seed=40, vertical=True)
    bands(cv, f["top"])
    for y in range(h):  # cross band over the top
        cv.px(x0 + w // 2 - 1, y0 + y, IRON)
        cv.px(x0 + w // 2, y0 + y, shade(IRON, 0.85))
    # palate: dark gums with ridges
    x0, y0, w, h = f["bottom"]
    fill(cv, f["bottom"], GUM, noise=0.05, seed=41, gradient=-0.4)
    for y in range(3, h, 4):
        hline(cv, f["bottom"], y, shade(GUM, 0.7), x_from=3, x_to=w - 3)


def paint_latch(cv, f):
    for r in f.values():
        fill(cv, r, GOLD, noise=0.04, seed=50)
    x0, y0, w, h = f["front"]
    cv.px(x0 + w // 2, y0 + h // 2, shade(GOLD, 0.4))


def paint_tooth(cv, f):
    for name, r in f.items():
        fill(cv, r, TOOTH, noise=0.03, seed=60, gradient=0.15)
    for name in ("front", "back", "left", "right"):
        x0, y0, w, h = f[name]
        hline(cv, f[name], h - 1, shade(TOOTH, 0.75))


def paint_fang(cv, f):
    paint_tooth(cv, f)
    for name in ("front", "back"):
        x0, y0, w, h = f[name]
        cv.px(x0, y0 + h - 1, (0, 0, 0, 0))
        cv.px(x0 + w - 1, y0 + h - 1, (0, 0, 0, 0))


def paint_tongue(cv, f):
    for name, r in f.items():
        fill(cv, r, TONGUE, noise=0.05, seed=70)
    x0, y0, w, h = f["top"]
    vline(cv, f["top"], w // 2, shade(TONGUE, 0.75))
    vline(cv, f["top"], w // 2 - 1, shade(TONGUE, 0.85))
    for x in range(2, w - 2, 3):
        cv.px(x0 + x, y0 + 2, shade(TONGUE, 1.25))


# ---------------------------------------------------------------------------------------------
# Geometry: part -> (pivot, cubes); identical tooth cubes share one UV region
# ---------------------------------------------------------------------------------------------
def build():
    parts = {
        "body": ((0, 14, 0), [(-7, 0, -7, 14, 10, 14, paint_base)]),
        "lid": ((0, 14, 7), [(-7, -5, -14, 14, 5, 14, paint_lid), (-1, -2, -15, 2, 4, 1, paint_latch)]),
        "lower_teeth": ((0, 0, 0), []),
        "upper_teeth": ((0, 0, 0), []),
        # pivot just inside the front lip, so a droop hangs the tongue out over the edge instead of into the wall
        "tongue": ((0, -0.5, -5.5), [(-2, -0.6, -6, 4, 1, 7, paint_tongue), (-1.5, -0.6, -9, 3, 1, 3, paint_tongue)]),
    }
    for x in (-5.5, -3.5, -1.5, 0.5, 2.5, 4.5):
        parts["lower_teeth"][1].append((x, -2, -6.8, 1, 2, 1, paint_tooth))
    for z in (-4.5, -2.5, -0.5, 1.5):
        parts["lower_teeth"][1].append((-6.8, -2, z, 1, 2, 1, paint_tooth))
        parts["lower_teeth"][1].append((5.8, -2, z, 1, 2, 1, paint_tooth))
    for x in (-6.5, -0.5, 0.5 + 4, 5.5):
        parts["upper_teeth"][1].append((x, 0, -13.8, 1, 2, 1, paint_tooth))
    for x in (-4.5, 1.5):
        parts["upper_teeth"][1].append((x, 0, -13.9, 2, 3, 1, paint_fang))
    for z in (-11.5, -9.5, -7.5, -5.5):
        parts["upper_teeth"][1].append((-6.8, 0, z, 1, 2, 1, paint_tooth))
        parts["upper_teeth"][1].append((5.8, 0, z, 1, 2, 1, paint_tooth))

    packer = Packer(TEX)
    shared = {}
    placed = {}
    unique = []
    for name, (_, cubes) in parts.items():
        for spec in cubes:
            key = (spec[3], spec[4], spec[5], spec[6].__name__)
            if key not in shared:
                shared[key] = None
                unique.append(key)
    for key in sorted(unique, key=lambda k: -(k[2] + k[1])):
        w, h, d, _ = key
        shared[key] = packer.alloc(int(w), int(h), int(d))
    for name, (pivot, cubes) in parts.items():
        placed[name] = (pivot, [])
        for spec in cubes:
            x, y, z, w, h, d, painter = spec
            u, v = shared[(w, h, d, painter.__name__)]
            placed[name][1].append((Cube(x, y, z, w, h, d, u, v, 0.0), painter))
    return placed


def f(v):
    s = f"{float(v):.2f}".rstrip("0")
    return (s + "0" if s.endswith(".") else s) + "F"


def main():
    parts = build()
    cv = Canvas(TEX * DENSITY, TEX * DENSITY)
    painted = set()
    for name, (_, cubes) in parts.items():
        for cube, painter in cubes:
            key = (cube.u, cube.v)
            if key not in painted:
                painted.add(key)
                painter(cv, face_rects(cube))
    Image.fromarray(cv.a, "RGBA").save(TEXTURE)

    def cubes_java(cubes):
        out = "CubeListBuilder.create()"
        for cube, _ in cubes:
            out += (f"\n                .texOffs({cube.u}, {cube.v}).addBox({f(cube.x)}, {f(cube.y)}, {f(cube.z)}, "
                    f"{f(cube.w)}, {f(cube.h)}, {f(cube.d)}, CubeDeformation.NONE)")
        return out

    parent = {"body": "root", "lid": "root", "lower_teeth": "body", "upper_teeth": "lid", "tongue": "body"}
    lines = []
    for name in ("body", "lid", "lower_teeth", "upper_teeth", "tongue"):
        pivot, cubes = parts[name]
        var = {"body": "body", "lid": "lid"}.get(name)
        prefix = f"PartDefinition {var} = " if var else ""
        lines.append(f"        {prefix}{parent[name]}.addOrReplaceChild(\"{name}\", {cubes_java(cubes)},\n"
                     f"            PartPose.offset({f(pivot[0])}, {f(pivot[1])}, {f(pivot[2])}));")
    body = "\n".join(lines)
    java = f'''// GENERATED by tools/gen_mimic.py - edit the mimic there, not this file.
package com.hunternado.frieren.client.render;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Mimic geometry: chest body and lid, plus teeth, fangs and tongue that only show once it is revealed. */
final class MimicLayer {{
    private MimicLayer() {{}}

    static LayerDefinition create() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{body}
        return LayerDefinition.create(mesh, {TEX}, {TEX});
    }}
}}
'''
    with open(JAVA, "w") as out:
        out.write(java)
    print("generated mimic layer and texture")


if __name__ == "__main__":
    main()
