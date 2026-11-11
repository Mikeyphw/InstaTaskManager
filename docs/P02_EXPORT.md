# P02 — User-initiated diagnostic export

Source state: implemented for PR review; **not yet device qualified**.

## UI

Inside Instagram, open the **TM** floating panel. Tap **Export** in the header
beside the close button from any tab. The export includes the latest sampled
performance overview and hot threads, the declared-component report if it has
been inspected, and the last completed **Social P01** scan. A running scan is
explicitly reported as incomplete; the exporter does not scan automatically.

On Android 10+ the file is written as UTF-8 text to:

`Downloads/InstaTaskManager/InstaTaskManager-YYYYMMDD-HHmmss-SSS.txt`

A success/failure Toast is displayed. On Android 9, where scoped Downloads is
not available, the button invokes the Android text share sheet instead of
requesting legacy storage permissions. No Internet permission or app/server
communication is introduced.

## Privacy and limits

All content comes only from existing read-only profiler measurements and
static component/method-shape metadata. No usernames, account IDs, captions,
post URLs, media files or private network messages are requested or recorded.
Never auto-export, never auto-share and do not add analytics. Export files are
saved **only when the user taps Export**, and can be removed with a file manager.

Each section is bounded and control characters are filtered. The publication
uses MediaStore Downloads pending-entry semantics; incomplete files are deleted
on error. Exporting is dispatched to the existing sampler worker, not the UI
thread. Existing Xposed hooks remain unchanged.

## Acceptance

- GitHub Actions: source policy, pure-Java report tests, Android build, JUnit,
  APK metadata + SHA256 artifact gates.
- On Android 16: reopen Instagram and TM, sample CPU/memory, run Social scan,
  tap Export. Confirm a `.txt` file exists under Downloads/InstaTaskManager and
  contains all four sections with bounded sanitized content.
- Tap Export twice to confirm unique timestamped files; close/reopen panel;
  restart app and confirm no background export and preserved InstaEclipse scope.
- Failure mode: reject MediaStore insert/write/publish failure without leaving
  a partially published file or exporting unexpected content.

No native Instagram feature-enablement or component disabling is added here.
