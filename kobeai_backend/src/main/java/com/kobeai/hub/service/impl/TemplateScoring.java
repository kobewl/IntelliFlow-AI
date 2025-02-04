package com.kobeai.hub.service.impl;

import com.kobeai.hub.model.PromptTemplate;
import com.kobeai.hub.constant.TemplateConstants.Score;
import com.kobeai.hub.constant.TemplateConstants.Patterns;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 模板评分系统
 * 用于计算模板与内容的匹配度，选择最佳模板
 */
@Slf4j
@Component
public class TemplateScoring {

    /**
     * 计算模板得分
     * 
     * @param template 模板
     * @param content  内容
     * @return 匹配得分
     */
    public int calculateTemplateScore(PromptTemplate template, String content) {
        int score = 0;

        // 1. Token 匹配度评分 (0-100分)
        int tokenMatchScore = calculateTokenMatchScore(template, content);
        score += tokenMatchScore;

        // 2. 内容特征匹配评分 (0-50分)
        int featureScore = calculateFeatureScore(template, content);
        score += featureScore;

        // 3. 压缩策略匹配评分 (0-30分)
        int strategyScore = calculateStrategyScore(template, content);
        score += strategyScore;

        // 4. 关键词匹配度评分
        double keywordScore = calculateKeywordMatchScore(template, content);
        score += keywordScore * Score.KEYWORD_MATCH_WEIGHT;

        log.debug(
                "Template score calculation - template: {}, tokenMatch: {}, feature: {}, strategy: {}, keyword: {}, total: {}",
                template.getName(), tokenMatchScore, featureScore, strategyScore, keywordScore, score);

        return score;
    }

    private int calculateTokenMatchScore(PromptTemplate template, String content) {
        int contentTokens = estimateTokens(content);
        int templateTokens = template.getEstimatedTokens();
        int diff = Math.abs(contentTokens - templateTokens);
        return Math.max(0, Score.MAX_TOKEN_MATCH_SCORE - diff);
    }

    private int calculateFeatureScore(PromptTemplate template, String content) {
        int score = 0;

        // SQL特征检测 (优先检查SQL，因为SQL也包含代码特征)
        if (containsSQLFeatures(content) && template.getType().contains("sql")) {
            score += Score.CODE_FEATURE_SCORE * 1.5; // SQL模板得分权重更高
            return score; // 如果匹配到SQL特征，直接返回
        }

        // 代码特征检测
        if (containsCodeFeatures(content) && template.getType().contains("code")) {
            score += Score.CODE_FEATURE_SCORE;
        }

        return score;
    }

    private int calculateStrategyScore(PromptTemplate template, String content) {
        String strategy = template.getCompressionStrategy();

        // 根据内容特征选择最佳压缩策略
        if ("semantic".equals(strategy) && !containsCodeFeatures(content)) {
            return Score.STRATEGY_MATCH_SCORE;
        }
        if ("keyword".equals(strategy) && containsCodeFeatures(content)) {
            return Score.STRATEGY_MATCH_SCORE;
        }
        if ("redundancy".equals(strategy)) {
            return Score.STRATEGY_MATCH_SCORE / 2; // 通用策略得分较低
        }

        return 0;
    }

    private double calculateKeywordMatchScore(PromptTemplate template, String content) {
        // TODO: 实现关键词匹配算法
        return 0.0;
    }

    private int estimateTokens(String content) {
        // TODO: 实现token估算算法
        return content.length() / 4;
    }

    private boolean containsCodeFeatures(String content) {
        return content.matches(".*" + Patterns.CODE_PATTERN + ".*");
    }

    private boolean containsSQLFeatures(String content) {
        // 增加对SQL关键字的检测
        if (content.toLowerCase().contains("sql") ||
                content.toLowerCase().contains("mysql") ||
                content.toLowerCase().contains("crud") ||
                content.toLowerCase().contains("数据库")) {
            return true;
        }
        return content.matches(".*" + Patterns.SQL_PATTERN + ".*");
    }
}