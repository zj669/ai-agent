package com.zj.aiagent.infrastructure.knowledge.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库数据集 PO (Persistent Object)
 * 对应数据库表: knowledge_dataset
 */
@Data
@TableName("knowledge_dataset")
public class KnowledgeDatasetPO {

    @TableId(type = IdType.INPUT)
    private String datasetId;

    private String name;
    private String description;
    private Long userId;
    private Long agentId;
    private Integer documentCount;
    private Integer totalChunks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
