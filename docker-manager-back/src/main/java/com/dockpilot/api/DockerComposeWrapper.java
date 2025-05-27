package com.dockpilot.api;

import com.dockpilot.common.exception.BusinessException;
import com.dockpilot.utils.LogUtil;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Docker Compose 操作封装类
 * 提供与 Docker Compose 交互的各种操作
 */
@Component
public class DockerComposeWrapper {

    /**
     * 部署 Compose 配置
     *
     * @param projectName    项目名称
     * @param composeContent Compose 配置内容
     * @return 部署结果
     */
    public String deployCompose(String projectName, String composeContent) {
        String workDir = createWorkDir(projectName);
        try {
            // 保存 compose 文件
            saveComposeFile(workDir, composeContent);

            // 执行 docker-compose up
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "up", "-d");
            pb.directory(new File(workDir));
            Process process = pb.start();

            // 等待执行完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BusinessException("Compose 部署失败");
            }

            LogUtil.logOpe("Compose 部署成功: " + projectName);
            return getContainerIds(workDir);
        } catch (Exception e) {
            LogUtil.logSysError("Compose 部署失败: " + e.getMessage());
            throw new BusinessException("Compose 部署失败: " + e.getMessage());
        } finally {
            // 清理工作目录
            deleteWorkDir(workDir);
        }
    }

    /**
     * 更新 Compose 配置
     *
     * @param projectName    项目名称
     * @param composeContent 新的 Compose 配置内容
     * @return 更新结果
     */
    public String updateCompose(String projectName, String composeContent) {
        String workDir = createWorkDir(projectName);
        try {
            // 保存新的 compose 文件
            saveComposeFile(workDir, composeContent);

            // 执行 docker-compose up -d
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "up", "-d", "--force-recreate");
            pb.directory(new File(workDir));
            Process process = pb.start();

            // 等待执行完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BusinessException("Compose 更新失败");
            }

            LogUtil.logOpe("Compose 更新成功: " + projectName);
            return getContainerIds(workDir);
        } catch (Exception e) {
            LogUtil.logSysError("Compose 更新失败: " + e.getMessage());
            throw new BusinessException("Compose 更新失败: " + e.getMessage());
        } finally {
            // 清理工作目录
            deleteWorkDir(workDir);
        }
    }

    /**
     * 停止并删除 Compose 项目
     *
     * @param projectName 项目名称
     */
    public void removeCompose(String projectName) {
        String workDir = createWorkDir(projectName);
        try {
            // 执行 docker-compose down
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "down");
            pb.directory(new File(workDir));
            Process process = pb.start();

            // 等待执行完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BusinessException("Compose 删除失败");
            }

            LogUtil.logOpe("Compose 删除成功: " + projectName);
        } catch (Exception e) {
            LogUtil.logSysError("Compose 删除失败: " + e.getMessage());
            throw new BusinessException("Compose 删除失败: " + e.getMessage());
        } finally {
            // 清理工作目录
            deleteWorkDir(workDir);
        }
    }

    /**
     * 获取 Compose 项目状态
     *
     * @param projectName 项目名称
     * @return 项目状态信息
     */
    public Map<String, Object> getComposeStatus(String projectName) {
        String workDir = createWorkDir(projectName);
        try {
            // 执行 docker-compose ps
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "ps", "--format", "json");
            pb.directory(new File(workDir));
            Process process = pb.start();

            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            // 等待执行完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BusinessException("获取 Compose 状态失败");
            }

            return parseComposeStatus(output.toString());
        } catch (Exception e) {
            LogUtil.logSysError("获取 Compose 状态失败: " + e.getMessage());
            throw new BusinessException("获取 Compose 状态失败: " + e.getMessage());
        } finally {
            // 清理工作目录
            deleteWorkDir(workDir);
        }
    }

    /**
     * 创建临时工作目录
     */
    private String createWorkDir(String projectName) {
        try {
            Path workDir = Paths.get(System.getProperty("java.io.tmpdir"), "docker-compose-" + projectName);
            Files.createDirectories(workDir);
            return workDir.toString();
        } catch (Exception e) {
            throw new BusinessException("创建工作目录失败: " + e.getMessage());
        }
    }

    /**
     * 保存 Compose 文件
     */
    private void saveComposeFile(String workDir, String content) {
        try {
            Path composeFile = Paths.get(workDir, "docker-compose.yml");
            Files.write(composeFile, content.getBytes());
        } catch (Exception e) {
            throw new BusinessException("保存 Compose 文件失败: " + e.getMessage());
        }
    }

    /**
     * 删除工作目录
     */
    private void deleteWorkDir(String workDir) {
        try {
            Path path = Paths.get(workDir);
            Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception e) {
                            LogUtil.logSysError("删除文件失败: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            LogUtil.logSysError("删除工作目录失败: " + e.getMessage());
        }
    }

    /**
     * 获取容器ID列表
     */
    private String getContainerIds(String workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "ps", "-q");
            pb.directory(new File(workDir));
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> containerIds = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                containerIds.add(line.trim());
            }

            return String.join(",", containerIds);
        } catch (Exception e) {
            throw new BusinessException("获取容器ID失败: " + e.getMessage());
        }
    }

    /**
     * 解析 Compose 状态信息
     */
    private Map<String, Object> parseComposeStatus(String statusJson) {
        // TODO: 实现 JSON 解析逻辑
        return null;
    }
} 