package com.kobeai.hub.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_QUEUE = "email.verification.queue";
    public static final String EMAIL_ROUTING_KEY = "email.verification";

    // RabbitMQ连接配置
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost("39.98.107.158");
        connectionFactory.setUsername("wangliang");
        connectionFactory.setPassword("kobe200800");
        return connectionFactory;
    }

    // 声明交换机
    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    // 声明队列
    @Bean
    public Queue emailVerificationQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    // 绑定队列到交换机
    @Bean
    public Binding bindingEmailQueue() {
        return BindingBuilder
                .bind(emailVerificationQueue())
                .to(emailExchange())
                .with(EMAIL_ROUTING_KEY);
    }

    // 配置消息转换器
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}