package com.zj.aiagent.infrastructure.user.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件发送日志 PO
 */
@Data
@TableName("email_log")
public class EmailLogPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标邮箱 */
    private String email;

    /** 验证码 */
    private String code;

    /** 操作类型: REGISTER, RESET_PASSWORD, etc. */
    private String operationType;

    /** 是否发送成功 */
    private Boolean sent;

    /** 发送失败时的错误信息 */
    private String errorMessage;

    /** 发送时间 */
    private LocalDateTime sentAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
