package com.zj.aiagent.shared.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP工具类 - 共享内核
 * 
 * <p>
 * 用于获取客户端真实IP地址
 * 
 * @author zj
 * @since 2025-12-21
 */
public class IpUtils {

    /**
     * 获取客户端真实IP地址
     * 
     * <p>
     * 依次检查以下HTTP头：
     * <ul>
     * <li>X-Forwarded-For - 标准代理头</li>
     * <li>X-Real-IP - Nginx常用头</li>
     * <li>Proxy-Client-IP - Apache ServerCluster头</li>
     * <li>WL-Proxy-Client-IP - WebLogic头</li>
     * <li>HTTP_X_FORWARDED_FOR - 其他代理头</li>
     * <li>HTTP_CLIENT_IP - 其他代理头</li>
     * <li>最后使用request.getRemoteAddr()</li>
     * </ul>
     * 
     * @param request HTTP请求对象
     * @return 客户端真实IP地址
     */
    public static String getRealIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            // X-Forwarded-For可能包含多个IP，取第一个
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (isValidIp(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    /**
     * 检查IP是否有效
     * 
     * @param ip IP地址
     * @return true-有效，false-无效
     */
    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }
}
