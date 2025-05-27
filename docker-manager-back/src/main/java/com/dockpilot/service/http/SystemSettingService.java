package com.dockpilot.service.http;

import java.util.Map;

public interface SystemSettingService {
    Map<String, Long> testProxyLatency();

    Map<String, Long> testProxyLatency(String proxyUrl);

    String get(String key);

    void set(String key, String value);

}