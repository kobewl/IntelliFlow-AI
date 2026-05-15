package com.kobeai.hub.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内容分析器
 * 用于分析内容结构、识别编程语言等
 */
@Slf4j
@Component
public class ContentAnalyzer {
    // 代码特征正则表达式
    private static final Map<String, Pattern> CODE_PATTERNS = new HashMap<>();
    static {
        CODE_PATTERNS.put("java", Pattern.compile("(class|interface|enum)\\s+\\w+"));
        CODE_PATTERNS.put("python", Pattern.compile("(def|class)\\s+\\w+"));
        CODE_PATTERNS.put("javascript", Pattern.compile("(function|class|const|let|var)\\s+\\w+"));
        CODE_PATTERNS.put("sql", Pattern.compile("SELECT|INSERT|UPDATE|DELETE", Pattern.CASE_INSENSITIVE));
    }

    /**
     * 分析内容结构
     * 
     * @param content 待分析内容
     * @return 结构分析结果
     */
    public Map<String, Object> analyzeStructure(String content) {
        Map<String, Object> structure = new HashMap<>();

        try {
            // 1. 提取代码块
            List<String> codeBlocks = extractCodeBlocks(content);
            structure.put("codeBlocks", codeBlocks);

            // 2. 分析段落
            List<String> paragraphs = Arrays.asList(content.split("\n\n"));
            structure.put("paragraphs", paragraphs);

            // 3. 识别编程语言
            Map<String, Double> languageProbs = detectProgrammingLanguage(content);
            structure.put("languages", languageProbs);

            // 4. 分析文本结构
            analyzeTextStructure(content, structure);

            log.debug("Content analysis completed - codeBlocks: {}, paragraphs: {}, languages: {}",
                    codeBlocks.size(), paragraphs.size(), languageProbs);

        } catch (Exception e) {
            log.error("Error analyzing content structure", e);
            structure.put("error", e.getMessage());
        }

        return structure;
    }

    /**
     * 提取代码块
     */
    private List<String> extractCodeBlocks(String content) {
        List<String> codeBlocks = new ArrayList<>();
        Pattern pattern = Pattern.compile("```[\\s\\S]*?```");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            codeBlocks.add(matcher.group());
        }

        return codeBlocks;
    }

    /**
     * 检测编程语言
     */
    private Map<String, Double> detectProgrammingLanguage(String content) {
        Map<String, Double> probabilities = new HashMap<>();

        for (Map.Entry<String, Pattern> entry : CODE_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(content);
            int matches = 0;
            while (matcher.find()) {
                matches++;
            }
            double probability = matches / 10.0; // 简单概率计算
            probabilities.put(entry.getKey(), Math.min(1.0, probability));
        }

        return probabilities;
    }

    /**
     * 分析文本结构
     */
    private void analyzeTextStructure(String content, Map<String, Object> structure) {
        // 1. 检测标题
        List<String> headers = new ArrayList<>();
        Pattern headerPattern = Pattern.compile("^#{1,6}\\s.+$", Pattern.MULTILINE);
        Matcher headerMatcher = headerPattern.matcher(content);
        while (headerMatcher.find()) {
            headers.add(headerMatcher.group());
        }
        structure.put("headers", headers);

        // 2. 检测列表
        List<String> lists = new ArrayList<>();
        Pattern listPattern = Pattern.compile("^[\\s]*[-*+]\\s.+$", Pattern.MULTILINE);
        Matcher listMatcher = listPattern.matcher(content);
        while (listMatcher.find()) {
            lists.add(listMatcher.group());
        }
        structure.put("lists", lists);

        // 3. 检测链接
        List<String> links = new ArrayList<>();
        Pattern linkPattern = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
        Matcher linkMatcher = linkPattern.matcher(content);
        while (linkMatcher.find()) {
            links.add(linkMatcher.group());
        }
        structure.put("links", links);
    }
}