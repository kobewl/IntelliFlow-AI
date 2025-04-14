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