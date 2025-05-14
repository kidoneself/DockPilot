package com.dsm.service.http;

import java.util.Map;

public interface SystemSettingService {
    Map<String, Long> testProxyLatency();

    String get(String key);

    void set(String key, String value);

}