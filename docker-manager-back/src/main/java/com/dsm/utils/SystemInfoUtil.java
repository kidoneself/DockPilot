package com.dsm.utils;

import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 系统信息获取工具类
 * 使用纯Java代码获取系统信息，无需依赖外部脚本
 */
@UtilityClass
public class SystemInfoUtil {

    // 网络统计缓存，用于计算网速
    private static final Map<String, NetworkStats> networkStatsCache = new ConcurrentHashMap<>();
    
    /**
     * 网络统计数据结构
     */
    private static class NetworkStats {
        private final long rxBytes;
        private final long txBytes;
        private final long timestamp;
        
        public NetworkStats(long rxBytes, long txBytes, long timestamp) {
            this.rxBytes = rxBytes;
            this.txBytes = txBytes;
            this.timestamp = timestamp;
        }
        
        public long getRxBytes() { return rxBytes; }
        public long getTxBytes() { return txBytes; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * 获取主机名
     */
    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            try {
                return System.getenv("HOSTNAME");
            } catch (Exception ex) {
                return "未知";
            }
        }
    }

    /**
     * 获取操作系统信息
     */
    public String getOsInfo() {
        try {
            // 尝试读取 /etc/os-release
            if (Files.exists(Paths.get("/etc/os-release"))) {
                try (BufferedReader reader = new BufferedReader(new FileReader("/etc/os-release"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("PRETTY_NAME=")) {
                            return line.substring(12).replaceAll("\"", "");
                        }
                    }
                }
            }
            
            // 尝试读取群晖DSM信息
            if (Files.exists(Paths.get("/etc.defaults/VERSION"))) {
                try (BufferedReader reader = new BufferedReader(new FileReader("/etc.defaults/VERSION"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("productversion=")) {
                            String version = line.substring(15).replaceAll("\"", "");
                            return "Synology DSM " + version;
                        }
                    }
                }
            }
            
            // 尝试读取Alpine信息
            if (Files.exists(Paths.get("/etc/alpine-release"))) {
                String version = Files.readString(Paths.get("/etc/alpine-release")).trim();
                return "Alpine " + version;
            }
            
            // 使用系统属性作为备选
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            return osName + " " + osVersion;
        } catch (Exception e) {
            return System.getProperty("os.name", "未知");
        }
    }

