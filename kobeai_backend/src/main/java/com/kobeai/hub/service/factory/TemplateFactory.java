package com.kobeai.hub.service.factory;

import com.kobeai.hub.model.PromptTemplate;
import com.kobeai.hub.constant.TemplateConstants.*;
import org.springframework.stereotype.Component;

/**
 * 模板工厂类
 */
@Component
public class TemplateFactory {

    /**
     * 创建模板
     * 
     * @param type    模板类型
     * @param name    模板名称
     * @param content 模板内容
     * @return 创建的模板
     */
    public PromptTemplate createTemplate(String type, String name, String content) {
        PromptTemplate template = new PromptTemplate();
        template.setType(type);
        template.setName(name);
        template.setContent(content);

        // 根据类型设置默认值
        switch (type) {
            case TemplateType.CODE:
                setupCodeTemplate(template);
                break;
            case TemplateType.CHAT:
                setupChatTemplate(template);
                break;
            case TemplateType.SQL:
                setupSqlTemplate(template);
                break;
            default:
                setupDefaultTemplate(template);
        }

        return template;
    }

    private void setupCodeTemplate(PromptTemplate template) {
        template.setCompressionStrategy(CompressionType.KEYWORD);
        template.setEstimatedTokens(TokenConfig.CODE_TEMPLATE_TOKENS);
        template.setDescription(Description.CODE_TEMPLATE);
    }

    private void setupChatTemplate(PromptTemplate template) {
        template.setCompressionStrategy(CompressionType.SEMANTIC);
        template.setEstimatedTokens(TokenConfig.CHAT_TEMPLATE_TOKENS);
        template.setDescription(Description.CHAT_TEMPLATE);
    }

    private void setupSqlTemplate(PromptTemplate template) {
        template.setCompressionStrategy(CompressionType.KEYWORD);
        template.setEstimatedTokens(TokenConfig.SQL_TEMPLATE_TOKENS);
        template.setDescription(Description.SQL_TEMPLATE);
        // 设置SQL特定的变量和配置
        template.setVariables("{\"dialect\":\"MySQL\", \"format\":\"standard\", \"includeComments\":true}");
    }

    private void setupDefaultTemplate(PromptTemplate template) {
        template.setCompressionStrategy(CompressionType.SEMANTIC);
        template.setEstimatedTokens(TokenConfig.DEFAULT_TEMPLATE_TOKENS);
        template.setDescription(Description.DEFAULT_TEMPLATE);
    }
}