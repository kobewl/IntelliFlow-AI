package com.kobeai.hub.service.impl;

import com.kobeai.hub.model.PromptTemplate;
import com.kobeai.hub.repository.PromptTemplateRepository;
import com.kobeai.hub.service.PromptOptimizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Slf4j
@Service
public class PromptOptimizationServiceImpl implements PromptOptimizationService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromptTemplateRepository templateRepository;

    private static final Map<String, Integer> TOKEN_WEIGHTS = new HashMap<>();

    // 定义关键词映射
    private static final Map<String, String> KEYWORD_TYPE_MAPPING = new HashMap<>();

    static {
        // 初始化不同类型内容的 token 权重
        TOKEN_WEIGHTS.put("code", 2); // 代码片段权重更高
        TOKEN_WEIGHTS.put("url", 1); // URL 权重正常
        TOKEN_WEIGHTS.put("number", 1); // 数字权重正常
        TOKEN_WEIGHTS.put("text", 1); // 普通文本权重正常

        // 初始化关键词到模板类型的映射
        KEYWORD_TYPE_MAPPING.put("代码生成", "code_generation");
        KEYWORD_TYPE_MAPPING.put("优化代码", "code_optimization");
        KEYWORD_TYPE_MAPPING.put("重构代码", "code_optimization");
        KEYWORD_TYPE_MAPPING.put("总结", "text_summary");
        KEYWORD_TYPE_MAPPING.put("概括", "text_summary");
        KEYWORD_TYPE_MAPPING.put("问题", "problem_solving");
        KEYWORD_TYPE_MAPPING.put("如何解决", "problem_solving");
        KEYWORD_TYPE_MAPPING.put("API", "api_doc");
        KEYWORD_TYPE_MAPPING.put("接口文档", "api_doc");
        KEYWORD_TYPE_MAPPING.put("数据分析", "data_analysis");
        KEYWORD_TYPE_MAPPING.put("分析数据", "data_analysis");
        KEYWORD_TYPE_MAPPING.put("测试用例", "test_case");
        KEYWORD_TYPE_MAPPING.put("单元测试", "test_case");
        KEYWORD_TYPE_MAPPING.put("代码审查", "code_review");
        KEYWORD_TYPE_MAPPING.put("review", "code_review");
        KEYWORD_TYPE_MAPPING.put("SQL优化", "sql_optimization");
        KEYWORD_TYPE_MAPPING.put("数据库优化", "sql_optimization");
    }

    @Override
    public String optimizePrompt(String content, PromptTemplate template, Map<String, Object> variables) {
        try {
            // 1. 应用模板
            String optimizedContent = applyTemplate(content, template, variables);

            // 2. 压缩内容
            optimizedContent = compressContent(optimizedContent, template.getCompressionStrategy());

            // 3. 检查 Token 数量
            int tokens = estimateTokens(optimizedContent);
            log.info("优化后的 Prompt Token 数量: {}", tokens);

            return optimizedContent;
        } catch (Exception e) {
            log.error("优化 Prompt 失败: {}", e.getMessage());
            return content; // 如果优化失败，返回原始内容
        }
    }

    @Override
    public String compressContext(String context, int maxTokens) {
        try {
            // 1. 分析上下文结构
            Map<String, Object> contextStructure = analyzeContext(context);

            // 2. 根据重要性排序
            List<String> sortedSegments = sortByImportance(contextStructure);

            // 3. 压缩内容直到满足 token 限制
            StringBuilder compressedContext = new StringBuilder();
            int currentTokens = 0;

            for (String segment : sortedSegments) {
                int segmentTokens = estimateTokens(segment);
                if (currentTokens + segmentTokens <= maxTokens) {
                    compressedContext.append(segment).append("\n");
                    currentTokens += segmentTokens;
                } else {
                    // 尝试部分压缩
                    String compressedSegment = compressSegment(segment, maxTokens - currentTokens);
                    if (compressedSegment != null) {
                        compressedContext.append(compressedSegment);
                    }
                    break;
                }
            }

            return compressedContext.toString();
        } catch (Exception e) {
            log.error("压缩上下文失败: {}", e.getMessage());
            return context;
        }
    }

    @Override
    public int estimateTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        // 1. 分词
        String[] words = content.split("\\s+");
        int totalTokens = 0;

        // 2. 根据不同类型计算 token
        for (String word : words) {
            if (isCode(word)) {
                totalTokens += word.length() * TOKEN_WEIGHTS.get("code");
            } else if (isUrl(word)) {
                totalTokens += TOKEN_WEIGHTS.get("url");
            } else if (isNumber(word)) {
                totalTokens += TOKEN_WEIGHTS.get("number");
            } else {
                totalTokens += word.length() * TOKEN_WEIGHTS.get("text");
            }
        }

        return totalTokens;
    }

    @Override
    public PromptTemplate findBestTemplate(String type, String content) {
        try {
            // 1. 首先尝试根据提供的类型直接查找
            List<PromptTemplate> templates = templateRepository.findByType(type);

            // 2. 如果没有找到，尝试通过内容关键词匹配
            if (templates.isEmpty()) {
                String matchedType = findTypeByContent(content);
                if (matchedType != null) {
                    templates = templateRepository.findByType(matchedType);
                }
            }

            // 3. 如果还是没有找到，使用通用对话模板
            if (templates.isEmpty()) {
                return templateRepository.findByTypeAndName("chat", "general_chat");
            }

            // 4. 如果找到多个模板，选择最合适的一个
            return selectBestTemplate(templates, content);

        } catch (Exception e) {
            log.error("查找最佳模板失败: {}", e.getMessage());
            // 出错时返回通用对话模板
            return templateRepository.findByTypeAndName("chat", "general_chat");
        }
    }

    private String findTypeByContent(String content) {
        // 将内容转换为小写进行匹配
        String lowerContent = content.toLowerCase();

        // 遍历关键词映射
        for (Map.Entry<String, String> entry : KEYWORD_TYPE_MAPPING.entrySet()) {
            if (lowerContent.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        // 检查是否包含代码特征
        if (containsCodeFeatures(content)) {
            return "code";
        }

        return null;
    }

    private boolean containsCodeFeatures(String content) {
        // 检查常见的代码特征
        return content.contains("{") ||
                content.contains("}") ||
                content.contains("function") ||
                content.contains("class") ||
                content.contains("public") ||
                content.contains("private") ||
                content.contains("SELECT") ||
                content.contains("INSERT") ||
                content.contains("UPDATE") ||
                content.contains("DELETE");
    }

    private PromptTemplate selectBestTemplate(List<PromptTemplate> templates, String content) {
        if (templates.size() == 1) {
            return templates.get(0);
        }

        // 计算每个模板的匹配分数
        Map<PromptTemplate, Integer> scores = new HashMap<>();
        for (PromptTemplate template : templates) {
            int score = calculateTemplateScore(template, content);
            scores.put(template, score);
        }

        // 返回得分最高的模板
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(templates.get(0));
    }

    private int calculateTemplateScore(PromptTemplate template, String content) {
        int score = 0;

        // 1. 基于 token 数量的匹配度
        int contentTokens = estimateTokens(content);
        int templateTokens = template.getEstimatedTokens();
        score += Math.max(0, 100 - Math.abs(contentTokens - templateTokens));

        // 2. 基于内容特征的匹配度
        if (containsCodeFeatures(content) && template.getType().contains("code")) {
            score += 50;
        }

        // 3. 基于压缩策略的匹配度
        if ("semantic".equals(template.getCompressionStrategy()) &&
                !containsCodeFeatures(content)) {
            score += 30;
        }

        if ("keyword".equals(template.getCompressionStrategy()) &&
                containsCodeFeatures(content)) {
            score += 30;
        }

        return score;
    }

    // 私有辅助方法
    private String applyTemplate(String content, PromptTemplate template, Map<String, Object> variables) {
        String result = template.getContent();

        // 替换模板变量
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        // 插入原始内容
        result = result.replace("{{content}}", content);

        return result;
    }

    private String compressContent(String content, String strategy) {
        switch (strategy) {
            case "keyword":
                return compressWithKeywords(content);
            case "semantic":
                return compressWithSemantic(content);
            case "redundancy":
                return removeRedundancy(content);
            default:
                return content;
        }
    }

    private Map<String, Object> analyzeContext(String context) {
        Map<String, Object> structure = new HashMap<>();

        // 分析代码块
        Pattern codePattern = Pattern.compile("```.*?```", Pattern.DOTALL);
        Matcher codeMatcher = codePattern.matcher(context);
        List<String> codeBlocks = new ArrayList<>();
        while (codeMatcher.find()) {
            codeBlocks.add(codeMatcher.group());
        }
        structure.put("code_blocks", codeBlocks);

        // 分析普通文本
        String[] paragraphs = context.split("\n\n");
        structure.put("paragraphs", paragraphs);

        return structure;
    }

    private List<String> sortByImportance(Map<String, Object> structure) {
        List<String> sorted = new ArrayList<>();

        // 代码块优先
        @SuppressWarnings("unchecked")
        List<String> codeBlocks = (List<String>) structure.get("code_blocks");
        if (codeBlocks != null) {
            sorted.addAll(codeBlocks);
        }

        // 其次是段落
        String[] paragraphs = (String[]) structure.get("paragraphs");
        if (paragraphs != null) {
            sorted.addAll(Arrays.asList(paragraphs));
        }

        return sorted;
    }

    private String compressSegment(String segment, int maxTokens) {
        // 如果是代码块，保持完整
        if (segment.startsWith("```") && segment.endsWith("```")) {
            return null;
        }

        // 普通文本尝试截断
        String[] words = segment.split("\\s+");
        StringBuilder compressed = new StringBuilder();
        int currentTokens = 0;

        for (String word : words) {
            int wordTokens = estimateTokens(word);
            if (currentTokens + wordTokens <= maxTokens) {
                compressed.append(word).append(" ");
                currentTokens += wordTokens;
            } else {
                break;
            }
        }

        return compressed.toString().trim();
    }

    private boolean isCode(String word) {
        return word.contains("{") || word.contains("}") || word.contains("(") ||
                word.contains(")") || word.contains(";") || word.startsWith("//");
    }

    private boolean isUrl(String word) {
        return word.startsWith("http://") || word.startsWith("https://");
    }

    private boolean isNumber(String word) {
        return word.matches("\\d+");
    }

    private String compressWithKeywords(String content) {
        // 保留关键词的压缩策略
        return content.replaceAll("\\s+", " ") // 合并空白字符
                .replaceAll("\\b(the|a|an)\\b", "") // 移除冠词
                .replaceAll("\\s+", " ") // 再次合并空白字符
                .trim();
    }

    private String compressWithSemantic(String content) {
        // 语义压缩（保持核心含义）
        return content.replaceAll("(?i)\\b(please|kindly|would you|could you)\\b", "") // 移除礼貌用语
                .replaceAll("(?i)\\b(I think|I believe|In my opinion)\\b", "") // 移除主观表达
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String removeRedundancy(String content) {
        // 移除重复内容
        String[] sentences = content.split("\\. ");
        Set<String> uniqueSentences = new LinkedHashSet<>(Arrays.asList(sentences));
        return String.join(". ", uniqueSentences);
    }
}