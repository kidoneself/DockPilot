package com.dsm.common.listener;

import com.alibaba.fastjson.JSONObject;
import com.dsm.common.config.AppConfig;
import com.dsm.common.event.SystemSettingChangedEvent;
import com.dsm.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SystemSettingChangedListener {

    @Autowired
    private AppConfig appConfig;

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
    }
} 