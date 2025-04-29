package com.dsm.api;

import com.dsm.config.AppConfig;
import com.dsm.model.dockerApi.ContainerCreateRequest;
import com.dsm.model.dto.ResourceUsageDTO;
import com.dsm.utils.DockerStatsConverter;
import com.dsm.utils.LogUtil;
import com.dsm.websocket.callback.PullImageCallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Docker服务类，提供与Docker引擎交互的各种操作
 * 包括容器管理、镜像管理、日志查看等功能
 */
@Service
public class DockerService {
    @Resource
    private DockerClientWrapper dockerClientWrapper;

    @Resource
    private AppConfig appConfig;

    /**
     * 获取所有容器列表
     *
     * @return 容器列表，包括运行中和已停止的容器
     */
    public List<Container> listContainers() {
        return dockerClientWrapper.listContainers();
    }

    /**
     * 获取所有镜像列表
     *
     * @return 镜像列表，包括所有本地镜像
     */
    public List<Image> listImages() {
        return dockerClientWrapper.listImages();
    }


    /**
     * 启动指定容器
     *
     * @param containerId 容器ID
     * @throws RuntimeException 如果启动失败
     */
    public void startContainer(String containerId) {
        dockerClientWrapper.startContainer(containerId);
    }

    /**
     * 停止指定容器
     *
     * @param containerId 容器ID
     * @throws RuntimeException 如果停止失败
     */
    public void stopContainer(String containerId) {
        dockerClientWrapper.stopContainer(containerId);
    }

    /**
     * 重启指定容器
     *
     * @param containerId 容器ID
     */
    public void restartContainer(String containerId) {
        dockerClientWrapper.restartContainer(containerId);
    }


    /**
     * 删除指定容器
     *
     * @param containerId 容器ID
     */
    public void removeContainer(String containerId) {
        dockerClientWrapper.removeContainer(containerId);
    }

    /**
     * 获取容器的统计信息
     *
     * @param containerId 容器ID
     * @return 容器统计信息对象
     */
    public ResourceUsageDTO getContainerStats(String containerId) {
        Statistics containerStats = dockerClientWrapper.getContainerStats(containerId);
        return DockerStatsConverter.convert(containerStats);
    }


    /**
     * 检查Docker服务是否可用
     *
     * @return true如果可用，false如果不可用
     */
    public boolean isDockerAvailable() {
        return dockerClientWrapper.isDockerAvailable();
    }

    /**
     * 获取容器日志
     *
     * @param containerId 容器ID
     * @param tail        要获取的日志行数
     * @param follow      是否持续获取新日志
     * @param timestamps  是否包含时间戳
     * @return 日志内容
     */
    public String getContainerLogs(String containerId, int tail, boolean follow, boolean timestamps) {
        return dockerClientWrapper.getContainerLogs(containerId, tail, follow, timestamps);
    }


    /**
     * 删除Docker镜像
     *
     * @param imageId 镜像ID
     */
    public void removeImage(String imageId) {
        dockerClientWrapper.removeImage(imageId);
    }

    /**
     * 获取镜像详细信息
     *
     * @param imageId 镜像ID
     * @return 镜像详细信息
     */
    public InspectImageResponse getInspectImage(String imageId) {
        return dockerClientWrapper.getInspectImage(imageId);
    }

    /**
     * 获取镜像详细信息
     *
     * @return 镜像详细信息
     */
    public InspectContainerResponse inspectContainerCmd(String containerId) {
        return dockerClientWrapper.inspectContainerCmd(containerId);
    }


    /**
     * 重命名容器
     *
     * @param containerId 容器ID
     * @param newName     新的容器名称
     */
    public void renameContainer(String containerId, String newName) {
        dockerClientWrapper.renameContainer(containerId, newName);
    }


    /**
     * 获取docker的网络信息
     *
     * @return List<Network>
     */
    public List<Network> listNetworks() {
        return dockerClientWrapper.listNetworks();
    }


