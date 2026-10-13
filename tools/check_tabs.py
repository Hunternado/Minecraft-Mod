#!/usr/bin/env python3
"""Every item of the mod must appear in exactly one creative tab: compares ModItems / ModBuildingBlocks with
the lists in ModCreativeTabs and reports strays and duplicates."""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/java/com/hunternado/frieren/registry")
items = re.findall(r"public static final RegistryObject<Item> ([A-Z0-9_]+)", open(os.path.join(ROOT, "ModItems.java")).read())
tabs = "\n".join(l for l in open(os.path.join(ROOT, "ModCreativeTabs.java")).read().splitlines() if ".icon(" not in l)
used = re.findall(r"ModItems\.([A-Z0-9_]+)", tabs)
counts = {i: used.count(i) for i in items}
problems = [f"not in any creative tab: {i}" for i, c in counts.items() if c == 0]
problems += [f"in {c} creative tabs: {i}" for i, c in counts.items() if c > 1]
if "ModBuildingBlocks.TAB_ORDER" not in tabs:
    problems.append("the building set is not in any creative tab")
for p in problems:
    print(p)
print(f"checked {len(items)} items against the creative tabs, {len(problems)} problem(s)")
sys.exit(1 if problems else 0)
