#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d "${TMPDIR:-/tmp}/itm-p04-javac.XXXXXX")"
trap 'rm -rf "$tmp"' EXIT
p='app/src/main/java/com/mikeyphw/instataskmanager'
javac -encoding UTF-8 -d "$tmp" \
 scripts/p04-stubs/android/app/*.java \
 scripts/p04-stubs/android/content/*.java \
 scripts/p04-stubs/android/content/pm/*.java \
 scripts/p04-stubs/android/view/*.java \
 scripts/p04-stubs/android/widget/*.java \
 scripts/p04-stubs/android/util/*.java \
 scripts/p04-stubs/io/github/libxposed/api/*.java \
 scripts/p04-stubs/com/mikeyphw/instataskmanager/*.java \
 "$p/P04PredicateTrace.java" "$p/P04VariantShape.java" "$p/P04TraceSummary.java" "$p/P04GateTrace.java" "$p/InstaTaskModule.java" \
 scripts/P04TraceSummarySmoke.java
java -cp "$tmp" com.mikeyphw.instataskmanager.P04TraceSummarySmoke
