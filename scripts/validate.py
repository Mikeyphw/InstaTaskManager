#!/usr/bin/env python3
"""Offline fail-closed read-only contract for Task Manager P00 + Social P01."""
from pathlib import Path
import hashlib
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'app/src/main/java/com/mikeyphw/instataskmanager'
META = ROOT / 'app/src/main/resources/META-INF/xposed'
APK_NAME = 'com.mikeyphw.instataskmanager'

def require(cond, message):
    if not cond:
        raise SystemExit('ITM_P00_REFUSED: ' + message)

for n in ('java_init.list', 'module.prop', 'scope.list'):
    require((META/n).is_file(), f'Missing module metadata: {n}')
require((META/'scope.list').read_text().strip() == 'com.instagram.android', 'Unexpected scope (Instagram-only required)')
require((META/'java_init.list').read_text().strip() == APK_NAME+'.InstaTaskModule', 'Wrong module entry')
props = dict(line.split('=',1) for line in (META/'module.prop').read_text().splitlines() if '=' in line)
require(props.get('minApiVersion') == props.get('targetApiVersion') == '102', 'Must use modern API 102')
require(props.get('staticScope') == 'true', 'Unexpected scope mode')
entry = (SOURCE/'InstaTaskModule.java').read_text()
require('com.instagram.android".equals(param.getPackageName())' in entry, 'No Instagram package guard')
require('com.instagram.android".equals(processName)' in entry, 'No main-process guard')
require(entry.count('chain.proceed()') == 7 and 'for (String name : new String[]{"onPause", "onDestroy"})' in entry, 'Lifecycle callback must call original exactly once per hook')
require('hook(resumed)' in entry and 'Activity.class.getDeclaredMethod(name)' in entry, 'Lifecycle interception missing')
require('onPackageReady' in entry and 'onModuleLoaded' in entry, 'API 102 entry missing')
require('io.github.libxposed:api:102.0.0' in (ROOT/'app/build.gradle').read_text(), 'Incorrect framework dependency')
all_source = '\n'.join(x.read_text() for x in SOURCE.glob('*.java'))
for forbidden in ('setComponentEnabledSetting', 'setApplicationEnabledSetting', 'killBackgroundProcesses',
                  'forceStopPackage', 'Runtime.getRuntime().exec', 'ProcessBuilder(', 'SmsManager',
                  'AccessibilityService', 'POST_NOTIFICATIONS', 'READ_CONTACTS', 'INTERNET',
                  'startForegroundService', 'android.permission.QUERY_ALL_PACKAGES'):
    require(forbidden not in all_source, 'Unsafe API in release P00: '+forbidden)
require('Debug.getMemoryInfo(info)' in all_source, 'Process memory measurement missing')
require('getProcessMemoryInfo(pids)' in all_source, 'Same-UID per-process memory measurement missing')
require('TrafficStats.getUidRxBytes' in all_source, 'UID network activity missing')
require('new File("/proc/self/task")' in all_source, 'Per-thread CPU measurement missing')
require('thread.quitSafely()' in all_source, 'Background sampling teardown missing')
require('ComponentCatalog.describe' in all_source, 'Component inventory missing')
require('getComponentEnabledSetting' in all_source, 'Component state reading missing')
# Social discovery requires exact IG455 and is presentation-neutral. The source
# may reflect method signatures, but MUST NOT call/hook any Instagram method.
social = (SOURCE/'SocialDiscovery.java').read_text()
shape = (SOURCE/'SocialMethodShape.java').read_text()
panel = (SOURCE/'TaskPanel.java').read_text()
require('versionCode 8' in (ROOT/'app/build.gradle').read_text() and "versionName '0.6.2-p04-hf03-upstream-predicates'" in (ROOT/'app/build.gradle').read_text(), 'Consolidated version metadata mismatch')
require('fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6' in social, 'Instagram APK evidence hash absent')
require('238346353L' in social and '455.0.0.8.72' in social, 'Exact APK version/size guard missing')
require('MessageDigest.getInstance("SHA-256")' in social and 'APK_SHA256.equals(actualHash.toString())' in social, 'APK SHA validation must precede reflection')
require(social.index('APK_SHA256.equals(actualHash.toString())') < social.index('Class.forName(target.owner, false, instagramLoader)'), 'Unsafe structural lookup before APK verification')
require('Class.forName(target.owner, false, instagramLoader)' in social and 'SocialMethodShape.examine' in social, 'Non-invoking structure discovery missing')
require('if (!keepRunning.getAsBoolean())' in social, 'Cancellation handling missing')
require('getDeclaredMethods()' in shape and 'AMBIGUOUS' in shape, 'Unique method-shape handling missing')
require('"Social","social"' in panel and 'Scan IG455 (read-only)' in panel and 'SocialDiscovery.inspect' in panel, 'Social panel not connected')
require('socialScanning=false;' in panel and 'worker.post(() ->' in panel, 'Social scan must be cancelable and off main thread')
require('No visibility or action overrides are installed.' in social, 'Discovery is not yet feature enablement')
for forbidden in ('setResult(', 'chain.proceed(', '.invoke(', 'hook(', 'getInvoker(', 'setAccessible(true)', 'getDeclaredField(', 'getFields(', 'DexKitBridge', 'android.os.ShellCommand', 'FileOutputStream(', 'getStringExtra(', 'getUserId(', 'getUsername('):
    require(forbidden not in social, 'Prohibited social-discovery operation: ' + forbidden)
