# InstaTaskManager development roadmap

## P00 — safe read-only baseline (source implemented; runtime unqualified)

- Independent modern libxposed API102 module scoped only to Instagram main process.
- Nonmodal draggable `TM` chip and panel; per-process CPU/PSS/RSS/Java/native, per-thread CPU, UID network rates.
- Android same-UID process PSS inventory; declaration/state inventory for Activities, Services, Receivers and Providers.
- Active only while panel open; explicit stop, hide and Android app settings handoff.
- No special permissions, action hooks, network service, shell calls or automatic component disabling.
- Source policy tests, pure Java profiler parser tests, GitHub-hosted APK build and module-metadata proof.
- **Must still qualify:** install on Android 16/Instagram 455, confirm sampling, lifecycle cleanup and no visible regressions with InstaEclipse + InstaActions both enabled; verify frame/jank overhead and APK metadata in generated APK.

## P01 — scenario-aware cost analysis

- Explicit user-started 30/60s recording windows for Feed, Reels, Explore, Stories, DMs and idle, with real baseline/active comparisons.
- Keep process CPU/core-time, PSS, UID network deltas and churn; compute confidence bounds and variance across repeated windows.
- Prevent misattribution from warm/cold caches, preloaded content, network traffic and Android background jobs; don't claim exact per-feature cost.
- End-to-end response-time and frame jank with safe Android APIs where possible; bounded sample overhead / opt-in sampling rate.

## P02 — verified subsystem attribution and dependency graph

- Read exact installed Instagram APK version, size and SHA256 before interacting with obfuscated classes.
- Independent DexKit discovery and uniquely resolved lifecycle invocation counters for optional subsystems (only when proven safe), no Media/value access.
- Map roots/tasks/threads and cached executors to user-visible features, with provenance and ambiguity refusal.
- Manifest components are catalogued as a dependency graph; never infer safeness from a class name alone.
- Separate independent detailed proof with InstaEclipse ON/OFF and the latest InstaActions runtime.

## P03 — genuinely reversible optional switches

- Introduce **only** verified nonessential feature-specific allowlist entries, each with exact APK hash, declared dependencies and failure/rollback behavior.
- Default OFF (no action). Explicit confirm and restart; no arbitrary service/receiver/provider blocking.
- Show expected effects (broken notifications/DMs/etc) before any change; refuse if dependency graph incomplete.
- Provide restore-all independent of Instagram launch, record no account data; test restore after crash/reboot/package update.
- Avoid duplicate controls already provided by InstaEclipse unless measured benefit is established.

## P04 — compatibility, cost and security acceptance

- Android 16 physical device, exact Instagram build and Vector API 102; coexistence matrix with InstaEclipse and InstaActions.
- JUnit, hosted emulator if supported, API 102 hook, source/commit/APK hashes, lifecycle stress, activity churn and repeated open/close panel.
- Measure profiler's own PSS/CPU/frame overhead; maintain a conservative sampling budget and fail closed under restrictions.

## P05 — release

- Source-pinned hosted release APK, manifest proof, release notes, rollback/uninstall path and understandable per-switch warnings.
- No automatic installation or switching by this source bootstrap. Stable upgrade path, version gates, privacy review, no undocumented privileges.

## Social P01 — consolidated into P00, read-only discovery (source implemented)

- Add **Social** panel with explicit `Scan IG455` action; never scan in background unless requested.
- Exact installed Instagram base APK 455.0.0.8.72 / 238346353 bytes / SHA256 guard first.
- Metadata-only, fail-closed reflection of Repost eligibility and UFI state, native action handlers, liked/reposted-by social context and facepile renderers, with ambiguous matches rejected.
- No extra Xposed hooks, no result overrides, no action dispatch, no invented social data.
- Pure-Java method-shape tests and source-level security/privacy regression.
- Physical device acceptance STILL pending. A static reflection match does not demonstrate button eligibility or native friend data.

## Social P02 — verified optional visual toggles (NOT IMPLEMENTED)

- Separate default-off controls: Feed Repost, Reels Repost, liked-by avatars, reposted-by avatars, friendly viewer bubbles.
- Identify exact isolated UI-hidden branches for the target IG build and confirm server eligibility/data independently. No all-purpose `Object` return substitution.
- Preserve native action handlers and Instagram server restrictions; fail closed if data missing.
- Test with InstaEclipse DisableRepost OFF, on-device Vector API102, source-hash pinned live hook proof, cross-account and lifecycle cases.
- Do not enable until source-level permission, collision and rollback tests are green.

## Social P03 — release qualification (NOT IMPLEMENTED)

- GitHub-hosted Android CI, APK SHA256 and Android 16 live device confirmation.
- Compare visible friend facepile counts to native source, never fabricate profiles.
- Demonstrate native eligible Repost actions without breaking Like, Save, Share or feed.
- Coexistence acceptance with InstaActions + InstaEclipse; no automatic publishing until accepted.

## P03 LOW-OVERHEAD + SOCIAL REQUEST TOGGLES — implemented, runtime unqualified

- 5s low-cost sample interval, /proc thread scans every ~20s, PSS every ~30s,
  process inventory every ~40s. Estimated profiler sampler-thread CPU included.
- Five independent persisted default-off Social feature requests.
- Single manual best-effort native Repost View reveal with IG455 exact SHA + 9/9
  scan guard, bounded view traversal, no repost/like action dispatch, reversible.
- Friend-avatar toggles record user intent only; **not active hooks**. Exact data
  and presentation suppression remain unverified.
- Requires hosted CI and physical device qualification; no automatic merge.

## P04 — IG455 account A/B gate trace and narrowly guarded scalar trial (2026-11-11)

Status: LOCAL SOURCE + JAVA SMOKE; HOSTED CI AND PHYSICAL VALIDATION PENDING.
Base: P03 HF01 commit `2423c2f76c8e55e914d24e9b2a829ee29da08fe4`.
Stacked PR P04 must target P03 PR #2 branch; no merge or force-push.

- In full Instagram 455, affected account misses Repost, Friends tab, and friend bubbles on multiple devices, but successfully reposts via Instagram Lite; another full-app account has features. This indicates account-by-client differences rather than universal repost backend rejection.
- Exact APK hash/byte length + two DEX-verified multi-purpose method descriptors are checked before passive hooks. `$t=4` in `X.0Ie0.invoke()` corresponds to Repost UI-state/eligibility traces. `$t=11/12` in `X.0F2B.invoke(Object)` corresponds to Repost UFI traces. All three selectors are diagnostic candidates, not proven unique eligibility predicates.
- Manual Account A/B captures count Boolean/null/object results without retaining identifiers, arguments, or result content. Transient label checks count only exact `Repost`/`Friends` words in visible native views, excluding the module panel; absence is not proof of absence in Litho or other rendering systems.
- The only active trial requires >=3 pure Boolean `false` results on A and >=3 pure Boolean `true` results on B from exact selector 4, manual A selected, and a separate explicit user action. It can replace at most eight scalar `false` values in the running process. It is off on startup, Stop, switching cohorts, or Clear. It does not force any complex UI state or social-data lists.
- Follow up only after exported A/B report: identify actual UI state field and Friends-tab/social-context data gate, then add verified, reversible presentation-only controls. Do not infer server eligibility or manufacture friend data.

## P04 HF01: Capture feedback

- Add a persistent trace state banner and minimized chip status, live A/B elapsed timer and gate-event counter.
- Surface Android hook verification outcomes without restarting the panel.
- Keep user-visible feedback for Start/Stop, UI label count, scan and export.
- Count-only A/B observers and bounded Boolean trial remain unchanged.
