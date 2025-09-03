package com.zj.aiagemt.common.design.singletonresponsibilitychain;


/**
 * 责任链装配
 *
 * @author QingshanWu
 * @date 2025/07/08
 */
public interface ILogicChainArmory<REQUEST, CONTEXT, RESULT> {

    ILogicLink<REQUEST, CONTEXT, RESULT> next();

    ILogicLink<REQUEST, CONTEXT, RESULT> appendNext(ILogicLink<REQUEST, CONTEXT, RESULT> next);

}
