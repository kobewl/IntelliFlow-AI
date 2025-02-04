package com.kobeai.hub.service.impl;

import com.kobeai.hub.model.PromptTemplate;
import com.kobeai.hub.repository.PromptTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 模板服务单元测试
 */
class TemplateServiceTest {

    @Mock
    private PromptTemplateRepository templateRepository;

    @Mock
    private ContentAnalyzer contentAnalyzer;

    @Mock
    private TemplateScoring templateScoring;

    @Mock
    private SemanticCompressor semanticCompressor;

    @InjectMocks
    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindBestTemplate() {
        // 准备测试数据
        String content = "public class Test { }";
        String type = "code";

        PromptTemplate template1 = new PromptTemplate();
        template1.setId(1L);
        template1.setType("code");
        template1.setName("code_template_1");
        template1.setCompressionStrategy("keyword");

        PromptTemplate template2 = new PromptTemplate();
        template2.setId(2L);
        template2.setType("code");
        template2.setName("code_template_2");
        template2.setCompressionStrategy("semantic");

        // 设置模拟行为
        when(templateRepository.findByType(type))
                .thenReturn(Arrays.asList(template1, template2));
        when(templateScoring.calculateTemplateScore(template1, content))
                .thenReturn(80);
        when(templateScoring.calculateTemplateScore(template2, content))
                .thenReturn(60);

        // 执行测试
        PromptTemplate result = templateService.findBestTemplate(type, content);

        // 验证结果
        assertNotNull(result);
        assertEquals(template1.getId(), result.getId());
        assertEquals("keyword", result.getCompressionStrategy());

        // 验证方法调用
        verify(templateRepository).findByType(type);
        verify(templateScoring).calculateTemplateScore(template1, content);
        verify(templateScoring).calculateTemplateScore(template2, content);
    }

    @Test
    void testCompressContent() {
        // 准备测试数据
        String content = "This is a test content with some code: public class Test { }";
        int maxTokens = 50;

        // 设置模拟行为
        when(contentAnalyzer.analyzeStructure(content))
                .thenReturn(mock(java.util.Map.class));
        when(semanticCompressor.compress(content, maxTokens))
                .thenReturn("Compressed content");

        // 执行测试
        String result = templateService.compressContent(content, maxTokens);

        // 验证结果
        assertNotNull(result);
        assertEquals("Compressed content", result);

        // 验证方法调用
        verify(contentAnalyzer).analyzeStructure(content);
        verify(semanticCompressor).compress(content, maxTokens);
    }

    @Test
    void testUpdateTemplateStats() {
        // 准备测试数据
        Long templateId = 1L;
        int score = 85;

        PromptTemplate template = new PromptTemplate();
        template.setId(templateId);
        template.setUsageCount(10);
        template.setAverageScore(80.0);

        // 设置模拟行为
        when(templateRepository.findById(templateId))
                .thenReturn(java.util.Optional.of(template));
        when(templateRepository.save(any(PromptTemplate.class)))
                .thenReturn(template);

        // 执行测试
        templateService.updateTemplateStats(templateId, score);

        // 验证结果
        assertEquals(11, template.getUsageCount());
        assertTrue(template.getAverageScore() > 80.0);

        // 验证方法调用
        verify(templateRepository).findById(templateId);
        verify(templateRepository).save(template);
    }

    @Test
    void testFindTemplatesByTokenRange() {
        // 准备测试数据
        int minTokens = 100;
        int maxTokens = 200;
        List<PromptTemplate> expectedTemplates = Arrays.asList(
                new PromptTemplate(), new PromptTemplate());

        // 设置模拟行为
        when(templateRepository.findByEstimatedTokensBetween(minTokens, maxTokens))
                .thenReturn(expectedTemplates);

        // 执行测试
        List<PromptTemplate> result = templateService.findTemplatesByTokenRange(minTokens, maxTokens);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证方法调用
        verify(templateRepository).findByEstimatedTokensBetween(minTokens, maxTokens);
    }
}