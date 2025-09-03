package com.zj.aiagemt.common.design.singletonresponsibilitychain;


/**
 * 责任链执行节点接口
 *
 * @author QingshanWu
 * @date 2025/07/08
 */
public interface ILogicLink<REQUEST, CONTEXT, RESULT> extends ILogicChainArmory<REQUEST, CONTEXT, RESULT> {

    RESULT apply(REQUEST request, CONTEXT dynamicContext);

}
