package com.zj.aiagemt.service.rag.split.fixedstructure;

import com.zj.aiagemt.service.rag.split.fixedstructure.base.AbstracStructureSplit;
import com.zj.aiagemt.service.rag.split.fixedstructure.impl.DefaulteStructureSplit;
import com.zj.aiagemt.service.rag.split.fixedstructure.model.StructureEnumType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class StructureSplitFactory {
    @Resource
    private List<AbstracStructureSplit> splits;
    @Resource
    private DefaulteStructureSplit defaulteStructureSplit;

    private HashMap<StructureEnumType, AbstracStructureSplit> hashMap;

    @PostConstruct
    public void init() {
        for (AbstracStructureSplit split : splits) {
            hashMap.put(split.getType(),  split);
        }
    }

    public AbstracStructureSplit getSplit(StructureEnumType type) {
        if(hashMap.containsKey(type)){
            return hashMap.get(type);
        }
        log.info("未找到对应的结构化分割器 {}，使用默认分割器", type);
        return defaulteStructureSplit;
    }
}
