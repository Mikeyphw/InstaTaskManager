# P04 HF10 — actual runtime Repost-data D3r receiver

## Evidence (2026-11-12)

HF09's local report (04:53:47 UTC) showed 42/42 A singleton results and
29/29 B structured results; `A03` was present throughout. The exact
`X.02nr.BdA(long)` first MobileConfig result was `false` for both accounts;
HF09 observed three config calls in each A frame and four in each B frame.
No exact-data getter result was observed, and no trial was armed.

Verified IG455 base APK: `Instagram_455.0.0.8.72.apk`, length 238346353,
SHA256 `fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6`.

Actual selector-4 Dalvik code (classes5.dex, `X.0Ie0.invoke()`):
- `0x0034–0x0043` gets `X.01ym.A02`, calls interface `BdA(long)` with
  `0x0081089a0045309d`; if `false`, jumps to `0x00b8`.
- `0x00b8–0x00cc` checks presence of the native Repost `A03` object,
  then invokes its `X.0FWn.A01` provider's
  `X.0ApG.D3r(0xdf82522e): Boolean`, and requires `Boolean.TRUE.equals`.
- At `0x0056`, a false eligibility check jumps to the singleton `X.09j4`
  return at `0x015f`. The structured `X.09ZC` constructor is at `0x014f`.
- This is a necessary branch decision, not proof that it is the **only**
  difference, or that overriding it will restore all Instagram features.

## HF10 implementation contract

1. Verify the exact installed base APK, caller selector and fields.
2. For an explicit A/B capture, examine **only** native `X.0Ie0.A03` and
   `X.0FWn.A01` object types. Neither object is retained or printed.
3. Resolve the existing object's public concrete `D3r(int): Boolean`
   implementation without invoking `D3r` or reading its arguments.
4. Install a scoped interceptor asynchronously (at most three distinct
   methods, once per owner; no retry on failure). The original must execute
   exactly once; only count the result when already inside selector 4.
5. Track only the pinned key `0xdf82522e` and pass its **original** result to
   the existing A/B comparison. Never display media, friend or account data.
6. Keep the existing experimental gate OFF by default. It can modify
   `Boolean.FALSE` only if strictly qualified A/B evidence is recorded,
   manually armed for A, native data exists, deoptimization succeeded,
   and the six-result/30-second limit is not exhausted.
7. If no concrete implementation can be resolved or hooked, report
   attempts/failures; the core Repost A/B capture continues unchanged.

## Safety and limits

This is **not yet a confirmed repost-enablement fix**. Hook installation
outside package-load can fail on some framework combinations and is optional.
All trial changes are ephemeral and restarting Instagram restores original
behavior. The Friends tab and avatar bubbles require separate, proven data/
visibility gates. Hosted compilation and device acceptance remain pending.
