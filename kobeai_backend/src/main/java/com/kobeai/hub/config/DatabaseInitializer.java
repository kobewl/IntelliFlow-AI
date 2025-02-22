package com.kobeai.hub.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 数据库初始化类，用于在应用程序启动时检查数据库连接。
 */
@Slf4j // 使用 Lombok 的日志记录功能，自动注入一个名为 log 的日志记录器
@Component // 将该类标记为 Spring 组件，使其被 Spring 容器管理
@RequiredArgsConstructor // 使用 Lombok 生成一个全参构造函数，确保依赖注入
public class DatabaseInitializer implements CommandLineRunner {

    /**
     * 数据源对象，由 Spring 自动注入。
     * 用于获取与数据库的连接。
     */
    private final DataSource dataSource;

    /**
     * 实现 CommandLineRunner 接口的 run 方法。
     * 该方法会在应用程序启动后立即执行。
     *
     * @param args 命令行参数（通常为空）
     * @throws Exception 如果数据库连接失败，则抛出异常
     */
    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            // 获取数据库连接，并使用 try-with-resources 确保连接在使用后自动关闭
            log.info("Database connection successful"); // 记录成功连接到数据库的日志信息
            log.info("Connected to: {}", connection.getMetaData().getURL()); // 记录连接的数据库 URL
        } catch (Exception e) {
            // 捕获任何可能发生的异常，并记录错误日志
            log.error("Database connection failed", e);
            throw e; // 重新抛出异常，以便上层调用者可以处理
        }
    }
}
