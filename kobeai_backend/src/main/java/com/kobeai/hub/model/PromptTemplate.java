package com.kobeai.hub.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 提示词模板实体类
 */
@Data
@Entity
@Table(name = "prompt_templates")
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 模板名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 模板描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 模板内容
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 模板类型
     * 如：chat, code, sql 等
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * 变量列表
     * JSON格式存储
     */
    @Column(columnDefinition = "TEXT")
    private String variables;

    /**
     * 压缩策略
     * semantic: 语义压缩
     * keyword: 关键词压缩
     * redundancy: 冗余移除
     */
    @Column(name = "compression_strategy", length = 50)
    private String compressionStrategy;

    /**
     * 预估token数量
     */
    @Column(name = "estimated_tokens")
    private Integer estimatedTokens;

    /**
     * 使用频率
     */
    @Column(name = "usage_count")
    private Integer usageCount = 0;

    /**
     * 平均得分
     */
    @Column(name = "average_score")
    private Double averageScore = 0.0;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 更新使用统计
     */
    public void updateUsageStats(int score) {
        this.usageCount++;
        this.averageScore = (this.averageScore * (this.usageCount - 1) + score) / this.usageCount;
    }
}