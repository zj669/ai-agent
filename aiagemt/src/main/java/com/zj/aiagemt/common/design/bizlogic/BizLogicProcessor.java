package com.zj.aiagemt.common.design.bizlogic;

/**
 * 业务逻辑抽象模板
 *
 * @author: wuqingshan
 * @since : 2025年6月26日 15:12:12
 */
public interface BizLogicProcessor<CONTEXT> {

    void init(CONTEXT context);

    void validate(CONTEXT context);

    void fill(CONTEXT context);

    void handle(CONTEXT context);

    default void post(CONTEXT context) {
    }
}
