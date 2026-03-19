package com.zj.aiagent.infrastructure.knowledge;

import com.zj.aiagent.domain.knowledge.port.TextSplitterPort;
import com.zj.aiagent.domain.knowledge.valobj.ChunkingConfig;
import com.zj.aiagent.domain.knowledge.valobj.ChunkingStrategy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 根据分块策略路由到具体实现
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class RoutedTextSplitterAdapter implements TextSplitterPort {

    private final List<ChunkingStrategySplitter> splitters;

    @Override
    public List<String> split(List<String> texts, ChunkingConfig config) {
        ChunkingConfig normalized = (config != null ? config : ChunkingConfig.fixedDefault()).normalized();
        normalized.validate();

        ChunkingStrategy strategy = normalized.getStrategy() != null
                ? normalized.getStrategy()
                : ChunkingStrategy.FIXED;

        return splitters.stream()
                .filter(splitter -> splitter.supports(strategy))
                .findFirst()
                .map(splitter -> splitter.split(texts, normalized))
                .orElseGet(() -> {
                    log.warn("未找到分块策略 {} 的实现，回退到 FIXED", strategy);
                    ChunkingConfig fallback = ChunkingConfig.fixedDefault();
                    return splitters.stream()
                            .filter(splitter -> splitter.supports(ChunkingStrategy.FIXED))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("未找到 FIXED 分块实现"))
                            .split(texts, fallback);
                });
    }
}
