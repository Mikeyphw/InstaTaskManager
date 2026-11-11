#!/usr/bin/env bash
set -euo pipefail
base="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d "${TMPDIR:-/tmp}/itm-social-XXXXXX")"
trap 'rm -rf "$work"' EXIT
javac -d "$work" \
  "$base/app/src/main/java/com/mikeyphw/instataskmanager/SocialMethodShape.java" \
  "$base/scripts/SocialShapeSmoke.java"
java -cp "$work" com.mikeyphw.instataskmanager.SocialShapeSmoke
