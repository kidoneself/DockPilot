package com.dockpilot.common.loader;

import com.alibaba.fastjson.JSONObject;
import com.dockpilot.common.config.AppConfig;
import com.dockpilot.service.http.SystemSettingService;
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
        // 加载代理配置
        String proxyUrl = systemSettingService.get("proxy");
        if (proxyUrl != null && !proxyUrl.isBlank()) {
            appConfig.setProxyUrl(proxyUrl.trim());
            log.info("✅已设置系统 HTTP 代理: {}", proxyUrl.trim());
        } else {
            log.info("未配置系统 HTTP 代理");
        }
        
        // 加载镜像加速地址配置
        String mirrorUrls = systemSettingService.get("mirror_urls");
        if (mirrorUrls != null && !mirrorUrls.isBlank()) {
            appConfig.setMirrorUrls(mirrorUrls.trim());
            log.info("✅已设置镜像加速地址: {}", mirrorUrls.trim().replace("\n", ", "));
        } else {
            log.info("未配置镜像加速地址");
        }
        
        // 加载Docker运行目录配置
        String dockerBaseDir = systemSettingService.get("docker_base_dir");
        if (dockerBaseDir != null && !dockerBaseDir.isBlank()) {
            appConfig.setDockerBaseDir(dockerBaseDir.trim());
            log.info("✅已设置Docker运行目录: {}", dockerBaseDir.trim());
        } else {
            // 未配置Docker运行目录，提示用户设置
            log.warn("⚠️ 未配置Docker运行目录，请在系统设置中配置Docker运行目录");
            appConfig.setDockerBaseDir(null);
        }
    }
}