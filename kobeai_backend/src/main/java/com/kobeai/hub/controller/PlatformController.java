package com.kobeai.hub.controller;

import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.service.PlatformService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.kobeai.hub.model.AIPlatform;
import java.util.List;
import java.util.Map;

@Api(tags = "AI平台管理")
@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    @ApiOperation("获取可用平台列表")
    @GetMapping("/list")
    public ApiResponse<?> listPlatforms() {
        return platformService.listPlatforms();
    }

    @ApiOperation("添加AI平台")
    @PostMapping("/add")
    public ApiResponse<?> addPlatform(
            @ApiParam("平台描述") @RequestParam String description,
            @ApiParam("API密钥") @RequestParam String apiKey,
            @ApiParam("API基础URL") @RequestParam String baseUrl) {
        return platformService.addPlatform(description, apiKey, baseUrl);
    }

    @ApiOperation("删除AI平台")
    @DeleteMapping("/{id}")
    public ApiResponse<?> deletePlatform(
            @ApiParam("平台ID") @PathVariable Long id) {
        return platformService.deletePlatform(id);
    }

    @ApiOperation("获取可用AI模型列表")
    @GetMapping("/api/ai-platforms")
    public ApiResponse<?> getAvailableAIPlatforms() {
        // 只返回 enabled=true 的 name、type、description 字段
        var platforms = platformService.listEnabledPlatformsSimple();
        return ApiResponse.success("获取成功", platforms);
    }
}