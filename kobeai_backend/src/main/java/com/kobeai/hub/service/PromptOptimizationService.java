package com.kobeai.hub.service;

import com.kobeai.hub.model.PromptTemplate;
import java.util.Map;

public interface PromptOptimizationService {

    /**
     * 优化 Prompt 内容
     * 
     * @param content   原始内容
     * @param template  使用的模板
     * @param variables 变量值
     * @return 优化后的内容
     */
    String optimizePrompt(String content, PromptTemplate template, Map<String, Object> variables);

    /**
     * 压缩上下文内容
     * 
     * @param context   上下文内容
     * @param maxTokens 最大 token 数
     * @return 压缩后的上下文
     */
    String compressContext(String context, int maxTokens);

    /**
     * 估算 Token 数量
     * 
     * @param content 内容
     * @return 预估的 token 数量
     */
    int estimateTokens(String content);

    /**
     * 获取适合的模板
     * 
     * @param type    模板类型
     * @param content 内容
     * @return 最适合的模板
     */
    PromptTemplate findBestTemplate(String type, String content);
}