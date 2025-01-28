package com.kobeai.hub.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobeai.hub.model.Message;
import com.kobeai.hub.repository.MessageRepository;
import com.kobeai.hub.service.DeepseekService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                userMessage.put("content", message);
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

                log.info("请求已发送，等待响应");
                int responseCode = conn.getResponseCode();
                log.info("收到响应状态码: {}", responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        StringBuilder contentBuilder = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            log.debug("收到响应行: {}", line);
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if ("[DONE]".equals(data)) {
                                    log.info("收到完成标记");
                                    String finalContent = contentBuilder.toString();
                                    // 保存完整的 AI 响应到数据库
                                    aiMessage.setContent(finalContent);
                                    messageRepository.save(aiMessage);
                                    log.info("AI 响应已保存到数据库");

                                    emitter.send(SseEmitter.event()
                                            .name("done")
                                            .data(finalContent)
                                            .build());
                                    break;
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
                                                log.debug("发送内容片段: {}", content);
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
                    }
                } else {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        String errorMessage = String.format("API请求失败，状态码: %d, 错误信息: %s",
                                responseCode, errorResponse.toString());
                        log.error(errorMessage);

                        // 保存错误信息到数据库
                        aiMessage.setContent("Error: " + errorMessage);
                        messageRepository.save(aiMessage);

                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(errorMessage)
                                .build());
                    }
                }

                log.info("消息处理完成");
                emitter.complete();
            } catch (Exception e) {
                log.error("消息处理过程中发生错误: {}", e.getMessage());
                try {
                    // 保存错误信息到数据库
                    aiMessage.setContent("Error: " + e.getMessage());
                    messageRepository.save(aiMessage);

                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("发送消息失败: " + e.getMessage())
                            .build());
                    emitter.complete();
                } catch (IOException ex) {
                    log.error("发送错误事件失败: {}", ex.getMessage());
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }
}