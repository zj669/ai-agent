#!/bin/bash

# Agent 管理模块测试脚本
# 测试工程师: 测试工程师3号
# 日期: 2026-02-10

BASE_URL="http://localhost:8080"
API_PREFIX="/api/agent"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试用户 Token (需要先登录获取)
TOKEN=""
USER_ID=1

# 测试数据
AGENT_ID=""
AGENT_VERSION=0

# 辅助函数: 打印测试标题
print_test_title() {
    echo ""
    echo "=========================================="
    echo "测试: $1"
    echo "=========================================="
}

# 辅助函数: 打印测试结果
print_result() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASSED${NC}: $2"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}✗ FAILED${NC}: $2"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# 辅助函数: 发送 HTTP 请求
http_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local headers=$4

    if [ -n "$TOKEN" ]; then
        headers="$headers -H 'Authorization: Bearer $TOKEN'"
    fi

    if [ "$method" = "GET" ]; then
        curl -s -X GET "$BASE_URL$endpoint" $headers
    elif [ "$method" = "POST" ]; then
        curl -s -X POST "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            $headers \
            -d "$data"
    elif [ "$method" = "DELETE" ]; then
        curl -s -X DELETE "$BASE_URL$endpoint" $headers
    fi
}

# 辅助函数: 检查 JSON 响应
check_json_field() {
    local json=$1
    local field=$2
    local expected=$3

    actual=$(echo "$json" | jq -r ".$field")
    if [ "$actual" = "$expected" ]; then
        return 0
    else
        echo "Expected: $expected, Actual: $actual"
        return 1
    fi
}

# ==========================================
# 测试前准备
# ==========================================

print_test_title "环境检查"

# 检查后端服务
response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health")
if [ "$response" = "200" ]; then
    print_result 0 "后端服务运行正常"
else
    print_result 1 "后端服务未启动 (HTTP $response)"
    exit 1
fi

# TODO: 登录获取 Token
# 这里需要先实现用户登录，获取 JWT Token
# TOKEN=$(curl -s -X POST "$BASE_URL/api/user/login" \
#     -H "Content-Type: application/json" \
#     -d '{"email":"test@example.com","password":"password"}' \
#     | jq -r '.data.token')

echo -e "${YELLOW}注意: 需要先登录获取 Token，当前测试将跳过需要认证的接口${NC}"

# ==========================================
# 3.1 Agent 创建功能测试
# ==========================================

print_test_title "3.1.1 正常创建 Agent"

response=$(http_request POST "$API_PREFIX/create" '{
    "name": "测试 Agent 1",
    "description": "这是一个测试 Agent",
    "icon": "https://example.com/icon.png"
}')

echo "Response: $response"

if echo "$response" | jq -e '.code == 200' > /dev/null 2>&1; then
    AGENT_ID=$(echo "$response" | jq -r '.data')
    print_result 0 "创建 Agent 成功，ID: $AGENT_ID"
else
    print_result 1 "创建 Agent 失败"
fi

# ==========================================
# 3.1.2 创建 Agent - 缺少必填字段
# ==========================================

print_test_title "3.1.2 创建 Agent - 缺少必填字段"

response=$(http_request POST "$API_PREFIX/create" '{
    "description": "缺少 name 字段"
}')

echo "Response: $response"

if echo "$response" | jq -e '.code != 200' > /dev/null 2>&1; then
    print_result 0 "正确拒绝了缺少必填字段的请求"
else
    print_result 1 "应该拒绝缺少必填字段的请求"
fi

# ==========================================
# 3.1.3 创建 Agent - 名称过长
# ==========================================

print_test_title "3.1.3 创建 Agent - 名称过长"

long_name=$(printf 'A%.0s' {1..150})
response=$(http_request POST "$API_PREFIX/create" "{
    \"name\": \"$long_name\",
    \"description\": \"测试长名称\"
}")

echo "Response: $response"

if echo "$response" | jq -e '.code != 200' > /dev/null 2>&1; then
    print_result 0 "正确拒绝了名称过长的请求"
else
    print_result 1 "应该拒绝名称过长的请求"
fi

# ==========================================
# 3.2 Agent 查询功能测试
# ==========================================

print_test_title "3.2.1 查询 Agent 列表"

response=$(http_request GET "$API_PREFIX/list")

echo "Response: $response"

if echo "$response" | jq -e '.code == 200' > /dev/null 2>&1; then
    count=$(echo "$response" | jq '.data | length')
    print_result 0 "查询 Agent 列表成功，共 $count 条记录"
