package com.zj.aiagent.infrastructure.knowledge;

import com.zj.aiagent.domain.knowledge.valobj.ChunkingConfig;
import com.zj.aiagent.domain.knowledge.valobj.ChunkingStrategy;
import java.util.List;

/**
 * 分块策略执行器
 */
public interface ChunkingStrategySplitter {

    boolean supports(ChunkingStrategy strategy);

    List<String> split(List<String> texts, ChunkingConfig config);
}