    /**
     * 取消镜像拉取操作
     *
     * @param imageWithTag 镜像名称和标签
     */
    public void cancelPullImage(String imageWithTag) {
        try {
            // 获取当前正在执行的拉取操作
            List<Thread> threads = Thread.getAllStackTraces().keySet().stream().filter(thread -> thread.getName().startsWith("docker-pull-")).collect(Collectors.toList());

            if (threads.isEmpty()) {
                LogUtil.logSysInfo("未找到正在执行的镜像拉取线程");
                return;
            }

            // 中断所有相关的拉取线程
            for (Thread thread : threads) {
                LogUtil.logSysInfo("正在中断镜像拉取线程: " + thread.getName());
                thread.interrupt();
            }

            // 查找并终止 skopeo 进程
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("pkill", "-f", "skopeo copy.*" + imageWithTag);
                Process process = processBuilder.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    LogUtil.logSysInfo("成功终止 skopeo 进程");
                } else {
                    LogUtil.logSysInfo("终止 skopeo 进程失败，退出码: " + exitCode);
                }
            } catch (Exception e) {
                LogUtil.logSysError("终止 skopeo 进程时出错: " + e.getMessage());
            }

            LogUtil.logSysInfo("已取消镜像拉取操作: " + imageWithTag);
        } catch (Exception e) {
            LogUtil.logSysError("取消镜像拉取操作失败: " + e.getMessage());
            throw new RuntimeException("取消镜像拉取操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取本地镜像的创建时间
     *
     * @param imageName 镜像名称
     * @param tag       镜像标签
     * @return 镜像创建时间字符串
     */
    public String getLocalImageCreateTime(String imageName, String tag) {
        try {
            String fullImageName = tag != null && !tag.isEmpty() ? imageName + ":" + tag : imageName;
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("inspect");
            command.add("--format");
            command.add("{{ .Created }}");
            command.add(fullImageName);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("获取本地镜像创建时间失败，退出码: " + exitCode);
            }

            return output.toString().trim();
        } catch (Exception e) {
            LogUtil.logSysError("获取本地镜像创建时间失败: " + e.getMessage());
            throw new RuntimeException("获取本地镜像创建时间失败: " + e.getMessage());
        }
    }

    /**
     * 获取远程镜像的创建时间
     *
     * @param imageName 镜像名称
     * @param tag       镜像标签
     * @return 镜像创建时间字符串
     */
    public String getRemoteImageCreateTime(String imageName, String tag) {
        String fullImageName = tag != null && !tag.isEmpty() ? imageName + ":" + tag : imageName;

        // 方法1：使用 skopeo
        try {
            List<String> command = new ArrayList<>();
            command.add("skopeo");
            command.add("inspect");

            // 检查当前系统架构
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            if (osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm64"))) {
                LogUtil.logSysInfo("检测到Mac ARM架构，强制指定arm64/linux架构参数");
                command.add("--override-arch");
                command.add("arm64");
                command.add("--override-os");
                command.add("linux");
            }

            command.add("--insecure-policy");
            command.add("--tls-verify=false");
            command.add("docker://" + fullImageName);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 如果代理配置不为空就使用代理
            String proxyUrl = appConfig.getProxyUrl();
            if (!proxyUrl.isBlank()) {
                processBuilder.environment().put("HTTP_PROXY", proxyUrl);
                processBuilder.environment().put("HTTPS_PROXY", proxyUrl);
            }
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // 使用 jq 解析 JSON 输出
                List<String> jqCommand = new ArrayList<>();
                jqCommand.add("jq");
                jqCommand.add("-r");
                jqCommand.add(".Created");

                ProcessBuilder jqProcessBuilder = new ProcessBuilder(jqCommand);
                Process jqProcess = jqProcessBuilder.start();

                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(jqProcess.getOutputStream()))) {
                    writer.write(output.toString());
                    writer.flush();
                }

                StringBuilder jqOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(jqProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jqOutput.append(line);
                    }
                }

                int jqExitCode = jqProcess.waitFor();
                if (jqExitCode == 0) {
                    String result = jqOutput.toString().trim();
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.logSysInfo("使用 skopeo 获取远程镜像时间失败: " + e.getMessage());
        }

        // 如果 skopeo 失败，尝试使用 regctl
        try {
            List<String> command = new ArrayList<>();
            command.add("regctl");
            command.add("image");
            command.add("inspect");
            command.add(fullImageName);
            command.add("--format");
            command.add("{{.Created}}");

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 如果代理配置不为空就使用代理
            // 如果代理配置不为空就使用代理
            String proxyUrl = appConfig.getProxyUrl();
            if (!proxyUrl.isBlank()) {
                processBuilder.environment().put("HTTP_PROXY", proxyUrl);
                processBuilder.environment().put("HTTPS_PROXY", proxyUrl);
            }
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String result = output.toString().trim();
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (Exception e) {
            LogUtil.logSysInfo("使用 regctl 获取远程镜像时间失败: " + e.getMessage());
        }

        // 如果两种方法都失败，返回本地镜像时间
        LogUtil.logSysInfo("获取远程镜像时间失败，返回本地镜像时间");
        return getLocalImageCreateTime(imageName, tag);
    }

