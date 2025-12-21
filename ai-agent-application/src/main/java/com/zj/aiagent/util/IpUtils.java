package com.zj.aiagent.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * IP工具类 - 应用层
 * 
 * 用于获取HTTP请求的真实IP地址，防止IP伪造
 * 注意：依赖HttpServletRequest，属于应用层
 */
@Slf4j
public class IpUtils {

    /**
     * 可信任的内网代理IP段
     */
    private static final Set<String> TRUSTED_PROXY_PREFIXES = Set.of(
            "10.",
            "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.",
            "172.24.", "172.25.", "172.26.", "172.27.",
            "172.28.", "172.29.", "172.30.", "172.31.",
            "192.168.",
            "127.0.0.1");

    /**
     * 获取真实IP地址(防伪造)
     * 
     * @param request HTTP请求
     * @return 真实IP地址
     */
    public static String getRealIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // 1. 优先获取Socket连接的真实IP
        String remoteAddr = request.getRemoteAddr();

        // 2. 如果请求来自可信代理,才解析X-Forwarded-For
        if (isTrustedProxy(remoteAddr)) {
            // 尝试从X-Forwarded-For获取
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
                // 取第一个IP(客户端真实IP)
                String clientIp = xff.split(",")[0].trim();
                if (isValidIp(clientIp)) {
                    log.debug("从X-Forwarded-For获取IP: {}", clientIp);
                    return clientIp;
                }
            }

            // 尝试从X-Real-IP获取
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty() && !"unknown".equalsIgnoreCase(realIp)) {
                if (isValidIp(realIp)) {
                    log.debug("从X-Real-IP获取IP: {}", realIp);
                    return realIp;
                }
            }
        }

        // 3. 默认返回Socket连接IP(无法伪造)
        log.debug("使用RemoteAddr作为IP: {}", remoteAddr);
        return remoteAddr;
    }

    /**
     * 检查是否为可信代理
     * 
     * @param ip IP地址
     * @return 是否可信
     */
    private static boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        return TRUSTED_PROXY_PREFIXES.stream()
                .anyMatch(ip::startsWith);
    }

    /**
     * 验证IP格式是否有效
     * 
     * @param ip IP地址
     * @return 是否有效
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }

        // 简单的IP格式验证
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private IpUtils() {
        // 工具类,禁止实例化
    }
}
