# Instagram 455 — repost button & friends' likes/reposts avatar bubbles

**Static source/DEX audit · 2026-11-10 · No Instagram hook installed or activated**

## Evidence and limits

- Supplied archive `instagram.tar.zst` contains one APK, `Instagram_455.0.0.8.72.apk`.
- Exact APK SHA-256: `fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6`.
- APK contains 31 `classes*.dex` files; selected DEX string references and code owners were inspected. This is **static evidence**, not proof of server-side account eligibility, server response contents, live UI visibility, or a hook working on the phone.
- Upstream InstaEclipse source pinned to `ReSo7200/InstaEclipse@e19ab0def2dc1ac197b7a2d4f8763f701ef68b1d`. Its `DisableRepostHook` comments discuss Instagram **447.0.0.21.81**. The relevant anchors exist in the attached **455** APK, but every discovered candidate still needs runtime shape verification.
- Some compiled methods share multiple trace strings; named trace strings do **not** imply there is a one-to-one Java method with that name.

## Finding 1: InstaEclipse's "Disable repost" is a suppression hook, not an enablement switch

Upstream references:

1. [DisableRepostHook.java](https://github.com/ReSo7200/InstaEclipse/blob/e19ab0def2dc1ac197b7a2d4f8763f701ef68b1d/app/src/main/java/ps/reso/instaeclipse/mods/ui/DisableRepostHook.java)
2. [FeatureFlags.java](https://github.com/ReSo7200/InstaEclipse/blob/e19ab0def2dc1ac197b7a2d4f8763f701ef68b1d/app/src/main/java/ps/reso/instaeclipse/utils/feature/FeatureFlags.java#L128-L136)
3. [FeaturesFragment.java](https://github.com/ReSo7200/InstaEclipse/blob/e19ab0def2dc1ac197b7a2d4f8763f701ef68b1d/app/src/main/java/ps/reso/instaeclipse/fragments/FeaturesFragment.java#L1634-L1640)
4. [Module.java registration](https://github.com/ReSo7200/InstaEclipse/blob/e19ab0def2dc1ac197b7a2d4f8763f701ef68b1d/app/src/main/java/ps/reso/instaeclipse/Xposed/Module.java#L360-L368)

`FeatureFlags.disableRepost` defaults to `false`. When enabled, the hook skips `void` repost action handlers through `param.setResult(null)`. That prevents local optimistic UI and the network commit. There are **two independently hooked action paths**:

- **Reels and profile grid:** methods located by DexKit stable trace strings `android_purge_26_q3_RepostActionHandler_handleRepostButtonTapped`, `...performRepostAction`, and `...OnBoardingExperiencesRepostActionHandlerProxy_handleRepostButtonTapped`.
- **Home feed:** `com.instagram.reposts.data.RepostsRepository$postRepost$2` identifies a repository whose `void(?, kotlin.jvm.functions.Function1)` launcher is suppressed; just blocking a network request does not stop the optimistic "Reposted" UI.

**Crucial:** setting `disableRepost=false` makes InstaEclipse **stop suppressing** actions; it **does not force the button to render** if Instagram's eligibility or server rollout hides it. Never invert `setResult(null)`; the observed option is not a visibility gate.

## Finding 2: direct IG455 repost action mapping (DEX static candidates)

| Surface | IG455 source anchor | Compiled candidate from APK | Evidence meaning |
|---|---|---|---|
| Reels/profile | `RepostActionHandler_handleRepostButtonTapped` | `classes5.dex` `LX/06GI;->EIk(View, X.00w8, X.09ZB, String, String, String, Function0, int):void` | Tap handler exists, **not** eligibility. |
| Reels/profile | `RepostActionHandler_performRepostAction` | `classes5.dex` `LX/06GI;->A00(...)V` | Action executor; also contains multiple trace strings. **Do not use this as enablement hook.** |
| Reels proxy | `OnBoardingExperiencesRepostActionHandlerProxy_handleRepostButtonTapped` | `classes5.dex` `LX/0Muj;->EIk(...)V` | Onboarding wrapper; suppresses if DisableRepost enabled. |
| Reels eligibility | `RepostButtonUseCase_isEligibleForRepostButton` & `...getUiState` | Both strings appear in `classes5.dex` `LX/0Ie0;->invoke():Object`, method code approximately 2.5 KB. | **Promising read-only diagnostic anchor only**. Returning Boolean `true` is **wrong** without resolving its actual return object / branch; this lambda also handles other UI state. |
| Reels UFI | `UfiUseCase_getRepostButtonAndCount` | `classes17.dex` `LX/0F2B;->invoke(Object):Object` | Potential renderer/state adapter for icon and count; needs runtime type verification. |
| Home feed backend | `RepostsRepository$postRepost$2` | Stable class in `classes17.dex`, `invokeSuspend(Object):Object` | Coroutine for optimistic apply+network. Not a safe eligibility or UI-enable hook. |

Two additional visible strings demonstrate independent content/account gates: `can_repost` is parsed by IG455 JSON/model code in `classes4.dex` and `classes10.dex`, while `isRepostButtonEnabled` appears in the string form of a UI state in `classes3.dex`. This makes a single global Boolean override **unlikely to be sufficient**.

## Finding 3: the small friends' avatars are more than one UI subsystem

The attached APK contains multiple likely candidates; they must be separated by surface and screenshot/device evidence:

| UI type | Verified IG455 anchor | Compiled static candidate | Qualification |
|---|---|---|---|
| **Liked-by social context on Reels** | `LikedBySocialContextRenderer_getFacepile`, `...getOnClick`, `...getText` | `classes15.dex` `LX/0G4F;->A03(...):LX/0G4I;` (24 parameters; multiple social-context responsibilities) | UI path exists; don't replace entire composite method result. |
| **Reposted-by social context** | `RepostedBySocialContextActionHandler_handleRepostedBySocialContextTap` | `classes5.dex` `LX/05NL;->EIn(X.03jS, List, int):void` | Tap handler exists; not evidence source data present. |
| **Reels friendship avatars** | `ClipsFriendlyViewerFacepileComponent_render`, `ClipsProfilePictureFacepileComponent_render` | `classes15.dex` `LX/0R3f;->A0q(...)` and `LX/0G9s;->A0q(...)` | Strong renderer candidates. |
| **Reels social-context assembly** | `MediaSocialContextFactory_renderMediaSocialContext`, `SocialContextFacepileComponent_socialContextFacepileComponent` | `classes15.dex` `LX/0G4F;->A03(...)`, `LX/0G4I;->A0q(...)` | Shared composition paths; can cover multiple labels and indicators. |
| **Reels Friends tab/facepile suppression** | `ClipsActionBarFacepileUiState(isFriendsTabFacepileDisabled=`, `resolveShouldDisableFacepile` | `classes5.dex` `LX/08Dh;->toString()`, trace in `LX/0ILk;` constructor | Indicates a distinct *disabled* state; a string in a constructor is not a safe ready-made hook. |
| **"Friendly" repost bubble** | `friendly_bubble_with_repost`, `friendly_system_without_repost` | `classes5.dex` `LX/0JMl;-><clinit>()` enum-like constants | Distinct friendly-system UI mode; not proof of forced eligibility. |
| **Liked-by followees and data** | `facepile_top_likers`, `liked_by_followee_mid_card_metadata`, `LIKED_BY_FACEPILE` | IG455 `Media` fields/accessors and additional GraphQL/Pando/enum classes | The server may return a list or no list. Empty data cannot be rendered into real friend avatars. |

`liked_by_bubble` also appears in a separate ad/friendly-viewer experiment path in `classes3.dex` (`AdsFriendlyViewerExperimentUtil_shouldShowBubble`). This is **not** automatically the same as the friend avatars on ordinary Reels. The target needs to be identified by on-screen behavior or a screenshot before hooking it.

## Feasibility / what can be force-enabled

### Repost button

**Potentially yes at the client UI level**, with conditions:

1. Verify `disableRepost=false` in InstaEclipse and that the account/media actually can repost in at least one surface (Feed, Reels, public content from another account).
2. In read-only API 102 observation, inspect the exact IG455 *predicate result type* and the UI state describing button visibility. Hook only a **narrow verified eligible/UI-hidden branch**, not the entire multi-purpose `invoke():Object` or action launcher.
3. Show the native Repost button through the existing UI component (not a fake button sending arbitrary private actions), ensuring its tap goes through native eligibility and server validation.
4. If `can_repost=false`, private owner restrictions, account-level disabled state, or missing backend permission prevent publishing, showing a button will **not** grant permission; it may fail or be rejected. Do not spoof social proof or force server mutations.

### Friend avatar bubbles (likes/reposts)

**Potentially yes when supporting data exists**: find the right surface and existing `Media`-owned facepile/like/repost social-context objects; override an isolated presentation suppression only when the correct list of user avatars is already present. The UI must remain empty when server payloads do not contain eligible friends. Don't invent identities, infer which friends liked a post, or perform new hidden API requests.

**Likely need separate toggles**, because these are not equivalent features: `Show Repost button: Feed`, `Show Repost button: Reels`, `Show liked-by friend avatars`, `Show reposted-by friend avatars`, and optionally `Show Reels friendly-system bubbles`.

## Suggested implementation contract (separate LSPosed module)

**Phase P01 READ ONLY**

- Lock to IG455 exact APK SHA256 and exact method/class signature, Vector API 102, scope `com.instagram.android` only. Keep InstaActions and InstaEclipse behavior unaffected.
- Capture only aggregated Boolean eligibility/UI visibility, renderer invoked/not invoked, list `null|empty|nonempty`, current Feed/Reels surface, and hook installation status. **Never record user IDs, profile photos/URLs, post IDs, usernames, media captions or graph responses.**
- Test feed vs Reels on 2–3 *user-supplied* posts with different visible conditions; verify if the missing button is client-only or `can_repost=false`. Capture before/after menu behavior with InstaEclipse `Disable repost` switched OFF.
- Instrument UI construction and eligibility separately; no hook that swallows or manufactures social actions. Preserve `chain.proceed()` exactly once in passive hooks.

**Phase P02 per-feature presentation switches**

- Implement each of the five feature classes above behind independent, default-off toggles. Resolve a single UI-shape-specific method and return type; fail closed on missing/multiple matches.
- Enable only when underlying native action/content exists. Test interaction and original button handling with InstaEclipse on and off, and across app restart/login/account changes.
- Back out UI changes on disable, process restart or version mismatch. Don't globally patch all `Media` model getters or suppress server eligibility/permission checks.

**Phase P03 qualification**

- Hosted Android/Gradle CI, no local Gradle in Termux.
- Android 16 + Vector API 102 on physical device, exact Instagram APK hash and package check, IN/OUT of Reels and Feed, dynamic scrolling, menu opening, app resume, rapid navigation, two-account scenario only if voluntarily available, and no app ANR/crashes.
- Demonstrate a genuinely successful native repost on eligible content without affecting like/save/share handlers. For avatars, compare native source data counts with rendered/avatar counts; empty source must remain empty.

## Bottom line

**Both capabilities are present in the APK.** The code gives us concrete, version-specific investigation anchors. The missing parts are **which branch gates visibility on the user's account** and **whether friend-avatar data is delivered for the exact viewed post**. A simple `true` return, turning off InstaEclipse's DisableRepost, or forcing the large `MediaSocialContextFactory` render result does not safely solve both.
