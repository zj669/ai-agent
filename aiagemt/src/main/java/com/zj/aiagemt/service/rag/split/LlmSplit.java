package com.zj.aiagemt.service.rag.split;

import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class LlmSplit extends TextSplitter {
    @Override
    protected List<String> splitText(String text) {
        return List.of();
    }
}
