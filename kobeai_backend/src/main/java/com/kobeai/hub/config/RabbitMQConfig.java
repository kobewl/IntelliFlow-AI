package com.kobeai.hub.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类，用于配置 RabbitMQ 的连接、交换机、队列、绑定和消息转换器。
 */
@Configuration // 将该类标记为 Spring 配置类，使其被 Spring 容器管理
public class RabbitMQConfig {

    // 声明从配置文件中读取的 RabbitMQ 连接相关信息
    @Value("${spring.rabbitmq.host}")
    private String host; // RabbitMQ 服务器地址

    @Value("${spring.rabbitmq.username}")
    private String username; // RabbitMQ 用户名

    @Value("${spring.rabbitmq.password}")
    private String password; // RabbitMQ 密码

    /**
     * RabbitMQ 交换机名称，用于发送电子邮件验证消息。
     */
    public static final String EMAIL_EXCHANGE = "email.exchange";

    /**
     * RabbitMQ 队列名称，用于存储电子邮件验证消息。
     */
    public static final String EMAIL_QUEUE = "email.verification.queue";

    /**
     * RabbitMQ 路由键，用于将消息路由到指定的队列。
     */
    public static final String EMAIL_ROUTING_KEY = "email.verification";

    /**
     * 配置 RabbitMQ 连接工厂，从 application.yml 中读取地址、用户名和密码等信息。
     *
     * @return ConnectionFactory 对象，用于建立与 RabbitMQ 服务器的连接
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        // 创建缓存连接工厂实例
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        // 使用从配置中注入的 host、username 和 password，不再硬编码
        connectionFactory.setHost(host); // 从配置文件中读取RabbitMQ服务器地址
        connectionFactory.setUsername(username); // 从配置文件中读取RabbitMQ用户名
        connectionFactory.setPassword(password); // 从配置文件中读取RabbitMQ密码
        return connectionFactory;
    }

    /**
     * 声明一个直连交换机（Direct Exchange）。
     *
     * @return DirectExchange 对象，用于路由消息到特定队列
     */
    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE); // 创建并返回指定名称的直连交换机
    }

    /**
     * 声明一个持久化的队列。
     *
     * @return Queue 对象，用于存储消息
     */
    @Bean
    public Queue emailVerificationQueue() {
        return new Queue(EMAIL_QUEUE, true); // 创建并返回指定名称的持久化队列
    }

    /**
     * 绑定队列到交换机，并指定路由键。
     *
     * @return Binding 对象，用于定义队列与交换机之间的绑定关系
     */
    @Bean
    public Binding bindingEmailQueue() {
        return BindingBuilder
                .bind(emailVerificationQueue()) // 绑定队列
                .to(emailExchange()) // 到指定的交换机
                .with(EMAIL_ROUTING_KEY); // 使用指定的路由键进行绑定
    }

    /**
     * 配置 JSON 消息转换器，用于将 Java 对象转换为 JSON 格式的消息。
     *
     * @return MessageConverter 对象，用于消息格式转换
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(); // 使用 Jackson 库进行 JSON 格式的转换
    }

    /**
     * 配置 RabbitTemplate，用于发送和接收消息。
     *
     * @param connectionFactory    连接工厂对象，用于建立连接
     * @param jsonMessageConverter 消息转换器对象，用于消息格式转换
     * @return RabbitTemplate 对象，用于操作 RabbitMQ
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory); // 创建 RabbitTemplate 实例
        rabbitTemplate.setMessageConverter(jsonMessageConverter); // 设置消息转换器
        return rabbitTemplate;
    }
}
