package com.dockpilot.common.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class AppConfig {
    private String proxyUrl;
    private String mirrorUrls;
}