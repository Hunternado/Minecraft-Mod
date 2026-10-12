"""
Cheap local consistency check for calls between the mod's own classes, run before pushing (CI is the real
compiler). For static-style calls `OurClass.method(args)` and `new OurClass(args)`, checks that a method or
constructor with that name accepts that many arguments in the class or one of its (mod-owned) superclasses.
Catches renamed methods and changed signatures, the most common reason a push fails to compile.

Limitations: purely syntactic. Members inherited from Minecraft/JDK types are not known, so calls that do not
resolve to any mod declaration are skipped rather than flagged; varargs count as "n-1 or more".
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "java")
MODIFIERS = {"public", "protected", "private", "static", "final", "abstract", "synchronized", "default", "native"}
NOT_DECL = {"return", "new", "throw", "else", "case", "yield", "assert", "if", "for", "while", "switch", "catch",
            "do", "try", "super", "this"}
HEADER = re.compile(r"\b(class|interface|enum|record)\s+(\w+)")
CALL_NAME = re.compile(r"\b(\w+)\s*\(")
STATIC_CALL = re.compile(r"\b([A-Z]\w*)\.(\w+)\s*\(")
NEW_CALL = re.compile(r"\bnew\s+([A-Z]\w*)\s*(?:<[^>(]*>)?\s*\(")
PREFIX = re.compile(r"^[\w<>\[\],.? @]*$")


def strip(src):
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if src.startswith("/*", i):
            j = src.find("*/", i + 2)
            j = n if j < 0 else j + 2
            out.append("".join(ch if ch == "\n" else " " for ch in src[i:j]))
            i = j
        elif src.startswith("//", i):
            j = src.find("\n", i)
            i = n if j < 0 else j
        elif src.startswith('"""', i):
            j = src.find('"""', i + 3)
            j = n if j < 0 else j + 3
            out.append('""' + "\n" * src.count("\n", i, j))
            i = j
        elif c in "\"'":
            j = i + 1
            while j < n and src[j] != c:
                j += 2 if src[j] == "\\" else 1
            out.append(c + c)
            i = j + 1
        else:
            out.append(c)
            i += 1
    return "".join(out)


def matching_paren(src, start):
    """src[start - 1] == '('; returns the index of the matching ')'."""
    depth, i = 0, start
    while i < len(src):
        ch = src[i]
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            if depth == 0:
                return i
            depth -= 1
        i += 1
    return len(src)


def count_list(text, angle=False):
    """Top-level comma-separated items in text (angle brackets count as nesting for declarations)."""
    if not text.strip():
        return 0, False
    opens, closes = ("([{<", ")]}>") if angle else ("([{", ")]}")
    depth, items = 0, 1
    for idx, ch in enumerate(text):
        if ch in opens:
            depth += 1
        elif ch in closes:
            if ch == ">" and idx > 0 and text[idx - 1] == "-":
                continue  # lambda arrow
            depth -= 1
        elif ch == "," and depth == 0:
            items += 1
    return items, "..." in text


def parse(sources):
    classes = {}

    def info(name):
        return classes.setdefault(name, {"methods": {}, "ctors": [], "super": None})

    for path, src in sources.items():
        top = os.path.splitext(os.path.basename(path))[0]
        info(top)
        for m in HEADER.finditer(src):
            name = m.group(2)
            brace = src.find("{", m.end())
            if brace < 0:
                continue
            header = src[m.end():brace]
            entry = info(name)
            ext = re.search(r"\bextends\s+([\w.]+)", header)
            if ext and m.group(1) == "class":
                entry["super"] = ext.group(1).split(".")[-1]
            if m.group(1) == "record":
                p = header.find("(")
                if p >= 0:
                    comps = header[p + 1:matching_paren(header, p + 1)]
                    entry["ctors"].append(count_list(comps, angle=True))
                    for comp in comps.split(","):
                        if comp.strip():
                            entry["methods"].setdefault(comp.split()[-1], []).append((0, False))
        for m in CALL_NAME.finditer(src):
            name = m.group(1)
            line_start = src.rfind("\n", 0, m.start()) + 1
            prefix = src[line_start:m.start()].strip()
            if name in NOT_DECL or not PREFIX.match(prefix):
                continue
            tokens = [t for t in re.split(r"\s+", prefix) if t and not t.startswith("@")]
            if tokens and tokens[-1] in NOT_DECL:
                continue
            close = matching_paren(src, m.end())
            after = src[close + 1:close + 80].lstrip()
            if not (after.startswith("{") or after.startswith(";") or after.startswith("throws")):
                continue
            params = count_list(src[m.end():close], angle=True)
            non_mod = [t for t in tokens if t not in MODIFIERS]
            if not non_mod and name[0].isupper():
                info(name)["ctors"].append(params)  # constructor: only modifiers before the name
            elif non_mod:
                owner = top
                info(owner)["methods"].setdefault(name, []).append(params)
    return classes


def main():
    sources = {}
    for dirpath, _, files in os.walk(ROOT):
        for f in files:
            if f.endswith(".java"):
                path = os.path.join(dirpath, f)
                with open(path, encoding="utf-8") as fh:
                    sources[path] = strip(fh.read())
    classes = parse(sources)

    def lookup(cls, name, depth=0):
        entry = classes.get(cls)
        if not entry or depth > 8:
            return None
        if name in entry["methods"]:
            return entry["methods"][name]
        return lookup(entry["super"], name, depth + 1) if entry["super"] else None

    def accepts(sigs, n):
        return any(count == n or (varargs and n >= count - 1) for count, varargs in sigs)

    problems = 0
    for path, src in sorted(sources.items()):
        rel = os.path.relpath(path, ROOT)
        for m in STATIC_CALL.finditer(src):
            cls, method = m.group(1), m.group(2)
            sigs = lookup(cls, method) if cls in classes else None
            if not sigs:
                continue
            n, _ = count_list(src[m.end():matching_paren(src, m.end())])
            if not accepts(sigs, n):
                line = src.count("\n", 0, m.start()) + 1
                print(f"{rel}:{line}: {cls}.{method}(...) with {n} arg(s); declared with {sorted({c for c, _ in sigs})}")
                problems += 1
        for m in NEW_CALL.finditer(src):
            cls = m.group(1)
            ctors = classes.get(cls, {}).get("ctors")
            if not ctors:
                continue
            n, _ = count_list(src[m.end():matching_paren(src, m.end())])
            if not accepts(ctors, n):
                line = src.count("\n", 0, m.start()) + 1
                print(f"{rel}:{line}: new {cls}(...) with {n} arg(s); constructors take {sorted({c for c, _ in ctors})}")
                problems += 1
    print(f"checked calls into {len(classes)} mod classes, {problems} problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
