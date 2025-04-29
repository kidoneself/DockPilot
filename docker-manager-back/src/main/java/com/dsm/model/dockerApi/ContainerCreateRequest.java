package com.dsm.model.dockerApi;

import com.github.dockerjava.api.model.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 容器创建请求
 */
@Data
public class ContainerCreateRequest {
    // ========= 基本配置 =========
    private String name;              // 容器名称
    private String hostName;          // 容器的主机名
    private String domainName;        // 容器的域名
    private String user;              // 容器内执行用户
    private String image;             // 镜像名

    private List<String> cmd;         // 容器启动命令（覆盖镜像的 CMD）
    private List<String> entrypoint;  // 容器启动入口（覆盖镜像的 ENTRYPOINT）
    private List<String> env;         // 环境变量，格式如 ["ENV=prod"]
    private String workingDir;        // 容器内的工作目录

    // ========= 卷挂载 =========
    private List<Volume> volumes;     // 容器内部路径定义（如 "/data"）
    private List<Bind> binds;         // 主机路径绑定到容器（hostPath:containerPath:rw）
    private VolumesFrom[] volumesFrom; // 从其他容器继承的卷

    // ========= 网络配置 =========
    private String networkMode;       // 网络模式，如 bridge、host、container:xxx
    private String[] links;           // 容器间连接（已废弃）
    private List<ExposedPort> exposedPorts; // 声明容器暴露端口
    private Ports portBindings;       // 端口映射（容器端口 -> 主机端口）
    private List<String> dns;         // 自定义 DNS
    private List<String> dnsSearch;   // DNS 搜索域
    private List<String> extraHosts;  // 额外的 hosts 映射

    private List<String> networks;    // 要连接的网络（用于自定义网络）
    private String ipv4Address;       // 指定 IPv4 地址（需配合自定义网络）
    private String ipv6Address;       // 指定 IPv6 地址
    private List<String> aliases;     // 网络别名

    // ========= HostConfig 安全和权限 =========
    private List<String> capAdd;   // 增加的 Linux capability（如 "NET_ADMIN"）
    private List<String> capDrop;  // 删除的 Linux capability（如 "MKNOD"）
    private Device[] devices;         // 授权设备映射
    private RestartPolicy restartPolicy; // 重启策略，如 always、on-failure 等
    private String logDriver;         // 日志驱动（如 json-file、syslog）
    private Map<String, String> logOptions; // 日志驱动的额外参数

    // ========= 资源限制 =========
    private Long memory;              // 限制内存上限（单位字节）
    private Long memorySwap;          // 总内存（包含 swap），memorySwap >= memory
    private Integer cpuShares;           // CPU 共享权重（相对优先级）
    private Long cpuPeriod;           // CPU 调度周期（微秒）
    private Long cpuQuota;            // CPU 时间配额（微秒）
    private String cpusetCpus;        // 指定使用的 CPU 核心（如 "0,1"）

    // ========= 运行行为 =========
    private Boolean tty;              // 是否为容器分配 TTY
    private Boolean stdinOpen;        // 是否保持 stdin 打开
    private Boolean attachStdin;      // 附加 stdin
    private Boolean attachStdout;     // 附加 stdout
    private Boolean attachStderr;     // 附加 stderr
    private Boolean privileged;       // 是否启用特权模式（root 权限）
    private Boolean publishAllPorts;  // 自动将暴露端口映射到主机端口
    private Boolean autoRemove;       // 容器退出后是否自动删除

    // ========= 安全配置 =========
    private String[] securityOpts;    // 安全模块配置（如 seccomp 配置）

    // ========= 元数据 =========
    //	•	Label 还可以加版本，比如 "version=1.0"，以后升级也能好分组。
    //	•	可以组合查询多个 Label，比如：
    //docker ps --filter "label=project=myapp" --filter "label=env=prod"
    //	•	用 Label 还能做监控、告警（Prometheus / Grafana 这些都支持 Label 过滤）。
    private Map<String, String> labels; // 自定义标签

    // ========= 限制与检查 =========
    private Ulimit[] ulimits;         // 系统资源限制（如最大文件数）
    private HealthCheck healthCheck;  // 健康检查配置

    // ========= 其他高级配置 =========
    private String stopSignal;        // 停止容器使用的信号（默认 SIGTERM）
    private Integer stopTimeout;      // 容器关闭前等待时间（秒）
    private Boolean oomKillDisable;   // OOM 时是否禁用自动 kill
    private Integer shmSize;          // /dev/shm 的大小（用于 Chrome 等共享内存）

}