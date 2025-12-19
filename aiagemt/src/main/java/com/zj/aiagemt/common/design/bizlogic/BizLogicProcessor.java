package com.zj.aiagemt.common.design.bizlogic;


public interface BizLogicProcessor<CONTEXT> {

    void init(CONTEXT context);

    void validate(CONTEXT context);

    void fill(CONTEXT context);

    void handle(CONTEXT context);

    default void post(CONTEXT context) {
    }
}
