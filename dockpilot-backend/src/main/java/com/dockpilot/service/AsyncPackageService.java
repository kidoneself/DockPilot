package com.dockpilot.service;

import com.dockpilot.model.PackageTask;
import com.dockpilot.utils.ComposeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步打包服务
 */
@Slf4j
@Service
public class AsyncPackageService {
    
    @Autowired
    private ComposeGenerator composeGenerator;
    
    // 🔥 从配置文件读取包存储路径
    @Value("${file.package.path}")
    private String packageStoragePath;
    
    // 内存存储任务状态（简单实现，生产环境可以用Redis）
    private final Map<String, PackageTask> taskMap = new ConcurrentHashMap<>();
    
    /**
     * 启动异步打包任务
     */
    public String startPackageTask(List<String> containerIds, String projectName, 
                                 String description, List<String> selectedPaths) {
        String taskId = UUID.randomUUID().toString();
        
        PackageTask task = new PackageTask();
        task.setTaskId(taskId);
        task.setStatus("pending");
        task.setProgress(0);
        task.setCurrentStep("准备开始打包...");
        task.setProjectName(projectName);
        task.setContainerIds(containerIds);
        task.setSelectedPaths(selectedPaths);
        task.setCreateTime(LocalDateTime.now());
        
        taskMap.put(taskId, task);
        
        // 异步执行打包
        executePackageAsync(taskId);
        
        log.info("📦 启动异步打包任务: {} (项目: {})", taskId, projectName);
        return taskId;
    }
    
    /**
     * 获取任务状态
     */
    public PackageTask getTaskStatus(String taskId) {
        return taskMap.get(taskId);
    }
    
    /**
     * 异步执行打包
     */
    @Async("packageTaskExecutor")
    public void executePackageAsync(String taskId) {
        PackageTask task = taskMap.get(taskId);
        if (task == null) {
            log.error("❌ 任务不存在: {}", taskId);
            return;
        }
        
        try {
            log.info("🚀 开始执行异步打包任务: {}", taskId);
            log.info("📁 使用包存储路径: {}", packageStoragePath);
            
            // 更新状态为处理中
            updateTaskStatus(taskId, "processing", 10, "正在生成YAML配置...");
            
            // 1. 生成YAML内容
            String yamlContent = composeGenerator.generateFromContainerIds(
                task.getContainerIds(), 
                task.getProjectName(), 
                "Docker容器管理项目"
            );
            
            updateTaskStatus(taskId, "processing", 30, "YAML生成完成，准备打包配置文件...");
            
            // 2. 创建导出目录 - 使用配置的包存储路径
            // 确保基础目录存在
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(packageStoragePath));
            
            String exportId = "export_" + taskId;
            String exportDir = packageStoragePath + "/" + exportId;
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(exportDir));
            
            // 3. 保存YAML文件
            String yamlFile = exportDir + "/docker-compose.yml";
            java.nio.file.Files.write(java.nio.file.Paths.get(yamlFile), 
                yamlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            updateTaskStatus(taskId, "processing", 50, "正在打包容器配置文件...");
            
            // 4. 生成配置包
            Map<String, String> configPackages = composeGenerator.generateConfigPackages(
                task.getContainerIds(), exportDir, task.getSelectedPaths()
            );
            
            updateTaskStatus(taskId, "processing", 80, "正在生成最终压缩包...");
            
            // 5. 生成README
            generateReadmeFile(exportDir, task.getProjectName(), configPackages);
            
            // 6. 创建ZIP包
            String fileName = (task.getProjectName() != null ? task.getProjectName() : "docker-project") + "-export.zip";
            String zipFile = exportDir + ".zip";
            createZipFromDirectory(exportDir, zipFile);
            
            // 7. 计算文件大小
            File file = new File(zipFile);
            long fileSize = file.exists() ? file.length() : 0;
            
            // 8. 任务完成
            task.setStatus("completed");
            task.setProgress(100);
            task.setCurrentStep("打包完成");
            task.setFilePath(zipFile);
            task.setFileName(fileName);
            task.setFileSize(fileSize);
            task.setCompleteTime(LocalDateTime.now());
            
            log.info("✅ 异步打包任务完成: {} (文件: {}, 大小: {}字节)", taskId, fileName, fileSize);
            
        } catch (Exception e) {
            log.error("❌ 异步打包任务失败: {}", taskId, e);
            
            task.setStatus("failed");
            task.setCurrentStep("打包失败");
            task.setErrorMessage(e.getMessage());
            task.setCompleteTime(LocalDateTime.now());
        }
    }
    
    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, String status, Integer progress, String currentStep) {
        PackageTask task = taskMap.get(taskId);
        if (task != null) {
            task.setStatus(status);
            task.setProgress(progress);
            task.setCurrentStep(currentStep);
            log.info("📊 任务进度更新: {} - {}% - {}", taskId, progress, currentStep);
        }
    }
    
    /**
     * 清理已完成的任务（可定时调用）
     */
    public void cleanupCompletedTasks() {
        taskMap.entrySet().removeIf(entry -> {
            PackageTask task = entry.getValue();
            if (("completed".equals(task.getStatus()) || "failed".equals(task.getStatus())) 
                && task.getCompleteTime() != null 
                && task.getCompleteTime().isBefore(LocalDateTime.now().minusHours(1))) {
                
                // 删除临时文件
                if (task.getFilePath() != null) {
                    try {
                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(task.getFilePath()));
                    } catch (Exception e) {
                        log.warn("删除临时文件失败: {}", task.getFilePath());
                    }
                }
                
                log.info("🧹 清理已完成任务: {}", task.getTaskId());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 生成README文件
     */
    private void generateReadmeFile(String exportDir, String projectName, Map<String, String> configPackages) throws Exception {
        StringBuilder readme = new StringBuilder();
        readme.append("# ").append(projectName).append(" 导出包\n\n");
        readme.append("## 📁 文件说明\n\n");
        readme.append("- `docker-compose.yml` - Docker Compose配置文件\n");
        
        if (!configPackages.isEmpty()) {
            readme.append("- 服务配置包:\n");
            for (Map.Entry<String, String> entry : configPackages.entrySet()) {
                readme.append("  - `").append(entry.getValue())
                      .append("` - ").append(entry.getKey()).append(" 服务配置\n");
            }
        }
        
        readme.append("\n## 🚀 使用方法\n\n");
        readme.append("1. 解压配置包到对应的服务目录\n");
        readme.append("2. 根据需要修改 `docker-compose.yml` 中的环境变量\n");
        readme.append("3. 运行: `docker-compose up -d`\n\n");
        
        String readmeFile = exportDir + "/README.md";
        java.nio.file.Files.write(java.nio.file.Paths.get(readmeFile), 
            readme.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    
    /**
     * 创建ZIP文件
     */
    private void createZipFromDirectory(String sourceDir, String zipFile) throws Exception {
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                java.nio.file.Files.newOutputStream(java.nio.file.Paths.get(zipFile)))) {
            
            java.nio.file.Path sourcePath = java.nio.file.Paths.get(sourceDir);
            java.nio.file.Files.walk(sourcePath)
                .filter(path -> !java.nio.file.Files.isDirectory(path))
                .forEach(path -> {
                    java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(
                        sourcePath.relativize(path).toString());
                    try {
                        zos.putNextEntry(zipEntry);
                        java.nio.file.Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (Exception e) {
                        throw new RuntimeException("添加文件到ZIP失败: " + path, e);
                    }
                });
        }
    }
} 