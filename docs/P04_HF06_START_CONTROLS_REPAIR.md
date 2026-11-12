# P04 HF06 — Restore Start A / Start B on IG455

## Physical-device failure
The P04 HF05 APK displayed `REFUSED: exact hook shape unavailable (NoSuchMethodException)` and both A/B Start buttons stayed disabled.

## Root cause, verified directly from original Instagram 455 APK
`classes22.dex` defines `LX/0T4C;` with class access flags `0x401` (public, abstract).
It nevertheless declares `BdA(J)Z` with method flags `0x11` (public final) and a nonzero code offset.
HF05 `P04ConfigTarget.verify` incorrectly rejected every abstract **class**, even when the exact method was concrete. The previous source smoke only tested concrete owner classes and rejected abstract owners.

HF06 correctly accepts an abstract owner with a concrete, public instance method. It still rejects interface owners, abstract/static/nonpublic methods, wrong signatures or interface mismatches.

## Failure isolation
Two core selector hooks (`X.0Ie0.invoke` and `X.0F2B.invoke`) remain mandatory after exact installed APK SHA/size validation. Input-presence, MobileConfig, nullable config and session observers are optional. Each optional observer reports its name and exception type on failure. Capture readiness depends only on the successful core pair, so an optional lookup error cannot disable Start A/B.

All Xposed interceptors call the original exactly once and preserve the original result. No account IDs or media data are captured. All source and hosted Android tests must pass before deploying.

## Test
Install the exact-current-PR-head successful HF06 APK, re-enable Vector scope if needed, force-stop/relaunch Instagram. Open TM → Social. Expect `READY: Repost A/B core (2/2)` (optional N/3 also displayed). Start A must become tappable. If MobileConfig remains unavailable, its name appears under `unavailable`, while A/B capture continues.
