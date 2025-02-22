package com.kobeai.hub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置类，用于配置 Spring WebSocket 和 STOMP 消息代理。
 */
@Configuration // 将该类标记为 Spring 配置类，使其被 Spring 容器管理
@EnableWebSocketMessageBroker // 启用 WebSocket 消息代理功能
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理设置。
     *
     * @param config MessageBrokerRegistry 对象，用于配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的内存消息代理，处理以 "/topic" 开头的消息目的地
        config.enableSimpleBroker("/topic");

        // 设置应用程序消息的目的地前缀为 "/app"，
        // 所有以 "/app" 开头的消息将被路由到控制器方法中进行处理
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 注册 STOMP 端点。
     *
     * @param registry StompEndpointRegistry 对象，用于注册 STOMP 端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 添加一个名为 "/ws" 的 WebSocket 端点
        // 允许所有来源（*）连接到此端点（在生产环境中应限制为特定来源）
        // 启用 SockJS 支持，以便在不支持 WebSocket 的浏览器中使用 SockJS 作为回退方案
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
