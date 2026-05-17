#!/usr/bin/env bash
set -euo pipefail

# Register or update the CSDN article publisher MCP server for one ai-agent user.
#
# Usage:
#   docker/scripts/register-csdn-mcp.sh <user_id>
#
# Optional environment variables:
#   MYSQL_CONTAINER          default: first running container whose name contains ai-agent-mysql
#   MYSQL_DATABASE           default: ai_agent
#   CSDN_MCP_NAME            default: csdn-article-publisher
#   CSDN_MCP_COMMAND         default: node
#   CSDN_MCP_SCRIPT_PATH     default: /app/mcp-servers/csdn-article-publisher/server.js

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <user_id>" >&2
  exit 2
fi

USER_ID="$1"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-ai_agent}"
CSDN_MCP_NAME="${CSDN_MCP_NAME:-csdn-article-publisher}"
CSDN_MCP_COMMAND="${CSDN_MCP_COMMAND:-node}"
CSDN_MCP_SCRIPT_PATH="${CSDN_MCP_SCRIPT_PATH:-/app/mcp-servers/csdn-article-publisher/server.js}"
CSDN_MCP_DESCRIPTION="${CSDN_MCP_DESCRIPTION:-CSDN 文章发布 MCP；工具名 send_article，认证 Cookie 从后端容器环境 CSDN_COOKIE 继承。}"

if ! [[ "$USER_ID" =~ ^[0-9]+$ ]]; then
  echo "user_id must be a positive integer, got: $USER_ID" >&2
  exit 2
fi

sql_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\'/\'\'}"
  printf "%s" "$value"
}

NAME_SQL="$(sql_escape "$CSDN_MCP_NAME")"
COMMAND_SQL="$(sql_escape "$CSDN_MCP_COMMAND")"
SCRIPT_PATH_SQL="$(sql_escape "$CSDN_MCP_SCRIPT_PATH")"
DESCRIPTION_SQL="$(sql_escape "$CSDN_MCP_DESCRIPTION")"

resolve_mysql_container() {
  if [[ -n "$MYSQL_CONTAINER" ]]; then
    printf "%s" "$MYSQL_CONTAINER"
    return
  fi

  local container
  container="$(docker ps --filter "name=ai-agent-mysql" --format "{{.Names}}" | head -n 1)"
  if [[ -z "$container" ]]; then
    echo "Cannot find a running MySQL container matching name=ai-agent-mysql. Set MYSQL_CONTAINER explicitly." >&2
    exit 1
  fi
  printf "%s" "$container"
}

MYSQL_CONTAINER="$(resolve_mysql_container)"

docker exec -i "$MYSQL_CONTAINER" sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --default-character-set=utf8mb4 \"$MYSQL_DATABASE\"" <<SQL
SET @user_id := ${USER_ID};
SET @name := '${NAME_SQL}';
SET @command := '${COMMAND_SQL}';
SET @script_path := '${SCRIPT_PATH_SQL}';
SET @description := '${DESCRIPTION_SQL}';
SET @config := JSON_OBJECT(
  'type', 'stdio',
  'command', @command,
  'args', JSON_ARRAY(@script_path),
  'env', JSON_OBJECT()
);

UPDATE mcp_server_config
SET
  server_type = 'stdio',
  config_json = @config,
  enabled = 1,
  status = 'DISCONNECTED',
  deleted = 0,
  description = @description,
  update_time = NOW()
WHERE user_id = @user_id
  AND name = @name
LIMIT 1;

INSERT INTO mcp_server_config (
  user_id,
  name,
  server_type,
  config_json,
  enabled,
  status,
  description,
  deleted
)
SELECT
  @user_id,
  @name,
  'stdio',
  @config,
  1,
  'DISCONNECTED',
  @description,
  0
WHERE NOT EXISTS (
  SELECT 1
  FROM mcp_server_config
  WHERE user_id = @user_id
    AND name = @name
  LIMIT 1
);

SELECT id, user_id, name, server_type, status, JSON_PRETTY(config_json) AS config_json
FROM mcp_server_config
WHERE user_id = @user_id
  AND name = @name
LIMIT 1;
SQL
