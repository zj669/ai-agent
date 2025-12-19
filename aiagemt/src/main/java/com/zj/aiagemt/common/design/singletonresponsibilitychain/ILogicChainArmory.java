package com.zj.aiagemt.common.design.singletonresponsibilitychain;



public interface ILogicChainArmory<REQUEST, CONTEXT, RESULT> {

    ILogicLink<REQUEST, CONTEXT, RESULT> next();

    ILogicLink<REQUEST, CONTEXT, RESULT> appendNext(ILogicLink<REQUEST, CONTEXT, RESULT> next);

}
