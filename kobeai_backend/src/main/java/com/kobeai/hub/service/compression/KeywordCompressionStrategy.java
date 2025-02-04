package com.kobeai.hub.service.compression;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.kobeai.hub.service.impl.ContentAnalyzer;
import com.kobeai.hub.constant.TemplateConstants.CompressionType;
import com.kobeai.hub.constant.TemplateConstants.Patterns;
import java.util.*;
import java.util.regex.*;

/**
 * 关键词压缩策略实现
 */
@Component
@RequiredArgsConstructor
public class KeywordCompressionStrategy implements CompressionStrategy {

    private final ContentAnalyzer contentAnalyzer;

    @Override
    public String compress(String content, int maxTokens) {
        // 实现关键词压缩逻辑
        try {
            // 1. 提取代码块和关键词
            Map<String, Object> structure = contentAnalyzer.analyzeStructure(content);
            @SuppressWarnings("unchecked")
            List<String> codeBlocks = (List<String>) structure.getOrDefault("codeBlocks", new ArrayList<>());

            // 2. 保留代码块和关键标识符
            StringBuilder compressed = new StringBuilder();
            for (String block : codeBlocks) {
                compressed.append(block).append("\n\n");
            }

            // 3. 压缩其他内容
            String nonCodeContent = content;
            for (String block : codeBlocks) {
                nonCodeContent = nonCodeContent.replace(block, "");
            }

            // 4. 提取关键词
            Pattern pattern = Pattern.compile(Patterns.WORD_PATTERN);
            Matcher matcher = pattern.matcher(nonCodeContent);
            Set<String> keywords = new HashSet<>();
            while (matcher.find()) {
                String word = matcher.group();
                if (word.length() > 3) { // 只保留较长的词
                    keywords.add(word);
                }
            }

            // 5. 添加关键词
            compressed.append(String.join(" ", keywords));

            return compressed.toString();
        } catch (Exception e) {
            return content;
        }
    }

    @Override
    public String getStrategyName() {
        return CompressionType.KEYWORD;
    }

    @Override
    public boolean isApplicable(String content) {
        // 检查内容是否包含代码块
        return contentAnalyzer.analyzeStructure(content)
                .containsKey("codeBlocks");
    }
}