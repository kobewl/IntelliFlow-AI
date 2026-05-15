package com.kobeai.hub.agent.controller;

import com.kobeai.hub.agent.service.AgentService;
import com.kobeai.hub.agent.service.ModelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final ModelRouter modelRouter;

    /**
     * 流式对话接口 (AgentScope Flux SSE)
     *
     * 对比旧版 ChatController 的 SseEmitter:
     * - Flux 是响应式的, 不阻塞线程
     * - 支持背压控制
     * - 事件类型更丰富: reasoning / tool_result / agent_result
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(
            @RequestParam String message,
            @RequestParam(defaultValue = "deepseek-chat") String model,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        String sid = sessionId != null ? sessionId : UUID.randomUUID().toString();
        log.info("Agent 流式对话: sessionId={}, model={}, message={}", sid, model, message);

        return agentService.streamCall(sid, message, model)
                .map(event -> ServerSentEvent.<String>builder()
                        .id(UUID.randomUUID().toString())
                        .event(event.getType().name().toLowerCase())
                        .data(event.getMessage() != null
                                ? event.getMessage().getTextContent()
                                : "")
                        .build())
                .onErrorResume(e -> {
                    log.error("Agent 流式对话出错: sessionId={}", sid, e);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("对话出错: " + e.getMessage())
                            .build());
                });
    }

    /**
     * 带知识库增强的流式对话
     */
    @GetMapping(value = "/chat/rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatWithRag(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String kbId,
            @RequestParam(defaultValue = "deepseek-chat") String model,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        String sid = sessionId != null ? sessionId : UUID.randomUUID().toString();
        log.info("Agent RAG流式对话: sessionId={}, kbId={}, model={}", sid, kbId, model);

        return agentService.streamCallWithRag(sid, message, model, kbId)
                .map(event -> ServerSentEvent.<String>builder()
                        .id(UUID.randomUUID().toString())
                        .event(event.getType().name().toLowerCase())
                        .data(event.getMessage() != null
                                ? event.getMessage().getTextContent()
                                : "")
                        .build())
                .onErrorResume(e -> {
                    log.error("Agent RAG流式对话出错: sessionId={}", sid, e);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("对话出错: " + e.getMessage())
                            .build());
                });
    }

    /**
     * 关闭会话
     */
    @DeleteMapping("/session/{sessionId}")
    public String closeSession(@PathVariable String sessionId) {
        agentService.closeSession(sessionId);
        return "会话已关闭: " + sessionId;
    }
}
