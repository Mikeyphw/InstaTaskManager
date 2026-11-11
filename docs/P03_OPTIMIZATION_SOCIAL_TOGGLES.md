# P03 — Low-overhead profiling and experimental Social toggles

Based on physical-device P02 report `InstaTaskManager-20261111-043323-324.txt`.

## Profiling change

- Former P02 reported a busy `ITM-profiler` thread at 31.7% of one CPU core in a
  single sample; baseline evidence only, not a controlled before/after study.
- Lightweight process CPU / UID network measurements now every 5s, instead of 2.5s.
- Bounded thread enumeration every fourth tick (~20s), `Debug.getMemoryInfo`
  every sixth tick (~30s), same-UID process memory inventory every eighth (~40s).
- Thread CPU uses its own 20s delta rather than the 5s process delta.
- The Overview and Export report include an estimated sampler-thread CPU percentage.
  This excludes UI rendering, loading and Xposed hook overhead; verify on device.

## Social controls

Social tab has **five independent, default-off, persisted** requests:
Feed Repost, Reels Repost, liked-by avatar facepile, reposted-by avatar facepile,
and floating friend-avatar bubble. Stored in a dedicated private Instagram
SharedPreferences file; they don't change server or other app settings.

Only the two Repost switches support a **manual** experimental action:
`Try native Repost` is enabled after the exact IG455 9/9 read-only scan. A bounded
traversal inspects up to 1,200 native Android Views in the currently active
Instagram window and makes an *already existing* hidden View visible ONLY if its
label equals "Repost", "Repostar", or "Republicar" and Instagram already attached
an OnClickListener. It does not invoke that listener, post, generate UI widgets,
or synthesize privileges. It may match zero, especially with Litho/Compose,
missing server eligibility or UI controls that are entirely absent.

**Important limitation**: the manual native-view trial does not yet distinguish
Feed vs Reels. The Repost toggles are independent preferences, but either
Repost request allows the same manual trial on the current screen. Do NOT claim
independent Feed and Reels activation has been verified.

Avatar switches currently record opt-in intent **only**; the underlying friends'
data and exact isolated visibility predicates have not been verified. They do not
modify avatars or trigger new requests. Export records requested vs off state
explicitly. Missing friend data cannot be synthesized, and no social actions
are invoked. `Disable repost` in InstaEclipse must be off for native reposts.

Native View recovery is restored on leaving Instagram's Activity, manually
turning the Repost switches off, or hiding the TM chip. The small in-app panel
may be closed to tap the existing Repost View (if one was recovered), without
immediately reverting it; the view is restored when the Activity stops.

## Validation

- `python3 scripts/validate.py`
- `bash scripts/javac-smoke.sh`
- `bash scripts/social-shape-smoke.sh`
- `bash scripts/export-smoke.sh`
- `bash scripts/p03-smoke.sh` (52 cadence assertions + native-View recovery simulations)
- hosted Android/Gradle via GitHub Actions
- physical Android 16 + Vector API 102: compare identical user scenarios, check
  CPU profiler overhead, Repost control behavior, switch persistence and rollback,
  avatar toggle transparency, no crashes or regressions.

No new dangerous Android permissions or component-disable APIs.
