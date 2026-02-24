package com.zj.aiagent.infrastructure.llm.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_provider_config")
public class LlmProviderConfigPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Boolean isDefault;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
