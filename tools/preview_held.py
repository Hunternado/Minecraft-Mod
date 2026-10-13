"""
Previews characters holding their items through the game's hand transform chain (tools/models/held.py):
idle, casting and walking poses. Usage: preview_held.py <out_dir> [character ...] [--item <id> --as <character>]
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "models"))
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image  # noqa: E402

import gen_held_items  # noqa: E402
from characters import pose_cast, pose_idle, pose_walk  # noqa: E402
from held import arm_matrix, element_faces, item_matrix  # noqa: E402
from render import grid, render  # noqa: E402
from roster import ROSTER  # noqa: E402


def held_extra(rig, model, pose, items, casting=False, raised=False):
    """Item faces for both hands; casting/raised apply the same wrist rules as CharacterModel.translateToHand."""
    import math
    from held import STAFF_AIM, WEAPON_AIM
    from held import back_matrix
    extra = []
    if rig.back_carry and not casting and items[0] and items[0].startswith("frieren:"):
        elements, disp, img = gen_held_items.build(items[0].split(":", 1)[1])
        extra.append((element_faces(elements, back_matrix(model, pose), img.width), img))
        items = (None, items[1])
    for arm, item_id in zip(("right", "left"), items):
        if not item_id or not item_id.startswith("frieren:"):
            continue
        key = item_id.split(":", 1)[1]
        if key not in gen_held_items.ITEMS:
            continue
        elements, disp, img = gen_held_items.build(key)
        t = disp["thirdperson_righthand" if arm == "right" else "thirdperson_lefthand"]
        wrist = 0.0
        if arm == "right" and casting:
            wrist = math.pi / 2 + (STAFF_AIM if key.endswith("staff") else WEAPON_AIM)
        elif arm == "left" and raised:
            wrist = -pose["left_arm"]["rotation"][0]
        am = arm_matrix(model, pose, arm, slim=rig.slim, arm_len=rig.arm_len, wrist=wrist)
        extra.append((element_faces(elements, item_matrix(am, t, arm), img.width), img))
    return extra


def sheet(name, items=None, background=(110, 150, 110, 255)):
    rig = ROSTER[name]()
    model = rig.build()
    img = Image.fromarray(rig.paint().a, "RGBA")
    items = items or rig.held
    views = []
    for pose_fn, angles in ((pose_idle, (0, 40, 90, 200)), (pose_cast, (0, 60, 90)), (pose_walk, (40,))):
        pose = pose_fn(rig)
        extra = held_extra(rig, model, pose, items, casting=pose_fn is pose_cast)
        for yaw in angles:
            views.append(render(model, img, yaw=yaw, pitch=8, scale=6, pose=pose, extra=extra, background=background))
    return grid(views, 8)


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    names = [n for n in sys.argv[2:] if n in ROSTER] or [n for n, f in ROSTER.items()]
    for n in names:
        rig = ROSTER[n]()
        if not any(rig.held):
            continue
        sheet(n).save(os.path.join(out, f"{n}_held.png"))
        print("wrote", n)


if __name__ == "__main__":
    main()
