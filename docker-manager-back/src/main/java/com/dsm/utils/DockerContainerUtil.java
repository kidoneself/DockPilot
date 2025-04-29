package com.dsm.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class DockerContainerUtil {

    /**
     * 根据传入的 JSON 参数和 DockerClient 创建并启动容器
     * @param dockerClient Docker 客户端实例
     * @param cmdJson 容器创建所需的 JSON 参数
     * @return 容器 ID
     */
    public static String createAndStartContainer(DockerClient dockerClient, JsonNode cmdJson) {
        try {
            // 创建容器命令
            CreateContainerCmd cmd = dockerClient
                    .createContainerCmd(cmdJson.get("Image").asText())
                    .withEnv(convertToStringArray(cmdJson.get("Env")))
                    .withExposedPorts(parseExposedPorts(cmdJson.get("ExposedPorts")));

            // 设置 HostConfig
            JsonNode hostConfig = cmdJson.get("HostConfig");
            cmd.withHostConfig(new HostConfig()
                    .withPortBindings(parsePortBindings(hostConfig.get("PortBindings")))
                    .withBinds(parseBinds(hostConfig.get("Binds")))
                    .withPrivileged(hostConfig.get("Privileged").asBoolean())
                    .withNetworkMode(hostConfig.get("NetworkMode").asText())
                    .withRestartPolicy(RestartPolicy.parse(hostConfig.get("RestartPolicy").asText()))
            );

            // 执行并启动
            String containerId = cmd.exec().getId();
            dockerClient.startContainerCmd(containerId).exec();
            return containerId;

        } catch (Exception e) {
            throw new RuntimeException("容器创建失败: " + e.getMessage(), e);
        }
    }

    private static Ports parsePortBindings(JsonNode portBindings) {
        Ports ports = new Ports();
        portBindings.fields().forEachRemaining(entry -> {
            String[] parts = entry.getKey().split("/");
            ports.bind(ExposedPort.tcp(Integer.parseInt(parts[0])),
                    Ports.Binding.bindPort(entry.getValue().asInt()));
        });
        return ports;
    }

    private static Bind[] parseBinds(JsonNode binds) {
        return StreamSupport.stream(binds.spliterator(), false)
                .map(bind -> Bind.parse(bind.asText()))
                .toArray(Bind[]::new);
    }

    private static String[] convertToStringArray(JsonNode jsonNode) {
        if (jsonNode == null || !jsonNode.isArray()) {
            return new String[0];
        }
        List<String> list = new ArrayList<>();
        jsonNode.forEach(node -> list.add(node.asText()));
        return list.toArray(new String[0]);
    }

    private static ExposedPort[] parseExposedPorts(JsonNode exposedPortsNode) {
        List<ExposedPort> exposedPorts = new ArrayList<>();
        exposedPortsNode.fieldNames().forEachRemaining(
                port -> exposedPorts.add(ExposedPort.parse(port))
        );
        return exposedPorts.toArray(new ExposedPort[0]);
    }
}