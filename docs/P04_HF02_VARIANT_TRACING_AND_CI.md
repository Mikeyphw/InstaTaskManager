# P04 HF02 · exact IG455 variant-state A/B capture & duplicate CI repair

## Evidence (user-supplied exact APK)
- Full Instagram 455.0.0.8.72, base APK length 238346353, SHA-256 `fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6`.
- Inspected `classes5.dex` DEX class_defs/field_ids. For selector **4**, `X.09j4` (one public static-final self field, implements `X.0Nhe`) and `X.09ZC` (eight public final instance fields: `A00:int`, `A01:X.00w8`, `A02:X.09ZB`, `A03..A05:String`, `A06..A07:boolean`, also implements `X.0Nhe`) were observed for the affected/working accounts respectively.
- For selector **11**, `X.09Xx` (one public static-final self field, implements `X.0Ngy`) and `X.09YD` (`A00:int`, implements `X.0Ngy`) were observed.
- Selector **12** was false in both cohorts, therefore cannot distinguish their Repost capability.
- **Interpretation**: variant type pairs indicate distinct runtime states. Obfuscation prevents confidently assigning user-facing meanings to individual numeric/Boolean fields. Presence and primitive *aggregate counts* are diagnostic; raw values, media/account IDs, contents or objects are never stored/exported.

## Changes
- New `P04VariantShape`: exact-type allowlist and exact-field-type verification. Reads only public primitive `int`/`boolean` fields and whether five sensitive reference fields are null. It does not call Instagram methods or convert values to strings. Unknown or mismatched shapes are counted and original results are always returned.
- `P04TraceSummary`: capped per-cohort/selector aggregate counters for variants, numeric sign buckets, booleans and reference presence. Previous raw-object exclusion and A/B tracing preserved.
- Android version code advanced to 7; no additional permissions or APK scope changes.
- GitHub Actions now builds PR branches only on `pull_request`, `main` on `push`, optional `workflow_dispatch`; concurrency cancels stale PR builds. A single PR update must create **one** run, not one push plus one PR run. Artifacts use PR HEAD SHA in their name instead of temporary merge SHA.

## Usage
Install the P04 HF02 CI APK. In full Instagram 455, on affected account choose **Start A**, browse one Reel and one Feed post, optionally Count UI labels, Stop. Switch to working account, choose **Start B**, browse the exact same content, optionally Count UI labels, Stop. Export. Inspect `IG455 object-shape` results. If different variants remain consistent, further bytecode analysis is needed to locate *which specific UI predicate selects the singleton path*. This build intentionally does **not** substitute or fabricate UI state objects.

## Safety
- No synthetic friend identities, no account data collection, no server interactions.
- No functional new forced overrides: the existing bounded scalar experiment remains disabled unless the exact Boolean gap appears.
- All unknown classes/field shapes fail closed; original Instagram return values are preserved.
- Do not merge PR #1/#2/#3 until hosted compilation and on-device qualification pass.
