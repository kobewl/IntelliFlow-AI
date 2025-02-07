package com.kobeai.hub.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeout:5000}")
    private int timeout;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(redisPort);
            config.setDatabase(database);

            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet(); // 确保工厂正确初始化

            log.info("Redis connection factory created successfully. Host: {}, Port: {}, Database: {}",
                    redisHost, redisPort, database);

            return factory;
        } catch (Exception e) {
            log.error("Failed to create Redis connection factory: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // 使用 Jackson2JsonRedisSerializer 作为值序列化器
            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
                    redisObjectMapper());
            StringRedisSerializer stringSerializer = new StringRedisSerializer();

            // 设置key使用字符串序列化器，value使用JSON序列化器
            template.setKeySerializer(stringSerializer);
            template.setValueSerializer(jsonSerializer);
            template.setHashKeySerializer(stringSerializer);
            template.setHashValueSerializer(jsonSerializer);
            template.setStringSerializer(stringSerializer);
            template.setDefaultSerializer(jsonSerializer);

            template.afterPropertiesSet();
            log.info("Redis template configured successfully with GenericJackson2JsonRedisSerializer");

            // 测试连接
            template.getConnectionFactory().getConnection().ping();
            log.info("Redis connection test successful");

            return template;
        } catch (Exception e) {
            log.error("Failed to create Redis template: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet(); // 添加这行确保模板正确初始化
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
                    redisObjectMapper());

            // 默认配置
            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(1))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                    .disableCachingNullValues();

            // 针对不同类型的数据配置不同的过期时间
            Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
            configMap.put("users", defaultConfig.entryTtl(Duration.ofHours(24)));
            configMap.put("messages", defaultConfig.entryTtl(Duration.ofHours(2)));
            configMap.put("conversations", defaultConfig.entryTtl(Duration.ofHours(12)));
            configMap.put("announcements", defaultConfig.entryTtl(Duration.ofDays(1)));

            log.info("Redis cache manager configured successfully");
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withInitialCacheConfigurations(configMap)
                    .build();
        } catch (Exception e) {
            log.error("Failed to create Redis cache manager: {}", e.getMessage(), e);
            throw e;
        }
    }
}