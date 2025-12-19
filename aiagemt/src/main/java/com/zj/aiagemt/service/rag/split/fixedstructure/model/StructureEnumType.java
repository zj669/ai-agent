package com.zj.aiagemt.service.rag.split.fixedstructure.model;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zj.aiagemt.model.common.IBaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum StructureEnumType implements IBaseEnum<Integer> {

    MD(0, "MD文档"),
    JAVA(1, "JAVA代码"),
    PYTHON(2, "Python代码"),
    ;

    @EnumValue
    private Integer value;
    @JsonValue
    private String label;

}
