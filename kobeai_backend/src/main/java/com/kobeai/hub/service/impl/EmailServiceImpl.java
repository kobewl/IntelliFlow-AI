package com.kobeai.hub.service.impl;

import com.kobeai.hub.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // Lombok注解，自动生成包含final字段的构造函数
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("IntelliFlow AI - 智能对话平台");
        message.setTo(to);
        message.setSubject("邮箱验证码");
        message.setText("您的验证码是: " + code + "\n验证码有效期为5分钟，请尽快验证。");

        mailSender.send(message);
    }
}