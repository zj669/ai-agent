package com.zj.aiagent.application.swarm.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class SwarmToolArgumentUtils {

    private SwarmToolArgumentUtils() {}

    static JsonNode parseArgumentsObject(
        String toolName,
        String rawArguments,
        ObjectMapper objectMapper
    ) {
        String candidate = rawArguments == null ? "" : rawArguments.trim();
        if (candidate.isEmpty()) {
            return objectMapper.createObjectNode();
        }

        try {
            JsonNode parsed = objectMapper.readTree(candidate);
            if (parsed == null || parsed.isNull()) {
                return objectMapper.createObjectNode();
            }
            if (!parsed.isObject()) {
                throw invalidArguments(toolName, rawArguments);
            }
            return parsed;
        } catch (JsonProcessingException e) {
            throw invalidArguments(toolName, rawArguments);
        }
    }

    static JsonNode tryParseArgumentsObject(
        String rawArguments,
        ObjectMapper objectMapper
    ) {
        String candidate = rawArguments == null ? "" : rawArguments.trim();
        if (candidate.isEmpty()) {
            return objectMapper.createObjectNode();
        }

        try {
            JsonNode parsed = objectMapper.readTree(candidate);
            if (parsed != null && parsed.isObject()) {
                return parsed;
            }
        } catch (JsonProcessingException ignored) {
            // ignore
        }
        return null;
    }

    static Object normalizeForStorage(
        String rawArguments,
        ObjectMapper objectMapper
    ) {
        JsonNode parsed = tryParseArgumentsObject(rawArguments, objectMapper);
        if (parsed != null) {
            return parsed;
        }
        return rawArguments == null ? "" : rawArguments;
    }

    static long getRequiredLong(JsonNode args, String... fieldNames) {
        Long value = getOptionalLong(args, fieldNames);
        if (value == null) {
            throw new IllegalArgumentException(
                "工具参数缺少必填字段: " + String.join("/", fieldNames)
            );
        }
        return value;
    }

    static Long getOptionalLong(JsonNode args, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = args.get(fieldName);
            if (node == null || node.isNull()) {
                continue;
            }
            if (node.isNumber()) {
                return node.asLong();
            }
            if (node.isTextual()) {
                String value = node.asText().trim();
                if (value.isEmpty()) {
                    continue;
                }
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException ignored) {
                    // try next field name
                }
            }
        }
        return null;
    }

    static String getRequiredText(JsonNode args, String... fieldNames) {
        String value = getOptionalText(args, fieldNames);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "工具参数缺少必填字段: " + String.join("/", fieldNames)
            );
        }
        return value;
    }

    static int getRequiredInt(JsonNode args, String... fieldNames) {
        Integer value = getOptionalInt(args, fieldNames);
        if (value == null) {
            throw new IllegalArgumentException(
                "工具参数缺少必填字段: " + String.join("/", fieldNames)
            );
        }
        return value;
    }

    static Integer getOptionalInt(JsonNode args, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = args.get(fieldName);
            if (node == null || node.isNull()) {
                continue;
            }
            if (node.isInt() || node.isLong() || node.isNumber()) {
                return node.asInt();
            }
            if (node.isTextual()) {
                String value = node.asText().trim();
                if (value.isEmpty()) {
                    continue;
                }
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException ignored) {
                    // try next field name
                }
            }
        }
        return null;
    }

    static String getOptionalText(JsonNode args, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = args.get(fieldName);
            if (node == null || node.isNull()) {
                continue;
            }
            if (node.isTextual()) {
                return node.asText();
            }
            if (node.isValueNode()) {
                return node.asText();
            }
            return node.toString();
        }
        return null;
    }

    private static IllegalArgumentException invalidArguments(
        String toolName,
        String rawArguments
    ) {
        String example = switch (toolName) {
            case "send" -> "{\"agentId\":123,\"message\":\"请继续处理这个子任务\"}";
            case "createAgent" -> "{\"role\":\"editor\",\"description\":\"负责文案统一与润色\"}";
            case "executeWorkflow" -> "{\"agentId\":123,\"input\":\"可选输入\"}";
            default -> "{\"key\":\"value\"}";
        };
        String actual =
            rawArguments == null ? "null" : abbreviate(rawArguments);
        return new IllegalArgumentException(
            "工具 " +
                toolName +
                " 的参数必须是 JSON 对象，例如 " +
                example +
                "；当前收到: " +
                actual
        );
    }

    private static String abbreviate(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 117) + "...";
    }
}
