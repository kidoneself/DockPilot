package com.dsm.event;

public class SystemSettingChangedEvent {
    private final String key;
    private final String oldValue;
    private final String newValue;

    public SystemSettingChangedEvent(String key, String oldValue, String newValue) {
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getKey() {
        return key;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }
} 