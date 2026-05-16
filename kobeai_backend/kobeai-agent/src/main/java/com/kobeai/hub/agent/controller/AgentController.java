package com.kobeai.hub.agent.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobeai.hub.agent.memory.LocalFileLongTermMemory;
import com.kobeai.hub.agent.service.AgentService;
import com.kobeai.hub.agent.service.ModelRouter;
import io.agentscope.core.agui.event.AguiEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final ModelRouter modelRouter;
    private final LocalFileLongTermMemory longTermMemory;
    private final List<Map<String, Object>> toolInfoList;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 流式对话接口 (AG-UI 协议 SSE)
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(
            @RequestParam String message,
            @RequestParam(defaultValue = "auto") String model,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        String sid = sessionId != null ? sessionId : UUID.randomUUID().toString();
        String effectiveModel = "auto".equals(model)
                ? modelRouter.routeByTask(message)
                : model;
        log.info("Agent 流式对话: sessionId={}, model={}, msg={}", sid, effectiveModel,
                message.length() > 80 ? message.substring(0, 80) + "..." : message);

        return agentService.streamCall(sid, message, effectiveModel)
                .map(event -> {
                    String eventType = event.getType().name().toLowerCase();
                    String data = convertAguiEvent(event);
                    return ServerSentEvent.<String>builder()
                            .id(UUID.randomUUID().toString())
                            .event(eventType)
                            .data(data)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Agent SSE 出错: sessionId={}", sid, e);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("对话出错: " + e.getMessage())
                            .build());
                });
    }

    /** 将 AguiEvent 转为前端可消费的 JSON/text */
    private String convertAguiEvent(AguiEvent event) {
        try {
            if (event instanceof AguiEvent.ToolCallStart s) {
                return objectMapper.writeValueAsString(Map.of(
                        "toolCallId", s.toolCallId(),
                        "toolCallName", s.toolCallName()
                ));
            }
            if (event instanceof AguiEvent.ToolCallArgs a) {
                return objectMapper.writeValueAsString(Map.of(
                        "toolCallId", a.toolCallId(),
                        "delta", a.delta()
                ));
            }
            if (event instanceof AguiEvent.ToolCallEnd e) {
                return objectMapper.writeValueAsString(Map.of(
                        "toolCallId", e.toolCallId()
                ));
            }
            if (event instanceof AguiEvent.ToolCallResult r) {
                return objectMapper.writeValueAsString(Map.of(
                        "toolCallId", r.toolCallId(),
                        "content", r.content() != null ? r.content() : ""
                ));
            }
            if (event instanceof AguiEvent.TextMessageContent t) {
                return t.delta() != null ? t.delta() : "";
            }
            if (event instanceof AguiEvent.ReasoningMessageContent r) {
                return r.delta() != null ? r.delta() : "";
            }
            // RUN_STARTED, RUN_FINISHED, STATE_*, etc — 不需要发给前端
            return "";
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    /**
     * 带知识库增强的流式对话
     */
    @GetMapping(value = "/chat/rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatWithRag(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String kbId,
            @RequestParam(defaultValue = "auto") String model,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        String sid = sessionId != null ? sessionId : UUID.randomUUID().toString();
        return agentService.streamCallWithRag(sid, message, model, kbId)
                .map(event -> {
                    String eventType = event.getType().name().toLowerCase();
                    String data = convertAguiEvent(event);
                    return ServerSentEvent.<String>builder()
                            .id(UUID.randomUUID().toString())
                            .event(eventType)
                            .data(data)
                            .build();
                })
                .onErrorResume(e -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data("对话出错: " + e.getMessage())
                        .build()));
    }

    /**
     * 获取可用工具列表
     */
    @GetMapping("/tools")
    public Mono<Map<String, Object>> getTools() {
        return Mono.just(Map.of(
                "code", 200,
                "data", toolInfoList,
                "total", toolInfoList.size()
        ));
    }

    /**
     * 获取活跃会话统计
     */
    @GetMapping("/sessions")
    public Mono<Map<String, Object>> getSessions(
            @RequestParam(value = "detail", defaultValue = "false") boolean detail,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        int count = agentService.getActiveSessionCount();
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("code", 200);
        result.put("activeSessions", count);

        if (detail && sessionId != null) {
            var stats = agentService.getSessionMemory(sessionId);
            if (stats != null) {
                result.put("currentSession", Map.of(
                        "sessionId", stats.sessionId(),
                        "messageCount", stats.messageCount(),
                        "createdAt", stats.createdAt().toString()
                ));
            }
        }

        return Mono.just(result);
    }

    /**
     * 关闭会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Mono<Map<String, Object>> closeSession(@PathVariable String sessionId) {
        agentService.closeSession(sessionId);
        return Mono.just(Map.of("code", 200, "message", "会话已关闭: " + sessionId));
    }

    /**
     * 获取核心长期记忆
     */
    @GetMapping("/memory/core")
    public Mono<Map<String, Object>> getCoreMemory() {
        String content = longTermMemory.getCoreMemory();
        return Mono.just(Map.of(
                "code", 200,
                "data", content,
                "hasMemory", !content.isEmpty()
        ));
    }

    /**
     * 更新核心长期记忆
     */
    @PostMapping("/memory/core")
    public Mono<Map<String, Object>> updateCoreMemory(@RequestBody Map<String, String> body) {
        String section = body.get("section");
        String content = body.get("content");
        if (section == null || section.isBlank() || content == null || content.isBlank()) {
            return Mono.just(Map.of("code", 400, "message", "section 和 content 不能为空"));
        }
        longTermMemory.updateCoreMemory(section, content);
        return Mono.just(Map.of("code", 200, "message", "核心记忆已更新 — " + section));
    }

    /**
     * 保存一条事实
     */
    @PostMapping("/memory/fact")
    public Mono<Map<String, Object>> saveFact(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String content = body.get("content");
        String tags = body.getOrDefault("tags", "");
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return Mono.just(Map.of("code", 400, "message", "title 和 content 不能为空"));
        }
        longTermMemory.saveFact(title, content, tags.split("[,，]"));
        return Mono.just(Map.of("code", 200, "message", "事实已保存 — " + title));
    }

    /**
     * 获取最近的会话文件列表
     */
    @GetMapping("/memory/sessions")
    public Mono<Map<String, Object>> getRecentSessions(
            @RequestParam(defaultValue = "7") int days) {
        List<Path> files = longTermMemory.getRecentSessionFiles(days);
        List<Map<String, Object>> list = files.stream()
                .map(p -> Map.of(
                        "date", (Object) p.getFileName().toString().replace(".md", ""),
                        "path", p.toAbsolutePath().toString()
                ))
                .toList();
        return Mono.just(Map.of("code", 200, "data", list, "total", list.size()));
    }
}
