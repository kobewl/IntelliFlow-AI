package com.kobeai.hub.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobeai.hub.model.AIPlatform;
import com.kobeai.hub.model.Message;
import com.kobeai.hub.model.Platform;
import com.kobeai.hub.model.PromptTemplate;
import com.kobeai.hub.model.User;
import com.kobeai.hub.repository.AIPlatformRepository;
import com.kobeai.hub.repository.MessageRepository;
import com.kobeai.hub.service.AI.DeepSeekService;
import com.kobeai.hub.service.PromptOptimizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Optional;

@Slf4j
@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    // 保留默认值，当数据库中没有配置时使用
    @Value("${ai.deepseek.base-url:https://api.deepseek.com/v1}")
    private String defaultBaseUrl;

    @Value("${ai.deepseek.api-key:}")
    private String defaultApiKey;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String defaultModel;

    @Value("${ai.deepseek.max-tokens:2000}")
    private int maxTokens;

    @Value("${ai.deepseek.temperature:0.7}")
    private double temperature;

    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final MessageRepository messageRepository;
    private final AIPlatformRepository aiPlatformRepository;

    @Autowired
    private PromptOptimizationService promptOptimizationService;

    public DeepSeekServiceImpl(MessageRepository messageRepository, AIPlatformRepository aiPlatformRepository) {
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newCachedThreadPool();
        this.messageRepository = messageRepository;
        this.aiPlatformRepository = aiPlatformRepository;
    }

    @PostConstruct
    @Override
    public void initialize() {
        log.info("DeepSeek服务正在初始化...");
    }

    @PreDestroy
    @Override
    public void close() {
        executorService.shutdown();
    }

    /**
     * 从数据库获取DeepSeek平台的配置信息
     * 如果数据库中不存在配置，则使用默认值
     * 
     * @param user 当前用户，用于获取用户自定义配置
     * @return 平台配置信息对象
     */
    private AIPlatform getDeepSeekConfig(User user) {
        // 首先尝试查找用户自定义的DeepSeek平台配置
        Optional<AIPlatform> userPlatform = Optional.empty();
        if (user != null) {
            userPlatform = aiPlatformRepository.findAll().stream()
                    .filter(p -> p.getType() == Platform.DEEPSEEK && user.getId().equals(p.getUserId()))
                    .findFirst();
        }

        // 如果用户没有自定义配置，则使用系统默认配置
        if (!userPlatform.isPresent()) {
            userPlatform = aiPlatformRepository.findByType(Platform.DEEPSEEK);
        }

        // 如果数据库中没有配置，则创建一个临时配置对象使用默认值
        if (!userPlatform.isPresent()) {
            AIPlatform defaultPlatform = new AIPlatform();
            defaultPlatform.setBaseUrl(defaultBaseUrl);
            defaultPlatform.setApiKey(defaultApiKey);
            defaultPlatform.setType(Platform.DEEPSEEK);
            defaultPlatform.setName("DeepSeek AI");

            log.warn("未在数据库中找到DeepSeek平台配置，使用默认配置。请检查系统初始化是否正确。");
            return defaultPlatform;
        }
        
        return userPlatform.get();
    }

    @Override
    public SseEmitter sendMessage(String message, Message aiMessage, User user) {
        log.info("准备发送消息到 DeepSeek API");
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        executorService.execute(() -> {
            try {
                log.info("开始处理消息发送");
                emitter.send(SseEmitter.event()
                        .name("init")
                        .data("连接已建立")
                        .build());

                // 获取DeepSeek平台配置
                AIPlatform platform = getDeepSeekConfig(user);
                String apiKey = platform.getApiKey();
                String baseUrl = platform.getBaseUrl();
                String model = defaultModel; // 目前model仍然使用默认值，后续可以扩展到数据库中
                
                // 验证配置是否有效
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new RuntimeException("DeepSeek API密钥未配置");
                }
                if (baseUrl == null || baseUrl.trim().isEmpty()) {
                    throw new RuntimeException("DeepSeek API基础URL未配置");
                }

                // 使用 Prompt 优化引擎优化消息
                PromptTemplate template = promptOptimizationService.findBestTemplate("chat", message);
                Map<String, Object> variables = new HashMap<>();
                variables.put("model", model);
                variables.put("temperature", temperature);

                String optimizedMessage = message;
                if (template != null) {
                    optimizedMessage = promptOptimizationService.optimizePrompt(message, template, variables);
                    log.info("消息已优化，Token 减少: {}%",
                            ((estimateOriginalTokens(message)
                                    - promptOptimizationService.estimateTokens(optimizedMessage)) * 100.0 /
                                    estimateOriginalTokens(message)));
                }

                // 准备请求体
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", model);

                List<Map<String, String>> messages = new ArrayList<>();
                Map<String, String> systemMessage = new HashMap<>();
                messages.add(systemMessage);

                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", optimizedMessage);
                messages.add(userMessage);

                requestBody.put("messages", messages);
                requestBody.put("stream", true);
                requestBody.put("max_tokens", maxTokens);

                String apiUrl = baseUrl + "/chat/completions";
                log.info("准备发送请求到 DeepSeek API: {}", apiUrl);

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setDoOutput(true);
                conn.setChunkedStreamingMode(0);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);

                String requestBodyJson = objectMapper.writeValueAsString(requestBody);
                log.info("发送请求体: {}", requestBodyJson);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBodyJson.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                log.info("API响应状态码: {}", responseCode);

                if (responseCode == 200) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder contentBuilder = new StringBuilder();
                        String line;
                        // 记录最后一次接收到有效数据（以"data:"开头）的时间
                        long lastValidTime = System.currentTimeMillis();
                        // 定义超时时间为60秒，如果60秒内只收到keep-alive而无有效数据，则终止读取
                        final long TIMEOUT_THRESHOLD = 60000;

                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty())
                                continue;

                            String trimmedLine = line.trim();
                            // 如果接收到的是keep-alive行，则检查是否超时
                            if (trimmedLine.equals(": keep-alive")) {
                                log.debug("跳过keep-alive行: {}", trimmedLine);
                                if (System.currentTimeMillis() - lastValidTime > TIMEOUT_THRESHOLD) {
                                    log.warn("长时间仅收到keep-alive，无有效数据，终止读取");
                                    break;
                                }
                                continue;
                            }

                            if (!trimmedLine.startsWith("data:")) {
                                log.debug("跳过非SSE数据行: {}", trimmedLine);
                                continue;
                            }

                            if (trimmedLine.equals("data: [DONE]")) {
                                String finalContent = contentBuilder.toString();
                                // 保存AI回复到数据库
                                aiMessage.setContent(finalContent);
                                messageRepository.save(aiMessage);
                                log.info("AI 响应已保存到数据库");

                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(finalContent)
                                        .build());
                                break;
                            }

                            // 更新最后接收到有效数据的时间
                            lastValidTime = System.currentTimeMillis();

                            String data = trimmedLine.substring(6).trim();
                            if (data.isEmpty()) {
                                continue;
                            }

                            try {
                                Map<String, Object> response = objectMapper.readValue(data, Map.class);
                                if (response.containsKey("choices")) {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response
                                            .get("choices");
                                    if (!choices.isEmpty()) {
                                        Map<String, Object> choice = choices.get(0);
                                        @SuppressWarnings("unchecked")
                                        Map<String, String> delta = (Map<String, String>) choice.get("delta");
                                        if (delta != null && delta.containsKey("content")) {
                                            String content = delta.get("content");
                                            contentBuilder.append(content);

                                            Map<String, Object> deltaMap = new HashMap<>();
                                            deltaMap.put("content", content);

                                            Map<String, Object> choiceMap = new HashMap<>();
                                            choiceMap.put("delta", deltaMap);

                                            Map<String, Object> responseMap = new HashMap<>();
                                            responseMap.put("choices", Arrays.asList(choiceMap));

                                            emitter.send(SseEmitter.event()
                                                    .name("message")
                                                    .data(responseMap)
                                                    .build());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("解析响应数据失败: {}", e.getMessage());
                            }
                        }

                        // 当读取结束后，若contentBuilder有累积内容，则发送完成事件；否则，返回错误提示
                        if (contentBuilder.length() > 0) {
                            String finalContent = contentBuilder.toString();
                            aiMessage.setContent(finalContent);
                            messageRepository.save(aiMessage);

                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(finalContent)
                                    .build());
                        } else {
                            log.warn("未收到有效的响应数据");
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("未收到有效的响应数据")
                                    .build());
                        }
                    }
                } else {
                    handleErrorResponse(conn, aiMessage, emitter);
                }

                log.info("消息处理完成");
                emitter.complete();
            } catch (Exception e) {
                handleException(e, aiMessage, emitter);
            }
        });

        return emitter;
    }

    private void handleErrorResponse(HttpURLConnection conn, Message aiMessage, SseEmitter emitter) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                errorResponse.append(line);
            }
            String errorMessage = String.format("API请求失败，状态码: %d, 错误信息: %s",
                    conn.getResponseCode(), errorResponse.toString());
            log.error(errorMessage);

            aiMessage.setContent("Error: " + errorMessage);
            messageRepository.save(aiMessage);

            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(errorMessage)
                    .build());
        } catch (IOException e) {
            log.error("处理错误响应失败: {}", e.getMessage());
        }
    }

    private void handleException(Exception e, Message aiMessage, SseEmitter emitter) {
        log.error("消息处理过程中发生错误: {}", e.getMessage());
        try {
            aiMessage.setContent("Error: " + e.getMessage());
            messageRepository.save(aiMessage);

            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("发送消息失败: " + e.getMessage())
                    .build());
            emitter.complete();
        } catch (IOException ex) {
            log.error("发送错误消息失败: {}", ex.getMessage());
        }
    }

    private int estimateOriginalTokens(String message) {
        // 简单估算，每个单词约等于1.3个token
        return (int) (message.split("\\s+").length * 1.3);
    }
}