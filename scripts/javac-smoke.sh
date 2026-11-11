#!/usr/bin/env bash
set -euo pipefail
base="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d "${TMPDIR:-/tmp}/itm-javac-XXXXXX")"
trap 'rm -rf "$work"' EXIT
javac -d "$work" "$base/app/src/main/java/com/mikeyphw/instataskmanager/ThreadStatParser.java" "$base/scripts/ParserSmoke.java"
java -cp "$work" com.mikeyphw.instataskmanager.ParserSmoke
