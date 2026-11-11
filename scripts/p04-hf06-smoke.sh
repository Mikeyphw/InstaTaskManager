#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Run the corrected provider-shape regression, including a concrete method
# declared by an abstract owner, and the established exact-shape guards.
bash scripts/p04-hf05-smoke.sh
python3 scripts/p04-hf06-startup-contract.py
