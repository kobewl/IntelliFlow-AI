package com.kobeai.hub.service;

import com.kobeai.hub.dto.response.ApiResponse;

public interface PlatformService {
    ApiResponse<?> addPlatform(String description, String apiKey, String baseUrl);
    ApiResponse<?> listPlatforms();
    ApiResponse<?> deletePlatform(Long id);
}