"""
CI helper: writes javap (public + protected) signatures of the classes listed in tools/ci_api_packages.txt,
one text file per package, from the deobfuscated Minecraft + Forge jars ForgeGradle produced. Lets API names be
checked against the real classes without access to Forge's or Mojang's servers.
Usage: python3 tools/ci_api_dump.py <output-dir>
"""
import glob
import os
import re
import subprocess
import sys
import zipfile

out_dir = sys.argv[1]
os.makedirs(out_dir, exist_ok=True)
specs = [line.strip() for line in open("tools/ci_api_packages.txt")
         if line.strip() and not line.startswith("#")]

jars = [j for j in glob.glob(".gradle/mavenizer/repo/**/*.jar", recursive=True)
        + glob.glob(os.path.expanduser("~/.gradle/caches/modules-2/files-2.1/net.minecraftforge/**/*.jar"), recursive=True)
        if not re.search(r"-(sources|javadoc)\.jar$", j)]
print(f"{len(jars)} jars on the classpath")


def wanted(cls):
    for spec in specs:
        if spec.endswith("/*"):
            base = spec[:-1]
            if cls.startswith(base) and "/" not in cls[len(base):]:
                return True
        elif spec.endswith("/"):
            if cls.startswith(spec):
                return True
        elif cls == spec or cls.startswith(spec + "$"):
            return True
    return False


by_package = {}
seen = set()
for jar in jars:
    try:
        names = zipfile.ZipFile(jar).namelist()
    except zipfile.BadZipFile:
        continue
    for name in names:
        if not name.endswith(".class") or name.startswith("META-INF/") or re.search(r"\$\d", name):
            continue
        cls = name[:-6]
        if cls in seen or not wanted(cls):
            continue
        seen.add(cls)
        by_package.setdefault(os.path.dirname(cls), []).append(cls.replace("/", "."))

classpath = ":".join(jars)
total = 0
for package, classes in sorted(by_package.items()):
    classes.sort()
    with open(os.path.join(out_dir, package.replace("/", ".") + ".txt"), "w") as out:
        for i in range(0, len(classes), 150):
            batch = classes[i:i + 150]
            result = subprocess.run(["javap", "-protected", "-cp", classpath] + batch,
                                    capture_output=True, text=True)
            out.write(result.stdout)
            if result.returncode != 0:
                out.write("\n# javap errors:\n" + result.stderr)
    total += len(classes)
print(f"dumped {total} classes in {len(by_package)} packages to {out_dir}")