require('STRUCTURE_FOUND' in shape and 'AMBIGUOUS' in shape and 'getDeclaredMethods()' in shape, 'Structure gate not defined')

# User-initiated P02 export must not change hooks, require storage permission,
# collect account information or perform hidden network activity.
export_ui = (SOURCE/'TaskPanel.java').read_text()
export_file = (SOURCE/'ExportFile.java').read_text()
export_report = (SOURCE/'ExportReport.java').read_text()
require('exportButton=button("Export", this::exportDiagnostics)' in export_ui, 'Header export button missing')
require('worker.post(() ->' in export_ui and 'ExportReport.build(' in export_ui and 'ExportFile.save(' in export_ui,
        'Export not worker-backed or incomplete')
require('MediaStore.Downloads.EXTERNAL_CONTENT_URI' in export_file and 'MediaStore.MediaColumns.IS_PENDING' in export_file,
        'Scoped Downloads report publication missing')
require('Intent.ACTION_SEND' in export_file and 'Build.VERSION.SDK_INT < Build.VERSION_CODES.Q' in export_file,
        'Android 9 safe fallback missing')
require('getBytes(StandardCharsets.UTF_8)' in export_file and 'resolver.delete(uri, null, null)' in export_file,
        'Export UTF-8 or fail-closed cleanup missing')
require('INSTA TASK MANAGER — LOCAL DIAGNOSTIC EXPORT' in export_report,
        'Report provenance missing')
for forbidden in ('getUserId(', 'getUsername(', 'getCaption(', 'queryMedia(', 'INTERNET', 'FileOutputStream(', 'setComponentEnabledSetting'):
    require(forbidden not in export_ui + export_file + export_report,
            'Unsafe exporter functionality: ' + forbidden)
require('staticScope=true' in (META/'module.prop').read_text(), 'Static scope required')
require('class TaskPanel' in all_source and 'decor.addView(panel,lp)' in all_source, 'Nonmodal in-app panel required')
require((ROOT/'app/src/main/AndroidManifest.xml').is_file(), 'Missing Android manifest')
xml = (ROOT/'app/src/main/AndroidManifest.xml').read_text()
require('<uses-permission' not in xml, 'P00 requires no Android permissions')
require(xml.count('<activity') == 1 and '<service' not in xml and '<receiver' not in xml, 'Only standalone Activity permitted')
if len(sys.argv)>1:
    require(len(sys.argv)==3 and sys.argv[1]=='--apk','Usage: scripts/validate.py [--apk PATH]')
    apk=Path(sys.argv[2])
    require(apk.is_file(), 'APK missing')
    with zipfile.ZipFile(apk) as z:
        for m in ('META-INF/xposed/java_init.list','META-INF/xposed/scope.list','META-INF/xposed/module.prop'):
            require(m in z.namelist(), f'APK missing {m}')
            require(z.read(m)==(META/m.rsplit('/',1)[1]).read_bytes(), f'APK contains different module metadata {m}')
        require(any(n.startswith('classes') and n.endswith('.dex') for n in z.namelist()), 'APK missing classes.dex')
    print('ITM_P00_HOSTED_APK_METADATA_PASS', hashlib.sha256(apk.read_bytes()).hexdigest())
