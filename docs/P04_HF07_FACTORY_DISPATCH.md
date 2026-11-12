# P04 HF07 — read-only runtime MobileConfig factory dispatch

## Reason
The physical A/B report after HF06 (2026-11-12 00:38:51 UTC) confirms
2/2 core and 3/3 optional hooks ready, with A 42/42 singleton and
B 54/54 payload results. `A0u` is present in 96/96 calls, but the
X.0T4C.BdA primitive observer records no results in either cohort.
Therefore the existing optional target may not be the runtime dispatch
receiver. No default Boolean value or server gating can be inferred.

## Exact APK evidence
SHA256 of original Instagram 455.0.0.8.72 base APK:
`fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6`.

DEX `classes5.dex` `X.0Ie0.invoke()` selector 4 executes the static
factory `X.01ym.A02(X.06g8,boolean):X.0mMk` BEFORE interface method
`MobileConfigUnsafeContext.BdA(long):boolean`. This factory is declared
in `classes.dex` and carries public static flags. Later in the same
branch it may call the factory again for the UI-state payload.

## Operation
HF07 observes the actual returned provider's **class metadata only**,
inside the existing thread-local selector-4 capture. It records up to
two calls per Repost frame and aggregates by manually labeled cohort
and resulting singleton/payload class. Known class labels `X.02ja` and
`X.0T4C` are distinguished; an unknown `X.*` class name is bounded to
40 characters and reported as code metadata, not content. No factory
arguments, configuration keys, identifiers, result contents, or account
data are inspected. It never calls the factory itself.

The additional observer is optional. Missing signatures, unavailable
hooks or errors do NOT disable core Start A / Start B. No hooked
Instagram return value is replaced.

## Physical acceptance
After hosted CI passes, install the exact-current-commit APK using
`python3 overlay.zip install-latest`, re-enable scope if needed and
restart Instagram. A/B capture using same Reel and post, Stop, Export.
Compare `HF07 factory result #1` and `HF07 factory result #2` lines.
If no factory results are observed despite 2/2 ready, treat as
instrumentation/inline issue; never infer false permission.

Hosted CI and live device behavior must qualify before any future
force-enable experiment. No fake repost or friend-avatar data.
