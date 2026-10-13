#!/usr/bin/env bash
# Local pre-push checks (CI is the real compiler): Java syntax, duplicate locals, imports against the 26.2
# references, call/constructor arity between mod classes, and resource consistency.
set -e
cd "$(dirname "$0")/.."
J25=/usr/lib/jvm/java-25-openjdk-amd64; [ -d "$J25" ] && export JAVA_HOME="$J25"
bash tools/javac_parse.sh 2>&1 | grep -v "^Picked up" | tail -1
"${JAVA_HOME:+$JAVA_HOME/bin/}java" tools/javac/DuplicateLocals.java src/main/java 2>&1 | grep -v "^Picked up"
python3 tools/check_imports.py | tail -1
python3 tools/check_calls.py
python3 tools/validate_resources.py | tail -1
python3 tools/check_tabs.py | tail -1
