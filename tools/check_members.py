"""
Checks static member references into Minecraft classes against the CI API dump (javap signatures of the real
26.2 classes, fetched from the ci-artifacts branch into ../ci-artifacts/api or $FRIEREN_API_DUMP).

For every `Simple.NAME` and `Simple.name(` where `Simple` is an imported net.minecraft / com.mojang class whose
package is in the dump, the member must be declared by that class or one of its dumped superclasses/interfaces.
Classes outside the dump are skipped, so a clean run means "nothing provably wrong", not "compiles".
This exists because constants such as BlockTags.COAL_ORES look right, are plausible, and do not exist.
"""
import os
import re
import sys

REPO = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
SRC = os.path.join(REPO, "src", "main", "java")
DUMP = os.environ.get("FRIEREN_API_DUMP", os.path.normpath(os.path.join(REPO, "..", "ci-artifacts", "api")))
IMPORT_RE = re.compile(r"^import\s+([\w.]+)\s*;", re.M)
CLASS_RE = re.compile(r"^(?:public |protected |private |final |abstract |sealed |non-sealed |static |strictfp )*"
                      r"(?:class|interface|enum|record|@interface) ([\w.$]+)(?:<[^{]*?>)?(.*)\{\s*$")
COMMENT_RE = re.compile(r"//[^\n]*|/\*.*?\*/", re.S)
STRING_RE = re.compile(r'"(?:\\.|[^"\\])*"')

_packages = {}


def package_classes(pkg):
    """fqcn -> (member names, supertypes) for every class in one dumped package file."""
    if pkg in _packages:
        return _packages[pkg]
    path = os.path.join(DUMP, pkg + ".txt")
    classes = {}
    if os.path.isfile(path):
        current = None
        with open(path, encoding="utf-8", errors="replace") as f:
            for line in f:
                m = CLASS_RE.match(line.strip())
                if m:
                    current = m.group(1)
                    supers = re.findall(r"[\w.$]+", re.sub(r"<[^>]*>", "", m.group(2)))
                    classes[current] = (set(), [s for s in supers if "." in s])
                    continue
                if current is None:
                    continue
                decl = line.strip()
                if not decl or decl == "}":
                    continue
                name = re.match(r".*?([\w$]+)\s*(\(|;|$)", decl.split(" throws ")[0])
                if name:
                    classes[current][0].add(name.group(1))
    _packages[pkg] = classes
    return classes


def lookup(fqcn, member, depth=0):
    """True if declared, False if provably absent, None if the class (or a supertype) is not in the dump."""
    if depth > 12:
        return None
    pkg = fqcn.rsplit(".", 1)[0]
    if "$" in pkg:
        pkg = pkg.split("$")[0].rsplit(".", 1)[0]
    classes = package_classes(pkg)
    if fqcn not in classes:
        return None
    members, supers = classes[fqcn]
    if member in members:
        return True
    unknown = False
    for parent in supers:
        if parent.startswith("java.") and parent not in ("java.lang.Enum", "java.lang.Record"):
            continue
        found = lookup(parent, member, depth + 1)
        if found:
            return True
        if found is None and not parent.startswith("java."):
            unknown = True
    return None if unknown else False


def main():
    if not os.path.isdir(DUMP):
        print(f"checked members: no API dump at {DUMP}, skipped")
        return 0
    problems = 0
    checked = 0
    for dirpath, _, files in os.walk(SRC):
        for name in files:
            if not name.endswith(".java"):
                continue
            path = os.path.join(dirpath, name)
            text = open(path, encoding="utf-8").read()
            imports = {imp.rsplit(".", 1)[1]: imp for imp in IMPORT_RE.findall(text)
                       if imp.startswith(("net.minecraft.", "com.mojang."))}
            body = STRING_RE.sub('""', COMMENT_RE.sub("", text))
            seen = set()
            for m in re.finditer(r"(?<![\w.])([A-Z]\w*)\.([A-Za-z_]\w*)\b(\s*\()?", body):
                simple, member, call = m.group(1), m.group(2), m.group(3)
                if simple not in imports or (simple, member) in seen:
                    continue
                constant = re.fullmatch(r"[A-Z][A-Z0-9_]*", member) is not None
                if member[0].isupper() and not constant:
                    continue   # Simple.Inner nested type, constructed or referenced
                if not call and not constant:
                    continue   # instance-looking access through a type name: not ours to judge
                seen.add((simple, member))
                result = lookup(imports[simple], member)
                if result is None:
                    continue
                checked += 1
                if not result:
                    problems += 1
                    line = body.count("\n", 0, m.start()) + 1
                    print(f"{os.path.relpath(path, REPO)}:{line}: {imports[simple]} has no member {member}")
    print(f"checked {checked} static member references against the API dump, {problems} problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
