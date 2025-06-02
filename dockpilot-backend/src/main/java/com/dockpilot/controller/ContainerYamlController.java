package com.dockpilot.controller;

import com.dockpilot.api.DockerService;
import com.dockpilot.model.ContainerYamlRequest;
import com.dockpilot.model.ContainerYamlResponse;
import com.dockpilot.utils.ApiResponse;
import com.dockpilot.utils.ComposeGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.dockpilot.model.ContainerPathInfo;
import com.dockpilot.model.PackageTask;
import com.dockpilot.service.AsyncPackageService;

/**
 * 容器YAML生成控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/containers")
@Tag(name = "容器YAML生成", description = "容器YAML配置生成接口")
@Validated
public class ContainerYamlController {
    
    @Autowired
    private ComposeGenerator composeGenerator;
    
    @Autowired
    private DockerService dockerService;
    
    @Autowired
    private AsyncPackageService asyncPackageService;
    
    /**
     * 根据容器ID列表生成YAML配置
     */
    @PostMapping("/generate-yaml")
    @Operation(summary = "生成YAML配置", description = "根据选中的容器ID列表生成Docker Compose YAML配置")
    public ApiResponse<ContainerYamlResponse> generateYaml(@Valid @RequestBody ContainerYamlRequest request) {
        try {
            // 确定项目名称和描述
            String projectName = request.getProjectName();
            if (projectName == null || projectName.trim().isEmpty()) {
                projectName = "容器项目-" + System.currentTimeMillis();
            }
            
            String projectDescription = request.getDescription();
            if (projectDescription == null || projectDescription.trim().isEmpty()) {
                projectDescription = "Docker容器管理项目";
            }
            
            // 生成YAML内容 - 传递用户提供的项目信息
            String yamlContent = composeGenerator.generateFromContainerIds(
                request.getContainerIds(), 
                projectName, 
                projectDescription
            );
            
            // 如果用户提供了环境变量描述配置，更新YAML内容
            if (request.getEnvDescriptions() != null && !request.getEnvDescriptions().isEmpty()) {
                yamlContent = updateEnvDescriptions(yamlContent, request.getEnvDescriptions());
            }
            
            // 构建响应
            ContainerYamlResponse response = ContainerYamlResponse.success(
                yamlContent, 
                request.getContainerIds().size(), 
                projectName
            );
            
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            String errorMessage;
            
            // 针对不同类型的异常，提供更精确的错误信息
            if (e instanceof IllegalStateException) {
                // 配置相关异常，直接使用原始错误信息
                errorMessage = e.getMessage();
            } else if (e.getMessage() != null && e.getMessage().startsWith("YAML生成失败:")) {
                // 已经有明确前缀的异常，直接使用
                errorMessage = e.getMessage();
            } else {
                // 其他异常，添加简洁的前缀
                errorMessage = "生成失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误");
            }
            
            ContainerYamlResponse response = ContainerYamlResponse.error(errorMessage);
            return ApiResponse.success(response);  // 返回业务错误，仍然是成功的HTTP响应
        }
    }
    
    /**
     * 更新YAML内容中的环境变量描述
     */
    private String updateEnvDescriptions(String yamlContent, Map<String, String> envDescriptions) {
        if (envDescriptions == null || envDescriptions.isEmpty()) {
            return yamlContent;
        }
        
        StringBuilder result = new StringBuilder();
        String[] lines = yamlContent.split("\n");
        
        for (String line : lines) {
            if (line.trim().endsWith(":") && line.contains("description:")) {
                String envName = null;
                // 查找对应的环境变量名
                for (String key : envDescriptions.keySet()) {
                    if (line.contains(key)) {
                        envName = key;
                        break;
                    }
                }
                if (envName != null && envDescriptions.containsKey(envName)) {
                    // 替换描述
                    String newDescription = envDescriptions.get(envName);
                    line = line.replaceAll("description:.*", "description: " + newDescription);
                }
            }
            result.append(line).append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * 预览容器YAML配置（不包含敏感信息）
     */
    @PostMapping("/preview-yaml")
    @Operation(summary = "预览YAML配置", description = "预览容器的YAML配置，不包含敏感信息")
    public ApiResponse<ContainerYamlResponse> previewYaml(@Valid @RequestBody ContainerYamlRequest request) {
        try {
            // 设置排除字段（排除敏感信息）
            HashSet<String> excludeFields = new HashSet<>();
            if (request.getExcludeFields() != null) {
                excludeFields.addAll(request.getExcludeFields());
            }
            // 默认排除一些敏感字段
            excludeFields.add("environment");
            
            // 确定项目名称和描述
            String projectName = request.getProjectName();
            if (projectName == null || projectName.trim().isEmpty()) {
                projectName = "预览项目";
            }
            
            String projectDescription = request.getDescription();
            if (projectDescription == null || projectDescription.trim().isEmpty()) {
                projectDescription = "Docker容器预览项目";
            }
            
            // 获取容器详细信息，并传递项目信息
            String yamlContent = composeGenerator.generateComposeContent(
                dockerService.listContainers().stream()
                    .filter(container -> request.getContainerIds().contains(container.getId()))
                    .map(container -> dockerService.inspectContainerCmd(container.getId()))
                    .collect(java.util.stream.Collectors.toList()),
                excludeFields,
                projectName,
                projectDescription
            );
            
            // 构建响应
            ContainerYamlResponse response = ContainerYamlResponse.success(
                yamlContent, 
                request.getContainerIds().size(), 
                projectName
            );
            response.setMessage("YAML预览生成成功（已排除敏感信息）");
            
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            String errorMessage;
            
            // 针对不同类型的异常，提供更精确的错误信息
            if (e instanceof IllegalStateException) {
                // 配置相关异常，直接使用原始错误信息
                errorMessage = e.getMessage();
            } else {
                // 其他异常，添加简洁的前缀
                errorMessage = "预览失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误");
            }
            
            ContainerYamlResponse response = ContainerYamlResponse.error(errorMessage);
            return ApiResponse.success(response);  // 返回业务错误，仍然是成功的HTTP响应
        }
    }
    
    /**
     * 导出项目包（YAML + 可选配置文件）
     */
    @PostMapping("/export-project")
    @Operation(summary = "导出项目包", description = "导出YAML配置，可选择是否包含服务配置包")
    public ResponseEntity<?> exportProject(@Valid @RequestBody ContainerYamlRequest request) {
        try {
            // 生成YAML内容
            String yamlContent = composeGenerator.generateFromContainerIds(
                request.getContainerIds(), 
                request.getProjectName(), 
                request.getDescription()
            );
            
            String projectName = request.getProjectName() != null ? 
                request.getProjectName() : "docker-project";
            
            // 根据用户选择决定导出方式
            if (request.getIncludeConfigPackages() != null && request.getIncludeConfigPackages()) {
                // 导出完整项目包（YAML + 配置包）
                return exportCompleteProject(yamlContent, request, projectName);
            } else {
                // 只导出YAML文件
                return exportYamlOnly(yamlContent, projectName);
            }
            
        } catch (Exception e) {
            log.error("导出失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("导出失败: " + e.getMessage()));
        }
    }
    
    /**
     * 导出完整项目包（YAML + 配置包）
     */
    private ResponseEntity<Resource> exportCompleteProject(String yamlContent, 
                                                         ContainerYamlRequest request, 
                                                         String projectName) throws Exception {
        // 创建临时导出目录
        String exportId = "export_" + System.currentTimeMillis();
        String exportDir = "/tmp/exports/" + exportId;
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(exportDir));
        
        try {
            // 1. 保存YAML文件
            String yamlFile = exportDir + "/docker-compose.yml";
            java.nio.file.Files.write(java.nio.file.Paths.get(yamlFile), 
                yamlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // 2. 生成配置包 - 传递用户选择的路径
            Map<String, String> configPackages = composeGenerator.generateConfigPackages(
                request.getContainerIds(), exportDir, request.getSelectedPaths()
            );
            
            // 3. 生成README
            generateReadmeFile(exportDir, projectName, configPackages);
            
            // 4. 创建ZIP包
            String zipFile = exportDir + ".zip";
            createZipFromDirectory(exportDir, zipFile);
            
            // 5. 返回ZIP文件
            org.springframework.core.io.Resource resource = 
                new org.springframework.core.io.FileSystemResource(new java.io.File(zipFile));
            
            // 🔥 修复中文文件名编码问题
            String filename = projectName + "-export.zip";
            String encodedFilename = encodeFilename(filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + sanitizeFilename(filename) + "\"; " +
                            "filename*=UTF-8''" + encodedFilename)
                    .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                    .body(resource);
                    
        } finally {
            // 清理临时文件（在另一个线程中延迟删除）
            String tempPath = exportDir;
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(30000); // 30秒后删除
                    deleteDirectory(tempPath);
                    deleteDirectory(tempPath + ".zip");
                } catch (Exception e) {
                    log.warn("清理临时文件失败: {}", tempPath, e);
                }
            });
        }
    }
    
    /**
     * 只导出YAML文件
     */
    private ResponseEntity<Resource> exportYamlOnly(String yamlContent, String projectName) {
        try {
            byte[] content = yamlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            org.springframework.core.io.Resource resource = 
                new org.springframework.core.io.ByteArrayResource(content);
            
            // 🔥 修复中文文件名编码问题
            String filename = projectName + ".yml";
            String encodedFilename = encodeFilename(filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + sanitizeFilename(filename) + "\"; " +
                            "filename*=UTF-8''" + encodedFilename)
                    .header(HttpHeaders.CONTENT_TYPE, "text/yaml; charset=UTF-8")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("生成YAML文件失败", e);
        }
    }
    
    /**
     * 🔥 新增：安全化文件名（移除特殊字符，保留基本字符）
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "download";
        }
        
        // 替换中文和特殊字符为安全字符
        String sanitized = filename
                .replaceAll("[^a-zA-Z0-9._-]", "_")  // 替换非安全字符为下划线
                .replaceAll("_{2,}", "_");          // 多个连续下划线合并为一个
        
        // 确保文件名不为空且不以点或下划线开头
        if (sanitized.isEmpty() || sanitized.startsWith(".") || sanitized.startsWith("_")) {
            sanitized = "download_" + sanitized;
        }
        
        return sanitized;
    }
    
    /**
     * 🔥 新增：URL编码文件名（RFC 5987标准）
     */
    private String encodeFilename(String filename) {
        try {
            return URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");  // 空格编码为%20而不是+
        } catch (Exception e) {
            log.warn("文件名编码失败，使用原始文件名: {}", filename, e);
            return filename;
        }
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
        readme.append("3. 更新 configUrl 为您上传的配置包地址\n");
        readme.append("4. 运行: `docker-compose up -d`\n\n");
        
        if (!configPackages.isEmpty()) {
            readme.append("## 📦 配置包部署说明\n\n");
            for (Map.Entry<String, String> entry : configPackages.entrySet()) {
                String serviceName = entry.getKey();
                String packageName = entry.getValue();
                readme.append("### ").append(serviceName).append("\n");
                readme.append("```bash\n");
                readme.append("# 1. 上传配置包到文件服务器，获得下载URL\n");
                readme.append("# 2. 修改 docker-compose.yml 中 ").append(serviceName).append(" 服务的 configUrl\n");
                readme.append("# 示例: configUrl: 'https://github.com/xxx/releases/download/v1.0/").append(packageName).append("'\n");
                readme.append("```\n\n");
            }
        }
        
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
    
    /**
     * 删除目录
     */
    private void deleteDirectory(String dirPath) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(dirPath);
            if (java.nio.file.Files.exists(path)) {
                if (java.nio.file.Files.isDirectory(path)) {
                    java.nio.file.Files.walk(path)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(java.nio.file.Path::toFile)
                        .forEach(java.io.File::delete);
                } else {
                    java.nio.file.Files.delete(path);
                }
            }
        } catch (Exception e) {
            log.warn("删除目录失败: {}", dirPath, e);
        }
    }

    /**
     * 获取容器路径信息供用户选择打包
     */
    @PostMapping("/container-paths")
    @Operation(summary = "获取容器路径信息", description = "获取指定容器的所有路径挂载信息，供用户选择要打包的路径")
    public ApiResponse<List<ContainerPathInfo>> getContainerPaths(@Valid @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> containerIds = (List<String>) request.get("containerIds");
            
            if (containerIds == null || containerIds.isEmpty()) {
                return ApiResponse.error("容器ID列表不能为空");
            }
            
            List<ContainerPathInfo> pathInfos = composeGenerator.getContainerPaths(containerIds);
            
            return ApiResponse.success(pathInfos);
            
        } catch (Exception e) {
            log.error("获取容器路径信息失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取容器路径信息失败: " + e.getMessage());
        }
    }

    /**
     * 启动异步打包任务
     */
    @PostMapping("/export-project-async")
    @Operation(summary = "启动异步打包任务", description = "启动异步打包任务，立即返回任务ID，避免超时问题")
    public ApiResponse<Map<String, Object>> startAsyncPackage(@Valid @RequestBody ContainerYamlRequest request) {
        try {
            String taskId = asyncPackageService.startPackageTask(
                request.getContainerIds(),
                request.getProjectName(),
                request.getDescription(),
                request.getSelectedPaths()
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("message", "打包任务已启动，请稍候...");
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("启动异步打包任务失败: {}", e.getMessage(), e);
            return ApiResponse.error("启动打包任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询打包任务状态
     */
    @GetMapping("/package-task/{taskId}")
    @Operation(summary = "查询打包任务状态", description = "查询异步打包任务的当前状态和进度")
    public ApiResponse<PackageTask> getPackageTaskStatus(@PathVariable String taskId) {
        try {
            PackageTask task = asyncPackageService.getTaskStatus(taskId);
            
            if (task == null) {
                return ApiResponse.error("任务不存在或已过期");
            }
            
            return ApiResponse.success(task);
            
        } catch (Exception e) {
            log.error("查询打包任务状态失败: {}", e.getMessage(), e);
            return ApiResponse.error("查询任务状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载打包完成的文件
     */
    @GetMapping("/download-package/{taskId}")
    @Operation(summary = "下载打包文件", description = "下载已完成的打包文件")
    public ResponseEntity<?> downloadPackageFile(@PathVariable String taskId) {
        try {
            PackageTask task = asyncPackageService.getTaskStatus(taskId);
            
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("任务不存在或已过期"));
            }
            
            if (!"completed".equals(task.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("任务未完成，当前状态: " + task.getStatus()));
            }
            
            if (task.getFilePath() == null || !java.nio.file.Files.exists(java.nio.file.Paths.get(task.getFilePath()))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("文件不存在"));
            }
            
            org.springframework.core.io.Resource resource = 
                new org.springframework.core.io.FileSystemResource(new java.io.File(task.getFilePath()));
            
            // 🔥 修复中文文件名编码问题
            String filename = task.getFileName();
            String encodedFilename = encodeFilename(filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + sanitizeFilename(filename) + "\"; " +
                            "filename*=UTF-8''" + encodedFilename)
                    .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                    .header(HttpHeaders.CONTENT_LENGTH, task.getFileSize().toString())
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("下载打包文件失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("下载失败: " + e.getMessage()));
        }
    }
} 