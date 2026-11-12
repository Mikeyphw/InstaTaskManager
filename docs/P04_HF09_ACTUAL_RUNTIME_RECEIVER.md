# P04 HF09: exact runtime MobileConfig receiver `X.02nr`

## Provenance

- On-device HF08 report from 2026-11-12 01:57:15 UTC observed `X.02nr` as the actual return class of the `X.01ym.A02` factory at selector 4, in **29/29 affected-account frames and 27/27 working-account first factory calls**. The working account also had 27 additional second factory returns of `X.02nr`. This evidence comes from live A/B tracing; *not* a static subclass guess.
- Exact Instagram `455.0.0.8.72` APK SHA-256: `fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6`.
- `classes.dex` declares public final class `LX/02nr;` with flags `0x11`. The class directly declares public final `BdA(J)Z` (flags `0x11`, code item at decimal byte offset `5532456` / `0x546b28`). It is an executable method; the seven-code-unit implementation delegates to lower-level MobileConfig handling.
- Both `X.02ja` and abstract owner `X.0T4C` were previous unproven receiver assumptions. They can be real classes with callable methods and still never be invoked on the traced Repost path.

## HF09 behavior

- `P04ActualConfigTarget.resolve` locates `X.02nr`, verifies it implements the declared configuration-provider return contract (`X.0mMk`) and declares public concrete instance `BdA(long): boolean`. No instance or key value is accessed to choose the receiver.
- The previous `X.0T4C` hook is **replaced**, not supplemented, to avoid broad hooks and unnecessary app overhead. The globally installed `BdA` interceptor takes a cheap thread-local fast path outside a manually recorded Repost frame, so it does not inspect arguments or perform aggregated tracing while idle. The source preserves the historical HF05 validator as a regression contract but no longer installs it.
- `P04PredicateTrace` adds an explicit `X.02nr` factory category so on-device reports confirm actual provider usage without account IDs or configuration values.
- Only within a manually selected A/B `X.0Ie0` selector-4 original invocation are `BdA` outcomes aggregated, and only the **exact DEX-derived first-gate key** is evaluated for the bounded opt-in UI trial. Argument keys and objects are never exported.
- Core 2/2 A/B hooks remain available even if any optional observer fails; original Instagram return objects are never substituted or copied between accounts. Any UI trial must still satisfy the prior A/B Boolean and native data gates, six-result cap, 30-second expiry, and manual A-only selection.

## Current limitation

HF09 has **not** proven that repost visibility can be restored. It should first produce nonzero `HF09 actual X.02nr MobileConfig Boolean` or `FIRST_MOBILE_CONFIG` counters. If no exact-gate callbacks occur despite the provider being present, investigate the method dispatch or key evidence; do not force any result blindly. Neither the Friends tab nor friend-avatar controls are enabled by this change.

## Install

`python3 InstaTaskManager_P04_HF09_Actual_Receiver_AB.zip check`
`python3 InstaTaskManager_P04_HF09_Actual_Receiver_AB.zip`
`python3 InstaTaskManager_P04_HF09_Actual_Receiver_AB.zip install-latest`

The last command requires the exact current PR #3 SHA to have a successful hosted run, downloads the hosted APK, verifies SHA-256, and uninstalls/reinstalls via `localhost:5555`. Re-enable Vector scope and restart Instagram after reinstall.
