package com.dockpilot.service;

import com.dockpilot.utils.HostDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 端口检测服务 - 使用nc命令
 * 检测宿主机端口是否被占用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortCheckService {
    
    private final HostDetector hostDetector;
    
    /**
     * 检测指定端口是否可用 - 使用nc命令
     * @param port 端口号system/favicon
     * @return true=可用, false=被占用
     */
    public boolean isPortAvailable(int port) {
        if (port <= 0 || port > 65535) {
            log.warn("无效的端口号: {}", port);
            return false;
        }
        
        String hostIP = hostDetector.getHostIP();
        log.info("使用nc检测端口 {} 可用性，宿主机IP: {}", port, hostIP);
        
        try {
            // 构建nc命令：nc -z -v -w3 host port
            ProcessBuilder pb = new ProcessBuilder("nc", "-z", "-v", "-w3", hostIP, String.valueOf(port));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                log.debug("nc命令超时 - {}:{}", hostIP, port);
                return true; // 超时认为端口可用
            }
            
            int exitCode = process.exitValue();
            // nc退出码: 0=成功连接(端口被占用), 非0=连接失败(端口可用)
            boolean available = (exitCode != 0);
            
            log.info("nc检测结果 - {}:{} = {}", hostIP, port, available ? "可用" : "被占用");
            return available;
            
        } catch (Exception e) {
            log.warn("nc命令执行失败 - {}:{}: {}", hostIP, port, e.getMessage());
            return true; // nc不可用时，保守判断为端口可用
        }
    }
    
    /**
     * 批量检测端口可用性
     * @param ports 端口数组
     * @return 端口可用性映射
     */
    public Map<Integer, Boolean> checkMultiplePorts(int[] ports) {
        Map<Integer, Boolean> result = new HashMap<>();
        
        for (int port : ports) {
            result.put(port, isPortAvailable(port));
        }
        
        return result;
    }
    
    /**
     * 查找指定范围内的可用端口
     * @param startPort 起始端口
     * @param endPort 结束端口
     * @param count 需要的端口数量
     * @return 可用端口列表
     */
    public int[] findAvailablePorts(int startPort, int endPort, int count) {
        if (startPort > endPort || count <= 0) {
            return new int[0];
        }
        
        List<Integer> availablePorts = new ArrayList<>();
        
        for (int port = startPort; port <= endPort && availablePorts.size() < count; port++) {
            if (isPortAvailable(port)) {
                availablePorts.add(port);
            }
        }
        
        return availablePorts.stream().mapToInt(Integer::intValue).toArray();
    }
} 