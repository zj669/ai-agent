package com.zj.aiagent.infrastructure.workflow.condition;

import com.zj.aiagent.domain.workflow.exception.ConditionConfigurationException;
import com.zj.aiagent.domain.workflow.port.ConditionEvaluatorPort;
import com.zj.aiagent.domain.workflow.valobj.ComparisonOperator;
import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.ConditionGroup;
import com.zj.aiagent.domain.workflow.valobj.ConditionItem;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.LogicalOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 结构化条件评估器
 * 基于 ConditionBranch/ConditionGroup/ConditionItem 三层模型评估条件分支，
 * 替代直接使用 SpEL 表达式的方式，提供类型安全的比较和安全的变量引用解析。
 *
 * <p>评估流程：
 * <ol>
 *   <li>校验分支配置（恰好一个 default 分支）</li>
 *   <li>按 priority 升序排序</li>
 *   <li>逐个评估非 default 分支的 conditionGroups（AND 关系）</li>
 *   <li>每个 group 内按 operator（AND/OR）评估 conditions</li>
 *   <li>首个命中的分支胜出</li>
 *   <li>无命中则返回 default 分支</li>
 * </ol>
 */
@Slf4j
@Component
public class StructuredConditionEvaluator implements ConditionEvaluatorPort {

    @Override
    public ConditionBranch evaluate(List<ConditionBranch> branches, ExecutionContext context) {
        // 1. 校验分支配置
        validateBranches(branches);

        // 2. 按 priority 升序排序
        List<ConditionBranch> sorted = branches.stream()
                .sorted(Comparator.comparingInt(ConditionBranch::getPriority))
                .toList();

        // 3. 逐个评估非 default 分支
        ConditionBranch defaultBranch = null;
        for (ConditionBranch branch : sorted) {
            if (branch.isDefault()) {
                defaultBranch = branch;
                continue;
            }

            // 4. 评估该分支的所有 conditionGroups（AND 关系：所有组都满足才命中）
            if (evaluateBranch(branch, context)) {
                log.info("条件分支命中, targetNodeId={}, priority={}, description={}",
                        branch.getTargetNodeId(), branch.getPriority(), branch.getDescription());
                return branch;
            }
        }

        // 5. 无命中则返回 default 分支
        log.info("无非 default 分支命中, 使用 default 分支, targetNodeId={}",
                defaultBranch != null ? defaultBranch.getTargetNodeId() : "null");
        return defaultBranch;
    }

    /**
     * 校验分支配置：恰好一个 default 分支
     *
     * @param branches 分支列表
     * @throws ConditionConfigurationException 当配置无效时
     */
    private void validateBranches(List<ConditionBranch> branches) {
        if (branches == null || branches.isEmpty()) {
            throw new ConditionConfigurationException("分支列表不能为空");
        }

        long defaultCount = branches.stream()
                .filter(ConditionBranch::isDefault)
                .count();

        if (defaultCount == 0) {
            throw new ConditionConfigurationException("缺少 default 分支，条件节点必须恰好有一个 default 分支");
        }

        if (defaultCount > 1) {
            throw new ConditionConfigurationException(
                    "存在多个 default 分支（" + defaultCount + " 个），条件节点必须恰好有一个 default 分支");
        }
    }

