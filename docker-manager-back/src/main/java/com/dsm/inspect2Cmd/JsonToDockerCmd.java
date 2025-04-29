package com.dsm.inspect2Cmd;

import com.dsm.utils.ContainerCmdFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.core.DockerClientBuilder;

import java.io.File;

/**
 * 根据简化 json 生成 docker-java 的 CreateContainerCmd
 * 生成时间：2024-06-09 20:00:00
 */
public class JsonToDockerCmd {
    public static void main(String[] args) {
        // 1. 读取简化 json 文件（如 naspt-mpv2.json）
        String jsonPath = "/Users/lizhiqiang/coding-my/docker/docker-manager-back/src/main/java/com/dsm/inspect2Cmd/naspt-mpv2.json";
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(new File(jsonPath));
            // 3. 构造 docker-java CreateContainerCmd（使用工具类）
            DockerClient dockerClient = DockerClientBuilder.getInstance().build();
            CreateContainerCmd cmd = ContainerCmdFactory.fromJson(dockerClient, root);
            // 实际创建容器
            String containerId = cmd.exec().getId();
            dockerClient.startContainerCmd(containerId).exec();
            System.out.println("容器已创建，ID: " + containerId);
        } catch (Exception e) {
            System.err.println("解析或生成 docker-java 命令失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 