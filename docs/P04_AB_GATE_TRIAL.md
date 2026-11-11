# P04: exact-IG455 Account A/B gate trace, optionally guarded Boolean trial

## Why

The affected account lacks Repost, the Reels Friends tab, and friends' avatars in full Instagram on two devices; the same account can repost normally in Instagram Lite. The other account in the full Instagram app has the features. This supports an account-and-client-specific discrepancy, not a globally disabled backend Repost capability. We do not yet know whether the full client receives different server data or checks a cached rollout flag.

## Static evidence from user-supplied Instagram 455 APK

- Version: `455.0.0.8.72`; exact base APK bytes: `238346353`; SHA-256: `fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6`.
- `classes5.dex`: `X.0Ie0.invoke():java.lang.Object`, switch on public `int $t`; packed-switch case **4** includes both `android_purge_26_q3_RepostButtonUseCase_getUiState` and `...isEligibleForRepostButton`. This is a **multipurpose** return; do not force its entire `Object` state to `true`.
- `classes17.dex`: `X.0F2B.invoke(java.lang.Object):java.lang.Object`, switch on public `int $t`; cases **11** and **12** include respectively `UfiUseCase_getRepostButtonAndCount` and `UfiUseCase_getRepostButtonCountClickable`.
- These are source-anchored *runtime candidates*, not final evidence of the controlling account-level flag. Nothing in this release hooks repost action executors, modifies media/user IDs, or requests a hidden API.

## How to collect

1. Install the APK produced by P04's hosted Android CI. Keep InstaEclipse's **Disable Repost** OFF.
2. Open full Instagram 455 on the **affected account**. Open TM → Social. Verify P04 hook status says `READY` (it hashes the installed base APK asynchronously on the first TM open; do not capture while verifying).
3. Tap **Start A**. Close the TM panel, view a public Feed post and one public Reel for which the other account shows Repost. Optionally open TM and tap **Count UI labels** on the relevant screen. Tap **Stop**.
4. Switch to the **working account in the same full Instagram app**. Tap **Start B**, repeat with the same public content, optionally count UI labels, then tap **Stop**.
5. Open TM → Social → **Export**. The P04 section reports two manually labeled aggregates, no account IDs, post IDs, URLs, usernames, friend names, or profile photos.
6. If Instagram restarts the process while changing accounts, A/B volatile memory is lost. Export A before switching and B separately, then compare the exports manually.

## Optional, bounded scalar trial

Use **Try A Boolean gate** only if the diagnostic shows *at least three* observations of `Boolean.FALSE` on A and *at least three* of `Boolean.TRUE` on B for **selector 4**, with no other result types and no dropped samples. Select **Start A** again, then tap **Try A Boolean gate**. P04 allows up to **eight** exact-branch `false`→`true` results in this process, auto-disarms, never touches other selectors, and cannot bypass server permissions. Tap **Trial OFF** or **Stop** at any time. Do not switch accounts with the trial armed.

If the result is non-Boolean, missing, or mixed, P04 **refuses** the override. This is expected and safer than returning an incorrect Kotlin/Java UI-state type. Do not interpret “no change” as proof that the feature is impossible to enable.

## Limitations

- A/B is **manually labeled**, not account identity detection. Don't capture a session under the wrong label.
- Exact label counts inspect displayed UI text transiently, but record *only aggregate counts*, never the text.
- `X.0Ie0` and `X.0F2B` are multi-purpose classes; selector filtering is mandatory, and static labels are not proof of account eligibility.
- The first three Social checkboxes from P03 remain requested-only. No fake friend-avatar/social-context data is generated. Friends tab and avatar eligibility need dedicated evidence.
- This release's changes are contingent on hosted Android compile and real-device acceptance, not qualified by local Java tests alone.
