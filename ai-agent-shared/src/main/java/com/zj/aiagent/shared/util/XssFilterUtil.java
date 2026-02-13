package com.zj.aiagent.shared.util;

import java.util.regex.Pattern;

/**
 * XSS 过滤工具类
 *
 * 用于防止存储型 XSS 攻击,过滤用户输入中的恶意脚本
 *
 * @author backend-developer-2
 * @since 2025-02-10
 */
public class XssFilterUtil {

    private XssFilterUtil() {
        // 工具类,禁止实例化
    }

    /**
     * XSS 危险字符模式
     * 匹配常见的 XSS 攻击模式
     */
    private static final Pattern[] XSS_PATTERNS = {
            // Script 标签
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),

            // JavaScript 事件
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),

            // JavaScript 协议
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),

            // iframe 标签
            Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),

            // object 和 embed 标签
            Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),

            // style 标签 (可能包含 expression)
            Pattern.compile("<style[^>]*>.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),

            // link 标签 (可能加载恶意 CSS)
            Pattern.compile("<link[^>]*>", Pattern.CASE_INSENSITIVE),

            // meta 标签 (可能用于重定向)
            Pattern.compile("<meta[^>]*>", Pattern.CASE_INSENSITIVE),

            // base 标签 (可能劫持相对路径)
            Pattern.compile("<base[^>]*>", Pattern.CASE_INSENSITIVE),

            // img 标签的 onerror 等事件
            Pattern.compile("<img[^>]*onerror[^>]*>", Pattern.CASE_INSENSITIVE),

            // eval 函数
            Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),

            // expression (IE 特有)
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 过滤字符串中的 XSS 危险字符
     *
     * @param input 输入字符串
     * @return 过滤后的字符串,如果输入为 null 则返回 null
     */
    public static String filter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String filtered = input;

        // 移除所有匹配的危险模式
        for (Pattern pattern : XSS_PATTERNS) {
            filtered = pattern.matcher(filtered).replaceAll("");
        }

        // HTML 实体转义 (防止绕过)
        filtered = escapeHtml(filtered);

        return filtered;
    }

    /**
     * HTML 实体转义
     *
     * 将特殊字符转换为 HTML 实体,防止 XSS 攻击
     *
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    private static String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder escaped = new StringBuilder(input.length() + 20);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#x27;");
                    break;
                case '&':
                    escaped.append("&amp;");
                    break;
                case '/':
                    escaped.append("&#x2F;");
                    break;
                default:
                    escaped.append(c);
            }
        }
        return escaped.toString();
    }

    /**
     * 检查字符串是否包含 XSS 危险字符
     *
     * @param input 输入字符串
     * @return 如果包含危险字符返回 true,否则返回 false
     */
    public static boolean containsXss(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 清理字符串,移除所有 HTML 标签
     *
     * @param input 输入字符串
     * @return 清理后的纯文本
     */
    public static String stripHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 移除所有 HTML 标签
        return input.replaceAll("<[^>]+>", "");
    }
}
