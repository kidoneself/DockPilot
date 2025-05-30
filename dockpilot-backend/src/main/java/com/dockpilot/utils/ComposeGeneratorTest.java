package com.dockpilot.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import java.time.Duration;
import java.util.*;

/**
 * ComposeGenerator 测试类
 * 用于获取真实容器信息并测试YAML生成功能
 */
public class ComposeGeneratorTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== ComposeGenerator 真实容器测试 ===");

            // 指定要获取的容器名称（请根据您的实际容器名称修改）
            String[] containerNames = {
                    "naspt-allinone-v2",           
                    "moviepilot-v2-23",
                    "naspt-115-emby",
                    "cloud-media-sync"
                    // 添加更多您想测试的容器名称
            };

            // 创建简单的Docker客户端（避免LogService）
            DockerClient dockerClient = createSimpleDockerClient();
            ComposeGenerator generator = new ComposeGenerator();

            // 获取容器信息
            List<InspectContainerResponse> containers = new ArrayList<>();

            for (String containerName : containerNames) {
                try {
                    System.out.println("正在获取容器信息: " + containerName);
                    InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerName).exec();
                    if (containerInfo != null) {
                        containers.add(containerInfo);
                        System.out.println("✓ 成功获取: " + containerName + " (镜像: " + containerInfo.getConfig().getImage() + ")");
                    }
                } catch (Exception e) {
                    System.out.println("✗ 获取失败: " + containerName + " - " + e.getMessage());
                }
            }

            if (containers.isEmpty()) {
                System.out.println("❌ 没有找到任何容器，请检查容器名称或确保容器正在运行");
                System.out.println("提示：请修改代码中的containerNames数组，填入您的实际容器名称");
                return;
            }

            System.out.println("\n=== 生成完整功能的YAML内容 (包含路径变量化) ===");
            // 使用完整功能生成并保存到文件
            String outputFile = "test-compose-full.yml";
            generator.generateComposeFile(containers, outputFile, null, new HashSet<>());
            System.out.println("✓ 完整YAML已保存到: " + outputFile);
            
            // 读取并显示文件内容
            try {
                String fullYamlContent = java.nio.file.Files.readString(java.nio.file.Path.of(outputFile));
                System.out.println("--- 完整功能生成的内容 ---");
                System.out.println(fullYamlContent);
            } catch (Exception e) {
                System.out.println("读取文件失败: " + e.getMessage());
            }
            
            System.out.println("\n=== 对比：简化版YAML内容 (仅端口变量化) ===");
            // 生成简化版内容用于对比
            String yamlContent = generator.generateComposeContent(containers, new HashSet<>());
            System.out.println("--- 简化版生成的内容 ---");
            System.out.println(yamlContent);

            System.out.println("\n=== 分割后的单服务YAML ===");
            // 测试分割功能 (使用完整版内容)
            try {
                String fullYamlContent = java.nio.file.Files.readString(java.nio.file.Path.of(outputFile));
                Map<String, String> splitYamls = generator.splitComposeContent(fullYamlContent);
                for (Map.Entry<String, String> entry : splitYamls.entrySet()) {
                    System.out.println("--- " + entry.getKey() + " ---");
                    System.out.println(entry.getValue());
                    System.out.println();
                }
            } catch (Exception e) {
                System.out.println("分割测试失败: " + e.getMessage());
            }

            // 保存到文件（可选）
            // generator.generateComposeFile(containers, "test-compose.yml", null, new HashSet<>());
            // System.out.println("YAML已保存到: test-compose.yml");

        } catch (Exception e) {
            System.out.println("❌ 测试失败:");
            e.printStackTrace();
        }
    }

    private static DockerClient createSimpleDockerClient() {
        // 使用默认配置创建DockerClient（避免LogService）
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
                
        return DockerClientImpl.getInstance(config, httpClient);
    }
} 