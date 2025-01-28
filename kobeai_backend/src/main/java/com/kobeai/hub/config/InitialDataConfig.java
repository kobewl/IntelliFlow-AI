package com.kobeai.hub.config;

import com.kobeai.hub.model.AIPlatform;
import com.kobeai.hub.model.Platform;
import com.kobeai.hub.repository.AIPlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InitialDataConfig {

    private final AIPlatformRepository platformRepository;

    @Value("${ai.deepseek.api-key:sk-ae9973eaca5748ed90a87a4e4e4784e6}")
    private String deepseekApiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            log.info("Checking if AI platforms need to be initialized...");

            if (!platformRepository.existsByType(Platform.DEEPSEEK)) {
                log.info("Initializing DeepSeek platform...");
                AIPlatform deepseek = new AIPlatform();
                deepseek.setType(Platform.DEEPSEEK);
                deepseek.setName("DeepSeek AI");
                deepseek.setDescription("DeepSeek AI Chat Platform");
                deepseek.setApiKey(deepseekApiKey);
                deepseek.setBaseUrl(deepseekBaseUrl);
                deepseek.setEnabled(true);
                platformRepository.save(deepseek);
                log.info("DeepSeek platform initialized successfully with API key: {}...",
                        deepseekApiKey.substring(0, 8));
            } else {
                log.info("DeepSeek platform already exists");
                // 更新现有平台的配置
                platformRepository.findByType(Platform.DEEPSEEK).ifPresent(platform -> {
                    platform.setApiKey(deepseekApiKey);
                    platform.setBaseUrl(deepseekBaseUrl);
                    platform.setEnabled(true);
                    platformRepository.save(platform);
                    log.info("Updated DeepSeek platform configuration");
                });
            }
        };
    }
}