//    public CreateContainerResponse configureContainerCmd(ContainerCreateRequest request) {
//        String imageName = request.getImage();
//        // 4. 构建HostConfig
//        HostConfig hostConfig = new HostConfig();
//
//        // 设置重启策略
//        if (request.getRestartPolicy() != null) {
//            hostConfig.withRestartPolicy(request.getRestartPolicy());
//        }
//
//        // 设置端口映射
//        if (request.getPortBindings() != null) {
//            hostConfig.withPortBindings(request.getPortBindings());
//        }
//
//        // 设置卷挂载
//        if (request.getBinds() != null) {
//            hostConfig.withBinds(request.getBinds());
//        }
//
//        // 设置其他HostConfig配置
//        if (request.getNetworkMode() != null) {
//            hostConfig.withNetworkMode(request.getNetworkMode());
//        }
//
//        if (request.getMemory() != null) {
//            hostConfig.withMemory(request.getMemory());
//        }
//        if (request.getMemorySwap() != null) {
//            hostConfig.withMemorySwap(request.getMemorySwap());
//        }
//        if (request.getCpuShares() != null) {
//            hostConfig.withCpuShares(request.getCpuShares());
//        }
//        if (request.getCpusetCpus() != null) {
//            hostConfig.withCpusetCpus(request.getCpusetCpus());
//        }
//        if (request.getCpuPeriod() != null) {
//            hostConfig.withCpuPeriod(request.getCpuPeriod());
//        }
//        if (request.getCpuQuota() != null) {
//            hostConfig.withCpuQuota(request.getCpuQuota());
//        }
//
//        if (request.getShmSize() != null) {
//            hostConfig.withShmSize(Long.valueOf(request.getShmSize()));
//        }
//
//        if (request.getDevices() != null) {
//            hostConfig.withDevices(request.getDevices());
//        }
//        if (request.getUlimits() != null) {
//            hostConfig.withUlimits(request.getUlimits());
//        }
//        if (request.getExtraHosts() != null && !request.getExtraHosts().isEmpty()) {
//            hostConfig.withExtraHosts(request.getExtraHosts().toArray(new String[0]));
//        }
//        if (request.getDns() != null && !request.getDns().isEmpty()) {
//            hostConfig.withDns(request.getDns());
//        }
//        if (request.getDnsSearch() != null && !request.getDnsSearch().isEmpty()) {
//            hostConfig.withDnsSearch(request.getDnsSearch());
//        }
//
//        CreateContainerCmd createContainerCmd = dockerClientWrapper.createContainerCmd(imageName).withName(request.getName()).withHostConfig(hostConfig);
//        // 设置环境变量
//        if (request.getEnv() != null && !request.getEnv().isEmpty()) {
//            createContainerCmd.withEnv(request.getEnv());
//        }
//        // 设置其他配置
//        if (request.getLabels() != null && !request.getLabels().isEmpty()) {
//            createContainerCmd.withLabels(request.getLabels());
//        }
//        //这个就好比IPTV用这个
//        if (request.getCmd() != null && !request.getCmd().isEmpty()) {
//            createContainerCmd.withCmd(request.getCmd());
//        }
//        // 如果指定了Entrypoint，则使用；否则保留镜像的默认Entrypoint
//        //这个似乎覆盖了就不行了
//        if (request.getEntrypoint() != null && !request.getEntrypoint().isEmpty()) {
//            createContainerCmd.withEntrypoint(request.getEntrypoint());
//        }
//        if (request.getWorkingDir() != null) {
//            createContainerCmd.withWorkingDir(request.getWorkingDir());
//        }
//        if (request.getUser() != null) {
//            createContainerCmd.withUser(request.getUser());
//        }
//        createContainerCmd.withPrivileged(request.getPrivileged());
//
//        return dockerClientWrapper.createContainer(createContainerCmd);
//    }

    /**
     * 配置并创建容器（精简 + 完整备注版）
     * 常用字段直接设置，不常用字段保留注释，且详细说明用途
     */
    public CreateContainerResponse configureContainerCmd(ContainerCreateRequest request) {
        String imageName = request.getImage();

        HostConfig hostConfig = new HostConfig();

        // ======================== 常用 HostConfig ========================

        // 重启策略（如 always, on-failure 等）
        if (request.getRestartPolicy() != null) {
            hostConfig.withRestartPolicy(request.getRestartPolicy());
        }

        // 端口映射（容器端口和宿主机端口的绑定）
        if (request.getPortBindings() != null) {
            hostConfig.withPortBindings(request.getPortBindings());
        }

        // 卷挂载（把宿主机目录挂到容器里）
        if (request.getBinds() != null) {
            hostConfig.withBinds(request.getBinds());
        }

        // 网络模式（如 bridge、host、自定义网络）
        if (request.getNetworkMode() != null) {
            hostConfig.withNetworkMode(request.getNetworkMode());
        }

        // ======================== 不常用 HostConfig（注释保留） ========================

        // 授权宿主机的物理设备（比如 GPU）给容器
        if (request.getDevices() != null) {
            hostConfig.withDevices(request.getDevices());
        }
    /*
    // 限制容器内存使用上限（单位：字节）
    if (request.getMemory() != null) {
        hostConfig.withMemory(request.getMemory());
    }

    // 限制容器内存+swap总量（单位：字节）
    if (request.getMemorySwap() != null) {
        hostConfig.withMemorySwap(request.getMemorySwap());
    }

    // 容器的 CPU 份额（相对权重）
    if (request.getCpuShares() != null) {
        hostConfig.withCpuShares(request.getCpuShares());
    }

    // 指定容器可以使用哪些 CPU 核心，比如 "0,1"
    if (request.getCpusetCpus() != null) {
        hostConfig.withCpusetCpus(request.getCpusetCpus());
    }

    // 设置 CPU 调度周期（单位：微秒）
    if (request.getCpuPeriod() != null) {
        hostConfig.withCpuPeriod(request.getCpuPeriod());
    }

    // 设置 CPU 配额（配合 Period 控制 CPU 时间）
    if (request.getCpuQuota() != null) {
        hostConfig.withCpuQuota(request.getCpuQuota());
    }

    // 容器的共享内存大小，默认64M，适合 Chrome/数据库应用调大
    if (request.getShmSize() != null) {
        hostConfig.withShmSize(Long.valueOf(request.getShmSize()));
    }

    // 设置系统资源限制（如打开文件数、进程数）
    if (request.getUlimits() != null) {
        hostConfig.withUlimits(request.getUlimits());
    }

    // 额外添加自定义 hosts 记录（比如 "myapp.local:127.0.0.1"）
    if (request.getExtraHosts() != null && !request.getExtraHosts().isEmpty()) {
        hostConfig.withExtraHosts(request.getExtraHosts().toArray(new String[0]));
    }

    // 配置容器使用的 DNS 服务器
    if (request.getDns() != null && !request.getDns().isEmpty()) {
        hostConfig.withDns(request.getDns());
    }

    // 配置 DNS 搜索域
    if (request.getDnsSearch() != null && !request.getDnsSearch().isEmpty()) {
        hostConfig.withDnsSearch(request.getDnsSearch());
    }
    */

        // ======================== CreateContainerCmd 配置 ========================

        CreateContainerCmd createContainerCmd = dockerClientWrapper.createContainerCmd(imageName).withName(request.getName()).withHostConfig(hostConfig);

        // 设置环境变量（如 ["ENV=prod", "DEBUG=false"]）
        if (request.getEnv() != null && !request.getEnv().isEmpty()) {
            createContainerCmd.withEnv(request.getEnv());
        }

        // 设置 Labels（容器的自定义标签，方便管理/查询）
        //docker stop $(docker ps -q --filter "label=app=nginx-stack")
        if (request.getLabels() != null && !request.getLabels().isEmpty()) {
            createContainerCmd.withLabels(request.getLabels());
        }

        // 设置启动命令（CMD），如果需要自定义启动指令
        if (request.getCmd() != null && !request.getCmd().isEmpty() && !isEmptyCommandList(request.getCmd())) {
            createContainerCmd.withCmd(request.getCmd());
        }

        // ======================== 不常用容器参数（注释保留） ========================

    /*
    // 覆盖镜像默认的 Entrypoint（慎用！）
    if (request.getEntrypoint() != null && !request.getEntrypoint().isEmpty() && !isEmptyCommandList(request.getEntrypoint())) {
        createContainerCmd.withEntrypoint(request.getEntrypoint());
    }

    // 指定容器内工作目录（一般镜像已定义）
    if (request.getWorkingDir() != null && !request.getWorkingDir().trim().isEmpty()) {
        createContainerCmd.withWorkingDir(request.getWorkingDir());
    }

    // 指定容器运行用户（一般镜像已定义，比如 nginx 用户）
    if (request.getUser() != null && !request.getUser().trim().isEmpty()) {
        createContainerCmd.withUser(request.getUser());
    }
    */

        // 设置是否启用特权模式（容器可以访问宿主机所有设备）
        createContainerCmd.withPrivileged(Boolean.TRUE.equals(request.getPrivileged()));

        return dockerClientWrapper.createContainer(createContainerCmd);
    }

    /**
     * 判断命令列表是否全是空白，避免误覆盖默认 CMD/Entrypoint
     */
    private boolean isEmptyCommandList(List<String> cmdList) {
        return cmdList.stream().allMatch(cmd -> cmd == null || cmd.trim().isEmpty());
    }


    public Network inspectNetwork(String networkId) {
        return null;
    }

    // FIXME: 功能尚未完成，暂不启用
    public String createNetwork(String name, String driver, Map<String, Object> ipamConfig) {
        return null;
    }

    // FIXME: 功能尚未完成，暂不启用
    public void removeNetwork(String networkId) {

    }

    // FIXME: 功能尚未完成，暂不启用
    public void connectContainerToNetwork(String containerId, String networkId, String ipAddress, String[] aliases) {

    }

    // FIXME: 功能尚未完成，暂不启用
    public void disconnectContainerFromNetwork(String containerId, String networkId, boolean force) {

    }


    /**
     * 使用skopeo从远程仓库拉取镜像到宿主机Docker
     *
     * @param image    镜像名称
     * @param tag      镜像标签
     * @param callback 进度回调
     */
    public void pullImageWithSkopeo(String image, String tag, PullImageCallback callback) {
        LogUtil.logSysInfo("开始使用 skopeo 拉取镜像: " + image + ":" + tag);
        try {
            // 构建完整的镜像名称
            String fullImageName = tag != null && !tag.isEmpty() ? image + ":" + tag : image;
            // 构建 skopeo 命令
            List<String> command = new ArrayList<>();
            command.add("skopeo");
            command.add("copy");
            // 添加源和目标
            // 检查当前系统架构
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            // 只有在Mac的ARM架构(M系列芯片)上才需要指定架构参数
            if (osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm64"))) {
                LogUtil.logSysInfo("检测到Mac ARM架构，强制指定arm64/linux架构参数");
                // 强制指定为amd64架构和linux系统，解决在Mac ARM芯片上的兼容性问题
                command.add("--override-arch");
                command.add("arm64");
                command.add("--override-os");
                command.add("linux");
            }
            command.add("docker://" + fullImageName);
            command.add("docker-daemon:" + fullImageName);
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 设置代理（如果启用）
            // 如果代理配置不为空就使用代理
            String proxyUrl = appConfig.getProxyUrl();
            if (!proxyUrl.isBlank()) {
                processBuilder.redirectErrorStream(true);
                processBuilder.environment().put("HTTP_PROXY", proxyUrl);
                processBuilder.environment().put("HTTPS_PROXY", proxyUrl);
            }
            processBuilder.redirectErrorStream(true);
            // 打印完整命令行
            LogUtil.logSysInfo("执行命令: " + String.join(" ", command) + ",是否使用代理 " + !proxyUrl.isBlank());
            Process process = processBuilder.start();
            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int progress = 0;
                while ((line = reader.readLine()) != null) {
                    LogUtil.logSysInfo("skopeo: " + line);
                    if (callback != null) {
                        // 解析进度
                        if (line.contains("Getting image source signatures")) {
                            progress = 10;
                        } else if (line.contains("Copying blob")) {
                            progress = 30;
                        } else if (line.contains("Copying config")) {
                            progress = 70;
                        } else if (line.contains("Writing manifest")) {
                            progress = 90;
                        } else if (line.contains("Storing signatures")) {
                            progress = 100;
                        }
                        callback.onProgress(progress, line);
                    }
                }
            }
            // 等待命令完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = "skopeo 命令执行失败，退出码: " + exitCode;
                if (callback != null) {
                    callback.onError(error);
                }
                throw new RuntimeException(error);
            }
            LogUtil.logSysInfo("镜像拉取完成: " + fullImageName);
            if (callback != null) {
                callback.onComplete();
            }
        } catch (Exception e) {
            LogUtil.logSysError("使用 skopeo 拉取镜像失败: " + e.getMessage());
            if (callback != null) {
                callback.onError(e.getMessage());
            }
            throw new RuntimeException("使用 skopeo 拉取镜像失败: " + e.getMessage());
        }
    }

    /**
     * 拉取Docker镜像（支持WebSocket回调）
     *
     * @param image    镜像名称
     * @param tag      镜像标签
     * @param callback 进度回调
     */
    public void pullImage(String image, String tag, PullImageCallback callback) {
        /**
         * 这里其实需要多种返回，使用代理，使用镜像加速，什么都不用
         */
        pullImageWithSkopeo(image, tag, callback);
    }

    public CreateContainerCmd getCmdByTempJson(JsonNode jsonNode) {
        return dockerClientWrapper.getCmdByTempJson(jsonNode);
    }

    public String startContainerWithCmd(CreateContainerCmd containerCmd) {
        return dockerClientWrapper.startContainerWithCmd(containerCmd);
    }
}



