package com.kobeai.hub.agent.service;

import com.kobeai.hub.agent.memory.LocalFileLongTermMemory;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AgentService {

    private static final String SYS_PROMPT = """
            你是 IntelliFlow AI，一个能力强大的智能助手。

            ## 你的能力
            - 获取当前时间、计算日期差
            - 执行复杂的数学计算（计算器）
            - 搜索知识库获取专业信息
            - 查看系统运行状态
            - 在沙盒中执行 Python / JavaScript / Shell 代码

            ## 代码沙盒使用说明
            - run_python: 执行 Python 代码，适用于数据处理、算法演示
            - run_javascript: 执行 JS 代码（Node.js），适用于前端逻辑测试
            - run_shell: 执行 Shell 命令（危险命令已过滤，超时 10 秒）
            - 代码输出限制 5000 字符，超大数据请用 print 分批输出

            ## 行为准则
            - 使用中文回答，语气友好专业
            - 回答问题前先思考，必要时使用工具
            - 如果你不确定答案，诚实告知并给出建议
            - 回答简洁精准，避免冗余
            """;

    private final ModelRouter modelRouter;
    private final RagService ragService;
    private final Toolkit toolkit;
    private final LocalFileLongTermMemory longTermMemory;
    private final MemorySummarizer memorySummarizer;
    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();

    public AgentService(ModelRouter modelRouter, RagService ragService,
                        Toolkit toolkit, LocalFileLongTermMemory longTermMemory,
                        MemorySummarizer memorySummarizer) {
        this.modelRouter = modelRouter;
        this.ragService = ragService;
        this.toolkit = toolkit;
        this.longTermMemory = longTermMemory;
        this.memorySummarizer = memorySummarizer;
    }

    private AgentSession getOrCreateSession(String sessionId, String modelName) {
        return sessions.computeIfAbsent(sessionId, sid -> {
            String resolvedModel = modelName != null ? modelName : "deepseek-v4-flash";
            Model model = modelRouter.resolve(resolvedModel);

            ReActAgent agent = ReActAgent.builder()
                    .name("IntelliFlow-AI")
                    .sysPrompt(SYS_PROMPT)
                    .model(model)
                    .toolkit(toolkit)
                    .memory(new InMemoryMemory())
                    .longTermMemory(longTermMemory)
                    .longTermMemoryMode(LongTermMemoryMode.STATIC_CONTROL)
                    .maxIters(10)
                    .build();

            AgentSession session = new AgentSession(agent, modelRouter, resolvedModel);
            log.info("Agent 会话已创建: sessionId={}, model={}, 已有 {} 个活跃会话",
                    sid, resolvedModel, sessions.size());
            return session;
        });
    }

    /**
     * AG-UI 协议事件流：包含 TOOL_CALL_START/TOOL_CALL_RESULT 等细粒度事件
     */
    public Flux<AguiEvent> streamCall(String sessionId, String userMessage, String modelName) {
        String effectiveModel = (modelName == null || modelName.isEmpty() || "auto".equals(modelName))
                ? modelRouter.routeByTask(userMessage)
                : modelName;

        AgentSession session = getOrCreateSession(sessionId, effectiveModel);

        RunAgentInput input = RunAgentInput.builder()
                .threadId(sessionId)
                .runId(UUID.randomUUID().toString())
                .messages(List.of(AguiMessage.userMessage("user", userMessage)))
                .build();

        return session.aguiAdapter.run(input)
                .doOnComplete(() -> log.info("Agent 回复完成: sessionId={}", sessionId))
                .doOnError(e -> log.error("Agent 会话出错: sessionId={}", sessionId, e));
    }

    public Flux<AguiEvent> streamCallWithRag(String sessionId, String userMessage,
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

    public MemoryStats getSessionMemory(String sessionId) {
        AgentSession session = sessions.get(sessionId);
        if (session == null) return null;
        var agent = session.agent;
        var memory = agent.getMemory();
        if (memory == null) return new MemoryStats(sessionId, 0, session.createdAt);
        return new MemoryStats(sessionId, memory.getMessages().size(), session.createdAt);
    }

    public void closeSession(String sessionId) {
        AgentSession session = sessions.remove(sessionId);
        if (session != null) {
            session.agent.interrupt();
            log.info("Agent 会话已关闭: sessionId={}, 剩余活跃会话: {}", sessionId, sessions.size());
        }
        memorySummarizer.summarizeOnClose();
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    // ---- 内部类 ----

    static class AgentSession {
        final ReActAgent agent;
        final AguiAgentAdapter aguiAdapter;
        final ModelRouter modelRouter;
        final String modelName;
        final LocalDateTime createdAt;

        AgentSession(ReActAgent agent, ModelRouter modelRouter, String modelName) {
            this.agent = agent;
            this.modelRouter = modelRouter;
            this.modelName = modelName;
            this.createdAt = LocalDateTime.now();
            this.aguiAdapter = new AguiAgentAdapter(agent, AguiAdapterConfig.builder()
                    .emitToolCallArgs(true)
                    .enableReasoning(true)
                    .build());
        }
    }

    public record MemoryStats(String sessionId, int messageCount, LocalDateTime createdAt) {}
}
