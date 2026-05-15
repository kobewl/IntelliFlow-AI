package com.kobeai.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EntityScan(basePackages = "com.kobeai.hub.model")
@EnableJpaRepositories(basePackages = "com.kobeai.hub.repository")
@ComponentScan(basePackages = "com.kobeai.hub")
public class KobeAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(KobeAiApplication.class, args);
    }
}
