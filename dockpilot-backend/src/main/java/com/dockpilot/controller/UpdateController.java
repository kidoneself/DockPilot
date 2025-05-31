// 热更新控制器
package com.dockpilot.controller;

import com.dockpilot.model.dto.UpdateInfoDTO;
import com.dockpilot.service.http.UpdateService;
import com.dockpilot.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * 系统热更新控制器
 * 提供容器内热更新功能，无需重启容器
 */
@Slf4j
@Tag(name = "系统热更新", description = "容器内热更新功能，支持前后端代码热替换")
@RestController
@RequestMapping("/update")
@SecurityRequirement(name = "JWT")
public class UpdateController {

    @Autowired
    private UpdateService updateService;

    @Operation(summary = "健康检查", description = "检查服务是否正常运行（无需认证）")
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "ok");
        health.put("service", "dockpilot-backend");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        return ApiResponse.success(health);
    }

    @Operation(summary = "检查新版本", description = "检查GitHub Releases是否有新版本可用")
    @GetMapping("/check")
    public ApiResponse<UpdateInfoDTO> checkUpdate() {
        try {
            log.info("🔍 开始检查系统更新...");
            UpdateInfoDTO updateInfo = updateService.checkForUpdates();
            
            if (updateInfo.isHasUpdate()) {
                log.info("🎉 发现新版本: {} -> {}", updateInfo.getCurrentVersion(), updateInfo.getLatestVersion());
            } else {
                log.info("✅ 当前已是最新版本: {}", updateInfo.getCurrentVersion());
            }
            
            return ApiResponse.success(updateInfo);
        } catch (Exception e) {
            log.error("❌ 检查更新失败", e);
            return ApiResponse.error("检查更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取当前版本信息", description = "获取当前应用的详细版本信息")
    @GetMapping("/version")
    public ApiResponse<Map<String, String>> getCurrentVersion() {
        try {
            Map<String, String> versionInfo = updateService.getCurrentVersionInfo();
            return ApiResponse.success(versionInfo);
        } catch (Exception e) {
            log.error("获取版本信息失败", e);
            return ApiResponse.error("获取版本信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "执行热更新", description = "下载并应用最新版本（容器内热更新，不重启容器）")
    @PostMapping("/apply")
    public ApiResponse<String> applyHotUpdate(@RequestParam(required = false) String version) {
        try {
            log.info("🚀 开始执行热更新，目标版本: {}", version != null ? version : "latest");
            String result = updateService.applyHotUpdate(version);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("❌ 执行热更新失败", e);
            return ApiResponse.error("执行热更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取更新进度", description = "获取当前更新操作的实时进度")
    @GetMapping("/progress")
    public ApiResponse<Map<String, Object>> getUpdateProgress() {
        try {
            Map<String, Object> progress = updateService.getUpdateProgress();
            return ApiResponse.success(progress);
        } catch (Exception e) {
            log.error("获取更新进度失败", e);
            return ApiResponse.error("获取更新进度失败: " + e.getMessage());
        }
    }

    @Operation(summary = "取消更新", description = "取消正在进行的更新操作")
    @PostMapping("/cancel")
    public ApiResponse<String> cancelUpdate() {
        try {
            String result = updateService.cancelUpdate();
            log.info("⏹️ 更新操作已取消");
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("取消更新失败", e);
            return ApiResponse.error("取消更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "清空版本检查缓存", description = "清空所有版本检查缓存，强制下次重新检查")
    @PostMapping("/clear-cache")
    public ApiResponse<Void> clearCache() {
        try {
            updateService.clearCache();
            log.info("🗑️ 版本检查缓存已清空");
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("清空缓存失败", e);
            return ApiResponse.error("清空缓存失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新当前版本记录", description = "手动更新当前版本记录为最新发布版本")
    @PostMapping("/update-version-record")
    public ApiResponse<String> updateCurrentVersionRecord() {
        try {
            String result = updateService.updateCurrentVersionToLatest();
            log.info("✅ 当前版本记录已更新: {}", result);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("更新版本记录失败", e);
            return ApiResponse.error("更新版本记录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "容器重启更新", description = "下载新版本文件并重启容器（推荐方式，最安全可靠）")
    @PostMapping("/apply-restart")
    public ApiResponse<String> applyContainerRestartUpdate(@RequestParam(required = false) String version) {
        try {
            log.info("🔄 开始执行容器重启更新，目标版本: {}", version != null ? version : "latest");
            String result = updateService.applyContainerRestartUpdate(version);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("❌ 执行容器重启更新失败", e);
            return ApiResponse.error("执行容器重启更新失败: " + e.getMessage());
        }
    }
} 