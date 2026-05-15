package com.kobeai.hub.controller;

import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.service.PlatformService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.kobeai.hub.model.AIPlatform;
import java.util.List;
import java.util.Map;

@Tag(name = "AI平台管理")
@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    @Operation(summary = "获取可用平台列表")
    @GetMapping("/list")
    public ApiResponse<?> listPlatforms() {
        return platformService.listPlatforms();
    }

    @Operation(summary = "添加AI平台")
    @PostMapping("/add")
    public ApiResponse<?> addPlatform(
            @Parameter(description = "平台描述") @RequestParam String description,
            @Parameter(description = "API密钥") @RequestParam String apiKey,
            @Parameter(description = "API基础URL") @RequestParam String baseUrl) {
        return platformService.addPlatform(description, apiKey, baseUrl);
    }

    @Operation(summary = "删除AI平台")
    @DeleteMapping("/{id}")
    public ApiResponse<?> deletePlatform(
            @Parameter(description = "平台ID") @PathVariable Long id) {
        return platformService.deletePlatform(id);
    }

    @Operation(summary = "获取可用AI模型列表")
    @GetMapping("/api/ai-platforms")
    public ApiResponse<?> getAvailableAIPlatforms() {
        // 只返回 enabled=true 的 name、type、description 字段
        var platforms = platformService.listEnabledPlatformsSimple();
        return ApiResponse.success("获取成功", platforms);
    }
}