package com.kobeai.hub.service.observer;

import com.kobeai.hub.model.PromptTemplate;

/**
 * 模板更新观察者接口
 */
public interface TemplateUpdateObserver {
    /**
     * 处理模板更新事件
     * 
     * @param template 更新的模板
     * @param score    本次使用的得分
     */
    void onTemplateUpdate(PromptTemplate template, int score);
}