print('ITM_P00_SOCIAL_P01_SOURCE_PRIVACY_SCOPE_CONTRACT_PASS')

# P03 low-overhead and user-controlled Social intents (all OFF by default).
cadence = (SOURCE/'SamplingCadence.java').read_text()
profiler = (SOURCE/'ProfilerSampler.java').read_text()
requests = (SOURCE/'SocialFeatureSettings.java').read_text()
native = (SOURCE/'NativeRepostRecovery.java').read_text()
require('BASE_INTERVAL_MS = 5000' in cadence and 'THREAD_EVERY = 4' in cadence
        and 'MEMORY_EVERY = 6' in cadence and 'PROCESSES_EVERY = 8' in cadence,
        'P03 low-overhead cadence mismatch')
require('SamplingCadence.threadScan(index)' in profiler and 'SamplingCadence.memoryScan(index)' in profiler
        and 'SamplingCadence.processScan(index)' in profiler and 'Debug.threadCpuTimeNanos()' in profiler,
        'P03 expensive sampler work is not gated or profiler cost is unmeasured')
require('SamplingCadence.BASE_INTERVAL_MS' in panel and 's.samplerCpuPct' in panel,
        'P03 UI must reflect overhead sampling')
for key in ('repost_feed','repost_reels','liked_by','reposted_by','friendly_bubbles'):
    require('"'+key+'"' in requests, 'Missing separate Social toggle '+key)
require('getBoolean(key, false)' in requests and 'getSharedPreferences(PREFS' in requests,
        'Social toggle defaults/persistence missing')
require('socialExactVerified && !socialScanning' in panel and 'nativeRecovery.tryExisting(decor)' in panel,
        'Unverified native view trial must be blocked')
require('v.hasOnClickListeners()' in native and 'MAX_VISITED = 1200' in native
        and 'state.view.get()' in native and 'view.setVisibility(state.visibility)' in native,
        'Native view recovery must be bounded, native-only and reversible')
for forbidden in ('performClick(', 'setOnClickListener(', 'startActivity(', 'setResult(', 'getInvoker(',
                  'getMediaId(', 'getUserId(', 'getUsername(', 'getCaption(', 'URL', 'HttpURLConnection'):
    require(forbidden not in native, 'P03 native view trial violation: '+forbidden)
require('socialSwitches.addView(box)' in panel and 'SocialFeatureSettings.report(activity)' in panel,
        'Social toggles or export integration missing')
require('nativeRecovery.restore();' in panel, 'Cannot restore recovered native UI')
require((ROOT/'scripts/p03-smoke.sh').is_file() and (ROOT/'scripts/P03CadenceSmoke.java').is_file()
        and (ROOT/'scripts/P03NativeViewSmoke.java').is_file(),
        'P03 sampler deterministic test missing')
require('bash scripts/p03-smoke.sh' in (ROOT/'.github/workflows/android.yml').read_text(),
        'P03 hosted Java smoke missing')
print('ITM_P03_LOW_OVERHEAD_SOCIAL_INTENT_CONTRACT_PASS')

# P04 verified IG455 selector-aware passive A/B observation (NO action/eligibility override).
trace=(SOURCE/'P04GateTrace.java').read_text()
summary=(SOURCE/'P04TraceSummary.java').read_text()
module=(SOURCE/'InstaTaskModule.java').read_text()
panel=(SOURCE/'TaskPanel.java').read_text()
require('versionCode 8' in (ROOT/'app/build.gradle').read_text(), 'P04 version must advance')
require('fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6' in trace
        and '238346353L' in trace and 'EXACT_APK_PASS' in trace,
        'P04 exact installed APK pin absent')
