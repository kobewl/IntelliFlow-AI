package com.kobeai.hub.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    // 加盐值，增加密码安全性
    private static final String SALT = "KobeAI@2024";

    /**
     * 对密码进行 MD5 加密
     * 
     * @param password 原始密码
     * @return 加密后的密码
     */
    public String encrypt(String password) {
        // 将密码和盐值组合后进行 MD5 加密
        return DigestUtils.md5Hex(password + SALT);
    }

    /**
     * 验证密码是否正确
     * 
     * @param inputPassword     输入的密码
     * @param encryptedPassword 数据库中存储的加密密码
     * @return 是否匹配
     */
    public boolean verify(String inputPassword, String encryptedPassword) {
        return encrypt(inputPassword).equals(encryptedPassword);
    }
}