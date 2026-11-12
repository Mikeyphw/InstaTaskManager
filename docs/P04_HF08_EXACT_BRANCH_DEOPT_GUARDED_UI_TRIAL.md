# P04 HF08 — Exact Repost branch, ART deoptimization, and bounded local UI trial

## Motivation and observed results

HF06 physical-device report (2026-11-12 00:38:51 UTC) confirmed 2/2 core and 3/3 optional hooks READY, but no MobileConfig observations. Manual Account A (affected) returned `X.09j4` 42/42 times while manual Account B (working) returned `X.09ZC` 54/54. `X.07XN.A0u` was present in all 96 frames. This does **not** show which eligibility input differed, and does not justify changing arbitrary Booleans. Account A can repost normally from official Instagram Lite; this is an account/client discrepancy rather than proof of universal backend refusal.

## Exact Instagram APK bytecode re-audit

Source package: `Instagram_455.0.0.8.72.apk`, size `238346353` bytes, SHA-256 `fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6`. The module independently checks these before installing runtime Repost observers.

The `classes5.dex` `X.0Ie0.invoke()` `packed-switch` maps selector **4** to code offset `12` (in 16-bit code units). Within that branch:

- `X.07XN.A0u` is tested for null at code unit **40**, branching to the singleton state at **351**.
- **`X.0Ie0.A03`**, cast to `X.0FWn`, is read at code unit **30**. This is another prerequisite for reaching the structured result; if null, code can exit at **184** or **88**. HF04 had NOT checked A03.
- The first config factory `X.01ym.A02(X.06g8,Z)` runs at **52**; `MobileConfigUnsafeContext.BdA(J):Z` at **63** uses constant **`0x0081089a0045309d`** (loaded at 56). A separate state-rendering call at **325** has a different long constant. HF08 only records the **first gate** in its structured counters.
- Repost-data `X.0FWn.A01` returns `X.0ApG`; its nullable Boolean `D3r(I)` at **76** and/or **191** uses `0xdf82522e`. **Only that key** is considered for the experimental data-gate candidate. The module does not query it itself.
- `X.09ZC` structured UI state is constructed by Instagram itself around **335–346**. HF08 never constructs or copies it.

The factory returns a lazily initialized `X.0mMk` interface. From the exact 31 DEX files, the direct `X.0T4C` abstract concrete-method owner has several subclasses, so a hooked method being resolvable is not enough to prove which implementation was called.

## API and tracing fix

Modern libxposed API 102 documents `deoptimize(Executable)` for the case where hooked small callees have been **inlined into their caller** and therefore never produce hook callbacks. HF08 calls `deoptimize` on **only** the exact `X.0Ie0.invoke()` method after the observer hooks install, and reports its Boolean success. A/B capture still works if deoptimization fails, but the new UI trial refuses to arm. No app-wide deoptimization or root/shell operation.

`P04BranchInputs` checks the single public Object field `A03` and its runtime type `X.0FWn`, without reading field contents. `P04PredicateTrace` identifies the exact keys within its thread-local selector-4 frame, then aggregates original outcome categories against account/manual cohort and native UI result class. No raw keys or personal data appear in the export.

## Guarded UI experiment

One explicit button, **Try A UI gate**, remains inactive outside manual Account A capture. It is guarded by ALL of these conditions:

1. `deoptimize` succeeded on the exact verified caller.
2. Both A and B were captured after this installation, in the same Instagram process.
3. The native A03 Repost data object was PRESENT, type-correct, for **all** sampled A and B frames. If missing or unavailable even once, refuse.
4. At least three A observations are Boolean.FALSE leading to the original singleton state, AND three B observations are Boolean.TRUE leading to the original structured state, for the **same exact keyed source**, without counterexamples.
5. The user explicitly starts **A again** and presses **Try A UI gate**.

The trial then substitutes at most **six** original false Boolean return values with true in that *one exact callsite context* over at most **30 seconds**. The method is otherwise unchanged. Stop, Clear, account-cohort changes, Trial OFF, trial expiry or process restart disarms the substitution. It does not fake a media model, server rights, friend data, Repost button event, or action handler. There is **no guarantee** that the UI will appear or that Instagram's backend will accept actions through the full client. The trial does not bind to an account ID; the user must never switch accounts during the 30-second window.

If A's input object is missing, or B's outcome isn't unambiguously true, **no override is available**. The export should explain that instead of letting the user repeatedly click inert toggles. When trial ends, Instagram may need its UI re-rendered or the process restarted to restore pretrial presentation.

## How to qualify

1. GitHub-hosted Android/Gradle CI: source/safety/JVM tests, Android compilation, SHA256 APK artifact (one PR workflow per commit). The ZIP embeds the exact-head installer.
2. On Android 16 with Vector libxposed 102 and exact Instagram 455 APK, open TM → Social and verify core READY and `Deoptimized caller: YES`.
3. Capture affected account A and working B on the same public feed post and Reel (prefer 4+ samples each), Stop; export report and inspect HF08 exact branch section.
4. If report identifies `Eligible guarded trial: ...`, switch back to affected account, Start A, press **Try A UI gate**, minimize panel, and reopen the SAME native feed/Reel. Observe whether the native button is created. Don't click if it appears over content that isn't yours to repost.
5. Stop / Trial OFF, and restart Instagram if UI changes persist. Inspect crashes and confirm original Like/Save/Share, InstaEclipse and session behavior. No live change can be called accepted on offline test results alone.

## Known limitations

- `deoptimize` is API-supported, not guaranteed by the current Vector runtime; if it returns false the trial is unavailable.
- A null A03 may reflect missing full-app account/media data and cannot be fixed by returning a Boolean.
- The earlier social-avatar checkboxes are **still requested-only**; this experiment targets Repost UI state only.
- Source-level tests cannot validate Instagram's runtime permissions, Android ART optimization state or backend behavior.
