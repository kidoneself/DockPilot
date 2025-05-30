package com.dockpilot.controller;

import com.dockpilot.model.application.ApplicationParseResult;
import com.dockpilot.model.application.dto.ApplicationSaveRequest;
import com.dockpilot.model.application.dto.ApplicationInstallInfo;
import com.dockpilot.model.application.dto.ApplicationDeployRequest;
import com.dockpilot.model.application.dto.ApplicationDeployResult;
import com.dockpilot.model.application.dto.ImageStatusInfo;
import com.dockpilot.model.application.dto.PullImageRequest;
import com.dockpilot.model.application.vo.ApplicationVO;
import com.dockpilot.service.ApplicationService;
import com.dockpilot.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 应用中心控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/applications")
@Tag(name = "应用中心", description = "应用中心管理接口")
@Validated
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    /**
     * 获取应用列表
     */
    @GetMapping
    @Operation(summary = "获取应用列表")
    public ApiResponse<List<ApplicationVO>> getApplications(@RequestParam(required = false) String category, @RequestParam(required = false) String keyword) {

        List<ApplicationVO> applications = applicationService.getApplications(category, keyword);
        return ApiResponse.success(applications);
    }

    /**
     * 根据ID获取应用详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取应用详情")
    public ApiResponse<ApplicationVO> getApplicationById(@PathVariable Long id) {
        ApplicationVO application = applicationService.getApplicationById(id);
        if (application == null) {
            return ApiResponse.error("应用不存在");
        }

        return ApiResponse.success(application);
    }

    /**
     * 保存应用 (从容器或导入YAML)
     */
    @PostMapping
    @Operation(summary = "保存应用")
    public ApiResponse<ApplicationVO> saveApplication(@Valid @RequestBody ApplicationSaveRequest request) {
        try {
            ApplicationVO savedApp = applicationService.saveApplication(request);
            return ApiResponse.success(savedApp);

        } catch (Exception e) {
            log.error("保存应用失败：{}", e.getMessage(), e);
            return ApiResponse.error("保存应用失败：" + e.getMessage());
        }
    }

    /**
     * 删除应用
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除应用")
    public ApiResponse<Void> deleteApplication(@PathVariable Long id) {
        try {
            applicationService.deleteApplication(id);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("删除应用失败：{}", e.getMessage(), e);
            return ApiResponse.error("删除应用失败：" + e.getMessage());
        }
    }

    /**
     * 分享应用 (获取YAML内容)
     */
    @GetMapping("/{id}/share")
    @Operation(summary = "分享应用")
    public ApiResponse<String> shareApplication(@PathVariable Long id) {
        try {
            String yamlContent = applicationService.shareApplication(id);
            return ApiResponse.success(yamlContent);
        } catch (Exception e) {
            log.error("分享应用失败：{}", e.getMessage(), e);
            return ApiResponse.error("分享应用失败：" + e.getMessage());
        }
    }

    /**
     * 获取分类列表
     */
    @GetMapping("/categories")
    @Operation(summary = "获取分类列表")
    public ApiResponse<List<String>> getCategories() {
        List<String> categories = applicationService.getCategories();
        return ApiResponse.success(categories);
    }

    /**
     * 解析应用配置
     */
    @PostMapping("/parse")
    @Operation(summary = "解析应用配置", description = "解析YAML配置，返回应用详细信息")
    public ApiResponse<ApplicationParseResult> parseApplication(@RequestBody ParseRequest request) {
        try {
            ApplicationParseResult result = applicationService.parseApplication(request.getYamlContent());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("解析应用配置失败: {}", e.getMessage());
            return ApiResponse.error("解析失败: " + e.getMessage());
        }
    }

    /**
     * 获取应用安装信息
     */
    @GetMapping("/{id}/install-info")
    @Operation(summary = "获取应用安装信息", description = "获取应用的详细安装信息，包括镜像、环境变量等")
    public ApiResponse<ApplicationInstallInfo> getInstallInfo(@PathVariable Long id) {
        try {
            ApplicationInstallInfo installInfo = applicationService.getInstallInfo(id);
            return ApiResponse.success(installInfo);
        } catch (Exception e) {
            log.error("获取应用安装信息失败: {}", e.getMessage());
            return ApiResponse.error("获取应用安装信息失败: " + e.getMessage());
        }
    }

    /**
     * 部署应用
     */
    @PostMapping("/{id}/deploy")
    @Operation(summary = "部署应用", description = "根据配置参数部署应用")
    public ApiResponse<ApplicationDeployResult> deployApplication(
            @PathVariable Long id, 
            @Valid @RequestBody ApplicationDeployRequest request) {
        try {
            ApplicationDeployResult result = applicationService.deployApplication(id, request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("部署应用失败: {}", e.getMessage());
            return ApiResponse.error("部署应用失败: " + e.getMessage());
        }
    }

    /**
     * 检查镜像状态
     */
    @PostMapping("/check-images")
    @Operation(summary = "检查镜像状态", description = "批量检查镜像是否存在")
    public ApiResponse<List<ImageStatusInfo>> checkImages(@RequestBody List<String> imageNames) {
        try {
            List<ImageStatusInfo> result = applicationService.checkImages(imageNames);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("检查镜像状态失败: {}", e.getMessage());
            return ApiResponse.error("检查镜像状态失败: " + e.getMessage());
        }
    }

    /**
     * 拉取镜像
     */
    @PostMapping("/pull-image")
    @Operation(summary = "拉取镜像", description = "拉取指定的Docker镜像")
    public ApiResponse<String> pullImage(@RequestBody PullImageRequest request) {
        try {
            String result = applicationService.pullImage(request.getImageName());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("拉取镜像失败: {}", e.getMessage());
            return ApiResponse.error("拉取镜像失败: " + e.getMessage());
        }
    }

    /**
     * 解析请求DTO
     */
    @Data
    public static class ParseRequest {
        private String yamlContent;
    }
} 