package com.dsm.common.loader;

import com.alibaba.fastjson.JSONObject;
import com.dsm.common.config.AppConfig;
import com.dsm.service.http.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Slf4j
@Component
public class ProxyConfigLoader {

    @Resource
    private SystemSettingService systemSettingService;

    @Autowired
    private AppConfig appConfig;

    @PostConstruct
    public void initProxy() {
        String proxyUrl = systemSettingService.get("proxy");
        if (proxyUrl != null && !proxyUrl.isBlank()) {
            appConfig.setProxyUrl(proxyUrl.trim()); // 直接设置URL字符串
            log.info("✅已设置系统 HTTP 代理: {}", proxyUrl.trim());
        } else {
            log.info("未配置系统 HTTP 代理");
        }
    }
}