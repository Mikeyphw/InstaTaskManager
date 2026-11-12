#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d "${TMPDIR:-/tmp}/itm-p04-hf08.XXXXXX")"
trap 'rm -rf "$tmp"' EXIT
p='app/src/main/java/com/mikeyphw/instataskmanager'
javac -encoding UTF-8 -d "$tmp" "$p/P04BranchInputs.java" "$p/P04ExactGateTrial.java" scripts/P04HF08GateSmoke.java
java -cp "$tmp" com.mikeyphw.instataskmanager.P04HF08GateSmoke
