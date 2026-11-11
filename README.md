# InstaTaskManager — an Instagram-only LSPosed / Vector performance inspector

**Milestones P00 + Social P01 (consolidated): read-only source candidate. Android APK compilation, runtime, and force-enablement are NOT qualified.**

Separate package: `com.mikeyphw.instataskmanager` (does **not** modify `Mikeyphw/InstaActions`).
Framework: modern libxposed API 102, Vector 2.2+, Android 9+, intended to run on Android 16/Instagram 455 as well as other versions where Android lifecycle hooks work. Source implementation has no dependency on Instagram's obfuscated names.

## User experience

Enable the **Instagram** scope for InstaTaskManager in Vector. Force-stop Instagram and reopen it. A compact draggable **TM** chip appears within Instagram. Tap it to open a nonmodal floating panel. Drag its title bar to move it; tap × to close. You can browse Instagram outside the floating panel while the on-demand sampler records the current process. **Closing the panel stops measurement and its worker thread.**

- **Overview:** current-main-process CPU (single-core units), PSS, RSS, Java used heap, native allocated heap, thread count, UID aggregate RX/TX rates, visible same-UID Instagram process names and their PSS (where the Android API allows it). Same-UID process information is polled every ~10s, not continuously.
- **Threads:** most active sampled threads in the main process, `% of one core` per thread. A question-mark hint can be given from the thread's name (media, network, rendering etc.). This is a **heuristic**, not proof of Reel/DM/Story ownership.
- **Parts:** declared Android Activities, Services, Receivers, and Providers with component-enabled states (read-only). Android does not expose per-component PSS/CPU accounting for all in-process features.
- **Social:** optional on-demand exact-APK verification and read-only Repost/facepile method-shape inventory for Instagram 455; the force-enable controls remain locked.
- **Controls:** open Android's native Instagram app settings, or hide the TM chip until the Instagram process restarts. No unverified Instagram component or network restriction is applied automatically.

### Measurement interpretation

- **PSS**: proportional memory for the current process; **RSS**: memory resident in RAM; Java and native heap figures **overlap with** PSS/RSS. Do not add them.
- **CPU %**: 100% means one full CPU core, so an app can exceed 100% when multiple cores are busy.
- **UID network RX/TX**: aggregates traffic for all processes using Instagram's UID; not attributable to a specific feature. Android can return `UNSUPPORTED`, shown as unavailable.
- **Thread names**: only an approximate clue. Instagram uses multiple shared pools; a `Media` or `Network` thread does not uniquely correspond to Reels or Stories.
- **Component inventory**: listing the declared services and receivers does **not** establish resource usage or safe disablement.
- Monitoring itself has overhead (debug memory query and bounded `/proc/self/task` reading every 2.5s). There is no persistent recorder or automatic upload.

### Permissions and coexistence

No Android runtime permissions, network requests, root shell operations, or access to Instagram account content. Hooks only Android `Activity.onResume`, `onPause` and `onDestroy` within Instagram's **main** process. Each callback calls `chain.proceed()` once; it does not suppress Instagram operations. No hook touches Reel menu code or InstaEclipse internals. The optional Social scan performs bounded metadata-only reflection after exact IG455 APK verification; it does not install further hooks. Multiple LSPosed lifecycle interceptors still require runtime coexistence testing.

The `TM` chip is injected into the current activity's decor view. It can obscure part of the screen; drag it or hide it. If the chip does not appear, check Vector's module and Instagram scope, API 102 compatibility, and `ITM_P00` logcat markers. Do not claim device acceptance from an APK compile alone.

## Build on GitHub Actions (recommended)

The ZIP contains an independent Gradle Android project and `.github/workflows/android.yml`. Do not run Gradle inside Termux. Create your own repository, push the project files, then run the hosted workflow. Sample commands after extracting project into `~/Code/InstaTaskManager`:

```zsh
cd "$HOME/Code/InstaTaskManager"
python3 scripts/validate.py
# Optional if javac is installed:
bash scripts/javac-smoke.sh

git init -b main
git add .
git commit -m 'feat: initial read-only InstaTaskManager P00'
gh repo create Mikeyphw/InstaTaskManager --private --source=. --remote=origin --push
```

After the GitHub Actions `Insta Task Manager — source and Android APK` run completes **successfully**, download its `InstaTaskManager-P00-debug-<commit sha>` artifact, verify `SHA256SUMS` and install `app-debug.apk` using `adb -s localhost:5555 install -r`. Only enable its Vector scope for `com.instagram.android`. Do **not** replace InstaActions or InstaEclipse.

## Why there's no arbitrary "disable" switch yet

Android app components and Instagram features are not 1:1. A foreground service might be needed by notifications or playback; a receiver can initialize other code; a DM/reel screen can share threads with the main feed. `PackageManager.setComponentEnabledSetting` can disable essential Android components, resulting in missing messages, crashes, or login problems. Therefore P00 has **zero** calls to this API. Disabling Instagram components is a later, separately version-verified milestone with a reversible allowlist. InstaEclipse's Distraction-Free feature already hides some sections, but hiding a UI tab does not establish less CPU/network work.

## Acceptance roadmap

See `docs/ROADMAP.md`. P00 is not a proof that any Instagram component is wasteful and not yet qualified on the physical device.

## Social P01 — Repost button and friend-avatar discovery (folded into P00)

This **separate InstaTaskManager feature** incorporates the APK/source audit from
`docs/INSTAGRAM455_REPOST_FACEPILE_SOURCE_AUDIT.md`. It does **not** change or
require InstaActions and does not copy InstaEclipse GPL code. Open **TM → Social**
and explicitly tap **Scan IG455 (read-only)**. The worker verifies installed
Instagram version `455.0.0.8.72`, exact base-APK length `238346353` bytes, and
SHA-256 `fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6`
before resolving any candidate class. Unknown APKs and ambiguous overloads are
reported as unavailable rather than patched. The scan is canceled when the
panel closes; it may temporarily delay performance sampling while hashing
the large APK in a background worker.

It inventories Repost button eligibility, native Repost UI/click handlers,
liked-by/reposted-by context and friendly avatar renderers. A method-shape
match is **not** an enabled Repost button, a method execution, proof of account
permission or proof that friend-avatar data exists. There are no calls to
obfuscated Instagram methods, no new Xposed interceptors, no network calls,
no user/media data capture, and no fake friend faces. Five separate eventual
presentation controls (Feed Repost, Reels Repost, liked-by faces, reposted-by
faces, friendly bubbles) remain **not implemented** until a verified UI-only
branch and server payload behavior can be tested safely. Set InstaEclipse's
"Disable repost" switch OFF during later tests.

The single consolidated bootstrap ZIP supports fresh installation and an
**exact-original-P00** project upgrade; it refuses to clobber modified P00
files and does not interact with Git remotes or the device.

## P04 HF04 early-branch input presence
Read-only exact-IG455 A/B diagnostic correlating `A02/A0u` nullness with original Repost UI state, guarding public field shapes and exporting counts only. No account/media references or return-value mutation.
