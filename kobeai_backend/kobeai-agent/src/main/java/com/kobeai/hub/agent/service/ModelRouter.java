package com.kobeai.hub.agent.service;

import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ModelRouter {

    private static final Pattern CODE_PATTERN =
            Pattern.compile("代码|编程|bug|debug|函数|算法|优化|报错|编译|运行|java|python|go|rust|sql");
    private static final Pattern CREATIVE_PATTERN =
            Pattern.compile("写.*文章|写.*故事|创作|翻译|改写|润色|总结|提炼|汇报|邮件");
    private static final Pattern MATH_PATTERN =
            Pattern.compile("计算|等于|多少|换算|公式|推导|证明|单位");
    private static final String DEFAULT_MODEL = "deepseek-v4-flash";

    private final Map<String, ? extends Model> modelMap;

    public ModelRouter(Map<String, OpenAIChatModel> modelMap) {
        this.modelMap = modelMap;
    }

    public Model resolve(String modelName) {
        Model model = modelMap.get(modelName);
        if (model != null) {
            return model;
        }
        log.warn("模型 {} 无法解析, 使用默认模型 {}", modelName, DEFAULT_MODEL);
        return modelMap.get(DEFAULT_MODEL);
    }

    public String routeByTask(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return DEFAULT_MODEL;
        }
        if (MATH_PATTERN.matcher(userMessage).find()) {
            return "deepseek-v4-pro";
        }
        if (CODE_PATTERN.matcher(userMessage).find()) {
            return "deepseek-v4-pro";
        }
        if (CREATIVE_PATTERN.matcher(userMessage).find()) {
            return "doubao-chat";
        }
        return DEFAULT_MODEL;
    }
}
