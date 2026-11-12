# P04 HF12: bounded manual trial with one working-account outlier

## Physical evidence (2026-11-12)

The user-provided A/B export recorded A (affected) 42 `false` exact Repost-data D3r outcomes (46 native A03 present) and B (working) 26 `true` / payload outcomes plus one `false` / singleton (27 native A03 present). The strict all-true-B rule in HF11 therefore refused the trial.

## HF12 experimental user choice

- A manual **Try A UI gate** accepts this specific pattern: >=10 A false/singleton and >=10 B true/payload **when an outlier is present** (otherwise inherits HF11 >=5), and at most **one** B false/singleton representing **<=5%** of measured B exact-key calls. A optional null/singleton remains bounded by HF11 at 25%, and is **never converted** to true.
- No B false/payload, B true/singleton, B null, A true/other, or other contradictory state is tolerated. At least 75% exact-gate coverage of native-data frames in each account, native A03 data in all, deoptimization, and no missing data still required.
- Only selector-4's pinned exact D3r key (`0xdf82522e`) when the actual Repost-data receiver is active; only during manually labeled A capture; only **Boolean.FALSE** changes to **Boolean.TRUE**. No automatic repost and no server permission bypass.
- Maximum six changes over 30 seconds, manually armed, immediate stop/Trial OFF, requalification before every changed result, baseline counters exclude intervention frames. A new contradictory observation immediately disarms.
- This is **not** proof of successful Repost. Test only on public, repostable posts; observe presence of native button and never infer account permissions from UI alone.

## Device verification

1. Install successful exact-head hosted APK; re-enable scope and restart full Instagram 455.
2. Clear A/B, capture affected account A and working B against the same public posts, stop each. Do not switch labeling incorrectly or restart the process between captures.
3. Return to affected account, **Start A**. Press **Try A UI gate**; confirm the indicator starts with `ARMED: HF12 EXPERIMENTAL`.
4. Visit one public Reel while still within 30 seconds; note button and optional tap behavior only if appropriate. Stop/Trial OFF and export the report; check **altered 0/6** vs positive count, Repost return type, and original trace preservation.
5. If app becomes unstable, disarm and restart Instagram. Never share account identifiers or private posts.

Source/Java tests are not physical qualification; there is no guarantee that a client-only eligibility flip will render or enable a native Repost action.
