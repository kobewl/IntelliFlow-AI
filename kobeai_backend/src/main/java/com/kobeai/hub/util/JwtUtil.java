package com.kobeai.hub.util;

import com.kobeai.hub.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private Key key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        // 确保密钥长度至少为 256 位（32 字节）
        byte[] keyBytes = new byte[32];
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, keyBytes.length));
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT key initialized successfully");
    }

    public String generateToken(User user) {
        if (user == null || user.getUsername() == null) {
            throw new IllegalArgumentException("User or username cannot be null");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("userId", user.getId());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return "Bearer " + token;
    }

    public String getUsernameFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            // 移除 Bearer 前缀
            String tokenValue = token;
            if (token.startsWith("Bearer ")) {
                tokenValue = token.substring(7).trim();
            }

            if (tokenValue.isEmpty()) {
                throw new IllegalArgumentException("Token value cannot be empty");
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(tokenValue)
                    .getBody();

            String username = claims.getSubject();
            if (username == null || username.trim().isEmpty()) {
                throw new JwtException("Token does not contain a valid username");
            }

            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                log.warn("Token is expired for user: {}", username);
                throw new ExpiredJwtException(null, claims, "Token has expired");
            }

            return username;
        } catch (ExpiredJwtException e) {
            log.warn("Token validation failed: token expired");
            throw e;
        } catch (JwtException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    public void invalidateToken(String token) {
        // 实际项目中可以将token加入黑名单
    }
}