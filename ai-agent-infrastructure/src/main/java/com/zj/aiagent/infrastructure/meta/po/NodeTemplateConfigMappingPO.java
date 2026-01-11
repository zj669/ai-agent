package com.zj.aiagent.infrastructure.meta.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("node_template_config_mapping")
public class NodeTemplateConfigMappingPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long nodeTemplateId;
    private Long fieldDefId;
    private String groupName;
    private Integer sortOrder;
    private String overrideDefault;
    private Integer isRequired;
    private LocalDateTime createTime;
}
