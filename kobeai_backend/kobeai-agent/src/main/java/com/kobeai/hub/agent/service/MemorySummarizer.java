package com.kobeai.hub.agent.service;

import com.kobeai.hub.agent.memory.LocalFileLongTermMemory;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 记忆自动摘要服务。
 *
 * 定时（每天凌晨 2 点）或会话关闭时，用 LLM 对当日对话做摘要，
 * 提取关键事实、用户偏好和重要决策，写入 MEMORY.md。
 */
@Slf4j
@Service
public class MemorySummarizer {

    private static final String SUMMARY_PROMPT = """
            你是一个专业的对话摘要助手。请分析以下对话记录，提取关键信息并分类整理：
            1. **关键事实** — 用户提到的个人信息、项目细节、技术栈等
            2. **用户偏好** — 用户表现出的喜好、习惯、风格偏好
            3. **重要决策** — 对话中做出的决定、选择的方案
            4. **待办事项** — 用户提到需要后续处理的事项

            请用简洁的 Markdown 格式输出，按上述分类组织。
            如果对话内容较少或没有重要信息，可以简短说明"本次对话无重要信息"。
            只输出摘要内容，不要解释你的分析过程。
            """;

    private final ModelRouter modelRouter;
    private final LocalFileLongTermMemory longTermMemory;
    private final Path sessionsDir;

    public MemorySummarizer(ModelRouter modelRouter, LocalFileLongTermMemory longTermMemory) {
        this.modelRouter = modelRouter;
        this.longTermMemory = longTermMemory;
        this.sessionsDir = longTermMemory.getMemoryDir().resolve("sessions");
        log.info("记忆摘要服务已启动 (cron=每天 02:00, 目录={})", sessionsDir.toAbsolutePath());
    }

    /**
     * 定时任务：每天凌晨 2 点自动摘要
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledSummary() {
        log.info("定时任务: 开始每日记忆摘要");
        int count = 0;
        List<Path> unsummarized = findUnsummarizedFiles();
        for (Path file : unsummarized) {
            if (summarizeSessionFile(file)) count++;
        }
        log.info("定时任务: 记忆摘要完成, 处理 {}/{} 个文件", count, unsummarized.size());
    }

    /**
     * 会话关闭时触发摘要
     */
    public void summarizeOnClose() {
        String today = LocalDate.now().toString();
        Path todayFile = sessionsDir.resolve(today + ".md");
        if (Files.exists(todayFile) && !isSummarized(todayFile)) {
            summarizeSessionFile(todayFile);
        }
    }

    // ==================== 内部方法 ====================

    private List<Path> findUnsummarizedFiles() {
        List<Path> recent = longTermMemory.getRecentSessionFiles(30);
        return recent.stream()
                .filter(f -> !isSummarized(f))
                .collect(Collectors.toList());
    }

    private boolean isSummarized(Path sessionFile) {
        return Files.exists(getMarkerFile(sessionFile));
    }

    private Path getMarkerFile(Path sessionFile) {
        return Path.of(sessionFile.toAbsolutePath() + ".summarized");
    }

    private void markSummarized(Path sessionFile) {
        try {
            Files.writeString(getMarkerFile(sessionFile),
                    LocalDate.now().toString(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.warn("写入摘要标记失败: {}", e.getMessage());
        }
    }

    /**
     * 对单个会话文件执行 LLM 摘要
     * @return true 如果摘要成功
     */
    boolean summarizeSessionFile(Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (content.isBlank()) {
                markSummarized(file);
                return false;
            }

            String dateStr = file.getFileName().toString().replace(".md", "");
            Model model = modelRouter.resolve("deepseek-v4-flash");

            List<Msg> messages = List.of(
                    Msg.builder().name("system").textContent(SUMMARY_PROMPT).build(),
                    Msg.builder().name("user")
                            .textContent("请摘要以下对话记录（" + dateStr + "）：\n\n" + content)
                            .build()
            );

            Flux<ChatResponse> flux = model.stream(
                    messages,
                    List.of(),
                    GenerateOptions.builder().stream(false).build()
            );

            String summary = flux
                    .map(ChatResponse::getContent)
                    .map(blocks -> blocks.stream()
                            .filter(b -> b instanceof TextBlock)
                            .map(b -> ((TextBlock) b).getText())
                            .collect(Collectors.joining()))
                    .collectList()
                    .block()
                    .stream()
                    .collect(Collectors.joining());

            if (summary == null || summary.isBlank()) {
                log.warn("摘要为空: {}", file.getFileName());
                markSummarized(file);
                return false;
            }

            String sectionName = dateStr + " 对话摘要";
            longTermMemory.updateCoreMemory(sectionName, summary.trim());
            markSummarized(file);
            log.info("记忆摘要完成: {} → MEMORY.md ({} 字符)", file.getFileName(), summary.length());
            return true;

        } catch (IOException e) {
            log.error("读取会话文件失败: {}", file, e);
        } catch (Exception e) {
            log.error("摘要生成失败: {} — {}", file, e.getMessage());
        }
        return false;
    }
}
