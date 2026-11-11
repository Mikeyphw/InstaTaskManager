#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d "${TMPDIR:-/tmp}/itm-p04-hf01-javac.XXXXXX")"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" scripts/P04Hf01SyntaxSmoke.java
p='app/src/main/java/com/mikeyphw/instataskmanager'
java -cp "$tmp" P04Hf01SyntaxSmoke "$p/TaskPanel.java" "$p/P04GateTrace.java" "$p/P04TraceSummary.java"
