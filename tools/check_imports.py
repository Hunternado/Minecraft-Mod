"""
Offline import audit for environments that cannot download Minecraft (no Gradle compile possible).

Checks every import in src/main/java:
  * com.hunternado.frieren.*  -> must resolve to a source file in this repo (or a nested type declared in it);
  * net.minecraftforge.*      -> must exist in a Forge source checkout, or be imported by Forge/NeoForge/Fabric code;
  * net.minecraft.*, com.mojang.* and other libraries -> must be imported somewhere in reference sources
    (Forge 65.x, NeoForge 26.2, Fabric API 26.2) or be a class patched by Forge.

Usage: python3 tools/check_imports.py <forge-src-root> <reference-root> [<reference-root> ...]
With no arguments it uses the paths of the original development container.
The real proof is still `./gradlew build`; this only catches renamed/moved classes early.
"""
import os
import re
import sys

REPO = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
SRC = os.path.join(REPO, "src", "main", "java")
DEFAULT_FORGE = "/home/user/minecraftforge/minecraftforge"
DEFAULT_REFS = ["/home/user/minecraftforge/minecraftforge", "/home/user/neoforged/neoforge", "/home/user/fabricmc/fabric-api"]
IMPORT_RE = re.compile(r"^import\s+(static\s+)?([\w.]+)\s*;", re.M)
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.S)
JDK_PREFIXES = ("java.", "javax.")
# Classes confirmed from patch bodies rather than import lines (same-package use needs no import).
USAGE_EVIDENCE = {
    "net.minecraft.world.entity.ai.goal.Goal": "Forge RangedBowAttackGoal.java.patch: 'extends Goal' (same package)",
    "net.minecraft.world.entity.PathfinderMob": "Forge MeleeAttackGoal.java.patch: 'MeleeAttackGoal(final PathfinderMob mob, ...)'",
    "net.minecraft.core.particles.ColorParticleOption": "Forge MobEffect.java.patch: 'ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ...)'",
    "net.minecraft.util.valueproviders.UniformInt": "Forge RedStoneOreBlock.java.patch: 'UniformInt.of(1, 5)'",
}


def java_files(root):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in (".git", "build", ".gradle", "test_old")]
        for name in filenames:
            if name.endswith(".java") or name.endswith(".patch"):
                yield os.path.join(dirpath, name)


def collect_reference(refs):
    known = set()
    patched = set()
    for root in refs:
        for path in java_files(root):
            text = BLOCK_COMMENT.sub("", open(path, encoding="utf-8", errors="replace").read())
            for match in IMPORT_RE.finditer(text):
                known.add(match.group(2))
            if path.endswith(".java.patch") and "/patches/" in path:
                rel = path.split("/patches/", 1)[1]
                rel = rel.split("minecraft/", 1)[-1] if rel.startswith("minecraft/") else rel
                patched.add(rel[:-len(".java.patch")].replace("/", "."))
    return known, patched


def forge_class_exists(forge_root, name):
    parts = name.split(".")
    for cut in range(len(parts), 0, -1):
        for sub in ("src/main/java", "src/fmlcore/java", "src/fmlloader/java", "src/client/java"):
            path = os.path.join(forge_root, sub, *parts[:cut]) + ".java"
            if os.path.exists(path):
                return True
    return False


def local_class_exists(name):
    parts = name.split(".")
    for cut in range(len(parts), 0, -1):
        path = os.path.join(SRC, *parts[:cut]) + ".java"
        if os.path.exists(path):
            nested = parts[cut:]
            if not nested:
                return True
            text = open(path, encoding="utf-8").read()
            return all(re.search(r"\b(class|interface|enum|record)\s+" + n + r"\b", text) for n in nested)
    return False


def main(argv):
    forge_root = argv[1] if len(argv) > 1 else DEFAULT_FORGE
    refs = argv[2:] if len(argv) > 2 else DEFAULT_REFS
    if not os.path.isdir(forge_root):
        print(f"reference sources not found ({forge_root}); nothing to check against")
        return 0
    known, patched = collect_reference(refs)
    problems = []
    checked = 0
    for path in java_files(SRC):
        text = BLOCK_COMMENT.sub("", open(path, encoding="utf-8").read())
        for match in IMPORT_RE.finditer(text):
            is_static, name = match.group(1), match.group(2)
            if is_static:
                name = name.rsplit(".", 1)[0]
            checked += 1
            if name.startswith(JDK_PREFIXES):
                continue
            if name.startswith("com.hunternado.frieren."):
                ok = local_class_exists(name)
            elif name.startswith("net.minecraftforge."):
                ok = name in known or forge_class_exists(forge_root, name)
            else:
                outer = name
                ok = name in known or name in patched or name in USAGE_EVIDENCE
                # A nested type is fine when its outer class is known and the nested import is used by someone.
                while not ok and "." in outer and outer.rsplit(".", 1)[1][:1].isupper():
                    outer = outer.rsplit(".", 1)[0]
                    if outer in known or outer in patched:
                        ok = any(k.startswith(outer + ".") for k in known) or name in known
                        if not ok:
                            problems.append((os.path.relpath(path, REPO), name, "nested type of a known class, unverified"))
                            ok = None
                        break
            if ok is False:
                problems.append((os.path.relpath(path, REPO), name, "not found in references"))
    for file, name, why in sorted(problems):
        print(f"{file}: {name}  [{why}]")
    print(f"checked {checked} imports, {len(problems)} flagged")
    return 1 if any(why == "not found in references" for _, _, why in problems) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
