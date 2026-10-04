#!/usr/bin/env bash
# Syntax gate that works without Minecraft jars: parses every Java source with JDK 25's javac.
# Usage: tools/javac_parse.sh   (uses $JAVA_HOME/bin/java if set, otherwise java on PATH)
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
exec "$java_bin" "$here/javac/ParseCheck.java" "$here/../src/main/java"
