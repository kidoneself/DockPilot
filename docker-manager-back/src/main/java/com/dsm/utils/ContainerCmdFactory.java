package com.dsm.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 将配置文件中的模板转换成cmd
 */
public class ContainerCmdFactory {

    public static CreateContainerCmd fromJson(DockerClient dockerClient, JsonNode root) {
        // 解析各字段
        String name = root.path("name").asText();
        String image = root.path("image").asText();
        JsonNode envNode = root.path("env");
        JsonNode portsNode = root.path("ports");
        JsonNode volumesNode = root.path("volumes");
        String restartPolicy = root.path("restartPolicy").asText();
        String networkMode = root.path("networkMode").asText();
        boolean privileged = root.path("privileged").asBoolean(false);
        JsonNode commandNode = root.path("cmd");

        CreateContainerCmd cmd = dockerClient.createContainerCmd(image)
                .withName(name)
                .withPrivileged(privileged);

        // 环境变量
        if (envNode != null && !envNode.isEmpty()) {
            List<String> envList = new ArrayList<>();
            envNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                String value = entry.getValue().asText();
                // 如果值包含空格，则用引号包裹
                if (value.contains(" ")) {
                    envList.add(String.format("%s='%s'", key, value));
                } else {
                    envList.add(String.format("%s=%s", key, value));
                }
            });
            cmd.withEnv(envList);
        }

        // 端口映射
        if (portsNode != null && !portsNode.isEmpty()) {
            List<PortBinding> portBindings = new ArrayList<>();
            List<ExposedPort> exposedPorts = new ArrayList<>();

            portsNode.fields().forEachRemaining(entry -> {
                String[] parts = entry.getKey().split("/");
                String port = parts[0];
                String protocol = parts.length > 1 ? parts[1] : "tcp";

                ExposedPort exposedPort = ExposedPort.tcp(Integer.parseInt(port));
                exposedPorts.add(exposedPort);

                String hostPort = entry.getValue().asText();
                portBindings.add(new PortBinding(
                        Ports.Binding.bindPort(Integer.parseInt(hostPort)),
                        exposedPort
                ));
            });

            cmd.withExposedPorts(exposedPorts);
            cmd.withPortBindings(portBindings);
        }

        // 卷映射
        if (volumesNode != null && !volumesNode.isEmpty()) {
            List<Bind> binds = new ArrayList<>();
            volumesNode.fields().forEachRemaining(entry -> {
                String hostPath = entry.getKey();
                String containerPath = entry.getValue().asText();
                // 使用绝对路径
                if (!hostPath.startsWith("/")) {
                    hostPath = "/" + hostPath;
                }
                if (!containerPath.startsWith("/")) {
                    containerPath = "/" + containerPath;
                }
                binds.add(new Bind(hostPath, new Volume(containerPath)));
            });
            cmd.withBinds(binds);
        }

        // 重启策略
        if (restartPolicy != null && !restartPolicy.isEmpty()) {
            RestartPolicy restartPolicyObj = RestartPolicy.parse(restartPolicy);
            cmd.withRestartPolicy(restartPolicyObj);
        }

        // 网络模式
        if (networkMode != null && !networkMode.isEmpty()) {
            cmd.withNetworkMode(networkMode);
        }

        // 命令
        if (commandNode != null && !commandNode.isEmpty()) {
            List<String> commands = new ArrayList<>();
            commandNode.forEach(node -> commands.add(node.asText()));
            cmd.withCmd(commands);
        }

        return cmd;
    }
}