package com.zj.aiagent.shared.constants;

/**
 * Redis Key常量 - 共享层
 * 
 * <p>
 * 定义系统中所有Redis Key的命名规范和常量
 * 
 * @author zj
 * @since 2025-12-21
 */
public class RedisKeyConstants {

    private RedisKeyConstants() {
        // 工具类,禁止实例化
    }
    /**
     * 邮件相关Redis Key常量
     */
    public static class Email {

        /**
         * 邮箱验证码存储Key
         * <p>
         * 格式: email:verification:code:{email}
         * <p>
         * 过期时间: 5分钟
         */
        public static final String VERIFICATION_CODE_PREFIX = "email:verification:code:";

        private Email() {
            // 禁止实例化
        }
    }

    /**
     * 限流相关Redis Key常量
     */
    public static class RateLimit {

        /**
         * IP限流Key前缀
         * <p>
         * 格式: rate_limit:ip:{ip}
         * <p>
         * 时间窗口: 60秒,最大次数: 5次
         */
        public static final String IP_PREFIX = "rate_limit:ip:";

        /**
         * 邮箱限流Key前缀
         * <p>
         * 格式: rate_limit:email:{email}
         * <p>
         * 时间窗口: 24小时,最大次数: 10次
         */
        public static final String EMAIL_PREFIX = "rate_limit:email:";

        /**
         * 设备指纹限流Key前缀
         * <p>
         * 格式: rate_limit:device:{deviceId}
         * <p>
         * 时间窗口: 1小时,最大次数: 20次
         */
        public static final String DEVICE_PREFIX = "rate_limit:device:";

        private RateLimit() {
            // 禁止实例化
        }
    }

    /**
     * 用户相关Redis Key常量
     * <p>
     * 预留用于未来扩展
     */
    public static class User {

        /**
         * Token黑名单Key前缀
         * <p>
         * 格式: user:token:blacklist:{token}
         * <p>
         * 用于存储已退出登录的Token，防止被重复使用
         */
        public static final String TOKEN_BLACKLIST_PREFIX = "user:token:blacklist:";

        private User() {
            // 禁止实例化
        }
    }

    /**
     * 幂等相关Redis Key常量
     */
    public static class Idempotent {

        /**
         * 幂等锁Key前缀
         * <p>
         * 格式: idempotent:{userId}:{method}:{paramsHash}
         * <p>
         * 默认过期时间: 5秒
         */
        public static final String PREFIX = "idempotent:";

        private Idempotent() {
            // 禁止实例化
        }
    }

    /**
     * 人工介入相关Redis Key常量
     */
    public static class HumanIntervention {

        /**
         * 人工介入暂停状态Key前缀
         * <p>
         * 格式: human_intervention:pause:{conversationId}:{nodeId}
         * <p>
         * 过期时间: 24小时
         */
        public static final String PAUSE_PREFIX = "human_intervention:pause:";

        /**
         * 执行上下文Key前缀
         * <p>
         * 格式: human_intervention:context:{conversationId}
         * <p>
         * 过期时间: 24小时
         */
        public static final String CONTEXT_PREFIX = "human_intervention:context:";

        /**
         * 默认过期时间: 24小时（秒）
         */
        public static final long DEFAULT_TTL_SECONDS = 24 * 60 * 60;

        /**
         * 已完成状态过期时间: 5分钟（秒）
         */
        public static final long COMPLETED_TTL_SECONDS = 5 * 60;

        private HumanIntervention() {
            // 禁止实例化
        }
    }

    /**
     * 工作流检查点相关Redis Key常量
     */
    public static class WorkflowCheckpoint {

        public static final String CHECKPOINT_MAP_PREFIX = "workflow:checkpoint:";

        public static final long DEFAULT_TTL_SECONDS = 24 * 60 * 60;

        private WorkflowCheckpoint() {
            // 禁止实例化
        }
    }
}
