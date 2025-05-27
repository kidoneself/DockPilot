package com.dockpilot.service.http.impl;

import com.dockpilot.api.DockerService;
import com.dockpilot.model.SystemStatusDTO;
import com.dockpilot.service.http.SystemStatusService;
import com.dockpilot.utils.LogUtil;
import com.dockpilot.utils.SystemInfoUtil;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemStatusServiceImpl implements SystemStatusService {

    @Autowired
    private DockerService dockerService;

    @Override
    public SystemStatusDTO getSystemStatus() {
        try {
            // 获取基础系统信息
            SystemStatusDTO systemStatus = getBasicSystemInfo();
            
            // 获取Docker相关信息
            enrichWithDockerInfo(systemStatus);
            
            return systemStatus;
        } catch (Exception e) {
            LogUtil.logSysError("获取系统状态失败: " + e.getMessage());
            return createFallbackSystemStatus();
        }
    }

    /**
     * 获取基础系统信息（使用纯Java代码）
     */
    private SystemStatusDTO getBasicSystemInfo() {
        // 获取基础系统信息
        String hostname = SystemInfoUtil.getHostname();
        String os = SystemInfoUtil.getOsInfo();
        String kernel = SystemInfoUtil.getKernelVersion();
        String uptime = SystemInfoUtil.getUptime();
        int cpuCores = SystemInfoUtil.getCpuCores();
        String cpuModel = SystemInfoUtil.getCpuModel();
        
        // 获取内存信息
        SystemInfoUtil.MemoryInfo memoryInfo = SystemInfoUtil.getMemoryInfo();
        
        // 获取磁盘信息
        SystemInfoUtil.DiskInfo diskInfo = SystemInfoUtil.getDiskInfo();
        
        // 获取网络信息
        SystemInfoUtil.NetworkInfo networkInfo = SystemInfoUtil.getNetworkInfo();
        
        // 获取CPU使用率
        Double cpuUsage = SystemInfoUtil.getCpuUsage();
        
        // 获取网络速度
        SystemInfoUtil.NetworkSpeedInfo networkSpeed = SystemInfoUtil.getNetworkSpeed();
        
        return SystemStatusDTO.builder()
                .hostname(hostname)
                .os(os)
                .kernel(kernel)
                .uptime(uptime)
                .cpuCores(cpuCores)
                .cpuModel(cpuModel)
                .cpuUsage(cpuUsage)
                .memoryTotal(memoryInfo.getTotal())
                .memoryUsed(memoryInfo.getUsed())
                .memoryUsage(memoryInfo.getUsagePercent())
                .diskUsage(diskInfo.getUsagePercent())
                .diskFree(diskInfo.getFreeSpace())
                .ipAddress(networkInfo.getIpAddress())
                .gateway(networkInfo.getGateway())
                .networkDownloadSpeed(networkSpeed.getDownloadSpeedFormatted())
                .networkUploadSpeed(networkSpeed.getUploadSpeedFormatted())
                .networkDownloadSpeedRaw(networkSpeed.getDownloadSpeedRaw())
                .networkUploadSpeedRaw(networkSpeed.getUploadSpeedRaw())
                .build();
    }

    /**
     * 补充Docker相关信息
     */
    private void enrichWithDockerInfo(SystemStatusDTO systemStatus) {
        try {
            // 获取容器信息
            List<Container> containers = dockerService.listContainers();
            long runningContainers = containers.stream()
                    .mapToLong(c -> "running".equals(c.getState()) ? 1 : 0)
                    .sum();
            
            systemStatus.setRunningContainers((int) runningContainers);
            systemStatus.setTotalContainers(containers.size());
            
            // 获取镜像数量
            List<Image> images = dockerService.listImages();
            systemStatus.setTotalImages(images.size());
            
            // 获取网络数量
            List<Network> networks = dockerService.listNetworks();
            systemStatus.setTotalNetworks(networks.size());
            
            // 获取Docker版本
            try {
                String dockerVersion = dockerService.getDockerVersion();
                systemStatus.setDockerVersion(dockerVersion);
            } catch (Exception e) {
                systemStatus.setDockerVersion("未知");
            }
            
        } catch (Exception e) {
            LogUtil.logSysError("获取Docker信息失败: " + e.getMessage());
            // 设置默认值
            systemStatus.setRunningContainers(0);
            systemStatus.setTotalContainers(0);
            systemStatus.setTotalImages(0);
            systemStatus.setTotalNetworks(0);
            systemStatus.setDockerVersion("未知");
        }
    }



    /**
     * 创建备用的系统状态（当脚本不可用时）
     */
    private SystemStatusDTO createFallbackSystemStatus() {
        LogUtil.logSysInfo("使用备用系统状态信息");
        
        SystemStatusDTO fallback = SystemStatusDTO.builder()
                .hostname("未知")
                .os("Linux")
                .kernel("未知")
                .uptime("未知")
                .cpuCores(Runtime.getRuntime().availableProcessors())
                .cpuModel("未知")
                .cpuUsage(0.0)
                .memoryTotal(0L)
                .memoryUsed(0L)
                .memoryUsage(0.0)
                .diskUsage("0%")
                .diskFree("未知")
                .ipAddress("未知")
                .gateway("未知")
                .runningContainers(0)
                .totalContainers(0)
                .totalImages(0)
                .totalNetworks(0)
                .dockerVersion("未知")
                .build();
        
        // 尝试获取Docker相关信息
        try {
            enrichWithDockerInfo(fallback);
        } catch (Exception e) {
            LogUtil.logSysError("备用模式下获取Docker信息失败: " + e.getMessage());
        }
        
        return fallback;
    }
} 