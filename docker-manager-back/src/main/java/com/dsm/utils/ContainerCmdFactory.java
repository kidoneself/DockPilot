package com.dsm.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;

import java.util.*;

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
            envNode.fields().forEachRemaining(entry -> 
                envList.add(entry.getKey() + "=" + entry.getValue().asText())
            );
            cmd.withEnv(envList);
        }

        // 端口映射
        if (portsNode != null && !portsNode.isEmpty()) {
            Map<ExposedPort, Ports.Binding> portBindings = new HashMap<>();
            List<ExposedPort> exposedPorts = new ArrayList<>();
            
            portsNode.fields().forEachRemaining(entry -> {
                String containerPortProto = entry.getKey(); // 例如 3000/tcp
                String hostPort = entry.getValue().asText();
                String[] portParts = containerPortProto.split("/");
                int port = Integer.parseInt(portParts[0]);
                String proto = portParts.length > 1 ? portParts[1] : "tcp";
                ExposedPort exposedPort = new ExposedPort(port, InternetProtocol.parse(proto));
                exposedPorts.add(exposedPort);
                Ports.Binding binding = Ports.Binding.bindPort(Integer.parseInt(hostPort));
                portBindings.put(exposedPort, binding);
            });

            cmd.withExposedPorts(exposedPorts);
            Ports ports = new Ports();
            portBindings.forEach(ports::bind);
            cmd.withPortBindings(ports);
        }

        // 卷挂载
        if (volumesNode != null && !volumesNode.isEmpty()) {
            List<Bind> binds = new ArrayList<>();
            List<Volume> volumes = new ArrayList<>();
            
            volumesNode.fields().forEachRemaining(entry -> {
                String hostPath = entry.getKey();
                String containerPath = entry.getValue().asText();
                Volume volume = new Volume(containerPath);
                Bind bind = new Bind(hostPath, volume);
                binds.add(bind);
                volumes.add(volume);
            });

            cmd.withBinds(binds);
            cmd.withVolumes(volumes);
        }

        // 重启策略
        if (!restartPolicy.isEmpty()) {
            cmd.withRestartPolicy(RestartPolicy.parse(restartPolicy));
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

        return cmd;
    }
}