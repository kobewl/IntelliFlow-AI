package com.kobeai.hub.repository;

import com.kobeai.hub.model.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 提示词模板数据访问接口
 */
@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    /**
     * 根据类型查找模板
     */
    List<PromptTemplate> findByType(String type);

    /**
     * 根据类型和名称查找模板
     */
    PromptTemplate findByTypeAndName(String type, String name);

    /**
     * 查找指定类型的最新模板
     */
    @Query("SELECT pt FROM PromptTemplate pt WHERE pt.type = :type ORDER BY pt.updatedAt DESC")
    List<PromptTemplate> findLatestByType(@Param("type") String type);

    /**
     * 查找预估 Token 数量在指定范围内的模板
     */
    List<PromptTemplate> findByEstimatedTokensBetween(int minTokens, int maxTokens);

    /**
     * 根据使用频率查找最常用的模板
     */
    @Query("SELECT pt FROM PromptTemplate pt WHERE pt.type = :type ORDER BY pt.usageCount DESC")
    List<PromptTemplate> findMostUsedByType(@Param("type") String type);

    /**
     * 根据平均得分查找最佳模板
     */
    @Query("SELECT pt FROM PromptTemplate pt WHERE pt.type = :type ORDER BY pt.averageScore DESC")
    List<PromptTemplate> findBestPerformingByType(@Param("type") String type);

    /**
     * 查找包含特定关键词的模板
     */
    @Query("SELECT pt FROM PromptTemplate pt WHERE pt.content LIKE %:keyword% OR pt.description LIKE %:keyword%")
    List<PromptTemplate> findByKeyword(@Param("keyword") String keyword);

    /**
     * 根据压缩策略查找模板
     */
    List<PromptTemplate> findByCompressionStrategy(String strategy);
}