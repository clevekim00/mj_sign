#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
PROTOC_BIN="${PROTOC_BIN:-protoc}"
PROTO_FILE="$ROOT_DIR/schema/landmark.proto"

if ! command -v "$PROTOC_BIN" >/dev/null 2>&1; then
  echo "protoc is required. Install Protocol Buffers compiler or set PROTOC_BIN." >&2
  exit 127
fi

echo "Using $("$PROTOC_BIN" --version)"

"$PROTOC_BIN" \
  -I "$ROOT_DIR" \
  --python_out="$ROOT_DIR/sign_gemma_mock" \
  "$PROTO_FILE"

echo "Python mock protobuf schema regenerated."
