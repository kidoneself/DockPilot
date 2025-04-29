package com.dsm.listener;

import com.alibaba.fastjson.JSONObject;
import com.dsm.config.AppConfig;
import com.dsm.event.SystemSettingChangedEvent;
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
            LogUtil.logSysInfo("未设置HTTP代理，跳过代理设置");
            return;
        }
        JSONObject jsonObject = JSONObject.parseObject(newValue);
        String url = jsonObject.getString("url");
        if (!url.isBlank()) {
            appConfig.setProxyUrl(url); // ✅ 设置到全局字段
            LogUtil.logSysInfo("已设置系统 HTTP 代理: " + url);
        } else {
            LogUtil.logSysInfo("未设置HTTP代理，跳过代理设置");
        }
    }
} 