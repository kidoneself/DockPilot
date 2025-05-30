package com.dockpilot.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * 宿主机地址检测工具
 * 自动检测容器访问宿主机的最佳IP地址
 */
@Slf4j
@Component
public class HostDetector {
    
    private String hostIP;
    private List<String> candidateIPs = new ArrayList<>();
    
    @PostConstruct
    public void detectHostIP() {
        this.hostIP = detectHostAddress();
        log.info("宿主机地址检测完成: {} (候选地址: {})", this.hostIP, candidateIPs);
    }
    
    private String detectHostAddress() {
        // 检测候选地址
        candidateIPs.clear();
        
        // 1. 检查Docker特殊地址
        candidateIPs.add("host.docker.internal");
        candidateIPs.add("gateway.docker.internal");
        
        // 2. 检查默认网关（从路由表获取）
        String gatewayIP = getDefaultGateway();
        if (gatewayIP != null) {
            candidateIPs.add(gatewayIP);
        }
        
        // 3. 添加常见Docker网桥地址
        candidateIPs.add("172.17.0.1");
        candidateIPs.add("172.18.0.1");
        candidateIPs.add("172.19.0.1");
        
        // 4. 添加本地地址
        candidateIPs.add("127.0.0.1");
        candidateIPs.add("localhost");
        
        // 测试每个候选地址的连通性
        for (String candidate : candidateIPs) {
            if (testConnectivity(candidate)) {
                log.info("✅ 选择宿主机地址: {} (连通性测试通过)", candidate);
                return candidate;
            }
        }
        
        // 如果都不通，使用默认地址
        String defaultIP = "172.17.0.1";
        log.warn("⚠️ 所有候选地址连通性测试都失败，使用默认地址: {}", defaultIP);
        return defaultIP;
    }
    
    /**
     * 从路由表获取默认网关
     */
    private String getDefaultGateway() {
        try {
            ProcessBuilder pb = new ProcessBuilder("route", "-n");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("0.0.0.0")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) {
                            String gateway = parts[1];
                            log.debug("从路由表获取默认网关: {}", gateway);
                            return gateway;
                        }
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            log.debug("获取默认网关失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 测试地址连通性
     */
    private boolean testConnectivity(String address) {
        try {
            // 尝试连接常见的服务端口（SSH）
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(address, 22), 2000);
                log.debug("✅ {} 连通性测试通过 (SSH端口)", address);
                return true;
            }
        } catch (Exception e) {
            log.debug("❌ {} 连通性测试失败: {}", address, e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 获取检测到的宿主机IP
     */
    public String getHostIP() {
        return hostIP;
    }
    
    /**
     * 获取所有候选IP地址
     */
    public List<String> getCandidateIPs() {
        return new ArrayList<>(candidateIPs);
    }
    
    /**
     * 重新检测宿主机地址（用于故障恢复）
     */
    public void refresh() {
        String oldIP = this.hostIP;
        this.hostIP = detectHostAddress();
        if (!oldIP.equals(this.hostIP)) {
            log.info("宿主机地址已更新: {} -> {}", oldIP, this.hostIP);
        }
    }
} 