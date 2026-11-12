# P04 HF05 · IG455 concrete MobileConfig dispatch target

## Primary evidence

The original Instagram `455.0.0.8.72` APK (base file 238346353 bytes,
SHA256 `fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6`)
contains the selector-4 branch of `X.0Ie0.invoke` in `classes5.dex`.

- At method code offset +52 (16-bit code units): `X.01ym.A02(X.06g8,Z):X.0mMk`.
- At +63: virtual `MobileConfigUnsafeContext.BdA(J):Z`, with result
  followed by a branch decision at +67. This call is downstream of the
  verified `A0u-present` input.
- A concrete class in `classes22.dex`, `X.0T4C`, **directly implements**
  `X.0mMk` and `MobileConfigUnsafeContext`; it declares non-abstract,
  non-static, public `BdA(long):boolean`.
- The previous HF03 `X.02ja` class implements `X.0Orl`, not `X.0mMk`
  or `MobileConfigUnsafeContext`. Hooking it was not justified for the
  Repost selector-4 call, and HF03/HF04 showed zero observations.

HF05 preflights those exact types with `isAssignableFrom`, checks the
concrete method shape, and installs only a passive hook. During a manual
A/B capture, existing thread-local frames collect `true/false` counters
and correlate them with returned singleton/payload UI states. The module
never records the long config key, account IDs, feature names, model data,
or result objects, never calls an Instagram getter outside the normal path,
and returns the exact original Boolean.

## Interpretation and limits

A/B first call-ordinal differences can suggest the controlling client gate,
but call ordinals can shift if other nested MobileConfig calls occur. A
Boolean observation is **not** proof of server permission. Absent counts
mean **not observed**, not a `false` result. No permission bypass, fake
Repost UI state, or friend-avatar modification is implemented.

## Qualification

1. Hosted CI source/JVM/Android API102 jobs green for exact PR HEAD.
2. Install exact-head successful APK via ZIP's `install-latest` mode. It
   verifies SHA256 before clean-uninstalling any older signed debug APK;
   this resets the module preferences and Vector/LSPosed scope.
3. Restart Instagram and confirm P04 status says HF05 config-dispatch.
4. A = affected, B = working; repeat same Feed/Reel and export Social.
5. Determine whether the concrete MobileConfig result differs before
   considering a very narrow reversible presentation-only experiment.
