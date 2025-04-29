package com.dsm.pojo.entity;

import lombok.Data;

@Data
public class SystemStats {
    private int cpuUsage;
    private int memoryUsage;
    private int diskUsage;
    private long timestamp;

    public SystemStats(int cpuUsage, int memoryUsage, int diskUsage, long timestamp) {
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.diskUsage = diskUsage;
        this.timestamp = timestamp;
    }
} 