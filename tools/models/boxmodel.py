"""
Minecraft-style box models in Python: the same parts, cubes, pivots, rotations and box-UV layout as
net.minecraft.client.model.geom (CubeListBuilder / PartPose). Used both to generate the Java layer definitions
(tools/gen_models.py) and to render previews (tools/models/render.py), so what the preview shows is what the game
draws.

Conventions (identical to the game's model space): units are pixels, +y points down, a part's pose is
offset (pivot) + rotation applied in Z * Y * X order, children inherit their parent's transform.
"""
from dataclasses import dataclass, field
import math


@dataclass
class Cube:
    x: float
    y: float
    z: float
    w: float
    h: float
    d: float
    u: int
    v: int
    inflate: float = 0.0
    mirror: bool = False


@dataclass
class Part:
    name: str
    cubes: list = field(default_factory=list)
    offset: tuple = (0.0, 0.0, 0.0)
    rotation: tuple = (0.0, 0.0, 0.0)
    children: list = field(default_factory=list)

    def box(self, u, v, x, y, z, w, h, d, inflate=0.0, mirror=False):
        self.cubes.append(Cube(x, y, z, w, h, d, u, v, inflate, mirror))
        return self

    def child(self, name, offset=(0.0, 0.0, 0.0), rotation=(0.0, 0.0, 0.0)):
        part = Part(name, offset=offset, rotation=rotation)
        self.children.append(part)
        return part

    def find(self, name):
        if self.name == name:
            return self
        for c in self.children:
            found = c.find(name)
            if found:
                return found
        return None

    def walk(self):
        yield self
        for c in self.children:
            yield from c.walk()


@dataclass
class Model:
    root: Part
    tex_w: int
    tex_h: int


# ---------------------------------------------------------------------------------------------
# Matrices (4x4, row-major lists) - small and dependency-free
# ---------------------------------------------------------------------------------------------
def mat_identity():
    return [[1.0 if i == j else 0.0 for j in range(4)] for i in range(4)]


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def mat_translate(x, y, z):
    m = mat_identity()
    m[0][3], m[1][3], m[2][3] = x, y, z
    return m


def mat_rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]]


def mat_rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]]


def mat_rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]


def apply(m, p):
    x, y, z = p
    return (m[0][0] * x + m[0][1] * y + m[0][2] * z + m[0][3],
            m[1][0] * x + m[1][1] * y + m[1][2] * z + m[1][3],
            m[2][0] * x + m[2][1] * y + m[2][2] * z + m[2][3])


def part_matrix(parent, part, pose_override=None):
    """ModelPart.translateAndRotate: translate(offset) then rotationZYX(z, y, x)."""
    offset, rotation = part.offset, part.rotation
    if pose_override and part.name in pose_override:
        o = pose_override[part.name]
        offset = o.get("offset", offset)
        rotation = o.get("rotation", rotation)
    m = mat_mul(parent, mat_translate(*offset))
    rx, ry, rz = rotation
    if rz:
        m = mat_mul(m, mat_rot_z(rz))
    if ry:
        m = mat_mul(m, mat_rot_y(ry))
    if rx:
        m = mat_mul(m, mat_rot_x(rx))
    return m


# ---------------------------------------------------------------------------------------------
# Box UV layout -> faces
# ---------------------------------------------------------------------------------------------
def cube_faces(cube):
    """
    Yields (corner0, s_edge_end, t_edge_end, (u0, v0, fw, fh), name) per face in local pixel space, where texture
    column u0 maps to corner0 and grows along corner0->s_edge_end, row v0 maps to corner0 and grows toward
    t_edge_end. Matches the unfolded-box skin layout (top above front, right side left of front, ...).
    """
    g = cube.inflate
    x0, y0, z0 = cube.x - g, cube.y - g, cube.z - g
    x1, y1, z1 = cube.x + cube.w + g, cube.y + cube.h + g, cube.z + cube.d + g
    u, v, w, h, d = cube.u, cube.v, int(cube.w), int(cube.h), int(cube.d)
    if cube.mirror:
        x0, x1 = x1, x0
    faces = [
        # name, corner (top-left of texture region), s end, t end, uv rect
        ("front", (x0, y0, z0), (x1, y0, z0), (x0, y1, z0), (u + d, v + d, w, h)),
        ("back", (x1, y0, z1), (x0, y0, z1), (x1, y1, z1), (u + d + w + d, v + d, w, h)),
        ("right", (x0, y0, z1), (x0, y0, z0), (x0, y1, z1), (u, v + d, d, h)),
        ("left", (x1, y0, z0), (x1, y0, z1), (x1, y1, z0), (u + d + w, v + d, d, h)),
        ("top", (x0, y0, z1), (x1, y0, z1), (x0, y0, z0), (u + d, v, w, d)),
        ("bottom", (x0, y1, z0), (x1, y1, z0), (x0, y1, z1), (u + d + w, v, w, d)),
    ]
    for name, c0, cs, ct, rect in faces:
        yield c0, cs, ct, rect, name
