package com.dsm.utils;

import com.dsm.pojo.entity.ContainerDetail;
import com.dsm.model.dockerApi.ContainerCreateRequest;
import com.github.dockerjava.api.model.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ContainerConvertUtil {

    public static ContainerCreateRequest fromDetail(ContainerDetail detail) {
        ContainerCreateRequest request = new ContainerCreateRequest();

        // 基本信息
        request.setName(detail.getName());
        request.setImage(detail.getImage());
        request.setRestartPolicy(RestartPolicy.parse(detail.getRestartPolicy()));
        request.setNetworkMode(detail.getNetworkMode());
        request.setPrivileged(detail.getPrivileged());

        // 环境变量
        request.setEnv(detail.getEnvList());

        // 卷挂载
        List<Bind> binds = detail.getBinds().stream()
                .map(Bind::parse)
                .collect(Collectors.toList());
        request.setBinds(binds);

        List<Volume> volumes = detail.getVolumeMappings().stream()
                .map(vol -> new Volume(vol.getContainerPath()))
                .collect(Collectors.toList());
        request.setVolumes(volumes);

        // 端口映射
        List<ExposedPort> exposedPorts = detail.getPortMappings().stream()
                .map(pm -> ExposedPort.tcp(pm.getContainerPort()))
                .collect(Collectors.toList());
        request.setExposedPorts(exposedPorts);

        Ports portBindings = new Ports();
        for (ContainerDetail.PortMapping mapping : detail.getPortMappings()) {
            ExposedPort exposedPort = ExposedPort.tcp(mapping.getContainerPort());
            Ports.Binding binding = Ports.Binding.bindPort(mapping.getHostPort());
            portBindings.bind(exposedPort, binding);
        }
        request.setPortBindings(portBindings);

        // 其他默认设置（可选）
        request.setAutoRemove(false);
        request.setAttachStdin(false);
        request.setAttachStdout(true);
        request.setAttachStderr(true);
        request.setTty(false);
        request.setPublishAllPorts(false);

        // 其余字段按需填充
        request.setCapAdd(Collections.emptyList());
        request.setCapDrop(Collections.emptyList());

        return request;
    }
}