package com.zj.aiagemt.service.rag.split;

import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.Arrays;
import java.util.List;

public class RegularSplit  extends TextSplitter {
    // 正则表达式
    private String regular;

    public RegularSplit(String regular) {
        this.regular = regular;
    }

    @Override
    protected List<String> splitText(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        
        if (regular == null || regular.isEmpty()) {
            return List.of(text);
        }
        
        String[] parts = text.split(regular);
        return Arrays.asList(parts);
    }
}