else
    print_result 1 "查询 Agent 列表失败"
fi

# ==========================================
# 3.2.3 查询 Agent 详情
# ==========================================

if [ -n "$AGENT_ID" ]; then
    print_test_title "3.2.3 查询 Agent 详情"

    response=$(http_request GET "$API_PREFIX/$AGENT_ID")

    echo "Response: $response"

    if echo "$response" | jq -e '.code == 200' > /dev/null 2>&1; then
        AGENT_VERSION=$(echo "$response" | jq -r '.data.version')
        print_result 0 "查询 Agent 详情成功，版本: $AGENT_VERSION"
    else
        print_result 1 "查询 Agent 详情失败"
    fi
fi

# ==========================================
# 3.2.4 查询不存在的 Agent
# ==========================================

print_test_title "3.2.4 查询不存在的 Agent"

response=$(http_request GET "$API_PREFIX/99999")

echo "Response: $response"

if echo "$response" | jq -e '.code != 200' > /dev/null 2>&1; then
    print_result 0 "正确返回了 Agent 不存在的错误"
else
    print_result 1 "应该返回 Agent 不存在的错误"
fi

# ==========================================
# 3.3 Agent 更新功能测试
# ==========================================

if [ -n "$AGENT_ID" ] && [ -n "$AGENT_VERSION" ]; then
    print_test_title "3.3.1 正常更新 Agent"

    response=$(http_request POST "$API_PREFIX/update" "{
        \"id\": $AGENT_ID,
        \"name\": \"更新后的 Agent\",
        \"description\": \"更新后的描述\",
        \"icon\": \"https://example.com/new-icon.png\",
        \"graphJson\": \"{}\",
        \"version\": $AGENT_VERSION
    }")

    echo "Response: $response"

    if echo "$response" | jq -e '.code == 200' > /dev/null 2>&1; then
        print_result 0 "更新 Agent 成功"
        AGENT_VERSION=$((AGENT_VERSION + 1))
    else
        print_result 1 "更新 Agent 失败"
    fi
fi

# ==========================================
# 3.3.2 更新 Agent - 乐观锁冲突
# ==========================================

if [ -n "$AGENT_ID" ]; then
    print_test_title "3.3.2 更新 Agent - 乐观锁冲突"

    response=$(http_request POST "$API_PREFIX/update" "{
        \"id\": $AGENT_ID,
        \"name\": \"尝试更新\",
        \"description\": \"使用错误的版本号\",
        \"version\": 0
    }")

    echo "Response: $response"

    if echo "$response" | jq -e '.code != 200' > /dev/null 2>&1; then
        if echo "$response" | jq -e '.message | contains("已被修改")' > /dev/null 2>&1; then
            print_result 0 "正确检测到乐观锁冲突"
        else
            print_result 1 "错误信息不正确"
        fi
    else
        print_result 1 "应该检测到乐观锁冲突"
    fi
fi

# ==========================================
# 3.4 Agent 发布功能测试
# ==========================================

if [ -n "$AGENT_ID" ]; then
    print_test_title "3.4.1 正常发布 Agent"

    response=$(http_request POST "$API_PREFIX/publish" "{
        \"id\": $AGENT_ID
    }")

    echo "Response: $response"

    if echo "$response" | jq -e '.code == 200' > /dev/null 2>&1; then
        print_result 0 "发布 Agent 成功"
    else
        print_result 1 "发布 Agent 失败"
    fi
fi

# ==========================================
# 3.7 版本历史查询测试
# ==========================================

if [ -n "$AGENT_ID" ]; then
    print_test_title "3.7.2 查询版本历史"

    response=$(http_request GET "$API_PREFIX/$AGENT_ID/versions")

    echo "Response: $response"

    if echo "$response" | jq -e '.code == 200' > /dev/null 2>&1; then
        version_count=$(echo "$response" | jq '.data.versions | length')
        print_result 0 "查询版本历史成功，共 $version_count 个版本"
    else
        print_result 1 "查询版本历史失败"
    fi
fi

# ==========================================
# 测试结果汇总
# ==========================================

echo ""
echo "=========================================="
echo "测试结果汇总"
echo "=========================================="
echo "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"
echo "通过率: $(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")%"
echo "=========================================="

# 返回退出码
if [ $FAILED_TESTS -eq 0 ]; then
    exit 0
else
    exit 1
fi
