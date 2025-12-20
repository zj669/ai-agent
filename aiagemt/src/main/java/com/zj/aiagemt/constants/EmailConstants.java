package com.zj.aiagemt.constants;

/**
 * 邮件相关常量
 */
public class EmailConstants {

    /**
     * 验证码长度
     */
    public static final int CODE_LENGTH = 6;

    /**
     * 验证码有效期(分钟)
     */
    public static final int CODE_EXPIRE_MINUTES = 5;

    /**
     * 邮件类型 - 验证码
     */
    public static final int EMAIL_TYPE_VERIFICATION_CODE = 1;

    /**
     * 发送状态 - 失败
     */
    public static final int SEND_STATUS_FAIL = 0;

    /**
     * 发送状态 - 成功
     */
    public static final int SEND_STATUS_SUCCESS = 1;

    /**
     * 邮件主题 - 验证码
     */
    public static final String EMAIL_SUBJECT_VERIFICATION_CODE = "【AI Agent】邮箱验证码";

    /**
     * 邮件内容模板 - 验证码
     */
    public static final String EMAIL_TEMPLATE_VERIFICATION_CODE = "<div style='padding: 20px; background-color: #f5f5f5;'>"
            +
            "<div style='max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;'>"
            +
            "<h2 style='color: #333; text-align: center;'>AI Agent 邮箱验证</h2>" +
            "<div style='margin: 30px 0; padding: 20px; background-color: #f8f9fa; border-left: 4px solid #007bff;'>" +
            "<p style='margin: 0; font-size: 14px; color: #666;'>您的验证码是：</p>" +
            "<p style='margin: 10px 0; font-size: 32px; font-weight: bold; color: #007bff; letter-spacing: 5px;'>%s</p>"
            +
            "<p style='margin: 0; font-size: 14px; color: #999;'>验证码有效期为 %d 分钟</p>" +
            "</div>" +
            "<p style='color: #666; font-size: 14px; line-height: 1.6;'>如果这不是您本人的操作，请忽略此邮件。</p>" +
            "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>" +
            "<p style='color: #999; font-size: 12px; text-align: center;'>此邮件由系统自动发送，请勿回复。</p>" +
            "</div>" +
            "</div>";

    private EmailConstants() {
        // 工具类,禁止实例化
    }
}
