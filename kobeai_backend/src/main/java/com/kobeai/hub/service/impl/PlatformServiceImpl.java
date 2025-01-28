package com.kobeai.hub.service.impl;

import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.AIPlatform;
import com.kobeai.hub.model.Platform;
import com.kobeai.hub.repository.AIPlatformRepository;
import com.kobeai.hub.service.PlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformServiceImpl implements PlatformService {

    private final AIPlatformRepository platformRepository;

    @PostConstruct
    @Transactional
    public void init() {
        try {
            // 检查是否已存在DeepSeek平台配置
            Optional<AIPlatform> existingPlatform = platformRepository.findByType(Platform.DEEPSEEK);
            if (!existingPlatform.isPresent()) {
                // 创建默认的DeepSeek平台配置
                AIPlatform platform = new AIPlatform();
                platform.setType(Platform.DEEPSEEK);
                platform.setName("DeepSeek AI");
                platform.setDescription("DeepSeek AI");
                platform.setApiKey("sk-ae9973eaca5748ed90a87a4e4e4784e6");
                platform.setBaseUrl("https://api.deepseek.com/v1");
                platform.setEnabled(true);
                platform.setCreatedAt(LocalDateTime.now());
                
                // 保存并获取保存后的实体
                AIPlatform savedPlatform = platformRepository.saveAndFlush(platform);
                log.info("DeepSeek平台配置已初始化, ID: {}, Type: {}", savedPlatform.getId(), savedPlatform.getType());
            } else {
                log.info("DeepSeek平台配置已存在, ID: {}, Type: {}", existingPlatform.get().getId(), existingPlatform.get().getType());
            }
        } catch (Exception e) {
            log.error("初始化平台配置失败: {}", e.getMessage(), e);
            // 重新抛出异常以确保Spring容器知道初始化失败
            throw new RuntimeException("初始化平台配置失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> addPlatform(String description, String apiKey, String baseUrl) {
        try {
            AIPlatform platform = new AIPlatform();
            platform.setType(Platform.DEEPSEEK);
            platform.setName(description);
            platform.setDescription(description);
            platform.setApiKey(apiKey);
            platform.setBaseUrl(baseUrl);
            platform.setEnabled(true);
            platform.setCreatedAt(LocalDateTime.now());
            platformRepository.save(platform);
            return ApiResponse.success("添加成功");
        } catch (Exception e) {
            log.error("添加平台失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    public ApiResponse<?> listPlatforms() {
        try {
            List<AIPlatform> platforms = platformRepository.findByEnabledTrue();
            return ApiResponse.success("获取成功", platforms);
        } catch (Exception e) {
            log.error("获取平台列表失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> deletePlatform(Long id) {
        try {
            platformRepository.deleteById(id);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            log.error("删除平台失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }
}