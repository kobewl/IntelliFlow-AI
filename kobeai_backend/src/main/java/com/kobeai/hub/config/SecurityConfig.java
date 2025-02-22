package com.kobeai.hub.config;

import com.kobeai.hub.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 安全配置类，用于配置 Spring Security 的安全策略。
 */
@Configuration // 将该类标记为 Spring 配置类，使其被 Spring 容器管理
@EnableWebSecurity // 启用 Web 安全功能
@EnableGlobalMethodSecurity(prePostEnabled = true) // 启用基于方法的安全注解（如 @PreAuthorize）
@RequiredArgsConstructor // 使用 Lombok 生成一个全参构造函数，确保依赖注入
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * JWT 认证过滤器，用于处理 JWT 认证逻辑。
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置 HTTP 安全设置。
     *
     * @param http HttpSecurity 对象，用于配置 HTTP 安全策略
     * @throws Exception 如果配置过程中发生异常
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors().and() // 启用 CORS 支持，并继续配置其他安全设置
                .csrf().disable() // 禁用 CSRF 保护（适用于无状态 API）
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 设置会话管理策略为无状态（不使用 HttpSession）
                .and()
                .authorizeRequests() // 配置请求授权规则
                    .antMatchers(
                        "/auth/**", // 允许所有用户访问认证相关接口
                        "/user/email/**", // 允许所有用户访问邮箱相关接口
                        "/docs/**", // 允许所有用户访问文档接口
                        "/swagger-ui/**", // 允许所有用户访问 Swagger UI 接口
                        "/v3/api-docs/**", // 允许所有用户访问 OpenAPI 文档接口
                        "/webjars/**" // 允许所有用户访问 webjars 相关资源
                    ).permitAll() // 上述路径允许所有用户访问
                    .anyRequest().authenticated(); // 所有其他请求需要经过身份验证

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 在用户名/密码认证过滤器之前添加 JWT 认证过滤器
    }

    /**
     * 创建并返回一个 BCrypt 密码编码器。
     *
     * @return PasswordEncoder 对象，用于加密和验证密码
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 使用 BCrypt 算法进行密码加密
    }

    /**
     * 配置 CORS（跨域资源共享）策略。
     *
     * @return CorsConfigurationSource 对象，用于提供 CORS 配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*")); // 允许所有来源（在生产环境中应限制为特定来源）
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 允许的 HTTP 方法
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token")); // 允许的请求头
        configuration.setExposedHeaders(Arrays.asList("x-auth-token")); // 暴露的响应头
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 将 CORS 配置应用到所有路径
        return source;
    }
}
