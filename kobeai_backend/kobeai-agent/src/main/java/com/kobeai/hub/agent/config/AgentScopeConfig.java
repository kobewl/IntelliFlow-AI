package com.kobeai.hub.agent.config;

import com.kobeai.hub.agent.memory.LocalFileLongTermMemory;
import com.kobeai.hub.agent.service.RagService;
import com.kobeai.hub.agent.tool.DateTimeTool;
import com.kobeai.hub.agent.tool.KnowledgeTool;
import com.kobeai.hub.agent.tool.MemoryTool;
import com.kobeai.hub.agent.tool.SandboxTool;
import com.kobeai.hub.agent.tool.SystemTool;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;
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

    @Value("${app.memory.dir:data/memory}")
    private String memoryDirPath;

    @Bean
    public Map<String, OpenAIChatModel> modelMap() {
        Map<String, OpenAIChatModel> models = new ConcurrentHashMap<>();

        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            // DeepSeek-V4 系列 (2026年4月发布)
            models.put("deepseek-v4-flash", OpenAIChatModel.builder()
                    .apiKey(deepseekApiKey)
                    .modelName("deepseek-v4-flash")
                    .baseUrl(deepseekBaseUrl + "/v1")
                    .build());
            models.put("deepseek-v4-pro", OpenAIChatModel.builder()
                    .apiKey(deepseekApiKey)
                    .modelName("deepseek-v4-pro")
                    .baseUrl(deepseekBaseUrl + "/v1")
                    .build());
            log.info("AgentScope: 已注册 DeepSeek-V4 模型 [deepseek-v4-flash, deepseek-v4-pro]");
        }

        if (doubaoApiKey != null && !doubaoApiKey.isEmpty()) {
            models.put("doubao-chat", OpenAIChatModel.builder()
                    .apiKey(doubaoApiKey)
                    .modelName("doubao-lite-32k")
                    .baseUrl(doubaoBaseUrl)
                    .build());
            log.info("AgentScope: 已注册豆包模型 [doubao-chat]");
        }

        return models;
    }

    @Bean
    public Toolkit agentToolkit(RagService ragService, LocalFileLongTermMemory longTermMemory) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new DateTimeTool());
        toolkit.registerTool(new SystemTool());
        toolkit.registerTool(new KnowledgeTool(ragService));
        toolkit.registerTool(new MemoryTool(longTermMemory));
        toolkit.registerTool(new SandboxTool());
        log.info("AgentScope: 已注册 {} 个自定义工具", 5);
        return toolkit;
    }

    @Bean
    public List<Map<String, Object>> toolInfoList() {
        return List.of(
                Map.of("name", "get_current_time", "description", "获取当前日期和时间"),
                Map.of("name", "calculate", "description", "执行数学计算，支持加减乘除、乘方、开方等"),
                Map.of("name", "date_diff", "description", "计算两个日期之间相差的天数"),
                Map.of("name", "get_system_info", "description", "获取系统运行状态"),
                Map.of("name", "search_knowledge_base", "description", "搜索知识库文档"),
                Map.of("name", "list_knowledge_bases", "description", "列出可用知识库"),
                Map.of("name", "remember_fact", "description", "记住重要的事实或偏好"),
                Map.of("name", "update_core_memory", "description", "更新核心长期记忆"),
                Map.of("name", "recall_memory", "description", "查看已存储的长期记忆"),
                Map.of("name", "run_python", "description", "在沙盒中执行 Python 代码"),
                Map.of("name", "run_javascript", "description", "在沙盒中执行 JavaScript 代码（Node.js）"),
                Map.of("name", "run_shell", "description", "在沙盒中执行 Shell 命令（危险命令已过滤）")
        );
    }

    @Bean
    public LocalFileLongTermMemory longTermMemory() {
        Path memoryPath = Path.of(memoryDirPath);
        log.info("AgentScope: 长期记忆目录 — {}", memoryPath.toAbsolutePath());
        return new LocalFileLongTermMemory(memoryPath);
    }
}
