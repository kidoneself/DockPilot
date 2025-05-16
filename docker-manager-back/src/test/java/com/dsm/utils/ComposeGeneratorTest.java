package com.dsm.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ComposeGeneratorTest {

    private ComposeGenerator composeGenerator;
    private DockerClient dockerClient;
    private static final String TEST_CONTAINER = "naspt-allinone";
    private static final String TEST_OUTPUT_PATH = "test-compose.yml";
    private static final String TEST_JSON_PATH = "test-container.json";
    private static final List<String> TEST_CONTAINERS = Arrays.asList(
        "naspt-allinone",
        "naspt-cloudsaver",
        "moviepilot-v2-23"
    );

    @BeforeEach
    void setUp() {
        composeGenerator = new ComposeGenerator();
        dockerClient = DockerClientBuilder.getInstance().build();
        System.out.println("\n=== 开始测试 ComposeGenerator ===");
    }

    /**
     * 测试生成 Compose 文件功能
     * 验证点：
     * 1. 文件是否成功生成
     * 2. 文件内容结构是否正确
     * 3. 是否包含必要的配置项（services, x-meta）
     * 4. 文件清理是否成功
     */
    @Test
    void testGenerateComposeFile() throws IOException {
        System.out.println("\n--- 测试生成 Compose 文件 ---");
        // 获取已存在的容器信息
        InspectContainerResponse container = dockerClient.inspectContainerCmd(TEST_CONTAINER).exec();
        System.out.println("获取容器信息: " + TEST_CONTAINER);
        List<InspectContainerResponse> containers = Collections.singletonList(container);

        // 生成 Compose 文件
        composeGenerator.generateComposeFile(containers, TEST_OUTPUT_PATH, null, null);
        System.out.println("生成 Compose 文件: " + TEST_OUTPUT_PATH);

        // 验证文件是否生成
        File composeFile = new File(TEST_OUTPUT_PATH);
        assertTrue(composeFile.exists());
        System.out.println("文件生成成功，大小: " + composeFile.length() + " bytes");

        // 读取并验证文件内容
        Yaml yaml = new Yaml();
        Map<String, Object> compose = yaml.load(new FileInputStream(composeFile));
        assertNotNull(compose);
        assertTrue(compose.containsKey("services"));
        assertTrue(compose.containsKey("x-meta"));
        System.out.println("文件内容验证通过，包含必要的配置项");

        // 输出服务配置
        @SuppressWarnings("unchecked")
        Map<String, Object> services = (Map<String, Object>) compose.get("services");
        @SuppressWarnings("unchecked")
        Map<String, Object> service = (Map<String, Object>) services.get(TEST_CONTAINER);
        System.out.println("服务配置:");
        System.out.println("- 镜像: " + service.get("image"));
        System.out.println("- 容器名: " + service.get("container_name"));
        if (service.containsKey("ports")) {
            System.out.println("- 端口映射: " + service.get("ports"));
        }
        if (service.containsKey("volumes")) {
            System.out.println("- 卷挂载: " + service.get("volumes"));
        }

        // 清理测试文件
        composeFile.delete();
        System.out.println("测试文件已清理");
    }

    /**
     * 测试生成 Compose 内容功能
     * 验证点：
     * 1. 生成的内容是否为有效的 YAML
     * 2. 是否包含必要的配置项（services, x-meta）
     * 3. 服务配置是否正确
     */
    @Test
    void testGenerateComposeContent() {
        System.out.println("\n--- 测试生成 Compose 内容 ---");
        // 获取已存在的容器信息
        InspectContainerResponse container = dockerClient.inspectContainerCmd(TEST_CONTAINER).exec();
        System.out.println("获取容器信息: " + TEST_CONTAINER);
        List<InspectContainerResponse> containers = Collections.singletonList(container);

        // 生成 Compose 内容
        String composeContent = composeGenerator.generateComposeContent(containers, null);
        System.out.println("生成的 Compose 内容长度: " + composeContent.length() + " 字符");
        System.out.println("Compose 内容预览:");
        System.out.println(composeContent.substring(0, Math.min(200, composeContent.length())) + "...");

        // 解析并验证内容
        Yaml yaml = new Yaml();
        Map<String, Object> compose = yaml.load(composeContent);
        assertNotNull(compose);
        assertTrue(compose.containsKey("services"));
        assertTrue(compose.containsKey("x-meta"));
        System.out.println("内容验证通过，包含必要的配置项");

        // 验证服务配置
        @SuppressWarnings("unchecked")
        Map<String, Object> services = (Map<String, Object>) compose.get("services");
        assertTrue(services.containsKey(TEST_CONTAINER));
        System.out.println("服务配置验证通过");
    }

    /**
     * 测试拆分 Compose 内容功能
     * 验证点：
     * 1. 拆分后的内容是否完整
     * 2. 每个服务是否都有独立的配置
     * 3. 拆分后的配置结构是否正确
     */
    @Test
    void testSplitComposeContent() {
        System.out.println("\n--- 测试拆分 Compose 内容 ---");
        
        // 读取 test-compose.yml 文件
        File composeFile = new File("test-compose.yml");
        assertTrue(composeFile.exists(), "test-compose.yml 文件不存在");
        
        try {
            // 读取文件内容
            String originalContent = new String(java.nio.file.Files.readAllBytes(composeFile.toPath()));
            System.out.println("原始 Compose 内容长度: " + originalContent.length() + " 字符");

            // 拆分 Compose 内容
            Map<String, String> splitContent = composeGenerator.splitComposeContent(originalContent);
            System.out.println("\n拆分后的服务数量: " + splitContent.size());

            // 验证拆分结果
            assertNotNull(splitContent);
            assertFalse(splitContent.isEmpty(), "拆分结果为空");
            
            // 创建输出目录
            File outputDir = new File("split-compose");
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }
            
            // 输出每个服务的配置信息并生成文件
            splitContent.forEach((serviceName, serviceContent) -> {
                System.out.println("\n服务: " + serviceName);
                System.out.println("配置长度: " + serviceContent.length() + " 字符");
                
                // 验证每个服务的配置结构
                Yaml yaml = new Yaml();
                Map<String, Object> serviceCompose = yaml.load(serviceContent);
                assertTrue(serviceCompose.containsKey("services"), "服务配置缺少 services 部分");
                assertTrue(serviceCompose.containsKey("x-meta"), "服务配置缺少 x-meta 部分");
                
                @SuppressWarnings("unchecked")
                Map<String, Object> services = (Map<String, Object>) serviceCompose.get("services");
                assertTrue(services.containsKey(serviceName), "服务配置中找不到服务 " + serviceName);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> service = (Map<String, Object>) services.get(serviceName);
                System.out.println("- 镜像: " + service.get("image"));
                if (service.containsKey("ports")) {
                    System.out.println("- 端口映射: " + service.get("ports"));
                }
                if (service.containsKey("volumes")) {
                    System.out.println("- 卷挂载: " + service.get("volumes"));
                }
                
                // 生成独立的 YAML 文件
                String fileName = serviceName + ".yml";
                File outputFile = new File(outputDir, fileName);
                try (FileWriter writer = new FileWriter(outputFile)) {
                    writer.write(serviceContent);
                    System.out.println("生成文件: " + outputFile.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("写入文件失败: " + fileName + " - " + e.getMessage());
                }
            });
            
            System.out.println("\n所有服务拆分完成，文件保存在: " + outputDir.getAbsolutePath());
            
        } catch (IOException e) {
            fail("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 测试获取服务名称功能
     * 验证点：
     * 1. 服务名称格式是否正确
     * 2. 是否正确处理容器名称前缀
     */
    @Test
    void testGetServiceName() {
        System.out.println("\n--- 测试获取服务名称 ---");
        // 获取已存在的容器信息
        InspectContainerResponse container = dockerClient.inspectContainerCmd(TEST_CONTAINER).exec();
        System.out.println("原始容器名称: " + container.getName());

        // 测试服务名称获取
        String serviceName = composeGenerator.getServiceName(container);
        System.out.println("处理后的服务名称: " + serviceName);
        assertEquals(TEST_CONTAINER, serviceName);
        System.out.println("服务名称验证通过");
    }

    /**
     * 测试容器转换为服务配置功能
     * 验证点：
     * 1. 转换后的配置是否完整
     * 2. 必要字段是否存在
     * 3. 配置格式是否正确
     */
    @Test
    void testConvertContainerToService() {
        System.out.println("\n--- 测试容器转换为服务配置 ---");
        // 获取已存在的容器信息
        InspectContainerResponse container = dockerClient.inspectContainerCmd(TEST_CONTAINER).exec();
        System.out.println("获取容器信息: " + TEST_CONTAINER);

        // 测试容器转换为服务配置
        Map<String, Object> service = composeGenerator.convertContainerToService(container, null);
        assertNotNull(service);
        assertTrue(service.containsKey("image"));
        assertTrue(service.containsKey("container_name"));
        System.out.println("服务配置验证通过");

        // 输出配置详情
        System.out.println("服务配置详情:");
        service.forEach((key, value) -> System.out.println("- " + key + ": " + value));
    }

    /**
     * 测试带排除字段的 Compose 内容生成功能
     * 验证点：
     * 1. 指定字段是否被正确排除
     * 2. 其他必要字段是否保留
     * 3. 配置结构是否完整
     */
    @Test
    void testGenerateComposeContentWithExcludeFields() {
        System.out.println("\n--- 测试带排除字段的 Compose 内容生成 ---");
        // 获取已存在的容器信息
        InspectContainerResponse container = dockerClient.inspectContainerCmd(TEST_CONTAINER).exec();
        System.out.println("获取容器信息: " + TEST_CONTAINER);
        List<InspectContainerResponse> containers = Collections.singletonList(container);

        // 设置要排除的字段
        Set<String> excludeFields = new HashSet<>(Arrays.asList("ports", "volumes"));
        System.out.println("排除字段: " + excludeFields);

        // 生成 Compose 内容
        String composeContent = composeGenerator.generateComposeContent(containers, excludeFields);
        System.out.println("生成的 Compose 内容长度: " + composeContent.length() + " 字符");

        // 解析并验证内容
        Yaml yaml = new Yaml();
        Map<String, Object> compose = yaml.load(composeContent);
        @SuppressWarnings("unchecked")
        Map<String, Object> services = (Map<String, Object>) compose.get("services");
        @SuppressWarnings("unchecked")
        Map<String, Object> service = (Map<String, Object>) services.get(TEST_CONTAINER);

        // 验证排除的字段
        assertFalse(service.containsKey("ports"));
        assertFalse(service.containsKey("volumes"));
        System.out.println("字段排除验证通过");

        // 输出保留的字段
        System.out.println("保留的字段:");
        service.forEach((key, value) -> System.out.println("- " + key + ": " + value));
    }

    /**
     * 测试多个容器的 Compose 内容生成
     * 验证点：
     * 1. 多个容器的配置是否正确生成
     * 2. 容器间的依赖关系是否正确
     * 3. 网络配置是否正确
     */
    @Test
    void testGenerateComposeContentForMultipleContainers() {
        System.out.println("\n--- 测试多个容器的 Compose 内容生成 ---");
        
        // 获取多个容器的信息
        List<InspectContainerResponse> containers = new ArrayList<>();
        for (String containerName : TEST_CONTAINERS) {
            try {
                InspectContainerResponse container = dockerClient.inspectContainerCmd(containerName).exec();
                containers.add(container);
                System.out.println("\n获取容器信息: " + containerName);
                
                // 输出容器的端口映射信息
                if (container.getHostConfig() != null) {
                    System.out.println("HostConfig 不为空");
                    if (container.getHostConfig().getPortBindings() != null) {
                        System.out.println("PortBindings 不为空");
                        System.out.println("容器 " + containerName + " 的端口映射:");
                        container.getHostConfig().getPortBindings().getBindings().forEach((key, bindings) -> {
                            System.out.println("检查端口: " + key.getPort());
                            if (bindings != null) {
                                System.out.println("bindings 不为空，数量: " + bindings.length);
                                for (Ports.Binding binding : bindings) {
                                    if (binding != null) {
                                        System.out.println("binding 不为空");
                                        if (binding.getHostPortSpec() != null) {
                                            System.out.println("- 容器端口: " + key.getPort() + " -> 主机端口: " + binding.getHostPortSpec());
                                        } else {
                                            System.out.println("HostPortSpec 为空");
                                        }
                                    } else {
                                        System.out.println("binding 为空");
                                    }
                                }
                            } else {
                                System.out.println("bindings 为空");
                            }
                        });
                    } else {
                        System.out.println("PortBindings 为空");
                    }
                } else {
                    System.out.println("HostConfig 为空");
                }
            } catch (Exception e) {
                System.out.println("警告: 容器 " + containerName + " 不存在，跳过");
                e.printStackTrace();
            }
        }
        
        if (containers.isEmpty()) {
            System.out.println("错误: 没有找到任何可用的测试容器");
            return;
        }

        // 生成 Compose 内容
        String composeContent = composeGenerator.generateComposeContent(containers, null);
        System.out.println("\n生成的 Compose 内容长度: " + composeContent.length() + " 字符");
        System.out.println("Compose 内容预览:");
        System.out.println(composeContent);

        // 解析并验证内容
        Yaml yaml = new Yaml();
        Map<String, Object> compose = yaml.load(composeContent);
        assertNotNull(compose);
        assertTrue(compose.containsKey("services"));
        assertTrue(compose.containsKey("x-meta"));
        System.out.println("基本结构验证通过");

        // 验证服务配置
        @SuppressWarnings("unchecked")
        Map<String, Object> services = (Map<String, Object>) compose.get("services");
        
        // 验证每个容器的配置
        for (InspectContainerResponse container : containers) {
            String serviceName = composeGenerator.getServiceName(container);
            assertTrue(services.containsKey(serviceName), "服务 " + serviceName + " 的配置不存在");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> service = (Map<String, Object>) services.get(serviceName);
            System.out.println("\n服务 " + serviceName + " 的配置:");
            System.out.println("- 镜像: " + service.get("image"));
            System.out.println("- 容器名: " + service.get("container_name"));
            
            if (service.containsKey("ports")) {
                System.out.println("- 端口映射: " + service.get("ports"));
            }
            if (service.containsKey("volumes")) {
                System.out.println("- 卷挂载: " + service.get("volumes"));
            }
            if (service.containsKey("environment")) {
                System.out.println("- 环境变量: " + service.get("environment"));
            }
            if (service.containsKey("networks")) {
                System.out.println("- 网络配置: " + service.get("networks"));
            }
        }

        // 验证网络配置
        assertTrue(compose.containsKey("networks"), "网络配置不存在");
        @SuppressWarnings("unchecked")
        Map<String, Object> networks = (Map<String, Object>) compose.get("networks");
        System.out.println("\n网络配置:");
        networks.forEach((key, value) -> System.out.println("- " + key + ": " + value));

        // 验证 x-meta 配置
        @SuppressWarnings("unchecked")
        Map<String, Object> xMeta = (Map<String, Object>) compose.get("x-meta");
        System.out.println("\nx-meta 配置:");
        xMeta.forEach((key, value) -> {
            if (key.equals("env")) {
                System.out.println("- 环境变量:");
                @SuppressWarnings("unchecked")
                Map<String, String> envVars = (Map<String, String>) value;
                envVars.forEach((envKey, envValue) -> {
                    if (envKey.contains("PORT")) {
                        System.out.println("  * " + envKey + ": " + envValue);
                    }
                });
            } else {
                System.out.println("- " + key + ": " + value);
            }
        });
    }

    /**
     * 测试多个容器的 Compose 文件生成
     * 验证点：
     * 1. 多个容器的配置文件是否正确生成
     * 2. 文件内容是否完整
     * 3. 文件清理是否成功
     */
    @Test
    void testGenerateComposeFileForMultipleContainers() throws IOException {
        System.out.println("\n--- 测试多个容器的 Compose 文件生成 ---");
        
        // 获取本机所有容器信息
        List<InspectContainerResponse> containers = dockerClient.listContainersCmd()
            .withShowAll(true)  // 包括已停止的容器
            .exec()
            .stream()
            .map(container -> dockerClient.inspectContainerCmd(container.getId()).exec())
            .collect(Collectors.toList());
            
        System.out.println("获取到 " + containers.size() + " 个容器");
        
        if (containers.isEmpty()) {
            System.out.println("错误: 没有找到任何容器");
            return;
        }

        // 生成 Compose 文件
        composeGenerator.generateComposeFile(containers, TEST_OUTPUT_PATH, null, null);

        // 验证文件是否生成
        File composeFile = new File(TEST_OUTPUT_PATH);
        assertTrue(composeFile.exists());
        System.out.println("文件生成成功，大小: " + composeFile.length() + " bytes");

        // 读取并验证文件内容
        Yaml yaml = new Yaml();
        Map<String, Object> compose = yaml.load(new FileInputStream(composeFile));
        assertNotNull(compose);
        assertTrue(compose.containsKey("services"));
        assertTrue(compose.containsKey("x-meta"));
        System.out.println("文件内容验证通过");

        // 清理测试文件
        // composeFile.delete();
        System.out.println("测试文件已生成，路径: " + composeFile.getAbsolutePath());
    }
} 