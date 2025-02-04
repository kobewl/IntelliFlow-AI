package com.kobeai.hub.service.compression;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.kobeai.hub.service.impl.ContentAnalyzer;
import com.kobeai.hub.constant.TemplateConstants.CompressionType;

/**
 * 语义压缩策略实现
 */
@Component
@RequiredArgsConstructor
public class SemanticCompressionStrategy implements CompressionStrategy {

    private final ContentAnalyzer contentAnalyzer;

    @Override
    public String compress(String content, int maxTokens) {
        // 使用现有的 SemanticCompressor 的逻辑
        return new com.kobeai.hub.service.impl.SemanticCompressor().compress(content, maxTokens);
    }

    @Override
    public String getStrategyName() {
        return CompressionType.SEMANTIC;
    }

    @Override
    public boolean isApplicable(String content) {
        // 检查内容是否适合语义压缩
        return !contentAnalyzer.analyzeStructure(content)
                .containsKey("codeBlocks");
    }
}