package com.zj.aiagemt.common.design.strategymode.factory;


import lombok.Data;

import org.springframework.util.CollectionUtils;

import com.zj.aiagemt.common.design.strategymode.StrategyHandler;
import com.zj.aiagemt.model.common.IBaseEnum;

import java.util.List;
import java.util.Objects;

/**
 * 默认策略路由工厂
 *
 * @author: wuqingshan
 * @since : 2025年6月26日 15:12:12
 */
@Data
public abstract class DefaultStrategyFactory<Request, Result> {


    public abstract List<StrategyHandler<Request, Result>> getAllHandlers();

    /**
     * 获取对应的适配
     *
     * @param type 原数据
     * @return handler
     */
    public StrategyHandler<Request, Result> getHandler(IBaseEnum type) {
        if (Objects.isNull(type) || CollectionUtils.isEmpty(getAllHandlers())) {
            return null;
        }
        return getAllHandlers().stream()
                .filter(handler -> handler.getType().equals(type))
                .findFirst()
                .orElse(null);
    }

}
