package com.dsm.loader;

import com.alibaba.fastjson.JSONObject;
import com.dsm.config.AppConfig;
import com.dsm.service.SystemSettingService;
import com.dsm.utils.LogUtil;
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
        String url = jsonObject.getString("url");
        if (url != null && !url.isBlank()) {
            appConfig.setProxyUrl(url); // ✅ 设置到全局字段
            LogUtil.logSysInfo("已设置系统 HTTP 代理: " + proxyUrl);
        }
    }
}