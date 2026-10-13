"""
Building blocks for less blocky characters:
  * cutout lock tips: transparent zigzags on the lower edge of hair faces, so hair ends in points, not lines;
  * plus-shaped segments: two crossing boxes give tails and braids a rounded (octagonal) cross-section;
  * thin plates: fringes, side locks and capes as 1-unit slabs that stand off the head or body;
  * chains: segments in nested parts (tail_left -> tail_left_2 -> ...) that the model bends progressively.
Everything paints through skinpaint's anime primitives, so it matches the faces and clothes.
"""
import random

from skinpaint import CLEAR, anime_hair, cel_fill, tone


def zigzag(cv, r, depth, seed=0, period=3, from_bottom=True):
    """Clears texels below a zigzag of pointed locks within the last `depth` rows of a face rectangle."""
    x0, y0, w, h = r
    if depth <= 0 or h <= 1:
        return
    rng = random.Random(seed)
    phase = rng.randint(0, period - 1)
    for xx in range(w):
        k = (xx + phase) % period
        tip = abs(k - (period - 1) / 2.0) / max(1.0, (period - 1) / 2.0)   # 0 at a lock's point, 1 between locks
        cut = int(round(tip * depth)) + (1 if rng.random() < 0.15 else 0)
        for i in range(min(h - 1, cut)):
            yy = (h - 1 - i) if from_bottom else i
            cv.px(x0 + xx, y0 + yy, CLEAR)


def soft_bundle(cv, r, hair_c, highlight_row=None):
    """A soft bundle of hair (tails, braids): flat base, one crease down the middle, a highlight band and tips
    that darken, instead of lock lines on every narrow face (which reads as ribbed pipes)."""
    x0, y0, w, h = r
    for yy in range(h):
        t = yy / max(1, h - 1)
        c = tone(hair_c, 1.0 - 0.12 * t * t)
        for xx in range(w):
            cv.px(x0 + xx, y0 + yy, c)
    if w >= 4:
        mid = w // 2
        for yy in range(h // 5, h):
            cv.px(x0 + mid, y0 + yy, tone(hair_c, 0.86 - 0.08 * yy / max(1, h)))
    if highlight_row is not None and 0 <= highlight_row < h:
        for xx in range(w):
            if xx != w // 2:
                cv.px(x0 + xx, y0 + highlight_row, tone(hair_c, 1.22))
    if w >= 2:
        for yy in range(h):
            cv.px(x0, y0 + yy, tone(cv.get(x0, y0 + yy), 0.94))


def hair_painter(hair_c, seed=0, highlight_row=2, tips=0, tip_faces=("front", "back", "left", "right"),
                 bottom=True, strands=True, soft=False):
    """Hair box painter: clean locks with a highlight band (soft=True: a smooth bundle), optional pointed tips cut
    into the lower edge."""
    def painter(cv, f):
        for i, (name, r) in enumerate(f.items()):
            if name == "bottom" and not bottom:
                continue
            hl = highlight_row if name != "top" else None
            if soft and name not in ("top", "bottom"):
                soft_bundle(cv, r, hair_c, hl)
            else:
                anime_hair(cv, r, hair_c, seed=seed + i, highlight_row=hl,
                           strands=strands and name not in ("top", "bottom"))
            if tips and name in tip_faces:
                zigzag(cv, r, tips, seed=seed + 7 * i)
    return painter


def cloth_painter(color, gradient=0.08, trim=None, trim_rows=1, folds=0.0, tips=0, extra=None):
    """Plain cloth box: flat colour, one shadow band, optional trim along the lower edge and a frayed/cut hem."""
    def painter(cv, f):
        for name, r in f.items():
            cel_fill(cv, r, color if name != "bottom" else tone(color, 0.7), gradient=gradient if name not in
                     ("top", "bottom") else 0.0, vertical_streaks=folds)
            if trim and name not in ("top", "bottom"):
                for t in range(trim_rows):
                    for xx in range(r[2]):
                        cv.px(r[0] + xx, r[1] + r[3] - 1 - t, trim if t == 0 else tone(trim, 0.9))
            if tips and name not in ("top", "bottom"):
                zigzag(cv, r, tips, seed=len(name))
        if extra:
            extra(cv, f)
    return painter


def plus_box(rig, part, x, y, z, w, h, d, painter):
    """A w x h x d volume with its four vertical edges bevelled: two crossing boxes (w x h x d-1, w-1 x h x d)."""
    rig.box(part, x, y, z + 0.5, w, h, d - 1, painter)
    rig.box(part, x + 0.5, y, z, w - 1, h, d, painter)


def chain(rig, names, pivots, rotations):
    """Poses a chain of nested parts: pivots and rotations per link, each relative to its parent link."""
    for name, pivot, rot in zip(names, pivots, rotations):
        rig.pose(name, offset=pivot, rotation=rot)
