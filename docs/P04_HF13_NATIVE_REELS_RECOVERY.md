# P04 HF13 — explicitly armed native Repost recovery, Reels tab only

## Evidence and boundary

HF12 on exact Instagram 455.0.0.8.72 (base APK SHA-256 `fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6`) changed six `D3r(0xdf82522e)` `false→true` results. The exact selector-4 UI became structured six times. The user tapped Instagram's own Repost button and later confirmed the repost on Instagram Web. This demonstrated a real action **on Reels viewed in the dedicated Reels tab only**. Reels embedded in posts, regular feed posts, Friends tab and facepiles are NOT QUALIFIED.

## HF13 design

- Explicit **Enable Repost · Reels tab**; in-memory, default OFF, never persisted. The button is a manual assertion, **not** automatic Reels tab detection.
- Require exact APK/hash and installed native `D3r` hook + ART caller deoptimization. Require at least 5 **fresh A since the latest Start A** original `false/singleton` frames from native Repost data with >=75% coverage, no contradicting A outcomes. In a session with B samples, the full strict HF12 A/B rule applies; the known HF12 physical result avoids forcing a repeated B visit after each restart.
- Only within `X.0Ie0.invoke()` selector 4 and on `X.0ApG.D3r(0xdf82522e)` originally `Boolean.FALSE`, with A03 native object present. `true`, `null`, other key, other receiver, UFI/count, action handlers and all requests are untouched. Nested/reentrant frames are blocked; thread-level frame epochs invalidate in-flight account/recording changes.
- Maximum **64** changes or **90 seconds** per manual re-arm. Disable immediately bypasses future results; Instagram may retain already-rendered UI until refreshed/restarted. Stop capture, start A/B, clear evidence, Activity onPause/onDestroy, hook/deopt failure and process restart turn it OFF.
- **Important limitation:** A/B cohort labels are manual. An account change or navigation that stays inside the same resumed Activity cannot yet be verified automatically. Therefore this is NOT identity-isolated persistence. Users must press Recovery OFF before switching accounts or leaving the Reels tab. No approval was given for accessing usernames/IDs; none are accessed.
- Header now distinguishes MIXED `X.09j4/X.09ZC` result types, and outputs per-result code-class counts for selector-11 unknown variants. No raw object values or media/account identifiers.

## Acceptance

Run `python3 scripts/validate.py`, `bash scripts/p04-hf13-smoke.sh`, and all existing smoke tests. Hosted `:app:assembleDebug :app:testDebugUnitTest` via GitHub Actions **only**. The Actions artifact is named `InstaTaskManager-P04-HF13-NativeReelsRecovery-debug-<exact PR head SHA>`, with SHA256SUMS. Source acceptance is not physical-device acceptance: check enabled/OFF/lease/restart/account-change, Reels tab, InstaEclipse Disable Repost OFF, button click with user's consent and Instagram Web proof, no crash/ANR. **Do not test normal posts or Reels-as-posts with recovery ON until their separate HF14 gates are verified.**

## Remaining roadmap (preserved)

HF14: independently qualify native Repost in Feed, post-Reels, preview, and refresh; distinguish actual media surfaces and auto-disarm if possible. HF15: Friends Reels tab and friend-avatar/floating facepile native-data ownership separately, never fabricate social data. Retain P00–P05 performance/compliance roadmap, stacked #1/#2/#3 PRs, hosted reproducibility, safe release and physical API36 acceptance. No surprise merge/rebase/force push.
