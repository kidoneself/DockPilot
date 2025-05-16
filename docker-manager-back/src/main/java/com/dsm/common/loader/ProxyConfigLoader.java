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
        JSONObject jsonObject = JSONObject.parseObject(proxyUrl);
        if (jsonObject != null && jsonObject.containsKey("url")) {
            String url = jsonObject.getString("url");
            if (url != null && !url.isBlank()) {
                appConfig.setProxyUrl(url); // ✅ 设置到全局字段
                log.info("✅已设置系统 HTTP 代理:{} ", proxyUrl);
            }
        }
    }
}