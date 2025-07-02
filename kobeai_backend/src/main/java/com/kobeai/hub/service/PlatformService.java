package com.kobeai.hub.service;

import com.kobeai.hub.dto.response.ApiResponse;
import java.util.List;
import java.util.Map;

public interface PlatformService {
    ApiResponse<?> addPlatform(String description, String apiKey, String baseUrl);
    ApiResponse<?> listPlatforms();
    ApiResponse<?> deletePlatform(Long id);
    List<Map<String, Object>> listEnabledPlatformsSimple();
}