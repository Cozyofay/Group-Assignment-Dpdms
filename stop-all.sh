#!/usr/bin/env bash
# Stops every DPDMS service started by start-all.sh.

cd "$(dirname "$0")"
LOGS="$(pwd)/logs"
stopped=0

if [ -d "$LOGS" ]; then
  for pidfile in "$LOGS"/*.pid; do
    [ -e "$pidfile" ] || continue
    name="$(basename "$pidfile" .pid)"
    pid="$(cat "$pidfile")"
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping $name (pid $pid)"
      kill "$pid" 2>/dev/null
      stopped=$((stopped + 1))
    fi
    rm -f "$pidfile"
  done
fi

if [ "$stopped" -eq 0 ]; then
  echo "Nothing to stop (no pid files in ./logs)."
else
  echo
  echo "Stopped $stopped process(es)."
fi

echo "RabbitMQ and Mailpit are Docker containers - stop them with: docker compose down"
