package com.zj.aiagemt.service.rag.split.fixedstructure.impl;

import com.zj.aiagemt.service.rag.split.fixedstructure.base.AbstracStructureSplit;
import com.zj.aiagemt.service.rag.split.fixedstructure.model.StructureEnumType;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class JavaSructureSplit extends AbstracStructureSplit {
    @Override
    public List<String> splitText(String text) {
        return List.of();
    }

    @Override
    public StructureEnumType getType() {
        return StructureEnumType.JAVA;
    }
}
