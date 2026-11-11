#!/usr/bin/env python3
"""Install exact-current-head successful CI APK; no secrets, no head guessing."""
from __future__ import annotations
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

REPO = 'Mikeyphw/InstaTaskManager'
PR = 3
BRANCH = 'feature/itm-p04-ig455-account-ab-gates-20261111'
BASE = 'feature/itm-p03-low-overhead-social-toggles-20261111'
APK_PACKAGE = 'com.mikeyphw.instataskmanager'
ADB_SERIAL = 'localhost:5555'


def command(args, *, allow_failure=False, live=False):
    args=list(map(str,args))
    print('$ '+' '.join(args),flush=True)
    p=subprocess.run(args,text=True,stdout=None if live else subprocess.PIPE,
                     stderr=None if live else subprocess.PIPE)
    if p.returncode and not allow_failure:
        raise RuntimeError('Command failed ('+str(p.returncode)+'): '+
                           ' '.join(args[:4])+' '+(p.stderr or 'inspect output')[-300:])
    return p


def gh_json(*args):
    return json.loads(command(['gh',*args]).stdout or '{}')


def choose_successful_run(runs, sha):
    valid=[x for x in runs if x.get('headSha')==sha and
           x.get('event')=='pull_request' and
           x.get('status')=='completed' and
           x.get('conclusion')=='success']
    if not valid:
        raise RuntimeError('REFUSED: current PR HEAD has no successful completed hosted Android run')
    return max(valid,key=lambda x:int(x['databaseId']))


def choose_artifact(artifacts,sha):
    names=[a for a in artifacts if isinstance(a.get('name'),str) and
           a['name'].startswith('InstaTaskManager-P04-') and
           a['name'].endswith('-debug-'+sha) and not a.get('expired',False)]
    if len(names)!=1:
        raise RuntimeError('REFUSED: expected one unexpired SHA-bound APK artifact; found '+str(len(names)))
    return names[0]['name']


def sha_from_sums(content):
    line=content.strip().splitlines()
    if len(line)!=1:raise RuntimeError('REFUSED: expected exactly one SHA256SUMS entry')
    m=re.fullmatch(r'([0-9a-fA-F]{64})\s+\*?(?:app/build/outputs/apk/debug/)?app-debug\.apk',line[0].strip())
    if not m:raise RuntimeError('REFUSED: malformed SHA256SUMS or unexpected APK filename')
    return m.group(1).lower()


def downloads_dir():
    candidates=[Path.home()/'storage/downloads',Path('/storage/emulated/0/Download')]
    for d in candidates:
        if d.is_dir():return d/'InstaTaskManager-P04'
    raise RuntimeError('No Termux storage path; run termux-setup-storage before installing')


def install_latest():
    for tool in ('gh','adb'):
        if shutil.which(tool) is None:raise RuntimeError('Missing Termux binary: '+tool)
    pr=gh_json('pr','view',str(PR),'-R',REPO,'--json',
               'number,state,baseRefName,headRefName,headRefOid,url')
    if (pr.get('number')!=PR or pr.get('state')!='OPEN' or
        pr.get('baseRefName')!=BASE or pr.get('headRefName')!=BRANCH or
        not re.fullmatch('[0-9a-f]{40}',pr.get('headRefOid') or '')):
        raise RuntimeError('REFUSED: PR #3 identity/base/branch changed')
    sha=pr['headRefOid']
    print('ITM_HF05_INSTALL_EXACT_PR_HEAD='+sha,flush=True)
    runs=gh_json('run','list','-R',REPO,'--branch',BRANCH,
                 '--workflow','android.yml','--limit','70','--json',
                 'databaseId,headSha,event,status,conclusion')
    run=choose_successful_run(runs,sha)
    rid=int(run['databaseId'])
    artifacts=gh_json('api','repos/'+REPO+'/actions/runs/'+str(rid)+'/artifacts')
    artifact_name=choose_artifact(artifacts.get('artifacts',[]),sha)
    tmp=os.environ.get('TMPDIR') or tempfile.gettempdir()
    with tempfile.TemporaryDirectory(prefix='itm-p04-hf05-apk-',dir=tmp) as t:
        folder=Path(t)
        command(['gh','run','download',str(rid),'-R',REPO,'-n',artifact_name,
                 '-D',folder],live=True)
        apk=folder/'app-debug.apk';sums=folder/'SHA256SUMS'
        if not apk.is_file() or not sums.is_file():
            raise RuntimeError('REFUSED: APK/SHA256SUMS missing from hosted artifact')
        expected=sha_from_sums(sums.read_text())
        actual=hashlib.sha256(apk.read_bytes()).hexdigest()
        if expected!=actual:raise RuntimeError('REFUSED: downloaded APK SHA256 mismatch')
        # Important race guard: the PR could advance during download.
        again=gh_json('pr','view',str(PR),'-R',REPO,'--json','headRefOid')
        if again.get('headRefOid')!=sha:
            raise RuntimeError('REFUSED: PR advanced while downloading; install cancelled')
        dest=downloads_dir();dest.mkdir(parents=True,exist_ok=True)
        target=dest/('InstaTaskManager-P04-'+sha[:12]+'.apk')
        staged=dest/('.InstaTaskManager-P04-'+sha[:12]+'.tmp')
        shutil.copyfile(apk,staged)
        if hashlib.sha256(staged.read_bytes()).hexdigest()!=expected:
            staged.unlink(missing_ok=True)
            raise RuntimeError('REFUSED: on-device copy hash mismatch')
        staged.replace(target)
        # Hosted debug keys may differ across CI runs. The user explicitly
        # requested a clean uninstall/install workflow for these APKs.
        # Verify/copy the new APK BEFORE touching any existing app.
        command(['adb','-s',ADB_SERIAL,'get-state'])
        prior=command(['adb','-s',ADB_SERIAL,'shell','pm','path',APK_PACKAGE],allow_failure=True)
        if (prior.stdout or '').strip().startswith('package:'):
            print('ITM_HF05_UNINSTALLING_PRIOR_DEBUG_APK (module settings and Vector scope reset)',flush=True)
            command(['adb','-s',ADB_SERIAL,'uninstall',APK_PACKAGE],live=True)
        command(['adb','-s',ADB_SERIAL,'install',target],live=True)
        location=command(['adb','-s',ADB_SERIAL,'shell','pm','path',APK_PACKAGE]).stdout or ''
        if 'package:' not in location:
            raise RuntimeError('APK install returned success but package path is missing')
        print('ITM_HF05_APK_INSTALL_PASS commit='+sha+' run='+str(rid),flush=True)
        print('APK='+str(target),flush=True)
        print('SHA256='+actual,flush=True)
        print('Next: restart Instagram to reload the LSPosed module, then capture Account A/B.',flush=True)
        return target

if __name__=='__main__':
    install_latest()
