package com.dockpilot.common.event;

import lombok.Getter;

/**
 * 系统参数热更新
 */

@Getter
public class SystemSettingChangedEvent {
    private final String key;
    private final String oldValue;
    private final String newValue;

    public SystemSettingChangedEvent(String key, String oldValue, String newValue) {
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

}