require('X.0Ie0' in trace and 'X.0F2B' in trace and 'getDeclaredField("$t")' in trace,
        'P04 runtime selector shape validation missing')
require('uiSelector.getInt(instance)!=4' in trace and 'selector==11 || selector==12' in trace,
        'P04 DEX-verified selector scope missing')
require('chain.proceed()' in module and 'Object resultValue=chain.proceed()' in module,
        'P04 original invocation missing')
require(module.count('return resultValue;') == 2 and 'return P04GateTrace.noteUi(chain.getThisObject(),resultValue);' in module and 'P04GateTrace.verifyExactApk(context)' in module
        and module.index('P04GateTrace.verifyExactApk(context)') < module.index('hook(ui)'),
        'P04 original-value/guarded-trial/hash-before-hook contract broken')
for disallowed in ('setResult(', 'getInvoker(', '.setAccessible(', 'performClick(', 'setVisibility(',
                   'startActivity(', 'getUserId(', 'getUsername(', 'getCaption(', 'HttpURLConnection',
                   'getSharedPreferences(', 'Runtime.getRuntime().exec', 'new URL('):
    require(disallowed not in trace+summary, 'P04 read-only observation violated: '+disallowed)
require('return Boolean.TRUE;' not in trace and 'booleanTrial' not in trace
        and 'Repost eligibility override: NOT INSTALLED' in trace,
        'HF03 must retire the ineffective scalar override')
require('result.getClass().getName()' in summary and 'LIMIT_PER_GATE = 256' in summary
        and 'ACCOUNT ' in summary and 'trueValues' in summary and 'falseValues' in summary,
        'P04 count-only bounded result classification missing')
require('P04GateTrace.begin(' in panel and 'P04GateTrace.countVisibleLabels(' in panel
        and 'P04GateTrace.report()' in panel and 'Start A' in panel and 'Start B' in panel,
        'P04 A/B UX and export integration missing')
require('bash scripts/p04-smoke.sh' in (ROOT/'.github/workflows/android.yml').read_text(),
        'P04 full source javac gate missing from hosted CI')
print('ITM_P04_EXACT_APK_PASSIVE_AB_CONTRACT_PASS')

# P04 HF01 visual capture feedback: no new Android permissions, no hidden data.
summary=(SOURCE/'P04TraceSummary.java').read_text()
trace=(SOURCE/'P04GateTrace.java').read_text()
panel=(SOURCE/'TaskPanel.java').read_text()
require('synchronized String feedback()' in summary and 'System.nanoTime()' in summary
        and 'gate events' in summary and 'activeIndex()' in summary,
        'P04 HF01 live count/timer unavailable')
require('static String feedback()' in trace and 'STATE.feedback()' in trace
        and 'static int activeIndex()' in trace,
        'P04 HF01 capture state not exposed to UI')
require('private void refreshCaptureFeedback()' in panel and 'private void startCaptureTicker()' in panel
        and 'MAIN.postDelayed(this,1000)' in panel and 'MAIN.removeCallbacks(captureTicker)' in panel
        and 'captureBanner=txt(' in panel and 'chip.setText(active == 0' in panel,
        'P04 HF01 ticker/status banner/chip/teardown missing')
require('lastP04Ready != newReady' in panel and 'renderSocialContent();' in panel,
        'P04 HF01 hook verification must refresh start controls')
require('Saving…' in panel and 'Saving diagnostics to Downloads' in panel
        and 'actionToast(p04Action)' in panel,
        'P04 HF01 action/export immediate feedback missing')
require('bash scripts/p04-smoke.sh' in (ROOT/'.github/workflows/android.yml').read_text(),
        'P04 HF01 hosted smoke gate missing')
print('ITM_P04_HF01_CAPTURE_FEEDBACK_SOURCE_PASS')
require('bash scripts/p04-hf01-smoke.sh' in (ROOT/'.github/workflows/android.yml').read_text()
        and (ROOT/'scripts/P04Hf01SyntaxSmoke.java').is_file(),
        'P04 HF01 full UI Java syntax parse gate missing')

