package com.kobeai.hub.agent.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AgentService {

    private final ModelRouter modelRouter;
    private final RagService ragService;
    private final Toolkit sharedToolkit;
    private final Map<String, ReActAgent> sessionAgents = new ConcurrentHashMap<>();

    public AgentService(ModelRouter modelRouter, RagService ragService) {
        this.modelRouter = modelRouter;
        this.ragService = ragService;
        this.sharedToolkit = new Toolkit();
    }

    public ReActAgent createAgent(String sessionId, String modelName) {
        Model model = modelRouter.resolve(modelName);

        ReActAgent agent = ReActAgent.builder()
                .name("IntelliFlow-AI")
                .sysPrompt("""
                        你是 IntelliFlow-AI，一个智能助手。
                        - 使用中文回答用户问题
                        - 回答简洁、准确、有逻辑
                        """)
                .model(model)
                .toolkit(sharedToolkit)
                .maxIters(10)
                .build();

        sessionAgents.put(sessionId, agent);
        log.info("Agent 已创建: sessionId={}, model={}", sessionId, modelName);
        return agent;
    }

    public Flux<Event> streamCall(String sessionId, String userMessage, String modelName) {
        String routedModel = modelRouter.routeByTask(userMessage);
        ReActAgent agent = createAgent(sessionId, routedModel);

        Msg msg = Msg.builder()
                .name("user")
                .textContent(userMessage)
                .build();

        StreamOptions options = StreamOptions.builder()
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT, EventType.AGENT_RESULT)
                .incremental(true)
                .build();

        return agent.stream(List.of(msg), options)
                .doOnComplete(() -> {
                    sessionAgents.remove(sessionId);
                    log.info("Agent 会话完成: sessionId={}", sessionId);
                })
                .doOnError(e -> {
                    sessionAgents.remove(sessionId);
                    log.error("Agent 会话出错: sessionId={}", sessionId, e);
                });
    }

    public Flux<Event> streamCallWithRag(String sessionId, String userMessage,
                                          String modelName, String kbId) {
        List<String> docs = ragService.retrieve(userMessage, kbId);
        String enhancedMessage = userMessage;
        if (docs != null && !docs.isEmpty()) {
            enhancedMessage = "参考以下知识库内容：\n"
                    + String.join("\n---\n", docs)
                    + "\n\n用户问题：" + userMessage;
        }
        return streamCall(sessionId, enhancedMessage, modelName);
    }

    public ReActAgent getSessionAgent(String sessionId) {
        return sessionAgents.get(sessionId);
    }

    public void closeSession(String sessionId) {
        ReActAgent agent = sessionAgents.remove(sessionId);
        if (agent != null) {
            agent.interrupt();
            log.info("Agent 会话已关闭: sessionId={}", sessionId);
        }
    }
}
