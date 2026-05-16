package com.kobeai.hub.agent.memory;

import io.agentscope.core.memory.LongTermMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于本地 Markdown 文件的长期记忆实现。
 *
 * 参考 OpenClaw 的记忆体系：
 * - sessions/YYYY-MM-DD.md  每日对话记录（append-only）
 * - MEMORY.md               核心长期记忆（偏好、决策、上下文）
 * - facts/*.md              提取的重要事实
 *
 * 文件格式采用 YAML frontmatter + Markdown 内容，人类和 AI 都可直接读写。
 */
@Slf4j
public class LocalFileLongTermMemory implements LongTermMemory {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final Pattern FRONTMATTER_PAT = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n", Pattern.DOTALL);

    private final Path memoryDir;
    private final Path sessionsDir;
    private final Path factsDir;
    private final Path coreMemoryFile;

    public LocalFileLongTermMemory(Path memoryDir) {
        this.memoryDir = memoryDir;
        this.sessionsDir = memoryDir.resolve("sessions");
        this.factsDir = memoryDir.resolve("facts");
        this.coreMemoryFile = memoryDir.resolve("MEMORY.md");
        try {
            Files.createDirectories(sessionsDir);
            Files.createDirectories(factsDir);
            log.info("长期记忆目录初始化完成: {}", memoryDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("无法创建记忆目录: {}", e.getMessage());
        }
    }

    // ==================== LongTermMemory 接口实现 ====================

    /**
     * 记录消息到长期记忆。
     * 框架在 Agent 完成回复后自动调用。
     */
    @Override
    public Mono<Void> record(List<Msg> msgs) {
        return Mono.fromRunnable(() -> {
            if (msgs == null || msgs.isEmpty()) return;
            try {
                appendToSessionFile(msgs);
            } catch (Exception e) {
                log.error("长期记忆记录失败", e);
            }
        });
    }

    /**
     * 检索相关记忆。
     * 框架在 Agent 推理前自动调用，返回的记忆内容会被注入到上下文中。
     */
    @Override
    public Mono<String> retrieve(Msg msg) {
        return Mono.fromCallable(() -> {
            String query = msg.getTextContent();
            if (query == null || query.isBlank()) return "";
            return searchRelevantMemories(query);
        });
    }

    // ==================== 公开方法（供工具和管理接口调用） ====================

    /**
     * 手动存储一条事实到 facts 目录
     */
    public void saveFact(String title, String content, String... tags) {
        try {
            String filename = sanitizeFilename(title) + ".md";
            Path factFile = factsDir.resolve(filename);
            String frontmatter = buildFrontmatter(Map.of(
                    "date", LocalDate.now().toString(),
                    "type", "fact",
                    "tags", Arrays.asList(tags)
            ));
            String markdown = frontmatter + "\n# " + title + "\n\n" + content + "\n";
            Files.writeString(factFile, markdown, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("事实已保存: {}", filename);
        } catch (IOException e) {
            log.error("保存事实失败: {}", e.getMessage());
        }
    }

    /**
     * 更新核心记忆 MEMORY.md
     */
    public void updateCoreMemory(String section, String content) {
        try {
            String existing = "";
            if (Files.exists(coreMemoryFile)) {
                existing = Files.readString(coreMemoryFile, StandardCharsets.UTF_8);
            }
            String now = LocalDate.now().toString();
            String newContent;

            if (existing.isEmpty()) {
                // 新建 MEMORY.md
                String frontmatter = buildFrontmatter(Map.of("updated", now));
                newContent = frontmatter + "\n"
                        + "# IntelliFlow-AI 长期记忆\n\n"
                        + "## " + section + "\n\n" + content + "\n";
            } else {
                // 追加或更新指定章节
                String body = stripFrontmatter(existing);
                String sectionHeader = "## " + section;
                if (body.contains(sectionHeader)) {
                    // 追加到已有章节
                    body = body.replace(sectionHeader, sectionHeader + "\n\n" + content);
                } else {
                    // 新增章节
                    body = body.trim() + "\n\n" + sectionHeader + "\n\n" + content + "\n";
                }
                String frontmatter = buildFrontmatter(Map.of("updated", now));
                newContent = frontmatter + "\n" + body;
            }

            Files.writeString(coreMemoryFile, newContent, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("核心记忆已更新: {} → {}", section,
                    content.length() > 60 ? content.substring(0, 60) + "..." : content);
        } catch (IOException e) {
            log.error("更新核心记忆失败: {}", e.getMessage());
        }
    }

    /**
     * 获取核心记忆内容
     */
    public String getCoreMemory() {
        try {
            if (Files.exists(coreMemoryFile)) {
                return Files.readString(coreMemoryFile, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("读取核心记忆失败: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 获取最近 N 天的会话文件列表
     */
    public List<Path> getRecentSessionFiles(int days) {
        List<Path> files = new ArrayList<>();
        LocalDate cutoff = LocalDate.now().minusDays(days);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionsDir, "*.md")) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                String dateStr = name.replace(".md", "");
                try {
                    LocalDate fileDate = LocalDate.parse(dateStr);
                    if (!fileDate.isBefore(cutoff)) {
                        files.add(p);
                    }
                } catch (Exception ignored) {
                    // 文件名不是日期格式，跳过
                }
            }
        } catch (IOException e) {
            log.error("读取会话文件列表失败: {}", e.getMessage());
        }
        files.sort(Comparator.reverseOrder());
        return files;
    }

    public Path getMemoryDir() {
        return memoryDir;
    }

    // ==================== 内部方法 ====================

    /**
     * 将消息列表追加到当日的会话文件
     */
    private void appendToSessionFile(List<Msg> msgs) {
        String today = LocalDate.now().toString();
        Path sessionFile = sessionsDir.resolve(today + ".md");
        String timestamp = LocalDateTime.now().format(TIME_FMT);

        StringBuilder block = new StringBuilder();
        boolean isNewFile = !Files.exists(sessionFile);

        if (isNewFile) {
            String frontmatter = buildFrontmatter(Map.of(
                    "date", today,
                    "created", LocalDateTime.now().toString()
            ));
            block.append(frontmatter).append("\n");
            block.append("# ").append(LocalDate.now().format(DATE_FMT)).append(" 对话记录\n\n");
        }

        block.append("---\n\n");
        block.append("## ").append(timestamp).append("\n\n");

        for (Msg msg : msgs) {
            String role = msg.getRole() != null ? formatRole(msg.getRole()) : "System";
            String text = msg.getTextContent();
            if (text == null || text.isBlank()) continue;

            if (msg.getRole() == MsgRole.TOOL) {
                block.append("> 🔧 **").append(role).append("**: ").append(truncate(text, 500)).append("\n\n");
            } else {
                block.append("**").append(role).append("**: ").append(text).append("\n\n");
            }
        }

        // 线程安全写入
        synchronized (getLockKey(sessionFile)) {
            try {
                Files.writeString(sessionFile, block.toString(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error("写入会话文件失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 搜索相关记忆
     */
    private String searchRelevantMemories(String query) {
        StringBuilder result = new StringBuilder();

        // 1. 始终包含核心记忆
        String coreMemory = getCoreMemory();
        if (!coreMemory.isEmpty()) {
            result.append(stripFrontmatter(coreMemory).trim()).append("\n\n");
        }

        // 2. 从最近的会话文件中搜索相关内容
        List<Path> recentFiles = getRecentSessionFiles(7);
        List<String> keywords = extractKeywords(query);

        if (!keywords.isEmpty() && !recentFiles.isEmpty()) {
            result.append("## 相关历史对话\n\n");
            int addedChars = 0;
            int maxChars = 3000;

            for (Path file : recentFiles) {
                if (addedChars >= maxChars) break;
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    String body = stripFrontmatter(content);

                    // 按段落分割，搜索匹配的段落
                    String[] sections = body.split("\\n---\\n");
                    for (String section : sections) {
                        if (addedChars >= maxChars) break;
                        int matches = countKeywordMatches(section, keywords);
                        if (matches > 0) {
                            String excerpt = truncate(section.trim(), 600);
                            result.append(excerpt).append("\n\n---\n\n");
                            addedChars += excerpt.length();
                        }
                    }
                } catch (IOException e) {
                    log.debug("搜索文件失败: {}", file, e);
                }
            }
        }

        return result.toString().trim();
    }

    // ==================== 工具方法 ====================

    private String formatRole(MsgRole role) {
        return switch (role) {
            case USER -> "用户";
            case ASSISTANT -> "Agent";
            case SYSTEM -> "系统";
            case TOOL -> "工具";
        };
    }

    /**
     * 从查询文本中提取关键词
     */
    private List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return List.of();

        // 去掉标点，按空白和常见分隔符分词
        String cleaned = text.replaceAll("[，。！？、；：\"\"''（）\\[\\]【】\\s]+", " ")
                .replaceAll("[,.!?;:'\"()\\[\\]{}]+", " ")
                .trim();

        if (cleaned.isEmpty()) return List.of(text);

        return Arrays.stream(cleaned.split(" "))
                .map(String::trim)
                .filter(w -> w.length() >= 2)
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 统计段落中匹配的关键词数量
     */
    private int countKeywordMatches(String text, List<String> keywords) {
        String lower = text.toLowerCase();
        int count = 0;
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 构建 YAML frontmatter
     */
    private String buildFrontmatter(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder("---\n");
        for (var entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List<?> list) {
                sb.append(entry.getKey()).append(": [");
                sb.append(list.stream()
                        .map(v -> "\"" + v.toString().replace("\"", "\\\"") + "\"")
                        .collect(Collectors.joining(", ")));
                sb.append("]\n");
            } else if (value instanceof String str && str.contains(":")) {
                sb.append(entry.getKey()).append(": \"").append(str.replace("\"", "\\\"")).append("\"\n");
            } else {
                sb.append(entry.getKey()).append(": ").append(value).append("\n");
            }
        }
        sb.append("---\n");
        return sb.toString();
    }

    /**
     * 去掉 frontmatter，返回正文
     */
    private String stripFrontmatter(String content) {
        if (content == null) return "";
        Matcher m = FRONTMATTER_PAT.matcher(content);
        if (m.find()) {
            return content.substring(m.end());
        }
        return content;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
    }

    /**
     * 为文件路径生成锁键（用于同步写入）
     */
    private String getLockKey(Path path) {
        return ("memory-lock:" + path.toAbsolutePath()).intern();
    }
}
