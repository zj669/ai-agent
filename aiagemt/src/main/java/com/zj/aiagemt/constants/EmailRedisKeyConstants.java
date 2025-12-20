package com.zj.aiagemt.constants;

/**
 * 邮件相关Redis Key常量
 */
public class EmailRedisKeyConstants {

    /**
     * 验证码存储Key - 5分钟过期
     * 格式: email:code:{email}
     */
    public static final String VERIFICATION_CODE = "email:code:%s";

    /**
     * IP限流 - 1分钟
     * 格式: email:ip_limit_1min:{ip}
     */
    public static final String IP_LIMIT_1MIN = "email:ip_limit_1min:%s";

    /**
     * IP限流 - 1小时
     * 格式: email:ip_limit_1hour:{ip}
     */
    public static final String IP_LIMIT_1HOUR = "email:ip_limit_1hour:%s";

    /**
     * IP限流 - 1天
     * 格式: email:ip_limit_1day:{ip}
     */
    public static final String IP_LIMIT_1DAY = "email:ip_limit_1day:%s";

    /**
     * 邮箱限流 - 1分钟
     * 格式: email:email_limit_1min:{email}
     */
    public static final String EMAIL_LIMIT_1MIN = "email:email_limit_1min:%s";

    /**
     * 邮箱限流 - 1小时
     * 格式: email:email_limit_1hour:{email}
     */
    public static final String EMAIL_LIMIT_1HOUR = "email:email_limit_1hour:%s";

    /**
     * 邮箱限流 - 1天
     * 格式: email:email_limit_1day:{email}
     */
    public static final String EMAIL_LIMIT_1DAY = "email:email_limit_1day:%s";

    /**
     * 设备指纹限流 - 1小时
     * 格式: email:device_limit:{deviceId}
     */
    public static final String DEVICE_LIMIT_1HOUR = "email:device_limit:%s";

    private EmailRedisKeyConstants() {
        // 工具类,禁止实例化
    }
}
