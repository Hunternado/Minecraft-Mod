"""Renders turnaround sheets of the character roster (idle, cast and walk poses) for visual review."""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "models"))
from PIL import Image  # noqa: E402

from characters import pose_cast, pose_idle, pose_walk  # noqa: E402
from render import grid, render, turnaround  # noqa: E402
from roster import ROSTER  # noqa: E402

out = sys.argv[1] if len(sys.argv) > 1 else "preview"
names = sys.argv[2:] or list(ROSTER)
os.makedirs(out, exist_ok=True)
sheets = []
for name in names:
    rig = ROSTER[name]()
    model = rig.build()
    tex = rig.paint()
    img = Image.fromarray(tex.a, "RGBA")
    img.save(os.path.join(out, f"{name}_texture.png"))
    a = turnaround(model, img, scale=6, pose=pose_idle(rig), angles=(0, 45, 90, 180, 315), label=f"{name} - idle")
    b = turnaround(model, img, scale=6, pose=pose_cast(rig), angles=(0, 30, 90), label=f"{name} - casting")
    sheet = grid([a, b], 1)
    sheet.save(os.path.join(out, f"{name}.png"))
    sheets.append(render(model, img, yaw=20, pitch=10, scale=5, pose=pose_idle(rig)))
grid(sheets, 6).save(os.path.join(out, "roster.png"))
print("wrote", out)
