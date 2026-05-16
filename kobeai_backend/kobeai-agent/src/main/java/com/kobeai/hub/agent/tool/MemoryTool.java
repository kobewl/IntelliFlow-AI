package com.kobeai.hub.agent.tool;

import com.kobeai.hub.agent.memory.LocalFileLongTermMemory;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 长期记忆工具 — 让 Agent 能够主动存取长期记忆。
 *
 * Agent 可以在对话中主动调用这些方法来：
 * - 记住用户的重要偏好
 * - 存储关键决策和结论
 * - 搜索历史记忆
 */
public class MemoryTool {

    private final LocalFileLongTermMemory memory;

    public MemoryTool(LocalFileLongTermMemory memory) {
        this.memory = memory;
    }

    @Tool(name = "remember_fact", description = "记住一个重要的事实或偏好。当你发现用户的重要信息、偏好或决策时，主动调用此工具存储。")
    public String rememberFact(
            @ToolParam(name = "title", description = "事实标题，简洁概括内容") String title,
            @ToolParam(name = "content", description = "事实详细内容") String content,
            @ToolParam(name = "tags", description = "标签，用逗号分隔，例如 '偏好,模型选择'") String tags) {
        if (title == null || title.isBlank()) return "标题不能为空";
        if (content == null || content.isBlank()) return "内容不能为空";

        String[] tagArray = (tags != null && !tags.isBlank())
                ? tags.split("[,，]")
                : new String[0];
        // 去除每个标签的前后空白
        for (int i = 0; i < tagArray.length; i++) {
            tagArray[i] = tagArray[i].trim();
        }
        memory.saveFact(title.trim(), content.trim(), tagArray);
        return "已记住: " + title.trim();
    }

    @Tool(name = "update_core_memory", description = "更新核心长期记忆的某个章节。用于存储用户偏好、项目上下文、重要决策等。")
    public String updateCoreMemory(
            @ToolParam(name = "section", description = "章节名称，例如 '用户偏好'、'项目上下文'、'重要决策'") String section,
            @ToolParam(name = "content", description = "要记录的内容，使用 Markdown 格式") String content) {
        if (section == null || section.isBlank()) return "章节名称不能为空";
        if (content == null || content.isBlank()) return "内容不能为空";

        memory.updateCoreMemory(section.trim(), content.trim());
        return "核心记忆已更新 — " + section.trim();
    }

    @Tool(name = "recall_memory", description = "查看核心长期记忆的内容。当用户问'你记得什么'或需要回顾之前的上下文时使用。")
    public String recallMemory() {
        String coreMemory = memory.getCoreMemory();
        if (coreMemory.isEmpty()) {
            return "暂无长期记忆。随着对话的进行，我会记住重要的信息。";
        }
        // 去掉 frontmatter，只返回正文
        return coreMemory.replaceFirst("(?s)^---\\s*\\n.*?\\n---\\s*\\n", "").trim();
    }
}
