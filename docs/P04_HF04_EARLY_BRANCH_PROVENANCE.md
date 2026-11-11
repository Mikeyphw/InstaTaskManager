# P04 HF04: exact IG455 early-branch input-presence A/B diagnostic

## Why this probe

2026-11-11 real-device HF03 report: manual A (affected): 39/39 `X.09j4` singleton states; manual B (working): 35/35 `X.09ZC` structured states. The three scoped upstream primitive observers did not see values on A, and saw only false session-check calls on B. This does not establish a Boolean override.

The exact IG455 `classes5.dex`, method `X.0Ie0.invoke():Object`, switch selector 4, reads outer `X.0Ie0.A02`, casts it to `X.07XN`, reads `X.07XN.A0u` of type `X.00w8`, then uses an `if-eqz` branch at code-unit 40 to the `X.09j4` singleton return at code-unit 351. This input's absence is one sufficient early-return reason, but there are other conditions that also return the singleton. The source describes data readiness, **not** account permission or a named MobileConfig flag.

## What the capture records

For each exact selector-4 call while manual A or B is active, preflight-checked public fields are read on the original lambda instance. One of four categories is recorded, never field contents: `A02-null`, `A0u-null`, `A0u-present`, `unavailable`. It is correlated with the original return variant, `X.09j4`, `X.09ZC`, or other. Counts are bounded by 256 per variant and A/B cohort. No identity/media value, content, URL, string or object reference is retained, and the result is returned unmodified.

New exported section: `HF04 early-branch input presence` underneath `P04 HF03 UPSTREAM PREDICATES`. This is a *structural diagnostic*, not a force-enable implementation. The previous HF03 hook coverage remains partial.

## Qualification

Source preimages pin exactly PR #3's HF03 head. Hosted CI must pass Android compilation and artifact metadata verification before installation. Live acceptance requires using same public Reel/post on A and B and exporting new A/B summary. No merges or releases.
