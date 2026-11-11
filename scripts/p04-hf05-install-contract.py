#!/usr/bin/env python3
"""Offline installed artifact selection and integrity-path smoke."""
from pathlib import Path
import importlib.util
p=Path(__file__).with_name('itm-p04-install-latest.py')
spec=importlib.util.spec_from_file_location('itm_p04_installer',p)
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
sha='a'*40
runs=[dict(databaseId=10,headSha=sha,event='pull_request',status='completed',conclusion='success'),
      dict(databaseId=12,headSha='b'*40,event='pull_request',status='completed',conclusion='success'),
      dict(databaseId=15,headSha=sha,event='pull_request',status='completed',conclusion='failure'),
      dict(databaseId=16,headSha=sha,event='push',status='completed',conclusion='success'),
      dict(databaseId=18,headSha=sha,event='pull_request',status='in_progress',conclusion=None)]
assert module.choose_successful_run(runs,sha)['databaseId']==10
try:module.choose_successful_run(runs,'c'*40);assert False
except RuntimeError:pass
artifact='InstaTaskManager-P04-HF05-ConfigDispatch-debug-'+sha
assert module.choose_artifact([{'name':artifact,'expired':False}],sha)==artifact
try:module.choose_artifact([{'name':'InstaTaskManager-P04-HF04-debug-'+'b'*40}],sha);assert False
except RuntimeError:pass
try:module.choose_artifact([{'name':artifact,'expired':True}],sha);assert False
except RuntimeError:pass
checksum='f'*64
assert module.sha_from_sums(checksum+'  app/build/outputs/apk/debug/app-debug.apk')==checksum
assert module.sha_from_sums(checksum+'  app-debug.apk')==checksum
for wrong in [checksum+'  other.apk',checksum+'  ../app-debug.apk',checksum+'  app-debug.apk\n'+checksum+'  other.apk']:
 try:module.sha_from_sums(wrong);assert False
 except RuntimeError:pass
print('ITM_P04_HF05_EXACT_CURRENT_SHA_APK_INSTALL_CONTRACT_PASS')

source=p.read_text().replace('scripts/p04-hf05-install-contract.py','') if False else Path(__file__).with_name('itm-p04-install-latest.py').read_text()
assert 'prior=command' in source and "'uninstall',APK_PACKAGE" in source
assert source.index('expected=sha_from_sums') < source.index("'uninstall',APK_PACKAGE")
assert "'install',target" in source and "'install','-r'" not in source
print('ITM_P04_HF05_UNINSTALL_ONLY_AFTER_CHECKSUM_CONTRACT_PASS')
