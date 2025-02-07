package com.kobeai.hub.mq;

import com.kobeai.hub.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
public class EmailVerificationConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = "email.verification.queue")
    public void handleEmailVerification(EmailVerificationMessage message) {
        emailService.sendVerificationCode(message.getEmail(), message.getCode());
    }

    // 消息对象
    public static class EmailVerificationMessage implements Serializable {
        private static final long serialVersionUID = 1L;

        private String email;
        private String code;

        // Getters and setters
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}