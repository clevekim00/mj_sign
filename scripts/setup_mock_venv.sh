#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
VENV_DIR="${MOCK_VENV_DIR:-$ROOT_DIR/sign_gemma_mock/.venv}"
PYTHON_BIN="${PYTHON_BIN:-python3}"

if [ ! -x "$VENV_DIR/bin/python" ]; then
  "$PYTHON_BIN" -m venv "$VENV_DIR"
fi

"$VENV_DIR/bin/python" -m pip install -r "$ROOT_DIR/sign_gemma_mock/requirements.txt"

echo "$VENV_DIR/bin/python"
