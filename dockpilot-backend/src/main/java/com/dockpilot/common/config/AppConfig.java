package com.dockpilot.common.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class AppConfig {
    private String proxyUrl;
    private String mirrorUrls;
    private String dockerBaseDir; // Docker运行目录，用户必须设置
    
    /**
     * 检查Docker运行目录是否已配置
     * @return true如果已配置，false如果未配置
     */
    public boolean isDockerBaseDirConfigured() {
        return dockerBaseDir != null && !dockerBaseDir.trim().isEmpty();
    }
    
    /**
     * 获取Docker运行目录，如果未配置则抛出异常
     * @return Docker运行目录路径
     * @throws IllegalStateException 如果Docker运行目录未配置
     */
    public String getDockerBaseDirOrThrow() {
        if (!isDockerBaseDirConfigured()) {
            throw new IllegalStateException("Docker运行目录未配置，请在系统设置中配置Docker运行目录");
        }
        return dockerBaseDir.trim();
    }
}