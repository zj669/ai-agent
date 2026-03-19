package com.zj.aiagent.infrastructure.workflow.template;

import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 解析 Prompt 模板中的占位符，支持：
 * - {{key}}
 * - {{inputs.key}}
 * - {{nodeId.output.key}}
 * - #{key} / #{inputs.key} / #{nodeId.output.key}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptTemplateResolver {

    private static final Pattern MUSTACHE_PATTERN = Pattern.compile(
        "\\{\\{\\s*([^{}]+?)\\s*\\}\\}"
    );
    private static final Pattern HASH_PATTERN = Pattern.compile(
        "#\\{\\s*([^{}]+?)\\s*\\}"
    );
    private static final String INPUTS_PREFIX = "inputs.";
    private static final String OUTPUT_SEGMENT = ".output.";

    private final PromptValueFormatter valueFormatter;

    public String resolve(
        String template,
        Map<String, Object> resolvedInputs,
        ExecutionContext context
    ) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        String resolved = resolvePattern(
            template,
            MUSTACHE_PATTERN,
            resolvedInputs,
            context
        );
        return resolvePattern(resolved, HASH_PATTERN, resolvedInputs, context);
    }

    private String resolvePattern(
        String template,
        Pattern pattern,
        Map<String, Object> resolvedInputs,
        ExecutionContext context
    ) {
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            ResolvedValue resolvedValue = resolveExpression(
                expression,
                resolvedInputs,
                context
            );
            String replacement = resolvedValue.found()
                ? valueFormatter.format(resolvedValue.value())
                : matcher.group(0);
            matcher.appendReplacement(
                sb,
                Matcher.quoteReplacement(replacement)
            );
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private ResolvedValue resolveExpression(
        String expression,
        Map<String, Object> resolvedInputs,
        ExecutionContext context
    ) {
        if (expression.isEmpty()) {
            return ResolvedValue.notFound();
        }

        if (resolvedInputs != null && resolvedInputs.containsKey(expression)) {
            return ResolvedValue.found(resolvedInputs.get(expression));
        }

        if (expression.startsWith(INPUTS_PREFIX)) {
            String key = expression.substring(INPUTS_PREFIX.length());
            if (
                context != null &&
                context.getInputs() != null &&
                context.getInputs().containsKey(key)
            ) {
                return ResolvedValue.found(context.getInputs().get(key));
            }
            log.warn(
                "[PromptTemplateResolver] Input reference not found: {}",
                expression
            );
            return ResolvedValue.notFound();
        }

        int outputIndex = expression.indexOf(OUTPUT_SEGMENT);
        if (
            outputIndex > 0 &&
            outputIndex < expression.length() - OUTPUT_SEGMENT.length()
        ) {
            String nodeId = expression.substring(0, outputIndex);
            String key = expression.substring(
                outputIndex + OUTPUT_SEGMENT.length()
            );

            if (context != null) {
                Map<String, Object> nodeOutput = context.getNodeOutput(nodeId);
                if (nodeOutput != null && nodeOutput.containsKey(key)) {
                    return ResolvedValue.found(nodeOutput.get(key));
                }
            }

            log.warn(
                "[PromptTemplateResolver] Node output reference not found: {}",
                expression
            );
            return ResolvedValue.notFound();
        }

        log.warn(
            "[PromptTemplateResolver] Unsupported prompt expression: {}",
            expression
        );
        return ResolvedValue.notFound();
    }

    private record ResolvedValue(boolean found, Object value) {
        private static ResolvedValue found(Object value) {
            return new ResolvedValue(true, value);
        }

        private static ResolvedValue notFound() {
            return new ResolvedValue(false, null);
        }
    }
}
