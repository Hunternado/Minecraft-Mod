"""
Software preview renderer for box models (tools/models/boxmodel.py): orthographic, z-buffered, nearest-texel
sampling, back-face culling and simple directional shading similar to entity lighting. Produces turnaround sheets
so models and skins can be judged without starting the game.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
from boxmodel import apply, cube_faces, mat_identity, part_matrix  # noqa: E402

LIGHT = np.array([0.35, -0.85, -0.4])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def _view(yaw, pitch):
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def f(p):
        x, y, z = p
        # yaw around +y, then pitch around +x (looking slightly down when pitch > 0)
        x, z = x * cy - z * sy, x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return x, y, z
    return f


def collect_faces(model, pose=None, hidden=()):
    out = []

    def visit(part, parent):
        m = part_matrix(parent, part, pose)
        if part.name in hidden:
            return
        for cube in part.cubes:
            for c0, cs, ct, rect, name in cube_faces(cube):
                if rect[2] <= 0 or rect[3] <= 0:
                    continue
                flat = cube.w == 0 or cube.h == 0 or cube.d == 0
                out.append((apply(m, c0), apply(m, cs), apply(m, ct), rect, flat))
        for child in part.children:
            visit(child, m)
    visit(model.root, mat_identity())
    return out


def render(model, texture, yaw=0.0, pitch=10.0, scale=8, pose=None, hidden=(), background=(0, 0, 0, 0), shade=True):
    tex = np.asarray(texture.convert("RGBA"), dtype=np.float32)
    th, tw = tex.shape[0], tex.shape[1]
    sx, sy = tw / model.tex_w, th / model.tex_h
    view = _view(yaw, pitch)
    faces = []
    for c0, cs, ct, rect, flat in collect_faces(model, pose, hidden):
        faces.append((np.array(view(c0)), np.array(view(cs)), np.array(view(ct)), rect, flat))
    if not faces:
        return Image.new("RGBA", (16, 16), background)
    pts = np.array([p for f in faces for p in f[:3]] + [f[1] + f[2] - f[0] for f in faces])
    minx, miny = pts[:, 0].min() - 2, pts[:, 1].min() - 2
    maxx, maxy = pts[:, 0].max() + 2, pts[:, 1].max() + 2
    W, H = int((maxx - minx) * scale) + 1, int((maxy - miny) * scale) + 1
    color = np.zeros((H, W, 4), dtype=np.float32)
    color[:] = background
    depth = np.full((H, W), np.inf, dtype=np.float32)

    for p0, ps, pt, (u0, v0, fw, fh), flat in faces:
        e1, e2 = ps - p0, pt - p0
        # Model space is y-down, so the outward normal is the negated cross product of the UV edges.
        normal = -np.cross(e1, e2)
        n_len = np.linalg.norm(normal)
        if n_len < 1e-9:
            continue
        normal = normal / n_len
        if normal[2] > 0 and not flat:
            continue  # back face: the camera sits at -z looking toward +z
        a = np.array([[e1[0], e2[0]], [e1[1], e2[1]]])
        det = np.linalg.det(a)
        if abs(det) < 1e-9:
            continue
        inv = np.linalg.inv(a)
        quad = np.array([p0, ps, pt, ps + pt - p0])
        bx0 = max(int((quad[:, 0].min() - minx) * scale), 0)
        bx1 = min(int((quad[:, 0].max() - minx) * scale) + 1, W)
        by0 = max(int((quad[:, 1].min() - miny) * scale), 0)
        by1 = min(int((quad[:, 1].max() - miny) * scale) + 1, H)
        if bx1 <= bx0 or by1 <= by0:
            continue
        gx, gy = np.meshgrid(np.arange(bx0, bx1) + 0.5, np.arange(by0, by1) + 0.5)
        px = gx / scale + minx - p0[0]
        py = gy / scale + miny - p0[1]
        s = inv[0, 0] * px + inv[0, 1] * py
        t = inv[1, 0] * px + inv[1, 1] * py
        inside = (s >= 0) & (s < 1) & (t >= 0) & (t < 1)
        if not inside.any():
            continue
        z = p0[2] + s * e1[2] + t * e2[2]
        tu = np.clip(((u0 + s * fw) * sx).astype(int), 0, tw - 1)
        tv = np.clip(((v0 + t * fh) * sy).astype(int), 0, th - 1)
        texel = tex[tv, tu]
        region = depth[by0:by1, bx0:bx1]
        mask = inside & (texel[..., 3] > 0) & (z < region)
        if not mask.any():
            continue
        light = 1.0
        if shade:
            n = normal if normal[2] <= 0 else -normal
            light = 0.62 + 0.38 * max(0.0, float(-np.dot(n, LIGHT)))
        rgb = texel[..., :3] * light
        target = color[by0:by1, bx0:bx1]
        target[mask, :3] = rgb[mask]
        target[mask, 3] = 255
        region[mask] = z[mask]
    return Image.fromarray(np.clip(color, 0, 255).astype(np.uint8), "RGBA")


def turnaround(model, texture, scale=8, pose=None, hidden=(), angles=(0, 45, 90, 180, 315), pitch=8, label=None,
               background=(52, 56, 66, 255)):
    """Several views side by side on a dark backdrop."""
    views = [render(model, texture, yaw=a, pitch=pitch, scale=scale, pose=pose, hidden=hidden) for a in angles]
    w = sum(v.width for v in views) + 16 * (len(views) + 1)
    h = max(v.height for v in views) + 32 + (18 if label else 0)
    sheet = Image.new("RGBA", (w, h), background)
    x = 16
    top = 16 + (18 if label else 0)
    for v in views:
        sheet.alpha_composite(v, (x, top + (h - 32 - (18 if label else 0) - v.height)))
        x += v.width + 16
    if label:
        ImageDraw.Draw(sheet).text((16, 6), label, fill=(235, 235, 240, 255))
    return sheet


def grid(images, columns, background=(52, 56, 66, 255), pad=12):
    rows = [images[i:i + columns] for i in range(0, len(images), columns)]
    w = max(sum(im.width for im in r) + pad * (len(r) + 1) for r in rows)
    h = sum(max(im.height for im in r) for r in rows) + pad * (len(rows) + 1)
    sheet = Image.new("RGBA", (w, h), background)
    y = pad
    for r in rows:
        x = pad
        for im in r:
            sheet.alpha_composite(im, (x, y))
            x += im.width + pad
        y += max(im.height for im in r) + pad
    return sheet
