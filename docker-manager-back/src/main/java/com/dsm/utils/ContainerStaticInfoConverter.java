package com.dsm.utils;

import com.dsm.model.ContainerStaticInfoDTO;
import com.dsm.model.VolumeMapping;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.Ports.Binding;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 容器详情转DTO
 */
public class ContainerStaticInfoConverter {

    public static ContainerStaticInfoDTO convert(InspectContainerResponse container/*, InspectImageResponse image*/) {
        ContainerStaticInfoDTO dto = new ContainerStaticInfoDTO();

        dto.setContainerId(container.getId());
        dto.setContainerName(cleanName(container.getName()));

        // 镜像信息
        dto.setImageId(container.getImageId());
        dto.setImageName(Optional.ofNullable(container.getConfig()).map(ContainerConfig::getImage).orElse(null));

        // 创建时间
        dto.setCreatedTime(container.getCreated());

        // 容器状态
        dto.setStatus(Optional.ofNullable(container.getState()).map(InspectContainerResponse.ContainerState::getStatus).orElse(null));

        // 重启次数
        dto.setRestartCount(Optional.ofNullable(container.getRestartCount()).orElse(0));

        // 启动命令
        dto.setCommand(
                Optional.ofNullable(container.getConfig())
                        .map(ContainerConfig::getCmd) // 获取 cmd 数组
                        .map(Arrays::asList) // 将数组转换为List
                        .orElse(Collections.emptyList()) // 如果没有命令，则设置为空列表
        );

        // 工作目录
        dto.setWorkingDir(Optional.ofNullable(container.getConfig()).map(ContainerConfig::getWorkingDir).orElse(null));

        // 入口点
        dto.setEntrypoints(Optional.ofNullable(container.getConfig()).map(ContainerConfig::getEntrypoint).map(Arrays::asList).orElse(Collections.emptyList()));

        // 标签
        dto.setLabels(Optional.ofNullable(container.getConfig()).map(ContainerConfig::getLabels).orElse(Collections.emptyMap()));

        // 环境变量
        dto.setEnvs(Optional.ofNullable(container.getConfig()).map(ContainerConfig::getEnv).map(envs -> Arrays.stream(envs).collect(Collectors.toList())).orElse(Collections.emptyList()));

        // 挂载卷
        dto.setVolumes(
                Optional.ofNullable(container.getMounts())
                        .orElse(Collections.emptyList()) // 如果 mounts 为 null，使用空列表
                        .stream()
                        .map(mount -> {
                            // 通过检查 'mode' 字段来判断是否为只读
                            boolean isReadOnly = "ro".equalsIgnoreCase(mount.getMode());

                            // 映射到前端所需格式
                            return new VolumeMapping(
                                    mount.getSource(), // 宿主机路径
                                    Objects.requireNonNull(mount.getDestination()).getPath(), // 容器路径
                                    isReadOnly // 是否只读
                            );
                        })
                        .collect(Collectors.toList())
        );
        // 端口映射
        Ports ports = Optional.ofNullable(container.getHostConfig())
                .map(HostConfig::getPortBindings)
                .orElse(null);

        if (ports != null) {
            dto.setPorts(
                    ports.getBindings().entrySet().stream()
                            .flatMap(entry -> Arrays.stream(Optional.ofNullable(entry.getValue()).orElse(new Binding[0]))
                                    .filter(binding -> binding.getHostIp() == null || !"::".equals(binding.getHostIp())) // 过滤掉 HostIp 是 "::" 的
                                    .map(binding -> entry.getKey().toString() + ":" + binding.getHostPortSpec())
                            )
                            .collect(Collectors.toList())
            );
        } else {
            dto.setPorts(Collections.emptyList());
        }

        // 暴露端口
        dto.setExposedPorts(Optional.ofNullable(container.getConfig()).map(ContainerConfig::getExposedPorts).map(exposedPorts -> Arrays.stream(exposedPorts).map(ExposedPort::toString).collect(Collectors.toList())).orElse(Collections.emptyList()));

        // 设备
        dto.setDevices(Optional.ofNullable(container.getHostConfig()).map(HostConfig::getDevices).map(devices -> Arrays.stream(devices).map(Device::getPathOnHost).collect(Collectors.toList())).orElse(Collections.emptyList()));

        // 网络模式
        dto.setNetworkMode(Optional.ofNullable(container.getHostConfig()).map(HostConfig::getNetworkMode).orElse(null));

        // 容器 IP
        dto.setIpAddress(Optional.ofNullable(container.getNetworkSettings()).map(NetworkSettings::getIpAddress).orElse(null));

        // 🔥 新增：重启策略
        HostConfig hostConfig = container.getHostConfig();
        if (hostConfig != null && hostConfig.getRestartPolicy() != null) {
            dto.setRestartPolicyName(hostConfig.getRestartPolicy().getName());
            dto.setRestartPolicyMaxRetry(hostConfig.getRestartPolicy().getMaximumRetryCount());
        }

        // 🔥 新增：特权模式
        dto.setPrivileged(Optional.ofNullable(hostConfig).map(HostConfig::getPrivileged).orElse(false));
        return dto;
    }

    private static String cleanName(String name) {
        if (name == null) return null;
        return name.startsWith("/") ? name.substring(1) : name;
    }
}