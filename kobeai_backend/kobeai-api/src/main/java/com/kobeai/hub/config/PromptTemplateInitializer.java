package com.kobeai.hub.config;

import com.kobeai.hub.model.PromptTemplate;
import com.kobeai.hub.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PromptTemplateInitializer {

    private final PromptTemplateRepository templateRepository;

    @Bean
    public CommandLineRunner initializePromptTemplates() {
        return args -> {
            log.info("开始初始化 Prompt 模板...");
            if (templateRepository.count() == 0) {
                List<PromptTemplate> templates = Arrays.asList(
                        // 1. 通用对话模板
                        createTemplate(
                                "general_chat",
                                "通用对话",
                                "chat",
                                "作为一个AI助手，请用简洁专业的方式回答以下问题：\n{{content}}",
                                "semantic",
                                100),

                        // 2. 代码生成模板
                        createTemplate(
                                "code_generation",
                                "代码生成",
                                "code",
                                "请生成符合以下要求的代码，并添加详细的中文注释：\n{{content}}\n" +
                                        "要求：\n" +
                                        "1. 代码要简洁易懂\n" +
                                        "2. 使用最佳实践\n" +
                                        "3. 包含错误处理\n" +
                                        "4. 性能优化",
                                "keyword",
                                150),

                        // 3. 代码优化模板
                        createTemplate(
                                "code_optimization",
                                "代码优化",
                                "code",
                                "请优化以下代码，重点关注：\n" +
                                        "1. 性能优化\n" +
                                        "2. 代码简化\n" +
                                        "3. 最佳实践\n" +
                                        "4. 可维护性\n\n" +
                                        "原始代码：\n{{content}}",
                                "keyword",
                                200),

                        // 4. 文本总结模板
                        createTemplate(
                                "text_summary",
                                "文本总结",
                                "summary",
                                "请对以下内容进行简洁的总结，突出关键点：\n{{content}}",
                                "semantic",
                                120),

                        // 5. 问题解答模板
                        createTemplate(
                                "problem_solving",
                                "问题解答",
                                "qa",
                                "请针对以下问题提供详细的解决方案：\n{{content}}\n" +
                                        "解答要求：\n" +
                                        "1. 分析问题根源\n" +
                                        "2. 提供具体步骤\n" +
                                        "3. 考虑多种情况\n" +
                                        "4. 给出最佳实践",
                                "semantic",
                                180),

                        // 6. API 文档生成模板
                        createTemplate(
                                "api_doc",
                                "API文档生成",
                                "doc",
                                "请为以下API生成标准的文档：\n{{content}}\n" +
                                        "包含以下部分：\n" +
                                        "1. 接口描述\n" +
                                        "2. 请求参数\n" +
                                        "3. 响应格式\n" +
                                        "4. 示例代码\n" +
                                        "5. 错误处理",
                                "keyword",
                                160),

                        // 7. 数据分析模板
                        createTemplate(
                                "data_analysis",
                                "数据分析",
                                "analysis",
                                "请对以下数据进行分析，重点关注：\n" +
                                        "1. 数据趋势\n" +
                                        "2. 关键指标\n" +
                                        "3. 异常情况\n" +
                                        "4. 优化建议\n\n" +
                                        "数据内容：\n{{content}}",
                                "semantic",
                                200),

                        // 8. 测试用例生成模板
                        createTemplate(
                                "test_case",
                                "测试用例生成",
                                "test",
                                "请为以下功能生成完整的测试用例：\n{{content}}\n" +
                                        "测试覆盖：\n" +
                                        "1. 正常流程\n" +
                                        "2. 边界条件\n" +
                                        "3. 异常情况\n" +
                                        "4. 性能测试",
                                "keyword",
                                150),

                        // 9. 代码审查模板
                        createTemplate(
                                "code_review",
                                "代码审查",
                                "review",
                                "请对以下代码进行审查，关注：\n" +
                                        "1. 代码质量\n" +
                                        "2. 潜在问题\n" +
                                        "3. 安全隐患\n" +
                                        "4. 改进建议\n\n" +
                                        "代码：\n{{content}}",
                                "keyword",
                                180),

                        // 10. SQL优化模板
                        createTemplate(
                                "sql_optimization",
                                "SQL优化",
                                "sql",
                                "请优化以下SQL查询，考虑：\n" +
                                        "1. 查询性能\n" +
                                        "2. 索引使用\n" +
                                        "3. 执行计划\n" +
                                        "4. 最佳实践\n\n" +
                                        "SQL：\n{{content}}",
                                "keyword",
                                150));

                templateRepository.saveAll(templates);
                log.info("成功初始化 {} 个 Prompt 模板", templates.size());
            } else {
                log.info("Prompt 模板已存在，跳过初始化");
            }
        };
    }

    private PromptTemplate createTemplate(
            String name,
            String description,
            String type,
            String content,
            String compressionStrategy,
            Integer estimatedTokens) {

        PromptTemplate template = new PromptTemplate();
        template.setName(name);
        template.setDescription(description);
        template.setType(type);
        template.setContent(content);
        template.setCompressionStrategy(compressionStrategy);
        template.setEstimatedTokens(estimatedTokens);
        return template;
    }
}