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
 * 系统更新控制器
 * 提供下载完成后重启的安全更新功能
 */
@Slf4j
@Tag(name = "系统更新", description = "下载完成后重启的安全更新功能")
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

    @Operation(summary = "开始下载新版本", description = "下载新版本文件，不立即重启")
    @PostMapping("/download")
    public ApiResponse<String> startDownload(@RequestParam(required = false) String version) {
        try {
            String targetVersion = version != null ? version : "latest";
            log.info("🚀 开始下载更新，目标版本: {}", targetVersion);
            String result = updateService.startDownload(targetVersion);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("❌ 开始下载失败", e);
            return ApiResponse.error("开始下载失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取下载状态", description = "获取当前下载任务的实时状态和进度")
    @GetMapping("/download/status")
    public ApiResponse<Map<String, Object>> getDownloadStatus() {
        try {
            Map<String, Object> status = updateService.getDownloadStatus();
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取下载状态失败", e);
            return ApiResponse.error("获取下载状态失败: " + e.getMessage());
        }
    }

    @Operation(summary = "确认重启更新", description = "下载完成后确认重启应用")
    @PostMapping("/restart")
    public ApiResponse<String> confirmRestart() {
        try {
            log.info("🔄 用户确认重启更新");
            String result = updateService.confirmRestart();
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("❌ 确认重启失败", e);
            return ApiResponse.error("确认重启失败: " + e.getMessage());
        }
    }

    @Operation(summary = "取消下载", description = "取消正在进行的下载任务")
    @PostMapping("/download/cancel")
    public ApiResponse<String> cancelDownload() {
        try {
            String result = updateService.cancelDownload();
            log.info("⏹️ 下载任务已取消");
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("取消下载失败", e);
            return ApiResponse.error("取消下载失败: " + e.getMessage());
        }
    }
} 