package com.kobeai.hub.service.impl;

import com.kobeai.hub.model.PromptTemplate;
import com.kobeai.hub.repository.PromptTemplateRepository;
import com.kobeai.hub.service.compression.CompressionStrategy;
import com.kobeai.hub.service.factory.TemplateFactory;
import com.kobeai.hub.service.observer.TemplateUpdateObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 模板服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final PromptTemplateRepository templateRepository;
    private final ContentAnalyzer contentAnalyzer;
    private final TemplateScoring templateScoring;
    private final TemplateFactory templateFactory;
    private final List<CompressionStrategy> compressionStrategies;
    private final List<TemplateUpdateObserver> updateObservers;

    /**
     * 查找最佳模板
     */
    public PromptTemplate findBestTemplate(String type, String content) {
        // 1. 获取指定类型的所有模板
        List<PromptTemplate> templates = templateRepository.findByType(type);
        if (templates.isEmpty()) {
            log.warn("No templates found for type: {}, creating default template", type);
            return templateFactory.createTemplate(type, "default_" + type, "");
        }

        // 2. 计算每个模板的得分
        PromptTemplate bestTemplate = null;
        int highestScore = -1;

        for (PromptTemplate template : templates) {
            int score = templateScoring.calculateTemplateScore(template, content);
            if (score > highestScore) {
                highestScore = score;
                bestTemplate = template;
            }
        }

        if (bestTemplate != null) {
            log.debug("Found best template: {}, score: {}", bestTemplate.getName(), highestScore);
        }

        return bestTemplate;
    }

    /**
     * 压缩内容
     */
    public String compressContent(String content, int maxTokens) {
        try {
            // 1. 分析内容结构
            Map<String, Object> structure = contentAnalyzer.analyzeStructure(content);

            // 2. 选择合适的压缩策略
            CompressionStrategy strategy = selectCompressionStrategy(content);

            // 3. 使用选中的策略压缩内容
            String compressedContent = strategy.compress(content, maxTokens);

            log.debug("Content compressed using strategy: {} - original length: {}, compressed length: {}",
                    strategy.getStrategyName(), content.length(), compressedContent.length());

            return compressedContent;
        } catch (Exception e) {
            log.error("Error compressing content", e);
            return content;
        }
    }

    /**
     * 更新模板使用统计
     */
    @Transactional
    public void updateTemplateStats(Long templateId, int score) {
        templateRepository.findById(templateId).ifPresent(template -> {
            // 通知所有观察者
            for (TemplateUpdateObserver observer : updateObservers) {
                observer.onTemplateUpdate(template, score);
            }
        });
    }

    /**
     * 创建新模板
     */
    public PromptTemplate createTemplate(String type, String name, String content) {
        PromptTemplate template = templateFactory.createTemplate(type, name, content);
        return templateRepository.save(template);
    }

    /**
     * 选择合适的压缩策略
     */
    private CompressionStrategy selectCompressionStrategy(String content) {
        return compressionStrategies.stream()
                .filter(strategy -> strategy.isApplicable(content))
                .findFirst()
                .orElseGet(() -> compressionStrategies.stream()
                        .filter(s -> "semantic".equals(s.getStrategyName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No compression strategy found")));
    }

    /**
     * 根据token范围查找模板
     */
    public List<PromptTemplate> findTemplatesByTokenRange(int minTokens, int maxTokens) {
        return templateRepository.findByEstimatedTokensBetween(minTokens, maxTokens);
    }

    /**
     * 根据使用频率查找最常用模板
     */
    public List<PromptTemplate> findMostUsedTemplates(String type) {
        return templateRepository.findMostUsedByType(type);
    }

    /**
     * 根据平均得分查找最佳表现模板
     */
    public List<PromptTemplate> findBestPerformingTemplates(String type) {
        return templateRepository.findBestPerformingByType(type);
    }
}