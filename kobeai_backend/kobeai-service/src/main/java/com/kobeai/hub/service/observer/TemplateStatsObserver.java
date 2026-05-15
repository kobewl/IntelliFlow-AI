package com.kobeai.hub.service.observer;

import com.kobeai.hub.model.PromptTemplate;
import com.kobeai.hub.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模板统计信息更新观察者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateStatsObserver implements TemplateUpdateObserver {

    private final PromptTemplateRepository templateRepository;

    @Override
    @Transactional
    public void onTemplateUpdate(PromptTemplate template, int score) {
        try {
            // 更新使用统计
            template.updateUsageStats(score);

            // 保存更新
            templateRepository.save(template);

            log.debug("Template stats updated - id: {}, score: {}, avgScore: {}",
                    template.getId(), score, template.getAverageScore());
        } catch (Exception e) {
            log.error("Error updating template stats", e);
        }
    }
}