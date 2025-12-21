package com.zj.aiagent.shared.design.strategymode;


import com.zj.aiagent.shared.model.enums.IBaseEnum;

public interface StrategyMapper {
    /**
     * 策略处理的类型
     *
     * @return IBaseEnum 策略处理的类型
     **/
    IBaseEnum getType();
}