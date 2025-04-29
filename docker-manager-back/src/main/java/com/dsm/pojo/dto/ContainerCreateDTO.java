package com.dsm.pojo.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 容器创建请求（前端友好版本）
 */
@Data
public class ContainerCreateDTO {
    // ========= 基本配置 =========
    private String name;
    private String hostName;
    private String domainName;
    private String user;
    private String image;

    private List<String> cmd;
    private List<String> entrypoint;
    private List<String> env;
    private String workingDir;

    // ========= 卷挂载 =========
    private List<String> volumePaths; // 只填写容器内路径，如 "/data"
    private List<String> binds;       // 主机路径:容器路径[:rw|ro]
    private List<String> volumesFrom; // 来源容器名或ID

    // ========= 网络配置 =========
    private String networkMode;
    private List<String> links;
    private List<Integer> exposedPorts; // 只填容器端口号即可
    private Map<Integer, Integer> portBindings; // 宿主机端口 -> 容器端口

    private List<String> dns;
    private List<String> dnsSearch;
    private List<String> extraHosts;

    private List<String> networks;
    private String ipv4Address;
    private String ipv6Address;
    private List<String> aliases;

    // ========= HostConfig 权限与日志 =========
    private List<String> capAdd;
    private List<String> capDrop;

    private List<DeviceMapping> devices;
    private String restartPolicy;
    private String logDriver;
    private Map<String, String> logOptions;

    // ========= 资源限制 =========
    private Long memory;
    private Long memorySwap;
    private Integer cpuShares;
    private Long cpuPeriod;
    private Long cpuQuota;
    private String cpusetCpus;

    // ========= 运行行为 =========
    private Boolean tty;
    private Boolean stdinOpen;
    private Boolean attachStdin;
    private Boolean attachStdout;
    private Boolean attachStderr;
    private Boolean privileged;
    private Boolean publishAllPorts;
    private Boolean autoRemove;

    // ========= 安全与元数据 =========
    private List<String> securityOpts;
    private Map<String, String> labels;

    // ========= 系统限制 =========
    private List<UlimitConfig> ulimits;
    private HealthCheckConfig healthCheck;

    // ========= 其他 =========
    private String stopSignal;
    private Integer stopTimeout;
    private Boolean oomKillDisable;
    private Integer shmSize;

    // ========= 子类结构 =========
    @Data
    public static class DeviceMapping {
        private String pathOnHost;
        private String pathInContainer;
        private String cgroupPermissions;
    }

    @Data
    public static class UlimitConfig {
        private String name;
        private Integer soft;
        private Integer hard;
    }

    @Data
    public static class HealthCheckConfig {
        private List<String> test;
        private Long interval;
        private Long timeout;
        private Long startPeriod;
        private Integer retries;
    }
}