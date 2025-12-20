package com.zj.aiagemt.utils;

import com.zj.aiagemt.constants.EmailConstants;

import java.security.SecureRandom;

/**
 * 邮箱验证码生成器
 */
public class EmailCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成随机验证码
     * 
     * @return 6位数字验证码
     */
    public static String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < EmailConstants.CODE_LENGTH; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    private EmailCodeGenerator() {
        // 工具类,禁止实例化
    }
}
