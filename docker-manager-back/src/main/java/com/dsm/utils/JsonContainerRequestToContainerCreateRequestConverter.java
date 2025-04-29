package com.dsm.utils;

import com.dsm.model.dockerApi.ContainerCreateRequest;
import com.dsm.model.dto.JsonContainerRequest;
import com.github.dockerjava.api.model.*;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 创建容器，前端JSON转dockerapi创建对象
 */
@UtilityClass
public class JsonContainerRequestToContainerCreateRequestConverter {

    public static ContainerCreateRequest convert(JsonContainerRequest jsonRequest) {
        ContainerCreateRequest request = new ContainerCreateRequest();

        // ========= 基本配置 =========
        request.setImage(jsonRequest.getImage() + ":" + jsonRequest.getTag()); // 拼接 image:tag
        request.setName(jsonRequest.getName());
        request.setCmd(List.of(jsonRequest.getCommand())); // 转为 List<String>
        request.setEntrypoint(null); // 可根据需要转换 entrypoint
        request.setEnv(convertEnvVars(jsonRequest.getEnvironmentVariables()));
        request.setWorkingDir(null); // 根据需要设置工作目录
        request.setHostName(null); // 可根据需要设置主机名
        request.setDomainName(null); // 可根据需要设置域名
        request.setUser(null); // 根据需要设置用户

        // ========= 卷挂载 =========
        request.setVolumes(convertVolumeMounts(jsonRequest.getVolumeMounts()));
        request.setBinds(convertVolumeMountsToBinds(jsonRequest.getVolumeMounts())); // 可选择转换为 Binds
        request.setVolumesFrom(null); // 根据需求设置

        // ========= 网络配置 =========
        request.setNetworkMode(jsonRequest.getNetworkMode());
        request.setExposedPorts(convertPortMappings(jsonRequest.getPortMappings()));
        request.setPortBindings(convertPortBindings(jsonRequest.getPortMappings()));
        request.setDns(jsonRequest.getDns());
        request.setDnsSearch(jsonRequest.getDnsSearch());
        request.setExtraHosts(jsonRequest.getExtraHosts());
        request.setIpv4Address(null); // 根据需要设置 IPv4 地址
        request.setIpv6Address(null); // 根据需要设置 IPv6 地址
        request.setAliases(null); // 设置网络别名

        // ========= 安全配置 =========
        request.setCapAdd(null); // 根据需求设置
        request.setCapDrop(null); // 根据需求设置
        request.setDevices(null); // 根据需求设置设备
        request.setPrivileged(jsonRequest.getPrivileged());
        request.setSecurityOpts(null); // 根据需要设置
        request.setRestartPolicy(RestartPolicy.parse(jsonRequest.getRestartPolicy().toLowerCase())); // 转为枚举

        // ========= 资源限制 =========
        request.setMemory(null); // 根据需要设置内存限制
        request.setMemorySwap(null); // 根据需要设置内存交换空间
        request.setCpuShares(null); // 根据需要设置 CPU 权重
        request.setCpuPeriod(null); // 根据需要设置 CPU 调度周期
        request.setCpuQuota(null); // 根据需要设置 CPU 配额
        request.setCpusetCpus(null); // 根据需要设置 CPU 核心

        // ========= 运行行为 =========
        request.setTty(null); // 是否分配 TTY
        request.setStdinOpen(null); // 是否保持 stdin 打开
        request.setAttachStdin(null); // 附加 stdin
        request.setAttachStdout(null); // 附加 stdout
        request.setAttachStderr(null); // 附加 stderr
        request.setPublishAllPorts(null); // 是否自动映射端口
        request.setAutoRemove(jsonRequest.getAutoRemove());

        // ========= 其他 =========
        request.setLabels(null); // 根据需要设置标签
        request.setHealthCheck(null); // 根据需要设置健康检查

        return request;
    }

    // 将环境变量列表转换为格式为 key=value 的 List<String>
    private static List<String> convertEnvVars(List<JsonContainerRequest.EnvVar> envVars) {
        List<String> env = new ArrayList<>();
        if (envVars != null) {
            for (JsonContainerRequest.EnvVar envVar : envVars) {
                env.add(envVar.getKey() + "=" + envVar.getValue());
            }
        }
        return env;
    }

    // 将 VolumeMounts 转换为 Volumes
    private static List<Volume> convertVolumeMounts(List<JsonContainerRequest.VolumeMount> volumeMounts) {
        List<Volume> volumes = new ArrayList<>();
        if (volumeMounts != null) {
            for (JsonContainerRequest.VolumeMount mount : volumeMounts) {
                volumes.add(new Volume(mount.getContainerPath())); // 根据需要设置 Volume 的类型
            }
        }
        return volumes;
    }

    // 将 VolumeMounts 转换为 Binds（主机路径：容器路径：读写权限）
    private static List<Bind> convertVolumeMountsToBinds(List<JsonContainerRequest.VolumeMount> volumeMounts) {
        List<Bind> binds = new ArrayList<>();
        if (volumeMounts != null && !volumeMounts.isEmpty()) {
            List<String> bindsStr = new ArrayList<>();
            for (JsonContainerRequest.VolumeMount vm : volumeMounts) {
                if (vm.getContainerPath() != null) {
                    String mode = vm.getReadOnly() != null && vm.getReadOnly() ? "ro" : "rw";
                    bindsStr.add(vm.getHostPath() + ":" + vm.getContainerPath() + ":" + mode);
                }
            }
            binds = bindsStr.stream().map(Bind::parse).collect(Collectors.toList());
        }
        return binds;

    }


    // 将 PortMappings 转换为 ExposedPort
    private static List<ExposedPort> convertPortMappings(List<JsonContainerRequest.PortMapping> portMappings) {
        List<ExposedPort> exposedPorts = new ArrayList<>();
        if (portMappings != null) {
            for (JsonContainerRequest.PortMapping portMapping : portMappings) {
                exposedPorts.add(ExposedPort.tcp(portMapping.getContainerPort())); // 可根据协议调整
            }
        }
        return exposedPorts;
    }

    private static Ports convertPortBindings(List<JsonContainerRequest.PortMapping> portMappings) {
        Ports portBindings = new Ports();
        if (portMappings != null) {
            for (JsonContainerRequest.PortMapping portMapping : portMappings) {
                ExposedPort exposedPort = ExposedPort.tcp(portMapping.getContainerPort());
                // 使用 Ports.Binding 创建绑定，而不是 PortBinding
                Ports.Binding binding = Ports.Binding.bindPort(portMapping.getHostPort());
                portBindings.bind(exposedPort, binding); // 绑定容器端口和主机端口
            }
        }
        return portBindings;
    }
}