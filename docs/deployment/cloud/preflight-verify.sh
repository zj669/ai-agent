#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-all}"               # env|bundle|all
ENV_FILE="${2:-/opt/ai-agent/.env.http}"
RELEASE_DIR="${3:-/opt/ai-agent/release}"

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

has_cmd() {
  command -v "$1" >/dev/null 2>&1
}

require_file() {
  local f="$1"
  if [[ -f "$f" ]]; then ok "file exists: $f"; else err "file missing: $f"; return 1; fi
}

require_dir() {
  local d="$1"
  if [[ -d "$d" ]]; then ok "dir exists: $d"; else err "dir missing: $d"; return 1; fi
}

check_env_key_non_placeholder() {
  local key="$1"
  if ! grep -Eq "^${key}=" "$ENV_FILE"; then
    err "env key missing: $key"
    return 1
  fi

  local value
  value="$(grep -E "^${key}=" "$ENV_FILE" | tail -n1 | cut -d'=' -f2- | tr -d '"' | tr -d "'")"

  if [[ -z "${value}" ]]; then
    err "env key empty: $key"
    return 1
  fi

  if [[ "${value}" =~ change_me|replace_with_ ]]; then
    err "env key still placeholder: $key=${value}"
    return 1
  fi

  ok "env key ready: $key"
}

check_env() {
  echo "== Env preflight =="
  require_file "$ENV_FILE"

  check_env_key_non_placeholder "PRIMARY_DB_PASSWORD"
  check_env_key_non_placeholder "REDIS_PASSWORD"
  check_env_key_non_placeholder "MINIO_SECRET_KEY"
  check_env_key_non_placeholder "JWT_SECRET"

  local jwt
  jwt="$(grep -E '^JWT_SECRET=' "$ENV_FILE" | tail -n1 | cut -d'=' -f2- | tr -d '"' | tr -d "'")"
  if [[ ${#jwt} -ge 32 ]]; then
    ok "JWT_SECRET length >= 32"
  else
    err "JWT_SECRET too short (<32)"
    return 1
  fi
}

check_bundle() {
  echo "== Bundle preflight =="
  require_dir "$RELEASE_DIR"
  require_file "$RELEASE_DIR/docker-compose.app.yml"
  require_dir "$RELEASE_DIR/backend"
  require_file "$RELEASE_DIR/backend/ai-agent-interfaces-1.0.0-SNAPSHOT.jar"
  require_dir "$RELEASE_DIR/frontend-dist"
  require_file "$RELEASE_DIR/ai-agent-foward/nginx.conf"
  require_dir "$RELEASE_DIR/docker"
  require_file "$RELEASE_DIR/docker/docker-compose.yml"
  require_dir "$RELEASE_DIR/docker/init/mysql"

  if has_cmd docker; then
    if compose_cmd -f "$RELEASE_DIR/docker/docker-compose.yml" config >/tmp/ai-agent-infra-compose.yml 2>/tmp/ai-agent-infra-compose.err; then
      ok "infra compose config render success"
    else
      err "infra compose config failed"
      cat /tmp/ai-agent-infra-compose.err || true
      return 1
    fi

    if compose_cmd --env-file "$ENV_FILE" -f "$RELEASE_DIR/docker-compose.app.yml" config >/tmp/ai-agent-app-compose.yml 2>/tmp/ai-agent-app-compose.err; then
      ok "app compose config render success"
    else
      err "app compose config failed"
      cat /tmp/ai-agent-app-compose.err || true
      return 1
    fi
  else
    warn "docker not found, skip docker compose config render"
  fi
}

case "$MODE" in
  env)
    check_env
    ;;
  bundle)
    check_bundle
    ;;
  all)
    check_env
    check_bundle
    ;;
  *)
    echo "Usage: $0 [env|bundle|all] [env_file] [release_dir]"
    exit 2
    ;;
esac
