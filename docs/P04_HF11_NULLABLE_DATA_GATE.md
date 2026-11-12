# HF11: Exact nullable D3r Repost UI-state trial

## Physical evidence (HF10, 2026-11-12 12:26:11 UTC)
- Affected A: native A03 present 50/50, D3r exact-key original false 42, null 7, one not captured; all returned X.09j4 singleton.
- Working B: native A03 present 31/31, exact-key D3r true 31/31, all returned X.09ZC payload.
- First MobileConfig false for both cohorts; cannot be treated as gating difference.
- Hook installation 1/1 successful; caller deoptimized.

## Controlled behavior
- `P04ExactGateTrial` now permits **only** a strictly false-only A override on the exact D3r key, with A null results retained unchanged.
- Requires >=5 false-singleton observations on A and >=5 true-payload observations on B, A false >=75% of observed A exact-key results, >=75% getter coverage of native-data frames on BOTH cohorts, no A true/other, no B nontrue, no unexpected singleton/payload relationship, native A03 present for every frame, ART deoptimization verified.
- The existing FIRST_CONFIG gate still requires a clean false-vs-true split.
- Must explicitly Start A and press Try A UI gate; never arm on B. Manual account labeling is not identity verification; don't switch accounts while recording or while the trial is active.
- Only original `Boolean.FALSE` on exactly the verified key within selector-4 frames can become `Boolean.TRUE`. Null remains null; there is no fabricated Repost payload and no action dispatch. Revalidates cohort qualification before each change.
- Experimental frames are excluded from the original-only A/B qualification counters; otherwise a successful trial would create false/payload observations and incorrectly invalidate the original baseline. A new contradictory *unmodified* outcome still disarms the trial before the next change.
- Trial automatically ends after up to six modifications or 30 seconds, or Stop/Trial OFF. Failure or strange UI: Trial OFF and restart Instagram.

## Qualification
- Local pure-Java tests and source-contract tests are not a demonstration that the Repost button appears or that the backend accepts the operation.
- After hosted CI pass, install exact-head APK; enable Vector scope, restart IG; run same-public-content A/B capture; on A Start A, Try A UI gate, navigate to a fresh eligible public Reel. If trial changes results, export while recording or after Stop, documenting visual outcome.
- If the UI remains hidden after local Boolean changes, do not force complex `X.09ZC` result objects; inspect subsequent state construction and UFI.
