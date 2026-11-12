#!/usr/bin/env python3
"""Prevent optional diagnostic failures from disabling mandatory A/B controls."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
p=root/'app/src/main/java/com/mikeyphw/instataskmanager'
m=(p/'InstaTaskModule.java').read_text()
t=(p/'P04GateTrace.java').read_text()
c=(p/'P04ConfigTarget.java').read_text()
w=(root/'.github/workflows/android.yml').read_text()
assert 'Modifier.isAbstract(provider.getModifiers())' not in c, 'abstract owner must be allowed'
assert 'Modifier.isAbstract(m)' in c, 'abstract method must still be rejected'
assert 'P04GateTrace.installed(optional,missing.toString()' in m
assert m.index('hook(ui)') < m.index('hook(mobile)')
assert m.index('hook(ufi)') < m.index('hook(mobile)')
assert m.count('hook(boxed)')==0 and 'maybeHookActualRepostData(' in m
assert all(m.count(x)==1 for x in ('hook(ui)','hook(ufi)','hook(mobile)','hook(session)'))
assert all('appendUnavailable(missing,"'+x+'",e)' in m for x in ('MobileConfig','ActualDataTarget','SessionCheck'))
assert 'ready=true;' in t and 'Repost A/B core (2/2)' in t
assert 'REFUSED: core Repost capture hook unavailable' in m
assert 'EXACT_APK_PASS' in m and m.index('verifyExactApk') < m.index('hook(ui)')
assert m.count('chain.proceed()')==8, 'original invocation count altered'
assert 'return Boolean.TRUE;' not in m+t, 'must not fabricate eligibility'
assert 'p04-hf06-smoke.sh' in w and 'p04-hf06-startup-contract.py' in w
assert 'branches: [main]' in w and 'cancel-in-progress: true' in w
print('ITM_P04_HF06_CORE_READY_INDEPENDENT_OPTIONAL_PASS')
