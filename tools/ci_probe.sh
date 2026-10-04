#!/usr/bin/env bash
# CI helper: prints selected member signatures from the deobfuscated Minecraft + Forge jars that ForgeGradle
# produced, so API names can be checked against the real 26.2 classes. Reads "class|regex" lines from
# tools/ci_probe.txt.
set -uo pipefail
repo=".gradle/mavenizer/repo"
echo "jars under $repo:"
find "$repo" -name '*.jar' -printf '  %s %p\n' 2>/dev/null | sort -rn | head -20
cp=$(find "$repo" -name '*.jar' ! -name '*sources*' ! -name '*javadoc*' 2>/dev/null | tr '\n' ':')
while IFS='|' read -r cls pattern; do
  [[ -z "${cls// }" || "$cls" == \#* ]] && continue
  echo "== $cls  /$pattern/"
  javap -cp "$cp" -p "$cls" 2>&1 | grep -iE -- "$pattern" | head -60
done < tools/ci_probe.txt
exit 0
