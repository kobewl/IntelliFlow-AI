package com.kobeai.hub.service.compression;

/**
 * 压缩策略接口
 */
public interface CompressionStrategy {
    /**
     * 压缩内容
     * 
     * @param content   原始内容
     * @param maxTokens 最大token数
     * @return 压缩后的内容
     */
    String compress(String content, int maxTokens);

    /**
     * 获取策略名称
     */
    String getStrategyName();

    /**
     * 是否适用于当前内容
     */
    boolean isApplicable(String content);
}