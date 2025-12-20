package com.zj.aiagemt.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邮件发送日志实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("email_send_log")
@Schema(description = "邮件发送日志")
public class EmailSendLog {

    /**
     * 主键ID
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 收件人邮箱
     */
    @Schema(description = "收件人邮箱")
    private String email;

    /**
     * 邮件类型(1:验证码)
     */
    @Schema(description = "邮件类型")
    private Integer emailType;

    /**
     * 发送状态(0:失败, 1:成功)
     */
    @Schema(description = "发送状态")
    private Integer sendStatus;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMsg;

    /**
     * 请求IP
     */
    @Schema(description = "请求IP")
    private String ip;

    /**
     * 设备指纹
     */
    @Schema(description = "设备指纹")
    private String deviceId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