# HF02: APK-supported variant shapes + one run per feature-branch PR.
shape=(SOURCE/'P04VariantShape.java').read_text()
workflow=(ROOT/'.github/workflows/android.yml').read_text()
require(all(token in shape for token in (
    'X.09j4','X.09ZC','X.09Xx','X.09YD','X.0Nhe','X.0Ngy',
    'A00','A01','A02','A03','A04','A05','A06','A07',
    'getBoolean','getInt','Observation.SHAPE_MISMATCH')),
    'HF02 IG455 variant type and field-shape guards missing')
for forbidden in ('setAccessible(', 'getDeclaredMethods()', 'Object.toString(', 'java.net.', 'android.accounts.'):
    require(forbidden not in shape, 'HF02 invasive object inspection: '+forbidden)
require('P04VariantShape.inspect(selector,result)' in summary and 'singletonVariant' in summary
        and 'referenceFieldsPresent' in summary and 'stringFieldsPresent' in summary,
        'HF02 aggregate shape counters missing')
require('branches: [main]' in workflow and 'types: [opened, synchronize, reopened]' in workflow
        and 'cancel-in-progress: true' in workflow and 'p04-hf02-smoke.sh' in workflow
        and 'p04-hf02-workflow-contract.py' in workflow,
        'HF02 duplicate-CI fix or hosted validation missing')
require((ROOT/'scripts/P04HF02VariantSmoke.java').is_file()
        and (ROOT/'scripts/p04-hf02-smoke.sh').is_file()
        and (ROOT/'docs/P04_HF02_VARIANT_TRACING_AND_CI.md').is_file(),
        'HF02 independent tests and provenance missing')
print('ITM_P04_HF02_EXACT_VARIANT_AND_CI_CONTRACT_PASS')

# HF03: source-anchored primitive predicate tracing, thread-scoped, nonmutating.
predicate=(SOURCE/'P04PredicateTrace.java').read_text()
require('class P04PredicateTrace' in predicate and 'ThreadLocal<Frame>' in predicate
        and 'MAX_FRAMES = 256' in predicate and 'MAX_ORDINAL = 4' in predicate
        and 'ACTIVE.set(frame.parent)' in predicate, 'HF03 scoped bounded probe unavailable')
require(all(token in trace for token in ('mobileConfigMethod','nullableConfigMethod','sessionCheckMethod',
        'Class.forName("X.02ja",false,loader)', 'Class.forName("X.02ib",false,loader)',
        'getDeclaredMethod("BdA",long.class)', 'getDeclaredMethod("D3r",int.class)',
        'getDeclaredMethod("A04",session,String.class)', 'P04PredicateTrace.leave(frame,originalResult)')),
        'HF03 verified upstream signatures or enter/leave missing')
require(all(token in module for token in ('hook(mobile)', 'hook(boxed)', 'hook(session)',
        'P04PredicateTrace.note(P04PredicateTrace.MOBILE_CONFIG,original)',
        'P04PredicateTrace.note(P04PredicateTrace.ELIGIBILITY,original)',
        'P04PredicateTrace.note(P04PredicateTrace.SESSION_CHECK,original)',
        'P04GateTrace.enterUi(', 'P04GateTrace.leaveUi(frame,null)', 'return original;')),
        'HF03 passive upstream instrumentation missing')
require('Try A Boolean gate' not in panel and 'Trial OFF' not in panel,
        'HF03 must retire misleading scalar trial controls')
for forbidden in ('setAccessible(', 'getUserId(', 'getUsername(', 'getCaption(',
                  'String.valueOf(value)', 'getArgs()', 'return Boolean.TRUE;',
                  'new URL(', 'HttpURLConnection'):
    require(forbidden not in predicate+trace, 'HF03 primitive privacy/safety contract: '+forbidden)
require('bash scripts/p04-hf03-smoke.sh' in workflow
        and (ROOT/'scripts/P04HF03PredicateSmoke.java').is_file()
        and (ROOT/'docs/P04_HF03_UPSTREAM_PREDICATES.md').is_file(),
        'HF03 hosted regression or provenance missing')
print('ITM_P04_HF03_UPSTREAM_READ_ONLY_PREDICATES_PASS')
