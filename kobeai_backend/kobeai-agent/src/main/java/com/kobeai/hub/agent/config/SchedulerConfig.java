package com.kobeai.hub.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启用 Spring 定时任务支持。
 * 用于记忆自动摘要等周期性任务。
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
