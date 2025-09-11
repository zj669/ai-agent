package com.zj.aiagemt.service.agent.armory.model;


import com.zj.aiagemt.service.agent.armory.factory.DefaultAgentArmoryFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentArmoryVO {

    private DefaultAgentArmoryFactory.DynamicContext dynamicContext;

}
