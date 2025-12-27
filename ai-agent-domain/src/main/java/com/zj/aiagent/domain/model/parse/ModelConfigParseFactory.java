package com.zj.aiagent.domain.model.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zj.aiagent.domain.model.parse.entity.ModelConfigEntity;
import com.zj.aiagent.domain.model.parse.entity.ModelConfigResult;
import jakarta.annotation.Resource;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ModelConfigParseFactory {
    @Resource
    private WebClient.Builder webClientBuilder1;
    @Resource
    private RestClient.Builder restClientBuilder1;

    public ModelConfigResult parseConfig(ModelConfigEntity config) {
        JSONObject jsonObject = JSON.parseObject(config.getConfig());
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(jsonObject.getString("baseUrl"))
                .apiKey(jsonObject.getString("apiKey"))
                .webClientBuilder(webClientBuilder1)
                .restClientBuilder(restClientBuilder1)
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(jsonObject.getString("modelName"))
                        .build()
                )
                .build();
        return ModelConfigResult.builder()
                .chatModel(chatModel)
                .build();
    }
}
