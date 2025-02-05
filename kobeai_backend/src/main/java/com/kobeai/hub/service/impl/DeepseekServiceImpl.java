package com.kobeai.hub.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobeai.hub.model.Message;
import com.kobeai.hub.model.PromptTemplate;
import com.kobeai.hub.repository.MessageRepository;
import com.kobeai.hub.service.DeepseekService;
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

@Slf4j
@Service
public class DeepseekServiceImpl implements DeepseekService {

    @Value("${ai.deepseek.base-url}")
    private String baseUrl;

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${ai.deepseek.max-tokens:2000}")
    private int maxTokens;

    @Value("${ai.deepseek.temperature:0.7}")
    private double temperature;

    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final MessageRepository messageRepository;

    @Autowired
    private PromptOptimizationService promptOptimizationService;

    public DeepseekServiceImpl(MessageRepository messageRepository) {
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newCachedThreadPool();
        this.messageRepository = messageRepository;
    }

    @PostConstruct
    @Override
    public void initialize() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("Deepseek API key not configured");
        }
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new RuntimeException("Deepseek base URL not configured");
        }
        log.info("Deepseek service initialized with API key: {}", apiKey.substring(0, 8) + "...");
    }

    @PreDestroy
    @Override
    public void close() {
        executorService.shutdown();
    }

    @Override
    public SseEmitter sendMessage(String message, Message aiMessage) {
        log.info("准备发送消息到 DeepSeek API");
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        executorService.execute(() -> {
            try {
                log.info("开始处理消息发送");
                emitter.send(SseEmitter.event()
                        .name("init")
                        .data("连接已建立")
                        .build());

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
                systemMessage.put("role", "system");
                systemMessage.put("content", "You are a helpful assistant.");
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
                conn.setDoOutput(true);
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

                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty())
                                continue;

                            // 检查是否是SSE数据前缀
                            if (!line.startsWith("data:")) {
                                log.debug("跳过非SSE数据行: {}", line);
                                continue;
                            }

                            if (line.equals("data: [DONE]")) {
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

                            // 确保数据行以"data:"开头并提取JSON部分
                            String data = line.substring(6).trim();
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