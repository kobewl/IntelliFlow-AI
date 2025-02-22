package com.kobeai.hub.mq;

import com.kobeai.hub.config.RabbitMQConfig;
import com.kobeai.hub.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * EmailVerificationConsumer类用于监听RabbitMQ中的邮件验证队列，并处理邮件验证消息。
 * 该类通过调用EmailService来发送验证码到指定的邮箱。
 */
@Component // 表示该类是一个Spring组件，会被Spring容器管理
@RequiredArgsConstructor // Lombok注解，自动生成包含final字段的构造函数
public class EmailVerificationConsumer {

    private final EmailService emailService; // 用于发送邮件的服务

    /**
     * 监听RabbitMQ中的邮件验证队列，处理接收到的邮件验证消息。
     *
     * @param message 包含邮箱地址和验证码的消息对象
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE) // 监听指定的RabbitMQ队列
    public void handleEmailVerification(EmailVerificationMessage message) {
        emailService.sendVerificationCode(message.getEmail(), message.getCode());
    }

    /**
     * EmailVerificationMessage类用于封装邮件验证消息的数据。
     * 该类实现了Serializable接口，以便在消息队列中传输。
     */
    public static class EmailVerificationMessage implements Serializable {
        private static final long serialVersionUID = 1L; // 序列化版本UID

        private String email; // 邮箱地址
        private String code; // 验证码

        /**
         * 获取邮箱地址。
         *
         * @return 邮箱地址
         */
        public String getEmail() {
            return email;
        }

        /**
         * 设置邮箱地址。
         *
         * @param email 邮箱地址
         */
        public void setEmail(String email) {
            this.email = email;
        }

        /**
         * 获取验证码。
         *
         * @return 验证码
         */
        public String getCode() {
            return code;
        }

        /**
         * 设置验证码。
         *
         * @param code 验证码
         */
        public void setCode(String code) {
            this.code = code;
        }
    }
}
