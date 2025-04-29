package com.dsm.utils;

import com.dsm.model.dto.ResourceUsageDTO;
import com.github.dockerjava.api.model.CpuStatsConfig;
import com.github.dockerjava.api.model.MemoryStatsConfig;
import com.github.dockerjava.api.model.StatisticNetworksConfig;
import com.github.dockerjava.api.model.Statistics;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class DockerStatsConverter {

    public ResourceUsageDTO convert(Statistics stats) {
        ResourceUsageDTO dto = new ResourceUsageDTO();

        // CPU 使用率计算
        double cpuPercent = calculateCPUPercent(stats.getCpuStats(), stats.getPreCpuStats());
        dto.setCpuPercent(cpuPercent);

        // 内存使用和限制
        MemoryStatsConfig mem = stats.getMemoryStats();
        dto.setMemoryUsage(mem != null ? mem.getUsage() : Long.valueOf(0L));
        dto.setMemoryLimit(mem != null ? mem.getLimit() : Long.valueOf(0L));

        // 网络流量统计
        Map<String, StatisticNetworksConfig> networksMap = stats.getNetworks(); // 正确类型
        long rx = 0L, tx = 0L;
        if (networksMap != null) {
            for (StatisticNetworksConfig netStats : networksMap.values()) {
                if (netStats != null) {
                    rx += toLong(netStats.getRxBytes());
                    tx += toLong(netStats.getTxBytes());
                }
            }
        }
        dto.setNetworkRx(rx);
        dto.setNetworkTx(tx);

        // 容器状态
        dto.setRunning(stats.getRead() != null);

        return dto;
    }

    private double calculateCPUPercent(CpuStatsConfig current, CpuStatsConfig previous) {
        if (current == null || previous == null ||
            current.getCpuUsage() == null || previous.getCpuUsage() == null) {
            return 0.0;
        }

        long cpuDelta = getSafe(current.getCpuUsage().getTotalUsage()) -
                        getSafe(previous.getCpuUsage().getTotalUsage());
        long systemDelta = getSafe(current.getSystemCpuUsage()) -
                           getSafe(previous.getSystemCpuUsage());
        long cpuCores = current.getOnlineCpus() != null ? current.getOnlineCpus() : 1;

        if (systemDelta > 0 && cpuDelta > 0) {
            return ((double) cpuDelta / systemDelta) * cpuCores * 100.0;
        } else {
            return 0.0;
        }
    }

    private long getSafe(Long value) {
        return value != null ? value : 0L;
    }

    private long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }
}