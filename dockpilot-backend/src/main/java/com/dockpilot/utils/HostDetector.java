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
        
        // 1. 优先获取宿主机真实网络IP（通过挂载的/mnt/host访问宿主机网络信息）
        List<String> hostRealIPs = getHostRealNetworkIPs();
        candidateIPs.addAll(hostRealIPs);
        
        // 2. 检查Docker特殊地址（备用）
        candidateIPs.add("host.docker.internal");
        candidateIPs.add("gateway.docker.internal");
        
        // 3. 检查默认网关（从路由表获取）
        String gatewayIP = getDefaultGateway();
        if (gatewayIP != null) {
            candidateIPs.add(gatewayIP);
        }
        
        // 4. 添加常见Docker网桥地址（最后备用）
        candidateIPs.add("172.17.0.1");
        candidateIPs.add("172.18.0.1");
        candidateIPs.add("172.19.0.1");
        
        // 5. 添加本地地址（兜底）
        candidateIPs.add("127.0.0.1");
        candidateIPs.add("localhost");
        
        // 直接返回第一个真实的宿主机IP，无需连通性测试
        for (String candidate : candidateIPs) {
            if (isRealHostIP(candidate)) {
                log.info("✅ 选择宿主机真实IP: {} (局域网可访问)", candidate);
                return candidate;
            }
        }
        
        // 如果没有真实IP，回退到连通性测试
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
     * 获取宿主机真实网络IP地址
     * 通过读取宿主机的网络接口信息（DockPilot容器挂载了/mnt/host）
     */
    private List<String> getHostRealNetworkIPs() {
        List<String> hostIPs = new ArrayList<>();
        
        try {
            // 通过读取宿主机的网络接口信息
            // DockPilot容器通常挂载了 -v /:/mnt/host
            ProcessBuilder pb = new ProcessBuilder("cat", "/mnt/host/proc/net/route");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                reader.readLine(); // 跳过标题行
                
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 8 && "00000000".equals(parts[1])) { // 默认路由
                        String gatewayHex = parts[2];
                        if (gatewayHex.length() == 8) {
                            // 将十六进制网关地址转换为IP
                            String gatewayIP = hexToIP(gatewayHex);
                            if (gatewayIP != null) {
                                // 根据网关推测宿主机IP段，获取宿主机IP
                                List<String> possibleIPs = getHostIPsFromGateway(gatewayIP);
                                hostIPs.addAll(possibleIPs);
                            }
                        }
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            log.debug("读取宿主机网络信息失败: {}", e.getMessage());
        }
        
        // 备用方法：通过hostname命令获取
        try {
            ProcessBuilder pb2 = new ProcessBuilder("sh", "-c", "chroot /mnt/host hostname -I");
            Process process2 = pb2.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process2.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    String[] ips = line.trim().split("\\s+");
                    for (String ip : ips) {
                        if (isRealHostIP(ip.trim())) {
                            hostIPs.add(ip.trim());
                            log.debug("通过hostname获取到宿主机IP: {}", ip.trim());
                        }
                    }
                }
            }
            
            process2.waitFor();
        } catch (Exception e) {
            log.debug("通过hostname获取宿主机IP失败: {}", e.getMessage());
        }
        
        log.debug("检测到宿主机真实IP: {}", hostIPs);
        return hostIPs;
    }
    
    /**
     * 将十六进制IP转换为点分十进制
     */
    private String hexToIP(String hex) {
        try {
            if (hex.length() != 8) return null;
            
            int ip = Integer.parseUnsignedInt(hex, 16);
            return String.format("%d.%d.%d.%d", 
                (ip & 0xFF), 
                (ip >> 8) & 0xFF, 
                (ip >> 16) & 0xFF, 
                (ip >> 24) & 0xFF);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 根据网关IP推测可能的宿主机IP
     */
    private List<String> getHostIPsFromGateway(String gatewayIP) {
        List<String> possibleIPs = new ArrayList<>();
        
        try {
            String[] parts = gatewayIP.split("\\.");
            if (parts.length == 4) {
                String subnet = parts[0] + "." + parts[1] + "." + parts[2] + ".";
                
                // 常见的宿主机IP（路由器通常是.1，宿主机可能是.2-.254）
                for (int i = 2; i <= 254; i++) {
                    if (i != Integer.parseInt(parts[3])) { // 跳过网关本身
                        possibleIPs.add(subnet + i);
                        if (possibleIPs.size() >= 10) break; // 限制数量
                    }
                }
            }
        } catch (Exception e) {
            log.debug("根据网关推测宿主机IP失败: {}", e.getMessage());
        }
        
        return possibleIPs;
    }
    
    /**
     * 判断是否为真实的宿主机IP（局域网私有IP）
     */
    private boolean isRealHostIP(String ip) {
        if (ip == null || ip.trim().isEmpty()) return false;
        
        try {
            String[] parts = ip.trim().split("\\.");
            if (parts.length != 4) return false;
            
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            
            // 私有IP地址段
            // 192.168.0.0/16
            if (first == 192 && second == 168) return true;
            
            // 10.0.0.0/8  
            if (first == 10) return true;
            
            // 172.16.0.0/12
            if (first == 172 && second >= 16 && second <= 31) return true;
            
            // 排除Docker内部网络
            if (first == 172 && second == 17) return false; // Docker默认网桥
            if (first == 172 && second >= 18 && second <= 23) return false; // Docker常见网络
            
            return false;
        } catch (Exception e) {
            return false;
        }
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