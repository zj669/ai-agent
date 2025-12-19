package com.zj.aiagemt.common.design.strategymode;

import com.zj.aiagemt.model.common.IBaseEnum;


public interface StrategyMapper {
    /**
     * 策略处理的类型
     *
     * @return IBaseEnum 策略处理的类型
     **/
    IBaseEnum getType();
}