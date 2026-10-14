"""
Generates the character models from tools/models/roster.py:
  * src/main/java/.../client/render/character/CharacterKind.java  (one enum constant + layer definition per rig)
  * src/main/resources/assets/frieren/textures/entity/character/<name>.png  (2x-density skins)
  * src/main/resources/assets/frieren/textures/item/<mob>_spawn_egg.png  (a portrait of the face, for FACE_EGGS)

The preview renderer (tools/preview_characters.py) reads the same rigs, so previews match the game exactly.
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "models"))
from PIL import Image  # noqa: E402

from characters import pose_idle, walk_tree  # noqa: E402
from render import render  # noqa: E402
from roster import ROSTER  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
JAVA = os.path.join(REPO, "src/main/java/com/hunternado/frieren/client/render/character/CharacterKind.java")
TEXTURES = os.path.join(REPO, "src/main/resources/assets/frieren/textures/entity/character")
ITEM_TEXTURES = os.path.join(REPO, "src/main/resources/assets/frieren/textures/item")

# Spawn eggs whose icon is the character's face (the rest are drawn in gen_assets.py).
FACE_EGGS = ("frieren", "examiner", "wandering_mage", "priest", "demon_soldier", "demon_mage", "linie", "draht", "lugner", "ubel")
_PORTRAIT_HIDDEN = ("body", "right_arm", "left_arm", "right_leg", "left_leg", "hair_back", "lock_left", "lock_right",
                    "tail_left", "tail_right")


def face_portrait(rig, model, skin):
    """
    The head straight on at one pixel per skin texel (skins are 2x density, so the face is exactly 16x16), cropped
    to the 16 rows above the chin: hair, fringe and eyes, like a player-head icon. Corners are rounded off.
    """
    full = render(model, skin, yaw=0, pitch=0, scale=2, pose=pose_idle(rig), hidden=_PORTRAIT_HIDDEN, shade=False)
    px = full.load()
    cx = full.width // 2
    chin = max(y for y in range(full.height) if px[cx, y][3] > 0)
    icon = full.crop((cx - 8, chin - 16, cx + 8, chin))
    ip = icon.load()
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        ip[x, y] = (0, 0, 0, 0)
    # a darker rim so pale hair still reads against the light inventory slot
    for y in range(16):
        for x in range(16):
            r, g, b, a = ip[x, y]
            rim = x in (0, 15) or y in (0, 15) or any(ip[x + dx, y + dy][3] == 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                                                      if 0 <= x + dx < 16 and 0 <= y + dy < 16)
            if a and rim:
                ip[x, y] = (int(r * 0.72), int(g * 0.72), int(b * 0.76), a)
    return icon


def f(v):
    s = f"{float(v):.4f}".rstrip("0")
    if s.endswith("."):
        s += "0"
    return s + "F"


def cubes_java(part):
    if not part.cubes:
        return "CubeListBuilder.create()"
    out = ["CubeListBuilder.create()"]
    for c in part.cubes:
        deform = "CubeDeformation.NONE" if c.inflate == 0 else f"new CubeDeformation({f(c.inflate)})"
        out.append(f"\n                .texOffs({c.u}, {c.v}).addBox({f(c.x)}, {f(c.y)}, {f(c.z)}, "
                   f"{f(c.w)}, {f(c.h)}, {f(c.d)}, {deform})")
    return "".join(out)


def pose_java(part):
    (x, y, z), (rx, ry, rz) = part.offset, part.rotation
    return f"PartPose.offsetAndRotation({f(x)}, {f(y)}, {f(z)}, {f(rx)}, {f(ry)}, {f(rz)})"


def layer_method(name, model):
    root = model.root
    lines = [f"    private static LayerDefinition {camel(name)}() {{",
             "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition root = mesh.getRoot();"]
    for parent, part_name, sub in walk_tree():
        part = root.find(part_name)
        holder = "root" if parent is None else camel(parent)
        prefix = f"PartDefinition {camel(part_name)} = " if sub else ""
        lines.append(f"        {prefix}{holder}.addOrReplaceChild(\"{part_name}\", {cubes_java(part)},\n"
                     f"            {pose_java(part)});")
    lines.append(f"        return LayerDefinition.create(mesh, {model.tex_w}, {model.tex_h});")
    lines.append("    }")
    return "\n".join(lines)


def camel(s):
    parts = s.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def main():
    os.makedirs(TEXTURES, exist_ok=True)
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    constants, cases, methods = [], [], []
    for name, factory in ROSTER.items():
        rig = factory()
        model = rig.build()
        skin = Image.fromarray(rig.paint().a, "RGBA")
        skin.save(os.path.join(TEXTURES, f"{name}.png"))
        if name in FACE_EGGS:
            face_portrait(rig, model, skin).save(os.path.join(ITEM_TEXTURES, f"{name}_spawn_egg.png"))
        for variant, post in rig.variants.items():
            Image.fromarray(rig.paint(post).a, "RGBA").save(os.path.join(TEXTURES, f"{variant}.png"))
        skirt = model.root.find("skirt")
        long_robe = sum(c.h for c in skirt.cubes) >= 10
        main_hand, off_hand = rig.held
        held = ", ".join(f"\"{i}\"" if i else "null" for i in (main_hand, off_hand))
        constants.append(f"    {name.upper()}(\"{name}\", {f(rig.render_scale)}, {str(long_robe).lower()}, "
                         f"{str(rig.slim).lower()}, {f(rig.arm_len)}, {held}, Carry.{rig.carry.upper()})")
        cases.append(f"            case {name.upper()} -> {camel(name)}();")
        methods.append(layer_method(name, model))
    constants_java = ",\n".join(constants)
    cases_java = "\n".join(cases)
    methods_java = "\n\n".join(methods)
    java = f'''// GENERATED by tools/gen_models.py from tools/models/roster.py - edit the rigs there, not this file.
package com.hunternado.frieren.client.render.character;

import com.hunternado.frieren.FrierenIds;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * Every character model of the mod. All share the part names animated by {{@link CharacterModel}}; textures are
 * 2x-density skins in {{@code textures/entity/character/}}.
 */
public enum CharacterKind {{
{constants_java};

    /** Where the main-hand signature weapon rides while the character is not fighting. */
    public enum Carry {{
        /** Always in the hand (staffs, the Demon King's blade). */
        HAND,
        /** Strapped flat on the back, head over the right shoulder (Stark's axe). */
        BACK,
        /** Sheathed on the left hip (Himmel's sword). */
        HIP
    }}

    private final String id;
    private final float scale;
    private final boolean longRobe;
    private final boolean slim;
    private final float armLength;
    private final @Nullable String mainHandItem;
    private final @Nullable String offHandItem;
    private final Carry carry;
    private final ModelLayerLocation layer;
    private final Identifier texture;
    private @Nullable ItemStack mainHandStack;
    private @Nullable ItemStack offHandStack;

    CharacterKind(String id, float scale, boolean longRobe, boolean slim, float armLength, @Nullable String mainHandItem,
                  @Nullable String offHandItem, Carry carry) {{
        this.id = id;
        this.scale = scale;
        this.longRobe = longRobe;
        this.slim = slim;
        this.armLength = armLength;
        this.mainHandItem = mainHandItem;
        this.offHandItem = offHandItem;
        this.carry = carry;
        this.layer = new ModelLayerLocation(FrierenIds.id("character/" + id), "main");
        this.texture = FrierenIds.id("textures/entity/character/" + id + ".png");
    }}

    /** Where the main-hand weapon rides out of combat; it comes to hand when the character fights. */
    public Carry carry() {{
        return carry;
    }}

    /** Slim (three pixel) arms: held items sit half a pixel closer to the body, as on slim player models. */
    public boolean slim() {{
        return slim;
    }}

    /** Arm length in model pixels (12 for player proportions); held items move up the arm for shorter arms. */
    public float armLength() {{
        return armLength;
    }}

    /** The item this character is drawn holding in its main (right) hand when it holds nothing itself. */
    public ItemStack mainHandStack() {{
        if (mainHandStack == null) {{
            mainHandStack = stackOf(mainHandItem);
        }}
        return mainHandStack;
    }}

    /** The item drawn in the off (left) hand, e.g. Aura's Scale of Obedience. */
    public ItemStack offHandStack() {{
        if (offHandStack == null) {{
            offHandStack = stackOf(offHandItem);
        }}
        return offHandStack;
    }}

    private static ItemStack stackOf(@Nullable String item) {{
        // Resolved lazily (registries are frozen by the first render); an unknown id resolves to air: nothing drawn.
        return item == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(item)));
    }}

    public String id() {{
        return id;
    }}

    /** Render scale relative to a player-sized model. */
    public float scale() {{
        return scale;
    }}

    /** Long robes and dresses: legs swing less so they stay inside the skirt. */
    public boolean longRobe() {{
        return longRobe;
    }}

    public ModelLayerLocation layer() {{
        return layer;
    }}

    public Identifier texture() {{
        return texture;
    }}

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {{
        for (CharacterKind kind : values()) {{
            event.registerLayerDefinition(kind.layer, kind::createLayer);
        }}
    }}

    private LayerDefinition createLayer() {{
        return switch (this) {{
{cases_java}
        }};
    }}

{methods_java}
}}
'''
    with open(JAVA, "w") as out:
        out.write(java)
    print(f"generated {len(ROSTER)} character models")


if __name__ == "__main__":
    main()
