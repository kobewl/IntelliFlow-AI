package com.kobeai.hub.agent.service;

import io.agentscope.core.model.OpenAIChatModel;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * 多模型路由单元测试
 */
class ModelRouterTest {

    private static final String DEFAULT_MODEL = "deepseek-v4-flash";

    private ModelRouter newRouter() {
        Map<String, OpenAIChatModel> modelMap = new HashMap<>();
        modelMap.put("deepseek-v4-flash", mock(OpenAIChatModel.class));
        modelMap.put("deepseek-v4-pro", mock(OpenAIChatModel.class));
        modelMap.put("doubao-chat", mock(OpenAIChatModel.class));
        return new ModelRouter(modelMap);
    }

    @Test
    void 已注册模型应能正确解析() {
        ModelRouter router = newRouter();
        assertEquals(router.resolve("deepseek-v4-pro"), router.resolve("deepseek-v4-pro"),
                "同一模型名应解析到同一实例");
    }

    @Test
    void 未知模型应回退到默认模型() {
        ModelRouter router = newRouter();
        assertSame(router.resolve(DEFAULT_MODEL), router.resolve("not-exist-model"),
                "未知模型名应回退到默认模型实例");
    }

    @Test
    void 数学类问题应路由到Pro模型() {
        assertEquals("deepseek-v4-pro", newRouter().routeByTask("帮我计算 3^10 等于多少"));
    }

    @Test
    void 编程类问题应路由到Pro模型() {
        assertEquals("deepseek-v4-pro", newRouter().routeByTask("帮我写一个 Python 快速排序算法"));
    }

    @Test
    void 创作类问题应路由到豆包() {
        assertEquals("doubao-chat", newRouter().routeByTask("帮我写一篇文章介绍春天"));
    }

    @Test
    void 普通闲聊应使用默认模型() {
        assertEquals(DEFAULT_MODEL, newRouter().routeByTask("今天心情不错"));
    }

    @Test
    void 空消息应使用默认模型() {
        ModelRouter router = newRouter();
        assertEquals(DEFAULT_MODEL, router.routeByTask(null));
        assertEquals(DEFAULT_MODEL, router.routeByTask(""));
    }
}
