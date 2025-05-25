package com.dsm.api;

import com.dsm.common.config.AppConfig;
import com.dsm.model.ContainerCreateRequest;
import com.dsm.model.ResourceUsageDTO;
import com.dsm.utils.DockerInspectJsonGenerator;
import com.dsm.utils.DockerStatsConverter;
import com.dsm.utils.LogUtil;
import com.dsm.utils.MessageCallback;
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
import java.util.concurrent.CompletableFuture;
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

    @Resource
    private DockerComposeWrapper dockerComposeWrapper;

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
     * 获取网络详情
     *
     * @param networkId 网络ID
     * @return 网络详情
     */
    public Network inspectNetwork(String networkId) {
        return dockerClientWrapper.inspectNetwork(networkId);
    }

    public void recreateContainerWithNewImage(String containerId, String imageName) {
        dockerClientWrapper.recreateContainerWithNewImage(containerId, imageName);
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
//                LogUtil.logSysInfo("检测到Mac ARM架构，强制指定arm64/linux架构参数");
                command.add("--override-arch");
                command.add("arm64");
                command.add("--override-os");
                command.add("linux");
            }

            command.add("--insecure-policy");
            command.add("--src-tls-verify=false");
            command.add("--dest-tls-verify=false");
            command.add("docker://" + fullImageName);
            command.add("docker-daemon:" + fullImageName);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 如果代理配置不为空就使用代理
            String proxyUrl = appConfig.getProxyUrl();
            if (proxyUrl != null && !proxyUrl.isBlank()) {
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
            if (proxyUrl != null && !proxyUrl.isBlank()) {
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


    /**
     * 使用skopeo从远程仓库拉取镜像到宿主机Docker
     *
     * @param image    镜像名称
     * @param tag      镜像标签
     * @param callback 进度回调
     */
    public CompletableFuture<Void> pullImageWithSkopeo(String image, String tag, MessageCallback callback) {
        return CompletableFuture.runAsync(() -> {

            LogUtil.logSysInfo("开始使用 skopeo 拉取镜像: " + image + ":" + tag);
            try {
                String fullImageName = tag != null && !tag.isEmpty() ? image + ":" + tag : image;
                List<String> command = new ArrayList<>();
                command.add("skopeo");
                command.add("copy");

                // mac arm64 fix
                String osName = System.getProperty("os.name").toLowerCase();
                String osArch = System.getProperty("os.arch").toLowerCase();
                if (osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm64"))) {
                    command.add("--override-arch");
                    command.add("arm64");
                    command.add("--override-os");
                    command.add("linux");
                }

                // 添加安全策略参数
                command.add("--insecure-policy");
                command.add("--src-tls-verify=false");
                command.add("--dest-tls-verify=false");
                command.add("docker://" + fullImageName);
                command.add("docker-daemon:" + fullImageName);

                // 设置代理
                String proxyUrl = appConfig.getProxyUrl();
                ProcessBuilder pb = new ProcessBuilder(command);
                if (proxyUrl != null && !proxyUrl.isBlank()) {
                    pb.environment().put("HTTP_PROXY", proxyUrl);
                    pb.environment().put("HTTPS_PROXY", proxyUrl);
                }

                // 不合并错误流，分别处理标准输出和错误输出
                // pb.redirectErrorStream(true); // 移除这行

                LogUtil.logSysInfo("执行命令: " + String.join(" ", command));
                final Process process = pb.start();

                // 用于收集标准输出和错误输出
                StringBuilder outputBuffer = new StringBuilder();
                StringBuilder errorBuffer = new StringBuilder();

                // 创建线程读取错误输出
                Thread errorReaderThread = new Thread(() -> {
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorBuffer.append(line).append("\n");
                        }
                    } catch (Exception e) {
                        LogUtil.logSysError("读取错误输出失败: " + e.getMessage());
                    }
                });
                errorReaderThread.start();

                // 读取标准输出并解析进度
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    int progress = 0;
                    while ((line = reader.readLine()) != null) {
                        outputBuffer.append(line).append("\n");

                        // 解析进度并回调
                        if (line.contains("Getting image source signatures")) {
                            progress = 10;
                        } else if (line.contains("Copying blob")) {
                            progress = Math.min(progress + 2, 80); // 每次 +2，但最多到 80
                        } else if (line.contains("Copying config")) {
                            progress = 80;
                        } else if (line.contains("Writing manifest")) {
                            progress = 100;
                        } else if (line.contains("timeout")) {
                            line = "网络连接超时，请检查网络连接或配置代理";
                        }

                        if (callback != null) {
                            callback.onProgress(progress); // 进度回调
                            callback.onLog(line); // 日志回调
                        }
                    }
                }

                // 等待错误输出读取完成
                errorReaderThread.join(5000); // 最多等待5秒

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    // 构建详细的错误消息
                    String errorOutput = errorBuffer.toString().trim();
                    String standardOutput = outputBuffer.toString().trim();

                    StringBuilder detailedError = new StringBuilder();
                    detailedError.append("skopeo 命令执行失败");

                    // 首先尝试从错误输出中解析具体错误类型
                    String specificErrorType = parseSkopeoErrorType(errorOutput, standardOutput);

                    // 解释常见的退出码，如果有具体错误类型则使用具体类型
                    switch (exitCode) {
                        case 1:
                            if (specificErrorType != null) {
                                detailedError.append("(").append(specificErrorType).append(")");
                            } else {
                                detailedError.append("(一般错误)");
                            }
                            break;
                        case 2:
                            detailedError.append("(参数错误)");
                            break;
                        case 125:
                            detailedError.append("(skopeo命令本身运行错误)");
                            break;
                        case 126:
                            detailedError.append("(skopeo命令无法执行)");
                            break;
                        case 127:
                            detailedError.append("(skopeo命令未找到)");
                            break;
                        case 130:
                            detailedError.append("(操作被中断)");
                            break;
                        default:
                            detailedError.append("(退出码: ").append(exitCode).append(")");
                            break;
                    }

                    // 添加详细错误信息
                    if (!errorOutput.isEmpty()) {
                        detailedError.append("\n错误详情: ").append(errorOutput);
                    } else if (!standardOutput.isEmpty()) {
                        // 如果没有错误输出，但有标准输出，也包含进来
                        detailedError.append("\n输出信息: ").append(standardOutput);
                    }

                    // 根据错误内容提供建议
                    String errorStr = detailedError.toString().toLowerCase();
                    if (errorStr.contains("timeout") || errorStr.contains("time out")) {
                        detailedError.append("\n建议: 网络连接超时，请检查网络连接或配置代理");
                    } else if (errorStr.contains("unauthorized") || errorStr.contains("401")) {
                        detailedError.append("\n建议: 镜像仓库认证失败，请检查镜像名称是否正确或配置认证信息");
                    } else if (errorStr.contains("not found") || errorStr.contains("404")) {
                        detailedError.append("\n建议: 镜像未找到，请检查镜像名称和标签是否正确");
                    } else if (errorStr.contains("connection refused") || errorStr.contains("network")) {
                        detailedError.append("\n建议: 网络连接被拒绝，请检查网络连接或配置代理");
                    } else if (errorStr.contains("permission denied") || errorStr.contains("403")) {
                        detailedError.append("\n建议: 权限不足，请检查是否有拉取该镜像的权限");
                    } else if (exitCode == 127) {
                        detailedError.append("\n建议: skopeo命令未安装，请先安装skopeo工具");
                    }

                    String finalError = detailedError.toString();
                    LogUtil.logSysError(finalError);

                    if (callback != null) {
                        callback.onError(finalError); // 错误回调
                    }
                    throw new RuntimeException(finalError);
                }

                if (callback != null) {
                    callback.onComplete(); // 完成回调
                }

                LogUtil.logSysInfo("镜像拉取完成: " + fullImageName);
            } catch (InterruptedException e) {
                String errorMessage = "镜像拉取被中断: " + e.getMessage();
                LogUtil.logSysError(errorMessage);

                if (callback != null) {
                    callback.onError(errorMessage);
                }
                Thread.currentThread().interrupt(); // 重置中断状态
                throw new RuntimeException(errorMessage);
            } catch (Exception e) {
                String errorMessage = "使用 skopeo 拉取镜像失败: " + e.getMessage();
                LogUtil.logSysError(errorMessage);

                if (callback != null) {
                    callback.onError(errorMessage); // 错误回调
                }
                throw new RuntimeException(errorMessage);
            }
        });
    }


    /**
     * 拉取Docker镜像（支持WebSocket回调）
     *
     * @param image    镜像名称
     * @param tag      镜像标签
     * @param callback 进度回调
     */
    public void pullImage(String image, String tag, MessageCallback callback) {
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

    /**
     * 创建容器命令
     *
     * @param image 镜像名称
     * @return 创建容器命令对象
     */
    public CreateContainerCmd createContainerCmd(String image) {
        return dockerClientWrapper.createContainerCmd(image);
    }

    /**
     * 根据容器ID生成JSON配置
     *
     * @param containerId 容器ID
     * @return 生成的JSON字符串
     * @throws RuntimeException 当生成配置失败时抛出异常
     */
    public String generateJsonFromContainerId(String containerId) {
        if (containerId == null || containerId.trim().isEmpty()) {
            throw new IllegalArgumentException("容器ID不能为空");
        }

        InspectContainerResponse containerInfo = dockerClientWrapper.inspectContainerCmd(containerId);
        return DockerInspectJsonGenerator.generateJsonFromContainerInfo(containerInfo);
    }

    /**
     * 使用 Compose 部署容器
     *
     * @param projectName    项目名称
     * @param composeContent Compose 配置内容
     * @return 部署结果
     */
    public String deployWithCompose(String projectName, String composeContent) {
        return dockerComposeWrapper.deployCompose(projectName, composeContent);
    }

    /**
     * 使用 Compose 更新容器
     *
     * @param projectName    项目名称
     * @param composeContent 新的 Compose 配置内容
     * @return 更新结果
     */
    public String updateWithCompose(String projectName, String composeContent) {
        return dockerComposeWrapper.updateCompose(projectName, composeContent);
    }

    /**
     * 使用 Compose 删除容器
     *
     * @param projectName 项目名称
     */
    public void removeWithCompose(String projectName) {
        dockerComposeWrapper.removeCompose(projectName);
    }

    /**
     * 获取 Compose 项目状态
     *
     * @param projectName 项目名称
     * @return 项目状态信息
     */
    public Map<String, Object> getComposeStatus(String projectName) {
        return dockerComposeWrapper.getComposeStatus(projectName);
    }

    /**
     * 解析skopeo错误输出，提取具体的错误类型
     *
     * @param errorOutput    错误输出
     * @param standardOutput 标准输出
     * @return 具体的错误类型描述，如果无法解析则返回null
     */
    private String parseSkopeoErrorType(String errorOutput, String standardOutput) {
        // 合并错误输出和标准输出进行分析
        String allOutput = (errorOutput + " " + standardOutput).toLowerCase();

        // 解析skopeo的结构化日志消息
        if (allOutput.contains("level=fatal") && allOutput.contains("msg=")) {
            // 提取fatal级别的错误消息
            String fatalMsg = extractFatalMessage(errorOutput + " " + standardOutput);
            if (fatalMsg != null) {
                return analyzeFatalMessage(fatalMsg);
            }
        }

        // 如果没有结构化日志，则根据关键词进行分析
        if (allOutput.contains("requested access to the resource is denied") ||
                allOutput.contains("access denied")) {
            return "镜像访问被拒绝";
        }

        if (allOutput.contains("manifest unknown") || allOutput.contains("not found")) {
            return "镜像或标签不存在";
        }

        if (allOutput.contains("unauthorized") || allOutput.contains("authentication required")) {
            return "认证失败";
        }

        if (allOutput.contains("connection refused") || allOutput.contains("connection reset")) {
            return "网络连接被拒绝";
        }

        if (allOutput.contains("timeout") || allOutput.contains("deadline exceeded")) {
            return "网络连接超时";
        }

        if (allOutput.contains("no such host") || allOutput.contains("name resolution")) {
            return "域名解析失败";
        }

        if (allOutput.contains("certificate") || allOutput.contains("tls") || allOutput.contains("ssl")) {
            return "TLS/SSL证书错误";
        }

        if (allOutput.contains("too many requests") || allOutput.contains("rate limit")) {
            return "请求频率限制";
        }

        if (allOutput.contains("disk") || allOutput.contains("space")) {
            return "磁盘空间不足";
        }

        return null; // 无法解析出具体错误类型
    }

    /**
     * 从skopeo日志中提取fatal级别的消息内容
     */
    private String extractFatalMessage(String output) {
        // 查找 level=fatal msg="..." 模式
        int fatalIndex = output.indexOf("level=fatal");
        if (fatalIndex == -1) return null;

        int msgIndex = output.indexOf("msg=\"", fatalIndex);
        if (msgIndex == -1) return null;

        int startQuote = msgIndex + 5; // msg=" 的长度
        int endQuote = output.indexOf("\"", startQuote);
        if (endQuote == -1) return null;

        return output.substring(startQuote, endQuote);
    }

    /**
     * 分析fatal消息内容，提取具体错误类型
     */
    private String analyzeFatalMessage(String fatalMsg) {
        String lowerMsg = fatalMsg.toLowerCase();

        if (lowerMsg.contains("requested access to the resource is denied")) {
            return "镜像访问被拒绝 - 镜像可能不存在或需要认证";
        }

        if (lowerMsg.contains("reading manifest") && lowerMsg.contains("not found")) {
            return "镜像清单未找到 - 镜像或标签不存在";
        }

        if (lowerMsg.contains("initializing source") && lowerMsg.contains("connection")) {
            return "初始化镜像源失败 - 网络连接问题";
        }

        if (lowerMsg.contains("unauthorized")) {
            return "未授权访问 - 需要登录认证";
        }

        if (lowerMsg.contains("forbidden")) {
            return "访问被禁止 - 权限不足";
        }

        if (lowerMsg.contains("timeout")) {
            return "操作超时 - 网络或服务响应慢";
        }

        // 如果包含具体的错误关键词，返回原始消息的简化版本
        if (lowerMsg.length() < 100) {
            return "skopeo错误: " + fatalMsg;
        }

        return null;
    }

}



