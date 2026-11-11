# P04 HF01 — Capture feedback

Only UI and capture-counter visibility are changed. All P04 gate hooks and their evidence thresholds remain unchanged.

- Permanent banner: verification / ready / recording A or B with a 1-second elapsed timer and aggregate gate-event counter.
- TM chip reads **● A** or **● B** while a capture is active, including when the panel is closed.
- Start/Stop feedback uses a short toast; active capture buttons cannot be used to switch cohorts without Stop.
- The Social report refreshes while visible without rebuilding the whole switch list or reading Instagram data.
- Scan and Export give visible start/completion status; Export button displays Saving… while busy.
- All captures are volatile and account labels are manual, not verified identities.
- A capture with **0 events** means no verified selector was observed, not that the trace failed to run.
- Closing the panel **does not stop** an A/B capture. Tap Stop explicitly before switching accounts. Terminating Instagram ends the capture.

The active capture banner is not proof of feature eligibility or successful override.
