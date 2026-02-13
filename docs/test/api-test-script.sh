#!/bin/bash

# AI Agent Platform - API 集成测试脚本
# 日期: 2026-02-10
# 测试范围: Agent CRUD、版本管理、发布回滚

set -e  # 遇到错误立即退出

# 配置
BASE_URL="http://localhost:8080"
TEST_EMAIL="test_$(date +%s)@example.com"
TEST_PASSWORD="Test123456"
TEST_USERNAME="TestUser"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# 测试函数
test_case() {
    local test_name=$1
    local expected_status=$2
    local actual_status=$3

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    if [ "$actual_status" -eq "$expected_status" ]; then
        log_info "✅ PASS: $test_name (HTTP $actual_status)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        log_error "❌ FAIL: $test_name (Expected: $expected_status, Got: $actual_status)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# 检查服务健康
check_health() {
    log_info "检查服务健康状态..."
    response=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_URL}/actuator/health)

    if [ "$response" -eq 200 ]; then
        log_info "✅ 服务健康检查通过"
        return 0
    else
        log_error "❌ 服务健康检查失败 (HTTP $response)"
        exit 1
    fi
}

# ========== 用户注册和登录 ==========

test_user_register() {
    log_info "测试用户注册..."

    response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/user/register \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"${TEST_EMAIL}\",
            \"password\": \"${TEST_PASSWORD}\",
            \"username\": \"${TEST_USERNAME}\"
        }")

    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    test_case "用户注册" 200 "$status"

    if [ "$status" -eq 200 ]; then
        USER_ID=$(echo "$body" | jq -r '.data')
        log_info "用户ID: $USER_ID"
    fi
}

test_user_login() {
    log_info "测试用户登录..."

    response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/user/login \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"${TEST_EMAIL}\",
            \"password\": \"${TEST_PASSWORD}\"
        }")

    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    test_case "用户登录" 200 "$status"

    if [ "$status" -eq 200 ]; then
        TOKEN=$(echo "$body" | jq -r '.data.token')
        log_info "Token: ${TOKEN:0:20}..."
    fi
}

# ========== Agent CRUD 测试 ==========

test_create_agent() {
    log_info "测试创建 Agent..."

    response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/agent/create \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Test Agent",
            "description": "Test Description",
            "icon": "icon.png"
        }')

    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    test_case "创建 Agent" 200 "$status"

    if [ "$status" -eq 200 ]; then
        AGENT_ID=$(echo "$body" | jq -r '.data')
        log_info "Agent ID: $AGENT_ID"
    fi
}

test_list_agents() {
    log_info "测试查询 Agent 列表..."

    response=$(curl -s -w "\n%{http_code}" -X GET ${BASE_URL}/api/agent/list \
        -H "Authorization: Bearer ${TOKEN}")

    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    test_case "查询 Agent 列表" 200 "$status"

    if [ "$status" -eq 200 ]; then
        count=$(echo "$body" | jq '.data | length')
        log_info "Agent 数量: $count"
    fi
}

test_get_agent() {
    log_info "测试查询 Agent 详情..."

    response=$(curl -s -w "\n%{http_code}" -X GET ${BASE_URL}/api/agent/${AGENT_ID} \
        -H "Authorization: Bearer ${TOKEN}")

    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    test_case "查询 Agent 详情" 200 "$status"

    if [ "$status" -eq 200 ]; then
        name=$(echo "$body" | jq -r '.data.name')
        log_info "Agent 名称: $name"
    fi
}

test_update_agent() {
    log_info "测试更新 Agent..."

    response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/agent/update \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Content-Type: application/json" \
        -d "{
            \"id\": ${AGENT_ID},
            \"name\": \"Updated Agent\",
            \"description\": \"Updated Description\",
            \"icon\": \"new-icon.png\",
            \"graphJson\": \"{\\\"nodes\\\":[{\\\"id\\\":\\\"start\\\",\\\"type\\\":\\\"START\\\"},{\\\"id\\\":\\\"end\\\",\\\"type\\\":\\\"END\\\"}],\\\"edges\\\":[{\\\"source\\\":\\\"start\\\",\\\"target\\\":\\\"end\\\"}]}\",
            \"version\": 1
        }")

    status=$(echo "$response" | tail -n1)

    test_case "更新 Agent" 200 "$status"
}

# ========== 版本管理测试 ==========

