package com.kobeai.hub.agent.tool;

import com.kobeai.hub.agent.service.RagService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.util.List;

public class KnowledgeTool {

    private final RagService ragService;

    public KnowledgeTool(RagService ragService) {
        this.ragService = ragService;
    }

    @Tool(name = "search_knowledge_base", description = "在知识库中搜索相关文档，返回匹配的内容")
    public String searchKnowledgeBase(
            @ToolParam(name = "query", description = "搜索关键词或问题") String query,
            @ToolParam(name = "kb_id", description = "知识库 ID，默认为 default") String kbId) {
        String kb = kbId != null ? kbId : "default";
        if (!ragService.hasKnowledgeBase(kb)) {
            return "知识库 [" + kb + "] 不存在或未初始化，请检查配置。";
        }
        List<String> results = ragService.retrieve(query, kb);
        if (results.isEmpty()) {
            return "在知识库中未找到与 \"" + query + "\" 相关的内容。";
        }
        return "检索到 " + results.size() + " 条相关文档:\n" + String.join("\n---\n", results);
    }

    @Tool(name = "list_knowledge_bases", description = "列出当前可用的所有知识库")
    public String listKnowledgeBases() {
        if (!ragService.hasKnowledgeBase("default")) {
            return "当前没有可用的知识库，请先配置 DashScope API Key。";
        }
        return "可用知识库: [default]";
    }
}
