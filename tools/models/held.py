"""
Held items in the preview: the exact transform chain the game uses to draw an item in a character's hand, so
display transforms can be designed and checked without starting the game.

ItemInHandLayer.submitArmWithItem (26.2 bytecode): translateToHand (root, then the arm; slim arms nudged 0.5 px
toward the body), rotX(-90), rotY(180), translate(+-1/16, 2/16, -10/16); then the item layer applies the model's
display transform (translate, rotationXYZ, scale) and draws element vertices at (pos / 16 - 0.5).
Everything here works in model pixels (y down) like boxmodel.py.
"""
import math

from boxmodel import apply, mat_identity, mat_mul, mat_rot_x, mat_rot_y, mat_rot_z, mat_translate, part_matrix


def mat_scale(x, y, z):
    m = mat_identity()
    m[0][0], m[1][1], m[2][2] = x, y, z
    return m


STAFF_AIM = math.radians(55)   # CharacterModel.STAFF_AIM
WEAPON_AIM = math.radians(35)  # CharacterModel.WEAPON_AIM


def arm_matrix(model, pose, arm, slim=False, arm_len=12, wrist=0.0):
    """
    Model-space matrix of the arm part (pixels), as CharacterModel.translateToHand sets it up. wrist is the extra
    x rotation applied at the hand (CharacterModel cancels a casting arm's raise and adds an aim tilt there).
    """
    name = "right_arm" if arm == "right" else "left_arm"
    root = model.root
    m = part_matrix(mat_identity(), root, pose)
    part = root.find(name)
    if slim:
        nudge = 0.5 if arm == "right" else -0.5
        ox, oy, oz = part.offset
        if pose and name in pose and "offset" in pose[name]:
            ox, oy, oz = pose[name]["offset"]
        pose = dict(pose or {})
        entry = dict(pose.get(name, {}))
        entry["offset"] = (ox + nudge, oy, oz)
        pose[name] = entry
    m = part_matrix(m, part, pose)
    if arm_len != 12:
        m = mat_mul(m, mat_translate(0, arm_len - 12, 0))
    if wrist:
        m = mat_mul(m, mat_translate(0, 10, 0))
        m = mat_mul(m, mat_rot_x(wrist))
        m = mat_mul(m, mat_translate(0, -10, 0))
    return m


def item_matrix(arm_m, transform, arm="right"):
    """
    Matrix taking item-model coordinates (0..16 block-model units, y up) to model pixels for an item held in
    arm_m. transform: {"rotation": [x, y, z] deg, "translation": [x, y, z] (1/16 units), "scale": [x, y, z]}.
    """
    sign = -1 if arm == "left" else 1
    m = mat_mul(arm_m, mat_scale(16, 16, 16))  # from here on: block units, like the PoseStack after translateToHand
    m = mat_mul(m, mat_rot_x(math.radians(-90)))
    m = mat_mul(m, mat_rot_y(math.radians(180)))
    m = mat_mul(m, mat_translate(sign / 16.0, 2 / 16.0, -10 / 16.0))
    t = transform.get("translation", [0, 0, 0])
    r = transform.get("rotation", [0, 0, 0])
    s = transform.get("scale", [1, 1, 1])
    m = mat_mul(m, mat_translate(t[0] / 16.0, t[1] / 16.0, t[2] / 16.0))
    m = mat_mul(m, mat_rot_x(math.radians(r[0])))
    m = mat_mul(m, mat_rot_y(math.radians(r[1])))
    m = mat_mul(m, mat_rot_z(math.radians(r[2])))
    m = mat_mul(m, mat_scale(*s))
    m = mat_mul(m, mat_translate(-0.5, -0.5, -0.5))
    return mat_mul(m, mat_scale(1 / 16.0, 1 / 16.0, 1 / 16.0))


FACE_CORNERS = {
    # direction: (corner0, s_end, t_end) as functions of (from, to); u runs along s, v along t (texture top first)
    "north": lambda a, b: ((b[0], b[1], a[2]), (a[0], b[1], a[2]), (b[0], a[1], a[2])),
    "south": lambda a, b: ((a[0], b[1], b[2]), (b[0], b[1], b[2]), (a[0], a[1], b[2])),
    "west": lambda a, b: ((a[0], b[1], a[2]), (a[0], b[1], b[2]), (a[0], a[1], a[2])),
    "east": lambda a, b: ((b[0], b[1], b[2]), (b[0], b[1], a[2]), (b[0], a[1], b[2])),
    "up": lambda a, b: ((a[0], b[1], a[2]), (b[0], b[1], a[2]), (a[0], b[1], b[2])),
    "down": lambda a, b: ((a[0], a[1], b[2]), (b[0], a[1], b[2]), (a[0], a[1], a[2])),
}


def element_faces(elements, m, tex_size):
    """Faces of item-model elements in model pixels, for render.render(extra=...). UVs are 0..16 model units."""
    out = []
    k = tex_size / 16.0
    for el in elements:
        a, b = el["from"], el["to"]
        for direction, face in el["faces"].items():
            c0, cs, ct = FACE_CORNERS[direction](a, b)
            u0, v0, u1, v1 = face["uv"]
            rect = (u0 * k, v0 * k, (u1 - u0) * k, (v1 - v0) * k)
            out.append((apply(m, c0), apply(m, cs), apply(m, ct), rect, True))
    return out
