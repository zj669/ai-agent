package com.zj.aiagent.domain.prompt.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zj.aiagent.domain.prompt.parse.entity.PromptConfigEntity;
import com.zj.aiagent.domain.prompt.parse.entity.PromptConfigResult;
import org.springframework.stereotype.Component;

@Component
public class PromptConfigParseFactory {
    public PromptConfigResult parseConfig(PromptConfigEntity config) {
        JSONObject jsonObject = JSON.parseObject(config.getConfig());
        return  PromptConfigResult.builder()
                .prompt(jsonObject.getString("systemPrompt"))
                .build();
    }
}
