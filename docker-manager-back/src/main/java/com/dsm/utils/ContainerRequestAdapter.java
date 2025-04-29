package com.dsm.utils;

import com.dsm.pojo.dto.ContainerCreateDTO;
import com.dsm.model.dto.JsonContainerRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 前端创建容器的JsonContainerRequest   转换成  ContainerCreateDTO
 * <p>
 * ContainerCreateDTO对应的是dockerAPI的创建对象
 */
public class ContainerRequestAdapter {

    public static ContainerCreateDTO adapt(JsonContainerRequest json) {
        ContainerCreateDTO dto = new ContainerCreateDTO();

        // 基本信息
        dto.setName(json.getName());
        dto.setImage(json.getImage() + ":" + Optional.ofNullable(json.getTag()).orElse("latest"));

        dto.setAutoRemove(json.getAutoRemove());
        dto.setRestartPolicy(json.getRestartPolicy());
        dto.setPrivileged(json.getPrivileged());
        dto.setNetworkMode(json.getNetworkMode());
        dto.setDns(json.getDns());
        dto.setDnsSearch(json.getDnsSearch());
        dto.setExtraHosts(json.getExtraHosts());

        // ========== Port Mapping ==========
        if (json.getPortMappings() != null) {
            Map<Integer, Integer> portBindings = new HashMap<>();
            List<Integer> exposedPorts = new ArrayList<>();

            for (JsonContainerRequest.PortMapping pm : json.getPortMappings()) {
                if (pm.getContainerPort() != null) {
                    exposedPorts.add(pm.getContainerPort());
                    if (pm.getHostPort() != null) {
                        portBindings.put(pm.getHostPort(), pm.getContainerPort());
                    }
                }
            }

            dto.setExposedPorts(exposedPorts);
            dto.setPortBindings(portBindings);
        }

        // ========== Volume Mounts ==========
        if (json.getVolumeMounts() != null && !json.getVolumeMounts().isEmpty()) {
            List<String> binds = new ArrayList<>();
            List<String> volumePaths = new ArrayList<>();

            for (JsonContainerRequest.VolumeMount vm : json.getVolumeMounts()) {
                if (vm.getContainerPath() != null) {
                    volumePaths.add(vm.getContainerPath());

                    String mode = vm.getReadOnly() != null && vm.getReadOnly() ? "ro" : "rw";
                    binds.add(vm.getHostPath() + ":" + vm.getContainerPath() + ":" + mode);
                }
            }

            dto.setVolumePaths(volumePaths);
            dto.setBinds(binds);
        }

        // ========== Environment Variables ==========
        if (json.getEnvironmentVariables() != null) {
            List<String> envs = json.getEnvironmentVariables().stream().map(ev -> ev.getKey() + "=" + ev.getValue()).collect(Collectors.toList());
            dto.setEnv(envs);
        }

        // ========== 命令 ==========
        if (json.getCommand() != null && !json.getCommand().isBlank()) {
            dto.setCmd(Arrays.asList(json.getCommand().split(" ")));
        }

        // ========== IP 地址 ==========
        dto.setIpv4Address(json.getIpAddress());

        return dto;
    }
}