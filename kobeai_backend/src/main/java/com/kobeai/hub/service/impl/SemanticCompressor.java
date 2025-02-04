package com.kobeai.hub.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 语义压缩器
 * 用于压缩文本内容，保持核心语义
 */
@Slf4j
@Component
public class SemanticCompressor {

    private static final double KEYWORD_WEIGHT = 0.5;
    private static final double POSITION_WEIGHT = 0.3;
    private static final double LENGTH_WEIGHT = 0.2;

    /**
     * 压缩内容
     * 
     * @param content   原始内容
     * @param maxTokens 最大token数
     * @return 压缩后的内容
     */
    public String compress(String content, int maxTokens) {
        try {
            // 1. 分句
            List<String> sentences = splitIntoSentences(content);

            // 2. 计算句子重要性
            Map<String, Double> importance = calculateImportance(sentences);

            // 3. 选择最重要的句子
            List<String> selectedSentences = selectTopSentences(sentences, importance, maxTokens);

            // 4. 重组内容
            String result = String.join(" ", selectedSentences);

            log.debug("Content compressed - original sentences: {}, selected: {}, compression ratio: {}",
                    sentences.size(), selectedSentences.size(),
                    (double) selectedSentences.size() / sentences.size());

            return result;
        } catch (Exception e) {
            log.error("Error compressing content", e);
            return content;
        }
    }

    /**
     * 分句
     */
    private List<String> splitIntoSentences(String content) {
        List<String> sentences = new ArrayList<>();
        // 使用正则表达式分句，考虑中英文标点
        String[] parts = content.split("[.。!！?？;；]+\\s*");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                sentences.add(part.trim());
            }
        }
        return sentences;
    }

    /**
     * 计算句子重要性
     */
    private Map<String, Double> calculateImportance(List<String> sentences) {
        Map<String, Double> importance = new HashMap<>();

        // 获取关键词频率表
        Map<String, Integer> keywordFrequencies = buildKeywordFrequencies(sentences);

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);

            // 计算特征词频率
            double keywordFrequency = calculateKeywordFrequency(sentence, keywordFrequencies);
            // 计算句子位置权重
            double positionWeight = calculatePositionWeight(i, sentences.size());
            // 计算句子长度权重
            double lengthWeight = calculateLengthWeight(sentence);

            // 综合评分
            double score = keywordFrequency * KEYWORD_WEIGHT +
                    positionWeight * POSITION_WEIGHT +
                    lengthWeight * LENGTH_WEIGHT;

            importance.put(sentence, score);
        }

        return importance;
    }

    /**
     * 构建关键词频率表
     */
    private Map<String, Integer> buildKeywordFrequencies(List<String> sentences) {
        Map<String, Integer> frequencies = new HashMap<>();
        Pattern wordPattern = Pattern.compile("\\b\\w+\\b");

        for (String sentence : sentences) {
            String[] words = wordPattern.split(sentence.toLowerCase());
            for (String word : words) {
                if (!isStopWord(word)) {
                    frequencies.merge(word, 1, Integer::sum);
                }
            }
        }

        return frequencies;
    }

    /**
     * 计算关键词频率
     */
    private double calculateKeywordFrequency(String sentence, Map<String, Integer> keywordFrequencies) {
        String[] words = sentence.toLowerCase().split("\\s+");
        double totalFrequency = 0;

        for (String word : words) {
            if (!isStopWord(word)) {
                totalFrequency += keywordFrequencies.getOrDefault(word, 0);
            }
        }

        return totalFrequency / words.length;
    }

    /**
     * 计算位置权重
     */
    private double calculatePositionWeight(int position, int total) {
        if (position == 0 || position == total - 1) {
            return 1.0; // 首尾句子权重最高
        }
        return 0.5; // 其他句子权重较低
    }

    /**
     * 计算长度权重
     */
    private double calculateLengthWeight(String sentence) {
        int length = sentence.length();
        if (length < 10) {
            return 0.3; // 过短的句子权重低
        } else if (length > 100) {
            return 0.7; // 过长的句子权重适中
        }
        return 1.0; // 适中长度的句子权重高
    }

    /**
     * 选择最重要的句子
     */
    private List<String> selectTopSentences(List<String> sentences,
            Map<String, Double> importance,
            int maxTokens) {
        // 按重要性排序
        List<String> sortedSentences = new ArrayList<>(sentences);
        sortedSentences.sort((s1, s2) -> Double.compare(importance.get(s2), importance.get(s1)));

        List<String> selected = new ArrayList<>();
        int currentTokens = 0;

        for (String sentence : sortedSentences) {
            int sentenceTokens = estimateTokens(sentence);
            if (currentTokens + sentenceTokens <= maxTokens) {
                selected.add(sentence);
                currentTokens += sentenceTokens;
            } else {
                break;
            }
        }

        // 恢复原始顺序
        selected.sort(Comparator.comparingInt(sentences::indexOf));

        return selected;
    }

    /**
     * 估算token数量
     */
    private int estimateTokens(String text) {
        return text.split("\\s+").length;
    }

    /**
     * 判断是否为停用词
     */
    private boolean isStopWord(String word) {
        // TODO: 实现停用词判断
        return word.length() <= 2;
    }
}