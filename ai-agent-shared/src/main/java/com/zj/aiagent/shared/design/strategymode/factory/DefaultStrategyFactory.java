package com.zj.aiagent.shared.design.strategymode.factory;


import com.zj.aiagent.shared.design.strategymode.StrategyHandler;
import com.zj.aiagent.shared.model.enums.IBaseEnum;
import lombok.Data;



import java.util.List;
import java.util.Objects;


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
        if (Objects.isNull(type) || getAllHandlers() == null || getAllHandlers().isEmpty()) {
            return null;
        }
        return getAllHandlers().stream()
                .filter(handler -> handler.getType().equals(type))
                .findFirst()
                .orElse(null);
    }

}
