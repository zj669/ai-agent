package com.zj.aiagemt.service.rag.split.fixedstructure.base;

import com.zj.aiagemt.model.common.IBaseEnum;
import com.zj.aiagemt.service.rag.split.fixedstructure.model.StructureEnumType;
import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.List;

public abstract class AbstracStructureSplit extends TextSplitter{
    public abstract List<String> splitText(String text);
    public abstract StructureEnumType getType();
}
