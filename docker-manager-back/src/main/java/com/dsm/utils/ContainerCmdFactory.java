package com.dsm.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
        JsonNode commandNode = root.path("command");
        JsonNode entrypointNode = root.path("entrypoint");

        CreateContainerCmd cmd = dockerClient.createContainerCmd(image);
        cmd.withName(name);
        // 环境变量
        if (envNode != null && !envNode.isEmpty()) {
            Iterator<Map.Entry<String, JsonNode>> envIter = envNode.fields();
            java.util.List<String> envList = new java.util.ArrayList<>();
            while (envIter.hasNext()) {
                Map.Entry<String, JsonNode> entry = envIter.next();
                envList.add(entry.getKey() + "=" + entry.getValue().asText());
            }
            cmd.withEnv(envList);
        }
        // 端口映射
        if (portsNode != null && !portsNode.isEmpty()) {
            java.util.Map<com.github.dockerjava.api.model.ExposedPort, com.github.dockerjava.api.model.Ports.Binding> portBindings = new java.util.HashMap<>();
            java.util.List<com.github.dockerjava.api.model.ExposedPort> exposedPorts = new java.util.ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> portIter = portsNode.fields();
            while (portIter.hasNext()) {
                Map.Entry<String, JsonNode> entry = portIter.next();
                String containerPortProto = entry.getKey(); // 例如 3000/tcp
                String hostPort = entry.getValue().asText();
                String[] portParts = containerPortProto.split("/");
                int port = Integer.parseInt(portParts[0]);
                String proto = portParts.length > 1 ? portParts[1] : "tcp";
                com.github.dockerjava.api.model.ExposedPort exposedPort = new com.github.dockerjava.api.model.ExposedPort(port, com.github.dockerjava.api.model.InternetProtocol.parse(proto));
                exposedPorts.add(exposedPort);
                com.github.dockerjava.api.model.Ports.Binding binding = com.github.dockerjava.api.model.Ports.Binding.bindPort(Integer.parseInt(hostPort));
                portBindings.put(exposedPort, binding);
            }
            cmd.withExposedPorts(exposedPorts);
            com.github.dockerjava.api.model.Ports ports = new com.github.dockerjava.api.model.Ports();
            for (Map.Entry<com.github.dockerjava.api.model.ExposedPort, com.github.dockerjava.api.model.Ports.Binding> entry : portBindings.entrySet()) {
                ports.bind(entry.getKey(), entry.getValue());
            }
            cmd.withPortBindings(ports);
        }
        // 卷挂载
        if (volumesNode != null && !volumesNode.isEmpty()) {
            java.util.List<com.github.dockerjava.api.model.Bind> binds = new java.util.ArrayList<>();
            java.util.List<com.github.dockerjava.api.model.Volume> volumes = new java.util.ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> volIter = volumesNode.fields();
            while (volIter.hasNext()) {
                Map.Entry<String, JsonNode> entry = volIter.next();
                String hostPath = entry.getKey();
                String containerPath = entry.getValue().asText();
                com.github.dockerjava.api.model.Volume volume = new com.github.dockerjava.api.model.Volume(containerPath);
                com.github.dockerjava.api.model.Bind bind = new com.github.dockerjava.api.model.Bind(hostPath, volume);
                binds.add(bind);
                volumes.add(volume);
            }
            cmd.withBinds(binds);
            cmd.withVolumes(volumes);
        }
        // 重启策略
        if (!restartPolicy.isEmpty()) {
            cmd.withRestartPolicy(com.github.dockerjava.api.model.RestartPolicy.parse(restartPolicy));
        }
        // 网络模式
        if (!networkMode.isEmpty()) {
            cmd.withNetworkMode(networkMode);
        }
        // command
        if (commandNode != null && commandNode.isArray() && !commandNode.isEmpty()) {
            String[] cmdArr = new String[commandNode.size()];
            for (int i = 0; i < commandNode.size(); i++) {
                cmdArr[i] = commandNode.get(i).asText();
            }
            cmd.withCmd(cmdArr);
        }
        // entrypoint
        if (entrypointNode != null && entrypointNode.isArray() && !entrypointNode.isEmpty()) {
            String[] entryArr = new String[entrypointNode.size()];
            for (int i = 0; i < entrypointNode.size(); i++) {
                entryArr[i] = entrypointNode.get(i).asText();
            }
            cmd.withEntrypoint(entryArr);
        }
        return cmd;
    }
}