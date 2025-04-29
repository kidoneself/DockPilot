package com.dsm.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;

import java.util.ArrayList;
import java.util.List;

public class DockerInspectConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    // 核心转换方法（入口）
    public static CreateContainerCmd convertToDockerJavaConfig(JsonNode inspectJson, DockerClient dockerClient) {
        // 提取基础配置
        String image = inspectJson.path("Config").path("Image").asText();
        CreateContainerCmd cmd = dockerClient.createContainerCmd(image);

        // 环境变量
        JsonNode envArray = inspectJson.path("Config").path("Env");
        cmd.withEnv(convertToStringArray(envArray));

        // 暴露端口
        JsonNode exposedPorts = inspectJson.path("Config").path("ExposedPorts");
        exposedPorts.fieldNames().forEachRemaining(port -> cmd.withExposedPorts(ExposedPort.parse(port)));

        // 入口点
        JsonNode entrypoint = inspectJson.path("Config").path("Entrypoint");
        if (entrypoint.isArray()) {
            cmd.withEntrypoint(convertToStringArray(entrypoint));
        }

        // 主机配置
        HostConfig hostConfig = buildHostConfig(inspectJson.path("HostConfig"));
        cmd.withHostConfig(hostConfig);

        return cmd;
    }

    // 构建主机配置
    private static HostConfig buildHostConfig(JsonNode hostConfigJson) {
        HostConfig hostConfig = new HostConfig();

        // 端口绑定
        JsonNode portBindings = hostConfigJson.path("PortBindings");
        Ports ports = new Ports();
        portBindings.fields().forEachRemaining(entry -> {
            String containerPort = entry.getKey();
            JsonNode bindingArray = entry.getValue();
            bindingArray.forEach(binding -> ports.bind(ExposedPort.parse(containerPort), Ports.Binding.parse(binding.path("HostPort").asText())));
        });
        hostConfig.withPortBindings(ports);

        // 卷绑定
        JsonNode binds = hostConfigJson.path("Binds");
        List<Bind> bindList = new ArrayList<>();
        binds.forEach(bind -> bindList.add(Bind.parse(bind.asText())));
        hostConfig.withBinds(bindList);

        // 重启策略
        JsonNode restartPolicy = hostConfigJson.path("RestartPolicy");
        hostConfig.withRestartPolicy(RestartPolicy.parse(restartPolicy.path("Name").asText() + ":" + restartPolicy.path("MaximumRetryCount").asInt()));

        // 特权模式
        hostConfig.withPrivileged(hostConfigJson.path("Privileged").asBoolean());

        return hostConfig;
    }

    // JSON数组转字符串数组
    private static String[] convertToStringArray(JsonNode jsonArray) {
        List<String> list = new ArrayList<>();
        jsonArray.elements().forEachRemaining(node -> list.add(node.asText()));
        return list.toArray(new String[0]);
    }
}