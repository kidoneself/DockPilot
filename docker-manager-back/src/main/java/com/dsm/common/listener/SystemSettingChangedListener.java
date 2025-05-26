package com.dsm.common.listener;

import com.alibaba.fastjson.JSONObject;
import com.dsm.common.config.AppConfig;
import com.dsm.common.event.SystemSettingChangedEvent;
import com.dsm.service.http.impl.ImageServiceImpl;
import com.dsm.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SystemSettingChangedListener {

    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private ImageServiceImpl imageService;

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
            } catch (Exception e) {
                LogUtil.logSysError("处理代理配置变更失败: " + e.getMessage());
                // 异常时清除代理配置
                appConfig.setProxyUrl(null);
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
    }
} 