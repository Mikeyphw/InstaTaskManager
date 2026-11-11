# P04 HF03 — exact IG455 upstream primitive decision trace

**Evidence and limits:** exact original Instagram `455.0.0.8.72` base APK SHA-256 `fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6`, `classes5.dex`. The `X.0Ie0.invoke` selector-4 branch returns either `X.09j4` (singleton) or `X.09ZC` (structured). User A produced singleton 38/38 and user B structured 33/35. These are **runtime classes, not proven entitlements**.

## Anchored primitive call sites inside selector 4
- `com.facebook.mobileconfig.factory.MobileConfigUnsafeContext.BdA(long): boolean` is called from this branch at DEX code units ~63 and ~325, **but is abstract**. Hook the verified concrete implementation `X.02ja.BdA(long): boolean` instead. This may not cover other implementations. The long configuration argument is never read or stored.
- `X.0ApG.D3r(int): java.lang.Boolean` is also an abstract interface method, used in the same branch at code units ~76 and ~191. Hook the verified concrete implementation `X.02ib.D3r(int): java.lang.Boolean` instead. It may not cover other implementations; no input or string is stored.
- `X.02xy.A04(UserSession,String):boolean`, a conditional check at ~245. Neither session nor string parameters are read or stored.
- The earlier `X.0F2B.invoke(Object)` selector 11 branches lead to corresponding downstream singleton and payload states; their original hooks remain read-only.

P04 HF03 verifies **all five concrete method signatures** plus APK SHA before installing observers. The abstract base interfaces are never hooked. It traces Boolean results only **on the same thread, while an original `X.0Ie0.invoke()` selector-4 call is on the stack**, and only while manual A/B recording is active. Calls outside that scope are not retained. Results aggregate by A/B, original result class (singleton/payload/other), getter identity, and getter invocation order (1–4). No configuration keys, object fields, account IDs, media IDs, usernames, session values, or string contents are exported. Each result state has a cap of 256 call frames; higher call ordinals are ignored. Original Xposed `chain.proceed()` calls occur exactly once and all original return objects are preserved.

**Caveat:** these methods may be hot for the wider app, so HF03 adds passive interceptors even when tracing is inactive. Monitor the profiler and disable the module if the UI feels sluggish. An implementation can be invoked through other concrete types, so empty samples from X.02ja/X.02ib do **not** prove the upstream checks were absent. 'No samples' is not a negative eligibility finding.

## User sequence
1. Upgrade/install HF03 APK from PR #3 hosted CI. Restart full Instagram 455; wait for `READY: 5 passive IG455 hooks; upstream read-only`.
2. On affected account, Start A; view the same public post/Reel; stop. Then switch to the working account, Start B; repeat; stop.
3. Export. The `P04 HF03 UPSTREAM PREDICATES` section distinguishes distinct primitive call sequences and variants; compare path differences before considering an override.

**Not supported yet:** forcing Repost/Friends UI or server eligibility. The P04 scalar Boolean override is removed because HF02 proved its eligibility assumption false. Friends tab/avatar features remain diagnostic-only. PR #1/#2/#3 stay unmerged.
