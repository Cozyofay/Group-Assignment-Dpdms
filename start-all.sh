#!/usr/bin/env bash
# Starts every DPDMS service in the correct order, in the background.
# Logs go to ./logs/<service>.log, PIDs to ./logs/<service>.pid
#
#   ./start-all.sh            start everything (builds first if jars are missing)
#   ./start-all.sh --build    force a rebuild
#   ./start-all.sh --no-ui    backend only

set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(pwd)"
LOGS="$ROOT/logs"
mkdir -p "$LOGS"

FORCE_BUILD=0
START_UI=1
for arg in "$@"; do
  case "$arg" in
    --build) FORCE_BUILD=1 ;;
    --no-ui) START_UI=0 ;;
    *) echo "Unknown option: $arg"; exit 1 ;;
  esac
done

# service:port:seconds-to-wait-before-the-next-one
SERVICES=(
  "discovery-service:8761:25"
  "gateway:8080:15"
  "auth-service:8081:12"
  "flood-service:8082:6"
  "drought-service:8083:6"
  "fire-service:8084:6"
  "zoonotic-disease-service:8085:6"
  "mining-accident-service:8086:6"
  "alert-service:8087:6"
  "report-service:8088:6"
  "dashboard-service:8089:6"
)
if [ "$START_UI" -eq 1 ]; then
  SERVICES+=("web-ui:8090:0")
fi

step() { printf '\n==> %s\n' "$1"; }
warn() { printf '    %s\n' "$1"; }

step "Pre-flight checks"
if [ ! -f "$ROOT/.env" ]; then
  warn "No .env file found. Copying .env.example -> .env"
  cp "$ROOT/.env.example" "$ROOT/.env"
  echo
  echo "    Open .env now and set JWT_SECRET, INTERNAL_API_KEY and DB_PASSWORD,"
  echo "    then run this script again."
  exit 1
fi
warn "Java: $(java -version 2>&1 | head -1)"

jar_for() {
  find "$ROOT/$1/target" -maxdepth 1 -name '*.jar' ! -name '*-sources.jar' 2>/dev/null | head -1
}

MISSING=()
for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"
  [ -z "$(jar_for "$name")" ] && MISSING+=("$name")
done

if [ "$FORCE_BUILD" -eq 1 ] || [ "${#MISSING[@]}" -gt 0 ]; then
  [ "${#MISSING[@]}" -gt 0 ] && warn "Missing jars for: ${MISSING[*]}"
  step "Building (mvn clean install -DskipTests)"
  mvn clean install -DskipTests
fi

for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"
  rest="${entry#*:}"
  port="${rest%%:*}"
  wait_for="${rest##*:}"

  jar="$(jar_for "$name")"
  if [ -z "$jar" ]; then
    echo "No jar for $name; skipping." >&2
    continue
  fi

  step "Starting $name on port $port"
  nohup java -jar "$jar" > "$LOGS/$name.log" 2>&1 &
  echo $! > "$LOGS/$name.pid"
  warn "pid $(cat "$LOGS/$name.pid"), log $LOGS/$name.log"

  if [ "$wait_for" -gt 0 ]; then
    printf '    waiting %ss...' "$wait_for"
    sleep "$wait_for"
    printf ' done\n'
  fi
done

step "All services launched"
cat <<'EOF'

    Front end      http://localhost:8090
    API gateway    http://localhost:8080
    Swagger UI     http://localhost:8080/swagger-ui.html
    Eureka         http://localhost:8761
    RabbitMQ       http://localhost:15672  (guest/guest)
    Mailpit inbox  http://localhost:8025

    Give Eureka ~30 more seconds before the gateway stops returning 503.
    Tail a log with:   tail -f logs/flood-service.log
    Stop everything:   ./stop-all.sh
EOF
