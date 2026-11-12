#!/usr/bin/env python3
from pathlib import Path
import re
s=(Path(__file__).resolve().parents[1]/'.github/workflows/android.yml').read_text()
required=('on:\n  push:\n    branches: [main]\n  pull_request:\n    types: [opened, synchronize, reopened]\n  workflow_dispatch:',
          'concurrency:', 'cancel-in-progress: true',
          'github.event.pull_request.number || github.ref',
          'github.event.pull_request.head.sha || github.sha',
          'bash scripts/p04-hf02-smoke.sh')
for r in required:
    assert r in s, 'HF02_CI_CONTRACT_REFUSED: missing '+r
assert not re.search(r'(?m)^  push:\s*(?:\n\s*pull_request:)',s), 'unrestricted branch push causes duplicate CI'
assert s.count('  pull_request:')==1 and s.count('  push:')==1
assert 'runs-on: ubuntu-latest' in s and 'gradle --no-daemon' in s
print('ITM_P04_HF02_SINGLE_PR_CI_TRIGGER_PASS')