test_publish_agent() {
    log_info "测试发布 Agent..."

    response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/agent/publish \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Content-Type: application/json" \
        -d "{\"id\": ${AGENT_ID}}")

    status=$(echo "$response" | tail -n1)

    test_case "发布 Agent" 200 "$status"
}

test_get_version_history() {
    log_info "测试查询版本历史..."

    response=$(curl -s -w "\n%{http_code}" -X GET ${BASE_URL}/api/agent/${AGENT_ID}/versions \
        -H "Authorization: Bearer ${TOKEN}")

    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    test_case "查询版本历史" 200 "$status"

    if [ "$status" -eq 200 ]; then
        version_count=$(echo "$body" | jq '.data.versions | length')
        log_info "版本数量: $version_count"
    fi
}

test_update_published_agent() {
    log_info "测试修改已发布的 Agent（应创建新版本）..."

    response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/agent/update \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Content-Type: application/json" \
        -d "{
            \"id\": ${AGENT_ID},
            \"name\": \"Updated Agent V2\",
            \"description\": \"Version 2\",
            \"icon\": \"v2-icon.png\",
            \"graphJson\": \"{\\\"nodes\\\":[{\\\"id\\\":\\\"start\\\",\\\"type\\\":\\\"START\\\"},{\\\"id\\\":\\\"llm\\\",\\\"type\\\":\\\"LLM\\\"},{\\\"id\\\":\\\"end\\\",\\\"type\\\":\\\"END\\\"}],\\\"edges\\\":[{\\\"source\\\":\\\"start\\\",\\\"target\\\":\\\"llm\\\"},{\\\"source\\\":\\\"llm\\\",\\\"target\\\":\\\"end\\\"}]}\",
            \"version\": 1
        }")

    status=$(echo "$response" | tail -n1)

    test_case "修改已发布的 Agent" 200 "$status"
}

test_rollback_agent() {
    log_info "测试回滚 Agent 到版本 1..."

    response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/agent/rollback \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Content-Type: application/json" \
        -d "{
            \"id\": ${AGENT_ID},
            \"targetVersion\": 1
        }")

    status=$(echo "$response" | tail -n1)

    test_case "回滚 Agent" 200 "$status"
}

# ========== 权限测试 ==========

test_unauthorized_access() {
    log_info "测试未授权访问..."

    response=$(curl -s -w "\n%{http_code}" -X GET ${BASE_URL}/api/agent/list)

    status=$(echo "$response" | tail -n1)

    test_case "未授权访问应返回 401" 401 "$status"
}

# ========== 清理 ==========

test_delete_agent() {
    log_info "测试删除 Agent..."

    response=$(curl -s -w "\n%{http_code}" -X DELETE ${BASE_URL}/api/agent/${AGENT_ID}/force \
        -H "Authorization: Bearer ${TOKEN}")

    status=$(echo "$response" | tail -n1)

    test_case "删除 Agent" 200 "$status"
}

# ========== 主测试流程 ==========

main() {
    log_info "=========================================="
    log_info "AI Agent Platform - API 集成测试"
    log_info "=========================================="
    echo ""

    # 检查服务健康
    check_health
    echo ""

    # 用户注册和登录
    log_info "========== 用户注册和登录 =========="
    test_user_register
    test_user_login
    echo ""

    # Agent CRUD 测试
    log_info "========== Agent CRUD 测试 =========="
    test_create_agent
    test_list_agents
    test_get_agent
    test_update_agent
    echo ""

    # 版本管理测试
    log_info "========== 版本管理测试 =========="
    test_publish_agent
    test_get_version_history
    test_update_published_agent
    test_rollback_agent
    echo ""

    # 权限测试
    log_info "========== 权限测试 =========="
    test_unauthorized_access
    echo ""

    # 清理
    log_info "========== 清理 =========="
    test_delete_agent
    echo ""

    # 测试总结
    log_info "=========================================="
    log_info "测试总结"
    log_info "=========================================="
    log_info "总测试数: $TOTAL_TESTS"
    log_info "通过: $PASSED_TESTS"
    log_info "失败: $FAILED_TESTS"

    if [ $FAILED_TESTS -eq 0 ]; then
        log_info "✅ 所有测试通过！"
        exit 0
    else
        log_error "❌ 有 $FAILED_TESTS 个测试失败"
        exit 1
    fi
}

# 执行主函数
main
