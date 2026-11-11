#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d "${TMPDIR:-/tmp}/itm-p04-hf02-javac.XXXXXX")"
trap 'rm -rf "$tmp"' EXIT
python3 scripts/p04-hf02-stubgen.py "$tmp"
p='app/src/main/java/com/mikeyphw/instataskmanager'
javac -encoding UTF-8 -cp "$tmp" -d "$tmp"  "$p/P04VariantShape.java" "$p/P04TraceSummary.java"  scripts/P04HF02VariantSmoke.java
java -cp "$tmp" com.mikeyphw.instataskmanager.P04HF02VariantSmoke
