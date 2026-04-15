#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.stack.kafka.yml"
KEEP_STACK="${KEEP_STACK:-0}"

cleanup() {
  if [ "$KEEP_STACK" = "1" ]; then
    echo "Leaving stack running because KEEP_STACK=1"
    return
  fi
  docker compose -f "$COMPOSE_FILE" down -v >/dev/null 2>&1 || true
}

trap cleanup EXIT

on_error() {
  echo
  echo "Verification failed. Recent bridge logs:"
  docker compose -f "$COMPOSE_FILE" logs --tail=80 bridge || true
}

trap on_error INT TERM HUP

docker compose -f "$COMPOSE_FILE" up -d

echo "Waiting for bridge health endpoint..."
for _ in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:8080/internal/healthz >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "Checking readiness..."
curl -fsS http://127.0.0.1:8080/internal/readyz
echo

echo "Sending WebSocket protobuf probe through Kafka-backed queue path..."
python3 "$ROOT_DIR/scripts/send_websocket_probe.py" --url ws://127.0.0.1:8080/ws/sign

echo "Metrics snapshot:"
METRICS="$(curl -fsS http://127.0.0.1:8080/internal/metrics)"
printf '%s\n' "$METRICS"

printf '%s\n' "$METRICS" | grep -q '"completed_inferences"[[:space:]]*:[[:space:]]*[1-9]'
printf '%s\n' "$METRICS" | grep -q '"idle_flushes"'

echo
echo "Kafka stack verification passed."
