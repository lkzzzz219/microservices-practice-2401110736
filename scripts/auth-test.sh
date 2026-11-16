#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
for python_cmd in python python3; do
  if command -v "$python_cmd" >/dev/null 2>&1 && "$python_cmd" -c 'import cryptography' >/dev/null 2>&1; then
    exec "$python_cmd" scripts/auth-test.py "$@"
  fi
done
echo 'FAIL: install Python 3 and run python -m pip install -r scripts/requirements-auth.txt' >&2
exit 1
