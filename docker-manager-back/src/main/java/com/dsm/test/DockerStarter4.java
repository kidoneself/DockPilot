package com.dsm.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class DockerStarter4 {

    public static void main(String[] args) throws Exception {
        // 1. 读取 cmd.json
        JsonNode cmdJson = new ObjectMapper().readTree(new File("/Users/lizhiqiang/coding-my/docker/docker-manager-back/src/main/java/com/dsm/test/cmd.json"));

        // 2. 初始化 Docker 客户端
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // 3. 构建容器配置
        CreateContainerCmd cmd = dockerClient.createContainerCmd(cmdJson.get("Image").asText()).withEnv(convertToStringArray(cmdJson.get("Env"))).withExposedPorts(parseExposedPorts(cmdJson.get("ExposedPorts")));

        // 4. 配置主机参数
        JsonNode hostConfig = cmdJson.get("HostConfig");
        cmd.withHostConfig(new HostConfig().withPortBindings(parsePortBindings(hostConfig.get("PortBindings"))).withBinds(parseBinds(hostConfig.get("Binds"))).withPrivileged(hostConfig.get("Privileged").asBoolean()).withNetworkMode(hostConfig.get("NetworkMode").asText()).withRestartPolicy(RestartPolicy.parse(hostConfig.get("RestartPolicy").asText())));

        // 5. 创建并启动容器
        String containerId = cmd.exec().getId();
        dockerClient.startContainerCmd(containerId).exec();
        System.out.println("容器启动成功！ID: " + containerId);
    }

    // 辅助方法：解析端口绑定
    private static Ports parsePortBindings(JsonNode portBindings) {
        Ports ports = new Ports();
        portBindings.fields().forEachRemaining(entry -> {
            String[] parts = entry.getKey().split("/");
            ports.bind(ExposedPort.tcp(Integer.parseInt(parts[0])), Ports.Binding.bindPort(entry.getValue().asInt()));
        });
        return ports;
    }

    // 辅助方法：解析卷绑定
    private static Bind[] parseBinds(JsonNode binds) {
        return StreamSupport.stream(binds.spliterator(), false).map(bind -> Bind.parse(bind.asText())).toArray(Bind[]::new);
    }

    private static String[] convertToStringArray(JsonNode jsonNode) {
        if (jsonNode == null || !jsonNode.isArray()) {
            return new String[0];
        }
        List<String> list = new ArrayList<>();
        jsonNode.forEach(node -> list.add(node.asText()));
        return list.toArray(new String[0]);
    }

    // 新增的端口解析方法
    private static ExposedPort[] parseExposedPorts(JsonNode exposedPortsNode) {
        List<ExposedPort> exposedPorts = new ArrayList<>();
        exposedPortsNode.fieldNames().forEachRemaining(port -> exposedPorts.add(ExposedPort.parse(port)) // 关键解析逻辑[6](@ref)
        );
        return exposedPorts.toArray(new ExposedPort[0]);
    }
}