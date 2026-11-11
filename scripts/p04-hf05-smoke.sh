#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d "${TMPDIR:-/tmp}/itm-p04-hf05.XXXXXX")"
trap 'rm -rf "$tmp"' EXIT
p='app/src/main/java/com/mikeyphw/instataskmanager'
javac -encoding UTF-8 -d "$tmp" "$p/P04ConfigTarget.java" scripts/P04HF05ConfigTargetSmoke.java
java -cp "$tmp" com.mikeyphw.instataskmanager.P04HF05ConfigTargetSmoke
