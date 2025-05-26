package com.dsm.controller;

import com.dsm.model.Route;
import com.dsm.model.SystemSetting;
import com.dsm.model.SystemStatusDTO;
import com.dsm.service.http.SystemSettingService;
import com.dsm.service.http.SystemStatusService;
import com.dsm.utils.ApiResponse;
import com.dsm.utils.FaviconFetcher;
import com.dsm.utils.LogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system")
@Tag(name = "系统管理", description = "系统状态和监控接口")
@SecurityRequirement(name = "JWT")
public class SystemController {

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private SystemStatusService systemStatusService;


    @Operation(summary = "设置系统配置", description = "设置系统配置项")
    @PostMapping("/settings")
    public ApiResponse<Void> setSetting(@RequestBody SystemSetting setting) {
        systemSettingService.set(setting.getKey(), setting.getValue());
        return ApiResponse.success(null);
    }

    @Operation(summary = "更新系统配置", description = "更新系统配置项")
    @PutMapping("/settings")
    public ApiResponse<Void> updateSetting(@RequestBody SystemSetting setting) {
        String oldValue = systemSettingService.get(setting.getKey());
        systemSettingService.set(setting.getKey(), setting.getValue());
        LogUtil.logSysInfo("更新系统配置: " + setting.getKey() + " 从 " + oldValue + " 更新为 " + setting.getValue());
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取系统配置", description = "获取系统配置项")
    @GetMapping("/settings")
    public ApiResponse<String> getSettings(@RequestParam String key) {
        String value = systemSettingService.get(key);
        return ApiResponse.success(value != null ? value : "");
    }

    @Operation(summary = "删除系统配置", description = "删除系统配置项")
    @DeleteMapping("/settings/{key}")
    public ApiResponse<Void> deleteSetting(@PathVariable String key) {
        String oldValue = systemSettingService.get(key);
        systemSettingService.set(key, "");
        LogUtil.logSysInfo("删除系统配置: " + key + " = " + oldValue);
        return ApiResponse.success(null);

    }

    @Operation(summary = "获取授权菜单", description = "获取授权菜单")
    @GetMapping("/getMenu")
    public ApiResponse<List<Route>> getMenu() {
        List<Route> objects = new ArrayList<>();
        objects.add(new Route());
        return ApiResponse.success(objects);
    }

    /**
     * @return 代理延迟信息
     */
    @Operation(summary = "测试代理延迟", description = "测试Docker镜像仓库的代理延迟")
    @GetMapping("/proxy/test")
    public ApiResponse<Map<String, Long>> testProxyLatency() {
        return ApiResponse.success(systemSettingService.testProxyLatency());
    }

    /**
     * 测试指定代理URL的延迟
     * @param proxyUrl 代理URL
     * @return 代理延迟信息
     */
    @Operation(summary = "测试指定代理延迟", description = "测试指定代理URL的延迟，不会影响当前代理配置")
    @PostMapping("/proxy/test")
    public ApiResponse<Map<String, Long>> testProxyLatency(@RequestParam String proxyUrl) {
        return ApiResponse.success(systemSettingService.testProxyLatency(proxyUrl));
    }

    @Operation(summary = "获取网站Logo", description = "获取指定网站的favicon图标URL")
    @GetMapping("/favicon")
    public ApiResponse<String> getFavicon(@RequestParam String url) {
        String faviconUrl = FaviconFetcher.getFavicon(url);
        return ApiResponse.success(faviconUrl);

    }

    @Operation(summary = "获取系统状态", description = "获取宿主机系统状态信息")
    @GetMapping("/status")
    public ApiResponse<SystemStatusDTO> getSystemStatus() {
        try {
            SystemStatusDTO systemStatus = systemStatusService.getSystemStatus();
            return ApiResponse.success(systemStatus);
        } catch (Exception e) {
            LogUtil.logSysError("获取系统状态失败: " + e.getMessage());
            return ApiResponse.error("获取系统状态失败: " + e.getMessage());
        }
    }


}