    /**
     * 获取内核版本
     */
    public String getKernelVersion() {
        try {
            Process process = Runtime.getRuntime().exec("uname -r");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.readLine();
            }
        } catch (Exception e) {
            return System.getProperty("os.version", "未知");
        }
    }

    /**
     * 获取系统运行时间
     */
    public String getUptime() {
        try {
            // 尝试读取 /proc/uptime
            if (Files.exists(Paths.get("/proc/uptime"))) {
                String content = Files.readString(Paths.get("/proc/uptime"));
                String[] parts = content.trim().split(" ");
                double uptimeSeconds = Double.parseDouble(parts[0]);
                
                long days = TimeUnit.SECONDS.toDays((long) uptimeSeconds);
                long hours = TimeUnit.SECONDS.toHours((long) uptimeSeconds) % 24;
                long minutes = TimeUnit.SECONDS.toMinutes((long) uptimeSeconds) % 60;
                
                if (days > 0) {
                    return String.format("%d天 %d小时 %d分钟", days, hours, minutes);
                } else if (hours > 0) {
                    return String.format("%d小时 %d分钟", hours, minutes);
                } else {
                    return String.format("%d分钟", minutes);
                }
            }
            
            // 备选方案：使用JVM运行时间
            long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
            long hours = TimeUnit.MILLISECONDS.toHours(jvmUptime);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(jvmUptime) % 60;
            return String.format("JVM运行: %d小时 %d分钟", hours, minutes);
        } catch (Exception e) {
            return "未知";
        }
    }

    /**
     * 获取CPU核心数
     */
    public int getCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 获取CPU型号
     */
    public String getCpuModel() {
        try {
            if (Files.exists(Paths.get("/proc/cpuinfo"))) {
                try (BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("model name")) {
                            return line.split(":")[1].trim();
                        }
                    }
                }
            }
            return "未知";
        } catch (Exception e) {
            return "未知";
        }
    }

    /**
     * 获取内存信息（单位：MB）
     */
    public MemoryInfo getMemoryInfo() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            
            // 尝试读取系统内存信息
            if (Files.exists(Paths.get("/proc/meminfo"))) {
                try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
                    String line;
                    long memTotal = 0, memFree = 0, memAvailable = 0;
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("MemTotal:")) {
                            memTotal = Long.parseLong(line.replaceAll("[^0-9]", "")) / 1024; // 转换为MB
                        } else if (line.startsWith("MemFree:")) {
                            memFree = Long.parseLong(line.replaceAll("[^0-9]", "")) / 1024;
                        } else if (line.startsWith("MemAvailable:")) {
                            memAvailable = Long.parseLong(line.replaceAll("[^0-9]", "")) / 1024;
                        }
                    }
                    
                    if (memTotal > 0) {
                        long memUsed = memTotal - (memAvailable > 0 ? memAvailable : memFree);
                        return new MemoryInfo(memTotal, memUsed);
                    }
                }
            }
            
            // 备选方案：使用JVM内存信息
            long totalMB = (heapMax > 0 ? heapMax : Runtime.getRuntime().maxMemory()) / (1024 * 1024);
            long usedMB = (heapUsed + nonHeapUsed) / (1024 * 1024);
            return new MemoryInfo(totalMB, usedMB);
            
        } catch (Exception e) {
            LogUtil.logSysError("获取内存信息失败: " + e.getMessage());
            return new MemoryInfo(0L, 0L);
        }
    }

    /**
     * 获取磁盘使用信息
     */
    public DiskInfo getDiskInfo() {
        try {
            // 尝试使用df命令
            Process process = Runtime.getRuntime().exec("df /");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.readLine(); // 跳过标题行
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 5) {
                        String usagePercent = parts[4]; // 使用百分比
                        String available = parts[3]; // 可用空间
                        
                        // 格式化可用空间
                        String freeSpace = formatDiskSpace(Long.parseLong(available));
                        
                        return new DiskInfo(usagePercent, freeSpace);
                    }
                }
            }
            
            // 备选方案：使用Java File API
            java.io.File root = new java.io.File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            
            if (totalSpace > 0) {
                long usedSpace = totalSpace - freeSpace;
                int usagePercent = (int) ((usedSpace * 100) / totalSpace);
                String freeSpaceStr = formatBytes(freeSpace);
                
                return new DiskInfo(usagePercent + "%", freeSpaceStr);
            }
            
            return new DiskInfo("0%", "未知");
        } catch (Exception e) {
            LogUtil.logSysError("获取磁盘信息失败: " + e.getMessage());
            return new DiskInfo("0%", "未知");
        }
    }

    /**
     * 获取网络信息
     */
    public NetworkInfo getNetworkInfo() {
        try {
            String ipAddress = "未知";
            String gateway = "未知";
            
            // 获取本机IP地址
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().contains(":")) continue; // 跳过IPv6
                    if (!addr.getHostAddress().startsWith("127.")) {
                        ipAddress = addr.getHostAddress();
                        break;
                    }
                }
                if (!"未知".equals(ipAddress)) break;
            }
            
            // 获取默认网关
            try {
                Process process = Runtime.getRuntime().exec("ip route show default");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && line.contains("via")) {
                        String[] parts = line.split("\\s+");
                        for (int i = 0; i < parts.length - 1; i++) {
                            if ("via".equals(parts[i])) {
                                gateway = parts[i + 1];
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略网关获取失败
            }
            
            return new NetworkInfo(ipAddress, gateway);
        } catch (Exception e) {
            LogUtil.logSysError("获取网络信息失败: " + e.getMessage());
            return new NetworkInfo("未知", "未知");
        }
    }

    /**
     * 获取CPU使用率
     */
    public Double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            // 如果是com.sun.management.OperatingSystemMXBean，可以获取更详细信息
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                double cpuLoad = sunOsBean.getProcessCpuLoad() * 100;
                if (cpuLoad >= 0) {
                    return Double.parseDouble(String.format("%.2f", cpuLoad));
                }
            }
            
            // 备选方案：使用系统命令
            Process process = Runtime.getRuntime().exec("cat /proc/loadavg");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.split(" ");
                    if (parts.length > 0) {
                        double load = Double.parseDouble(parts[0]);
                        // 简单计算：负载 / CPU核心数 * 100
                        double cpuUsage = (load / getCpuCores()) * 100;
                        return Double.parseDouble(String.format("%.2f", Math.min(cpuUsage, 100.0)));
                    }
                }
            }
            
            return 0.0;
        } catch (Exception e) {
            LogUtil.logSysError("获取CPU使用率失败: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * 获取网络速度信息
     */
    public NetworkSpeedInfo getNetworkSpeed() {
        try {
            long totalRxBytes = 0;
            long totalTxBytes = 0;
            
            // 检测操作系统类型
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("linux")) {
                // Linux系统：读取 /proc/net/dev
                if (Files.exists(Paths.get("/proc/net/dev"))) {
                    try (BufferedReader reader = new BufferedReader(new FileReader("/proc/net/dev"))) {
                        String line;
                        reader.readLine(); // 跳过头部
                        reader.readLine(); // 跳过分割线
                        
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.trim().split("\\s+");
                            if (parts.length >= 10) {
                                String interfaceName = parts[0].replace(":", "");
                                
                                // 跳过回环接口和虚拟接口
                                if (interfaceName.equals("lo") || 
                                    interfaceName.startsWith("docker") || 
                                    interfaceName.startsWith("br-") ||
                                    interfaceName.startsWith("veth")) {
                                    continue;
                                }
                                
                                try {
                                    // 接收字节数（第1列）和发送字节数（第9列）
                                    long rxBytes = Long.parseLong(parts[1]);
                                    long txBytes = Long.parseLong(parts[9]);
                                    
                                    totalRxBytes += rxBytes;
                                    totalTxBytes += txBytes;
                                } catch (NumberFormatException e) {
                                    // 忽略解析错误的行
                                }
                            }
                        }
                    }
                } else {
                    return new NetworkSpeedInfo("Linux系统文件不存在", "Linux系统文件不存在", 0.0, 0.0);
                }
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                // macOS系统：使用 netstat -ib 命令
                Process process = Runtime.getRuntime().exec("netstat -ib");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    reader.readLine(); // 跳过头部
                    
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 7) {
                            String interfaceName = parts[0];
                            
                            // 跳过回环接口、虚拟接口和无效接口
                            if (interfaceName.equals("lo0") || 
                                interfaceName.startsWith("utun") ||
                                interfaceName.startsWith("awdl") ||
                                interfaceName.startsWith("llw") ||
                                interfaceName.startsWith("bridge") ||
                                interfaceName.startsWith("vmenet") ||
                                interfaceName.startsWith("anpi") ||
                                interfaceName.startsWith("gif") ||
                                interfaceName.startsWith("stf") ||
                                interfaceName.contains("*") ||
                                interfaceName.contains("#") ||
                                interfaceName.contains(":")
                                ) {
                                continue;
                            }
                            
                            try {
                                // netstat -ib 格式: Name Mtu Network Address Ipkts Ierrs Ibytes Opkts Oerrs Obytes Coll
                                // Ibytes在第6列（索引6），Obytes在第9列（索引9）
                                if (parts.length >= 10) {
                                    long rxBytes = Long.parseLong(parts[6]); // Ibytes
                                    long txBytes = Long.parseLong(parts[9]); // Obytes
                                    
                                    totalRxBytes += rxBytes;
                                    totalTxBytes += txBytes;
                                }
                            } catch (NumberFormatException e) {
                                // 忽略解析错误的行
                            }
                        }
                    }
                    process.waitFor();
                }
            } else {
                return new NetworkSpeedInfo("不支持的操作系统", "不支持的操作系统", 0.0, 0.0);
            }
            
            long currentTime = System.currentTimeMillis();
            String cacheKey = "total_network";
            
            // 获取上次的统计数据
            NetworkStats lastStats = networkStatsCache.get(cacheKey);
            
            if (lastStats != null) {
                // 计算时间差（秒）
                double timeDiffSeconds = (currentTime - lastStats.getTimestamp()) / 1000.0;
                
                // 确保时间差足够大，避免计算误差
                if (timeDiffSeconds >= 1.0) {
                    // 计算字节差
                    long rxDiff = totalRxBytes - lastStats.getRxBytes();
                    long txDiff = totalTxBytes - lastStats.getTxBytes();
                    
                    // 防止负数（可能因为网络接口重置）
                    if (rxDiff >= 0 && txDiff >= 0) {
                        // 计算速度（字节/秒）
                        double downloadSpeed = rxDiff / timeDiffSeconds;
                        double uploadSpeed = txDiff / timeDiffSeconds;
                        
                        // 更新缓存
                        networkStatsCache.put(cacheKey, new NetworkStats(totalRxBytes, totalTxBytes, currentTime));
                        
                        return new NetworkSpeedInfo(
                            formatNetworkSpeed(downloadSpeed), 
                            formatNetworkSpeed(uploadSpeed),
                            downloadSpeed,
                            uploadSpeed
                        );
                    }
                }
                
                // 时间差太小或数据异常，返回上次的结果
                return new NetworkSpeedInfo("计算中...", "计算中...", 0.0, 0.0);
            }
            
            // 第一次获取，保存当前数据，返回初始化状态
            networkStatsCache.put(cacheKey, new NetworkStats(totalRxBytes, totalTxBytes, currentTime));
            return new NetworkSpeedInfo("初始化中...", "初始化中...", 0.0, 0.0);
            
        } catch (Exception e) {
            LogUtil.logSysError("获取网络速度失败: " + e.getMessage());
            return new NetworkSpeedInfo("获取失败", "获取失败", 0.0, 0.0);
        }
    }

    /**
     * 格式化网络速度显示
     */
    private String formatNetworkSpeed(double bytesPerSecond) {
        if (bytesPerSecond < 0) {
            bytesPerSecond = 0;
        }
        
        if (bytesPerSecond >= 1024 * 1024 * 1024) { // GB/s
            return String.format("%.1fGB/s", bytesPerSecond / (1024.0 * 1024.0 * 1024.0));
        } else if (bytesPerSecond >= 1024 * 1024) { // MB/s
            return String.format("%.1fMB/s", bytesPerSecond / (1024.0 * 1024.0));
        } else if (bytesPerSecond >= 1024) { // KB/s
            return String.format("%.1fKB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.0fB/s", bytesPerSecond);
        }
    }

    private String formatDiskSpace(long kbytes) {
        if (kbytes >= 1024 * 1024 * 1024) { // GB
            return String.format("%.1fTB", kbytes / (1024.0 * 1024.0 * 1024.0));
        } else if (kbytes >= 1024 * 1024) { // GB
            return String.format("%.1fGB", kbytes / (1024.0 * 1024.0));
        } else if (kbytes >= 1024) { // MB
            return String.format("%.1fMB", kbytes / 1024.0);
        } else {
            return kbytes + "KB";
        }
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L * 1024L) { // TB
            return String.format("%.1fTB", bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0));
        } else if (bytes >= 1024L * 1024L * 1024L) { // GB
            return String.format("%.1fGB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else if (bytes >= 1024L * 1024L) { // MB
            return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
        } else if (bytes >= 1024L) { // KB
            return String.format("%.1fKB", bytes / 1024.0);
        } else {
            return bytes + "B";
        }
    }

    /**
     * 内存信息类
     */
    public static class MemoryInfo {
        private final long total; // MB
        private final long used;  // MB

        public MemoryInfo(long total, long used) {
            this.total = total;
            this.used = used;
        }

        public long getTotal() { return total; }
        public long getUsed() { return used; }
        public long getFree() { return total - used; }
        public double getUsagePercent() {
            return total > 0 ? (double) used / total * 100 : 0;
        }
    }

    /**
     * 磁盘信息类
     */
    public static class DiskInfo {
        private final String usagePercent;
        private final String freeSpace;

        public DiskInfo(String usagePercent, String freeSpace) {
            this.usagePercent = usagePercent;
            this.freeSpace = freeSpace;
        }

        public String getUsagePercent() { return usagePercent; }
        public String getFreeSpace() { return freeSpace; }
    }

    /**
     * 网络信息类
     */
    public static class NetworkInfo {
        private final String ipAddress;
        private final String gateway;

        public NetworkInfo(String ipAddress, String gateway) {
            this.ipAddress = ipAddress;
            this.gateway = gateway;
        }

        public String getIpAddress() { return ipAddress; }
        public String getGateway() { return gateway; }
    }

    /**
     * 网络速度信息类
     */
    public static class NetworkSpeedInfo {
        private final String downloadSpeedFormatted;
        private final String uploadSpeedFormatted;
        private final double downloadSpeedRaw;
        private final double uploadSpeedRaw;

        public NetworkSpeedInfo(String downloadSpeedFormatted, String uploadSpeedFormatted, 
                              double downloadSpeedRaw, double uploadSpeedRaw) {
            this.downloadSpeedFormatted = downloadSpeedFormatted;
            this.uploadSpeedFormatted = uploadSpeedFormatted;
            this.downloadSpeedRaw = downloadSpeedRaw;
            this.uploadSpeedRaw = uploadSpeedRaw;
        }

        public String getDownloadSpeedFormatted() { return downloadSpeedFormatted; }
        public String getUploadSpeedFormatted() { return uploadSpeedFormatted; }
        public double getDownloadSpeedRaw() { return downloadSpeedRaw; }
        public double getUploadSpeedRaw() { return uploadSpeedRaw; }
        
        public String getCombinedSpeed() {
            return "↓" + downloadSpeedFormatted + " ↑" + uploadSpeedFormatted;
        }
    }
} 