    /**
     * 评估单个分支：所有 conditionGroups 都满足才命中（AND 关系）
     */
    private boolean evaluateBranch(ConditionBranch branch, ExecutionContext context) {
        List<ConditionGroup> groups = branch.getConditionGroups();
        if (groups == null || groups.isEmpty()) {
            // 无条件组的非 default 分支视为不匹配
            log.warn("非 default 分支无条件组, targetNodeId={}, 视为不匹配", branch.getTargetNodeId());
            return false;
        }

        // 所有组都满足才命中（AND 关系）
        for (ConditionGroup group : groups) {
            if (!evaluateGroup(group, context)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 按 LogicalOperator (AND/OR) 评估组内条件
     *
     * @param group   条件组
     * @param context 执行上下文
     * @return 组评估结果
     */
    private boolean evaluateGroup(ConditionGroup group, ExecutionContext context) {
        List<ConditionItem> conditions = group.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            // 空条件组视为不满足
            return false;
        }

        LogicalOperator operator = group.getOperator();
        if (operator == null) {
            log.warn("条件组的 LogicalOperator 为 null, 默认使用 AND");
            operator = LogicalOperator.AND;
        }

        if (operator == LogicalOperator.AND) {
            // AND: 所有条件都满足
            for (ConditionItem item : conditions) {
                if (!evaluateItem(item, context)) {
                    return false;
                }
            }
            return true;
        } else {
            // OR: 至少一个条件满足
            for (ConditionItem item : conditions) {
                if (evaluateItem(item, context)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 评估单个条件项
     */
    private boolean evaluateItem(ConditionItem item, ExecutionContext context) {
        if (item.getOperator() == null) {
            log.warn("ConditionItem 的 operator 为 null, leftOperand={}, 跳过该条件, 视为 false",
                    item.getLeftOperand());
            return false;
        }

        Object leftValue = resolveOperand(item.getLeftOperand(), context);
        Object rightValue = resolveOperand(item.getRightOperand(), context);

        return compareValues(leftValue, item.getOperator(), rightValue);
    }

    /**
     * 解析操作数，从 ExecutionContext 获取值
     * <p>
     * 支持的变量引用格式：
     * <ul>
     *   <li>{@code nodes.{nodeId}.{key}} — 引用上游节点输出</li>
     *   <li>{@code inputs.{key}} — 引用全局输入</li>
     * </ul>
     * 非变量引用格式的操作数直接返回原值（作为字面值）。
     *
     * @param operand 操作数（变量引用或字面值）
     * @param context 执行上下文
     * @return 解析后的值，变量不存在时返回 null
     */
    private Object resolveOperand(Object operand, ExecutionContext context) {
        if (operand == null) {
            return null;
        }

        if (!(operand instanceof String strOperand)) {
            // 非字符串操作数（如数字字面值），直接返回
            return operand;
        }

        // 尝试解析 "nodes.{nodeId}.{key}" 格式
        if (strOperand.startsWith("nodes.")) {
            return resolveNodeReference(strOperand, context);
        }

        // 尝试解析 "inputs.{key}" 格式
        if (strOperand.startsWith("inputs.")) {
            return resolveInputReference(strOperand, context);
        }

        // 非变量引用，作为字面值返回
        return operand;
    }

    /**
     * 解析节点输出引用: nodes.{nodeId}.{key}
     */
    private Object resolveNodeReference(String reference, ExecutionContext context) {
        // 格式: nodes.{nodeId}.{key}
        // 至少需要 3 段: "nodes", nodeId, key
        String withoutPrefix = reference.substring("nodes.".length());
        int dotIndex = withoutPrefix.indexOf('.');
        if (dotIndex <= 0 || dotIndex >= withoutPrefix.length() - 1) {
            log.warn("无效的节点引用格式: {}, 期望格式: nodes.{{nodeId}}.{{key}}", reference);
            return null;
        }

        String nodeId = withoutPrefix.substring(0, dotIndex);
        String key = withoutPrefix.substring(dotIndex + 1);

        Map<String, Object> nodeOutput = context.getNodeOutput(nodeId);
        if (nodeOutput == null || nodeOutput.isEmpty()) {
            log.warn("节点输出不存在, nodeId={}, reference={}", nodeId, reference);
            return null;
        }

        Object value = nodeOutput.get(key);
        if (value == null) {
            log.warn("节点输出中不存在指定 key, nodeId={}, key={}, reference={}", nodeId, key, reference);
        }
        return value;
    }

    /**
     * 解析全局输入引用: inputs.{key}
     */
    private Object resolveInputReference(String reference, ExecutionContext context) {
        String key = reference.substring("inputs.".length());
        if (key.isEmpty()) {
            log.warn("无效的输入引用格式: {}, 期望格式: inputs.{{key}}", reference);
            return null;
        }

        Map<String, Object> inputs = context.getInputs();
        if (inputs == null) {
            log.warn("ExecutionContext 的 inputs 为 null, reference={}", reference);
            return null;
        }

        Object value = inputs.get(key);
        if (value == null) {
            log.warn("全局输入中不存在指定 key, key={}, reference={}", key, reference);
        }
        return value;
    }

    /**
     * 基于 ComparisonOperator 执行类型安全比较
     * <p>
     * 支持 String、Number、null 的比较。
     * 类型不兼容时条件视为不满足，记录 WARN 日志。
     *
     * @param left  左操作数值
     * @param op    比较操作符
     * @param right 右操作数值
     * @return 比较结果
     */
    private boolean compareValues(Object left, ComparisonOperator op, Object right) {
        try {
            return switch (op) {
                case EQUALS -> compareEquals(left, right);
                case NOT_EQUALS -> !compareEquals(left, right);
                case CONTAINS -> compareContains(left, right);
                case NOT_CONTAINS -> !compareContains(left, right);
                case GREATER_THAN -> compareNumeric(left, right) > 0;
                case LESS_THAN -> compareNumeric(left, right) < 0;
                case GREATER_THAN_OR_EQUAL -> compareNumeric(left, right) >= 0;
                case LESS_THAN_OR_EQUAL -> compareNumeric(left, right) <= 0;
                case IS_EMPTY -> isEmpty(left);
                case IS_NOT_EMPTY -> !isEmpty(left);
                case STARTS_WITH -> compareStartsWith(left, right);
                case ENDS_WITH -> compareEndsWith(left, right);
            };
        } catch (Exception e) {
            log.warn("比较操作失败, left={}, op={}, right={}, error={}",
                    left, op, right, e.getMessage());
            return false;
        }
    }

    /**
     * 等于比较：支持 null、Number（数值比较）、String（字符串比较）
     */
    private boolean compareEquals(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }

        // 如果两者都是 Number，进行数值比较
        if (left instanceof Number && right instanceof Number) {
            return Double.compare(
                    ((Number) left).doubleValue(),
                    ((Number) right).doubleValue()
            ) == 0;
        }

        // 字符串比较（将两者都转为字符串）
        return left.toString().equals(right.toString());
    }

    /**
     * 包含比较：左操作数的字符串表示是否包含右操作数的字符串表示
     */
    private boolean compareContains(Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }
        return left.toString().contains(right.toString());
    }

    /**
     * 数值比较：将两个操作数转为 double 进行比较
     *
     * @return 负数表示 left < right，0 表示相等，正数表示 left > right
     * @throws IllegalArgumentException 当操作数无法转为数值时
     */
    private int compareNumeric(Object left, Object right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("数值比较不支持 null 值, left=" + left + ", right=" + right);
        }

        double leftNum = toDouble(left);
        double rightNum = toDouble(right);
        return Double.compare(leftNum, rightNum);
    }

    /**
     * 将对象转为 double 值
     */
    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法将值转为数值: " + value);
        }
    }

    /**
     * 判断值是否为空（null 或空字符串）
     */
    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        return value.toString().isEmpty();
    }

    /**
     * 以...开头比较
     */
    private boolean compareStartsWith(Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }
        return left.toString().startsWith(right.toString());
    }

    /**
     * 以...结尾比较
     */
    private boolean compareEndsWith(Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }
        return left.toString().endsWith(right.toString());
    }
}
