package com.kobeai.hub.agent.config;

import io.agentscope.core.model.OpenAIChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class AgentScopeConfig {

    @Value("${app.ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${app.ai.deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

    @Value("${app.ai.doubao.api-key:}")
    private String doubaoApiKey;

    @Value("${app.ai.doubao.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String doubaoBaseUrl;

    @Bean
    public Map<String, OpenAIChatModel> modelMap() {
        Map<String, OpenAIChatModel> models = new ConcurrentHashMap<>();

        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            models.put("deepseek-chat", OpenAIChatModel.builder()
                    .apiKey(deepseekApiKey)
                    .modelName("deepseek-chat")
                    .baseUrl(deepseekBaseUrl + "/v1")
                    .build());
            log.info("AgentScope: 已注册 DeepSeek 模型 [deepseek-chat]");
        } else {
            log.warn("AgentScope: DeepSeek API Key 未配置, 跳过注册");
        }

        if (doubaoApiKey != null && !doubaoApiKey.isEmpty()) {
            models.put("doubao-chat", OpenAIChatModel.builder()
                    .apiKey(doubaoApiKey)
                    .modelName("doubao-lite-32k")
                    .baseUrl(doubaoBaseUrl)
                    .build());
            log.info("AgentScope: 已注册豆包模型 [doubao-chat]");
        } else {
            log.warn("AgentScope: 豆包 API Key 未配置, 跳过注册");
        }

        return models;
    }
}
