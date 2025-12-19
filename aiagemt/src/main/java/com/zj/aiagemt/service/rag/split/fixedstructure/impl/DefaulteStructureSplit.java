package com.zj.aiagemt.service.rag.split.fixedstructure.impl;

import com.zj.aiagemt.service.rag.split.fixedstructure.base.AbstracStructureSplit;
import com.zj.aiagemt.service.rag.split.fixedstructure.model.StructureEnumType;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class DefaulteStructureSplit extends AbstracStructureSplit {
    @Override
    public List<String> splitText(String text) {
        return List.of(text);
    }

    @Override
    public StructureEnumType getType() {
        return null;
    }
}
