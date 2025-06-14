package com.dockpilot.common.listener;

import com.alibaba.fastjson.JSONObject;
import com.dockpilot.common.config.AppConfig;
import com.dockpilot.common.event.SystemSettingChangedEvent;
import com.dockpilot.service.http.impl.ImageServiceImpl;
import com.dockpilot.service.http.ProxyHttpClientService;
import com.dockpilot.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SystemSettingChangedListener {

    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private ImageServiceImpl imageService;
    
    @Autowired
    private ProxyHttpClientService proxyHttpClientService;

    @EventListener
    public void handleSystemSettingChanged(SystemSettingChangedEvent event) {
        String key = event.getKey();
        String newValue = event.getNewValue();
        
        if ("proxy".equals(key)) {
            try {
                if (newValue != null && !newValue.isBlank()) {
                    // 直接使用URL字符串作为代理配置
                    appConfig.setProxyUrl(newValue.trim());
                    LogUtil.logSysInfo("✅已设置系统 HTTP 代理: " + newValue.trim());
                } else {
                    // 配置值为空，清除代理
                    appConfig.setProxyUrl(null);
                    LogUtil.logSysInfo("✅已清除系统 HTTP 代理");
                }
                
                // 🔥 重要：清除ProxyHttpClientService缓存，强制重新创建HttpClient
                proxyHttpClientService.clearCache();
                LogUtil.logSysInfo("🔄 代理HttpClient缓存已清除，下次请求将使用新的代理配置");
                
            } catch (Exception e) {
                LogUtil.logSysError("处理代理配置变更失败: " + e.getMessage());
                // 异常时清除代理配置
                appConfig.setProxyUrl(null);
                // 也要清除缓存，确保状态一致
                proxyHttpClientService.clearCache();
            }
        }
        
        if ("mirror_urls".equals(key)) {
            try {
                if (newValue != null && !newValue.isBlank()) {
                    appConfig.setMirrorUrls(newValue.trim());
                    LogUtil.logSysInfo("✅已设置镜像加速地址: " + newValue.trim().replace("\n", ", "));
                } else {
                    // 配置值为空，清除镜像加速
                    appConfig.setMirrorUrls(null);
                    LogUtil.logSysInfo("✅已清除镜像加速地址");
                }
            } catch (Exception e) {
                LogUtil.logSysError("处理镜像加速配置变更失败: " + e.getMessage());
                // 异常时清除镜像加速配置
                appConfig.setMirrorUrls(null);
            }
        }
        
        // 🎯 处理镜像检查间隔配置变更
        if ("imageCheckInterval".equals(key)) {
            try {
                if (newValue != null && !newValue.isBlank()) {
                    imageService.updateImageCheckIntervalFromEvent(newValue.trim());
                } else {
                    // 配置值为空，使用默认值60分钟
                    imageService.updateImageCheckIntervalFromEvent("60");
                    LogUtil.logSysInfo("✅镜像检查间隔配置为空，已重置为默认值60分钟");
                }
            } catch (Exception e) {
                LogUtil.logSysError("处理镜像检查间隔配置变更失败: " + e.getMessage());
                // 异常时使用默认值
                try {
                    imageService.updateImageCheckIntervalFromEvent("60");
                    LogUtil.logSysInfo("✅异常时已重置镜像检查间隔为默认值60分钟");
                } catch (Exception ex) {
                    LogUtil.logSysError("重置镜像检查间隔为默认值也失败: " + ex.getMessage());
                }
            }
        }

        if ("docker_base_dir".equals(key)) {
            try {
                if (newValue != null && !newValue.isBlank()) {
                    // 设置Docker运行目录
                    appConfig.setDockerBaseDir(newValue.trim());
                    LogUtil.logSysInfo("✅已设置Docker运行目录: " + newValue.trim());
                } else {
                    // 配置值为空，提示用户设置
                    appConfig.setDockerBaseDir(null);
                    LogUtil.logSysInfo("⚠️ Docker运行目录配置为空，请在系统设置中配置Docker运行目录");
                }
            } catch (Exception e) {
                LogUtil.logSysError("处理Docker运行目录配置变更失败: " + e.getMessage());
                // 异常时设置为null
                appConfig.setDockerBaseDir(null);
                LogUtil.logSysInfo("⚠️ Docker运行目录配置异常，已清空，请重新配置");
            }
        }
    }
} 