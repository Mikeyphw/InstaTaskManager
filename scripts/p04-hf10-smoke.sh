#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d "${TMPDIR:-/tmp}/itm-hf10.XXXXXX")"
trap 'rm -rf "$tmp"' EXIT
p='app/src/main/java/com/mikeyphw/instataskmanager'
javac -encoding UTF-8 -d "$tmp" "$p/P04ActualDataTarget.java" scripts/P04HF10DataTargetSmoke.java
java -cp "$tmp" com.mikeyphw.instataskmanager.P04HF10DataTargetSmoke
