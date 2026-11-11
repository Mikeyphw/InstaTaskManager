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
require(entry.count('chain.proceed()') == 2 and 'for (String name : new String[]{"onPause", "onDestroy"})' in entry, 'Lifecycle callback must call original exactly once per hook')
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
require('versionCode 2' in (ROOT/'app/build.gradle').read_text() and "versionName '0.2.0-p00-social-p01'" in (ROOT/'app/build.gradle').read_text(), 'Consolidated version metadata mismatch')
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
