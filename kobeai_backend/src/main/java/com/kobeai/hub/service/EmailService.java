package com.kobeai.hub.service;

public interface EmailService {
    void sendVerificationCode(String to, String code);
}