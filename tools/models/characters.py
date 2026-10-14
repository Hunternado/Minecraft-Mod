"""
Character rigs. Every character shares one set of named parts (so one Java model class animates them all); parts a
character does not use simply have no cubes. Cubes get UVs from a deterministic shelf packer and a painter that
draws their faces into a 2x-density texture (a 128x128 UV space, grown to 256x256 for rigs with many parts).

Part tree (Java names; nested parts are chains the model sways progressively):
  head -> hat, hair_top, bangs, lock_left -> lock_left_2, lock_right -> lock_right_2, hair_back -> hair_back_2,
          tail_left -> tail_left_2 -> tail_left_3, tail_right -> tail_right_2 -> tail_right_3,
          ear_left, ear_right, horn_left -> horn_left_2 -> horn_left_3, horn_right -> ..., crown
  body -> jacket, collar, cape -> cape_2, skirt -> skirt_2, coat_left, coat_right
  right_arm -> right_sleeve, prop        left_arm -> left_sleeve, prop_left
  right_leg -> right_pants               left_leg -> left_pants
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from boxmodel import Cube, Model, Part  # noqa: E402
from skinpaint import Canvas  # noqa: E402

TEX = 128      # UV space
DENSITY = 2    # texels per UV unit

def _chain(*names):
    """Nested dict for a chain of parts: a -> b -> c."""
    out = {}
    for name in reversed(names):
        out = {name: out}
    return out


PART_TREE = {
    "head": {"hat": {}, "hair_top": {}, "bangs": {}, **_chain("lock_left", "lock_left_2"),
             **_chain("lock_right", "lock_right_2"), **_chain("hair_back", "hair_back_2"),
             **_chain("tail_left", "tail_left_2", "tail_left_3"), **_chain("tail_right", "tail_right_2", "tail_right_3"),
             "ear_left": {}, "ear_right": {}, **_chain("horn_left", "horn_left_2", "horn_left_3"),
             **_chain("horn_right", "horn_right_2", "horn_right_3"), "crown": {}},
    "body": {"jacket": {}, "collar": {}, **_chain("cape", "cape_2"), **_chain("skirt", "skirt_2"),
             "coat_left": {}, "coat_right": {}},
    "right_arm": {"right_sleeve": {}, "prop": {}},
    "left_arm": {"left_sleeve": {}, "prop_left": {}},
    "right_leg": {"right_pants": {}},
    "left_leg": {"left_pants": {}},
}
ROOT_PARTS = list(PART_TREE.keys())


def walk_tree(tree=None, parent=None):
    """Yields (parent_name, name, subtree) depth-first, parents before children."""
    tree = PART_TREE if tree is None else tree
    for name, sub in tree.items():
        yield parent, name, sub
        yield from walk_tree(sub, name)


class Packer:
    """Shelf packer over the UV space; regions are 2*(d+w) by (d+h)."""

    def __init__(self, size=TEX):
        self.size = size
        self.shelves = []  # [y, height, next_x]
        self.next_y = 0

    def alloc(self, w, h, d):
        rw, rh = 2 * (d + w), d + h
        for shelf in self.shelves:
            y, sh, nx = shelf
            if rh <= sh and nx + rw <= self.size:
                shelf[2] += rw
                return nx, y
        if self.next_y + rh > self.size or rw > self.size:
            raise ValueError(f"texture full: cannot fit {rw}x{rh}")
        self.shelves.append([self.next_y, rh, rw])
        self.next_y += rh
        return 0, self.next_y - rh


def face_rects(cube):
    """Texel rectangles of each face (2x density)."""
    u, v, w, h, d = cube.u, cube.v, int(cube.w), int(cube.h), int(cube.d)
    k = DENSITY
    return {
        "top": (k * (u + d), k * v, k * w, k * d),
        "bottom": (k * (u + d + w), k * v, k * w, k * d),
        "right": (k * u, k * (v + d), k * d, k * h),
        "front": (k * (u + d), k * (v + d), k * w, k * h),
        "left": (k * (u + d + w), k * (v + d), k * d, k * h),
        "back": (k * (u + d + w + d), k * (v + d), k * w, k * h),
    }


class Rig:
    """A character: the part tree, pending cubes (UVs assigned on build) and their painters."""

    def __init__(self, name, render_scale=1.0):
        self.name = name
        self.render_scale = render_scale
        self.post_paint = None
        self.variants = {}  # extra texture name -> post-paint applied to a fresh copy of the base painting
        self.held = (None, None)  # (main hand, off hand) item ids the character is drawn holding
        self.carry = "hand"   # where the main-hand weapon rides out of combat: "hand", "back" (axes) or "hip" (swords)
        self.slim = False
        self.arm_len = 12
        self.root = Part("root")
        self.parts = {}
        self.pending = []  # (part, cube, painter)
        self.tex = TEX
        for parent, name, _ in walk_tree():
            holder = self.root if parent is None else self.parts[parent]
            self.parts[name] = holder.child(name)

    def pose(self, name, offset=None, rotation=None):
        part = self.parts[name]
        if offset is not None:
            part.offset = offset
        if rotation is not None:
            part.rotation = rotation
        return self

    def box(self, part, x, y, z, w, h, d, painter=None, inflate=0.0):
        cube = Cube(x, y, z, w, h, d, 0, 0, inflate)
        self.pending.append((self.parts[part], cube, painter))
        return cube

    def build(self):
        order = sorted(range(len(self.pending)),
                       key=lambda i: (-(self.pending[i][1].d + self.pending[i][1].h), i))
        for size in (TEX, TEX * 2):
            packer = Packer(size)
            try:
                for i in order:
                    part, cube, _ = self.pending[i]
                    cube.u, cube.v = packer.alloc(int(cube.w), int(cube.h), int(cube.d))
            except ValueError:
                continue
            self.tex = size
            break
        else:
            raise ValueError(f"{self.name}: texture full even at {TEX * 2}")
        for part, cube, _ in self.pending:
            part.cubes.append(cube)
        return Model(self.root, self.tex, self.tex)

    def paint(self, post=None):
        cv = Canvas(self.tex * DENSITY, self.tex * DENSITY)
        for part, cube, painter in self.pending:
            if painter:
                painter(cv, face_rects(cube))
        post = post or self.post_paint
        if post:
            post(cv)
        return cv


# ---------------------------------------------------------------------------------------------
# Standard bodies
# ---------------------------------------------------------------------------------------------
def humanoid(rig, painters, slim=False, legs=12, body_h=12, arm_len=12, headless=False):
    """
    Player-proportioned body. painters maps part names to painter callables. body_h/legs let dwarves and giants
    reuse the same rig; the arm/leg pivots follow.
    """
    arm_w = 3 if slim else 4
    leg_top = body_h
    rig.slim = slim
    rig.arm_len = arm_len
    rig.pose("head", offset=(0, 0, 0))
    rig.pose("body", offset=(0, 0, 0))
    rig.pose("right_arm", offset=(-5, 2 if not slim else 2.5, 0))
    rig.pose("left_arm", offset=(5, 2 if not slim else 2.5, 0))
    rig.pose("right_leg", offset=(-1.9, leg_top, 0))
    rig.pose("left_leg", offset=(1.9, leg_top, 0))
    if not headless:
        rig.box("head", -4, -8, -4, 8, 8, 8, painters.get("head"))
        rig.box("hat", -4, -8, -4, 8, 8, 8, painters.get("hat"), inflate=0.5)
    rig.box("body", -4, 0, -2, 8, body_h, 4, painters.get("body"))
    rig.box("jacket", -4, 0, -2, 8, body_h, 4, painters.get("jacket"), inflate=0.25)
    rx = -3 if not slim else -2
    rig.box("right_arm", rx, -2, -2, arm_w, arm_len, 4, painters.get("right_arm"))
    rig.box("right_sleeve", rx, -2, -2, arm_w, arm_len, 4, painters.get("right_sleeve"), inflate=0.25)
    rig.box("left_arm", -1, -2, -2, arm_w, arm_len, 4, painters.get("left_arm"))
    rig.box("left_sleeve", -1, -2, -2, arm_w, arm_len, 4, painters.get("left_sleeve"), inflate=0.25)
    rig.box("right_leg", -2, 0, -2, 4, legs, 4, painters.get("right_leg"))
    rig.box("right_pants", -2, 0, -2, 4, legs, 4, painters.get("right_pants"), inflate=0.25)
    rig.box("left_leg", -2, 0, -2, 4, legs, 4, painters.get("left_leg"))
    rig.box("left_pants", -2, 0, -2, 4, legs, 4, painters.get("left_pants"), inflate=0.25)
    return arm_w


def hand_prop_pivot(rig, slim, arm_len=12, side="right"):
    """Pivot of a held prop: the middle of the hand, so props point forward when the arm is raised to cast."""
    x = (-0.5 if slim else -1.0) if side == "right" else (0.5 if slim else 1.0)
    rig.pose("prop" if side == "right" else "prop_left", offset=(x, arm_len - 3, 0))


# ---------------------------------------------------------------------------------------------
# Preview poses (mirrors the Java idle / cast animation closely enough for judging art)
# ---------------------------------------------------------------------------------------------
def pose_idle(rig):
    return {
        "right_arm": {"rotation": (0.0, 0.0, 0.08)},
        "left_arm": {"rotation": (0.0, 0.0, -0.08)},
    }


def pose_cast(rig):
    return {
        "right_arm": {"rotation": (-math.pi / 2 + 0.15, -0.1, 0.0)},
        "left_arm": {"rotation": (-0.5, 0.25, -0.1)},
    }


def pose_walk(rig, phase=0.7):
    a = math.sin(phase) * 0.7
    return {
        "right_arm": {"rotation": (-a, 0.0, 0.08)},
        "left_arm": {"rotation": (a, 0.0, -0.08)},
        "right_leg": {"rotation": (a, 0.0, 0.0)},
        "left_leg": {"rotation": (-a, 0.0, 0.0)},
    }
