package com.dsm.service;

import com.dsm.pojo.entity.SystemSetting;

import java.util.List;
import java.util.Map;

public interface SystemSettingService {
    Map<String, Long> testProxyLatency();

    String get(String key);

    void set(String key, String value);

}