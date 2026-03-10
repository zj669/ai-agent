#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-all}"
APP_HEALTH_URL="${2:-http://127.0.0.1:8080/actuator/health}"
FRONTEND_URL="${3:-http://127.0.0.1/}"

ok()   { echo "[OK]   $*"; }
warn() { echo "[WARN] $*"; }
err()  { echo "[ERR]  $*"; }

compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

check_cmd() {
  local c="$1"
  if command -v "$c" >/dev/null 2>&1; then ok "command exists: $c"; else err "command missing: $c"; return 1; fi
}

check_port_listen() {
  local p="$1"
  if ss -lntp 2>/dev/null | awk '{print $4}' | grep -q ":${p}$"; then
    ok "port listening: ${p}"
  else
    warn "port not listening: ${p}"
  fi
}

check_docker_container() {
  local name="$1"
  local status
  status="$(docker inspect -f '{{.State.Status}}' "$name" 2>/dev/null || true)"
  if [[ "$status" == "running" ]]; then
    ok "container running: $name"
  elif [[ -n "$status" ]]; then
    warn "container not running: $name (status=$status)"
  else
    warn "container missing: $name"
  fi
}

check_infra() {
  echo "== Infra checks =="
  check_cmd docker
  check_cmd curl
  if docker compose version >/dev/null 2>&1 || command -v docker-compose >/dev/null 2>&1; then
    ok "compose command ready"
  else
    err "compose command missing"
    return 1
  fi

  check_docker_container ai-agent-mysql
  check_docker_container ai-agent-redis
  check_docker_container ai-agent-etcd
  check_docker_container ai-agent-minio
  check_docker_container ai-agent-milvus

  check_port_listen 13306
  check_port_listen 6379
  check_port_listen 9000
  check_port_listen 19530
}

check_app() {
  echo "== App checks =="
  check_cmd curl

  if curl -fsS "$APP_HEALTH_URL" >/tmp/ai-agent-health.json 2>/dev/null; then
    ok "health endpoint reachable: $APP_HEALTH_URL"
    cat /tmp/ai-agent-health.json
  else
    err "health endpoint failed: $APP_HEALTH_URL"
    return 1
  fi

  if curl -fsS "$FRONTEND_URL" >/tmp/ai-agent-frontend.html 2>/dev/null; then
    ok "frontend reachable: $FRONTEND_URL"
  else
    err "frontend unreachable: $FRONTEND_URL"
    return 1
  fi
}

case "$MODE" in
  infra)
    check_infra
    ;;
  app)
    check_app
    ;;
  all)
    check_infra
    check_app
    ;;
  *)
    echo "Usage: $0 [infra|app|all] [app_health_url] [frontend_url]"
    exit 2
    ;;
esac
