#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
build="$(mktemp -d "${TMPDIR:-/tmp}/itm-p03-smoke.XXXXXXXX")"
trap 'rm -rf -- "$build"' EXIT
javac -encoding UTF-8 -d "$build" \
 app/src/main/java/com/mikeyphw/instataskmanager/SamplingCadence.java \
 scripts/P03CadenceSmoke.java
java -cp "$build" com.mikeyphw.instataskmanager.P03CadenceSmoke

javac -encoding UTF-8 -d "$build" \
 scripts/p03-stubs/android/view/View.java \
 scripts/p03-stubs/android/view/ViewGroup.java \
 scripts/p03-stubs/android/widget/TextView.java \
 app/src/main/java/com/mikeyphw/instataskmanager/NativeRepostRecovery.java \
 scripts/P03NativeViewSmoke.java
java -cp "$build" com.mikeyphw.instataskmanager.P03NativeViewSmoke

# HF01: javac MUST typecheck the entire Android-backed ProfilerSampler. The tiny
# android stubs live only under scripts/ and never enter the release APK.
# This prevents a stray member assignment (HotThread vs Snapshot) from
# passing the source-only gates and failing :app:compileDebugJavaWithJavac.
javac -encoding UTF-8 -d "$build" \
 -sourcepath "scripts/p03-android-stubs" \
 app/src/main/java/com/mikeyphw/instataskmanager/ThreadStatParser.java \
 app/src/main/java/com/mikeyphw/instataskmanager/SamplingCadence.java \
 app/src/main/java/com/mikeyphw/instataskmanager/ProfilerSampler.java \
 scripts/P03ProfilerCompileSmoke.java
java -cp "$build" com.mikeyphw.instataskmanager.P03ProfilerCompileSmoke
