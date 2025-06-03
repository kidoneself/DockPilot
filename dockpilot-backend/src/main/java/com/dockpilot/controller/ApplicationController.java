package com.dockpilot.controller;

import com.dockpilot.model.application.ApplicationParseResult;
import com.dockpilot.model.application.dto.ApplicationSaveRequest;
import com.dockpilot.model.application.dto.ApplicationInstallInfo;
import com.dockpilot.model.application.dto.ApplicationDeployRequest;
import com.dockpilot.model.application.dto.ApplicationDeployResult;
import com.dockpilot.model.application.dto.ImageStatusInfo;
import com.dockpilot.model.application.dto.PullImageRequest;
import com.dockpilot.model.application.vo.ApplicationVO;
import com.dockpilot.service.ApplicationService;
import com.dockpilot.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 应用中心控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/applications")
@Tag(name = "应用中心", description = "应用中心管理接口")
@Validated
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;
    
    @Value("${file.upload.path:uploads/}")
    private String uploadBasePath;

    /**
     * 获取应用列表
     */
    @GetMapping
    @Operation(summary = "获取应用列表")
    public ApiResponse<List<ApplicationVO>> getApplications(@RequestParam(required = false) String category, @RequestParam(required = false) String keyword) {

        List<ApplicationVO> applications = applicationService.getApplications(category, keyword);
        return ApiResponse.success(applications);
    }

    /**
     * 根据ID获取应用详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取应用详情")
    public ApiResponse<ApplicationVO> getApplicationById(@PathVariable Long id) {
        ApplicationVO application = applicationService.getApplicationById(id);
        if (application == null) {
            return ApiResponse.error("应用不存在");
        }

        return ApiResponse.success(application);
    }

    /**
     * 保存应用 (从容器或导入YAML)
     */
    @PostMapping
    @Operation(summary = "保存应用")
    public ApiResponse<ApplicationVO> saveApplication(@Valid @RequestBody ApplicationSaveRequest request) {
        try {
            ApplicationVO savedApp = applicationService.saveApplication(request);
            return ApiResponse.success(savedApp);

        } catch (Exception e) {
            log.error("保存应用失败：{}", e.getMessage(), e);
            return ApiResponse.error("保存应用失败：" + e.getMessage());
        }
    }

    /**
     * 删除应用
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除应用")
    public ApiResponse<Void> deleteApplication(@PathVariable Long id) {
        try {
            applicationService.deleteApplication(id);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("删除应用失败：{}", e.getMessage(), e);
            return ApiResponse.error("删除应用失败：" + e.getMessage());
        }
    }

    /**
     * 分享应用 (获取YAML内容)
     */
    @GetMapping("/{id}/share")
    @Operation(summary = "分享应用")
    public ApiResponse<String> shareApplication(@PathVariable Long id) {
        try {
            String yamlContent = applicationService.shareApplication(id);
            return ApiResponse.success(yamlContent);
        } catch (Exception e) {
            log.error("分享应用失败：{}", e.getMessage(), e);
            return ApiResponse.error("分享应用失败：" + e.getMessage());
        }
    }

    /**
     * 获取分类列表
     */
    @GetMapping("/categories")
    @Operation(summary = "获取分类列表")
    public ApiResponse<List<String>> getCategories() {
        List<String> categories = applicationService.getCategories();
        return ApiResponse.success(categories);
    }

    /**
     * 解析应用配置
     */
    @PostMapping("/parse")
    @Operation(summary = "解析应用配置", description = "解析YAML配置，返回应用详细信息")
    public ApiResponse<ApplicationParseResult> parseApplication(@RequestBody ParseRequest request) {
        try {
            ApplicationParseResult result = applicationService.parseApplication(request.getYamlContent());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("解析应用配置失败: {}", e.getMessage());
            return ApiResponse.error("解析失败: " + e.getMessage());
        }
    }

    /**
     * 获取应用安装信息
     */
    @GetMapping("/{id}/install-info")
    @Operation(summary = "获取应用安装信息", description = "获取应用的详细安装信息，包括镜像、环境变量等")
    public ApiResponse<ApplicationInstallInfo> getInstallInfo(@PathVariable Long id) {
        try {
            ApplicationInstallInfo installInfo = applicationService.getInstallInfo(id);
            return ApiResponse.success(installInfo);
        } catch (Exception e) {
            log.error("获取应用安装信息失败: {}", e.getMessage());
            return ApiResponse.error("获取应用安装信息失败: " + e.getMessage());
        }
    }

    /**
     * 部署应用
     */
    @PostMapping("/{id}/deploy")
    @Operation(summary = "部署应用", description = "根据配置参数部署应用")
    public ApiResponse<ApplicationDeployResult> deployApplication(
            @PathVariable Long id, 
            @Valid @RequestBody ApplicationDeployRequest request) {
        try {
            ApplicationDeployResult result = applicationService.deployApplication(id, request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("部署应用失败: {}", e.getMessage());
            return ApiResponse.error("部署应用失败: " + e.getMessage());
        }
    }

    /**
     * 检查镜像状态
     */
    @PostMapping("/check-images")
    @Operation(summary = "检查镜像状态", description = "批量检查镜像是否存在")
    public ApiResponse<List<ImageStatusInfo>> checkImages(@RequestBody List<String> imageNames) {
        try {
            List<ImageStatusInfo> result = applicationService.checkImages(imageNames);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("检查镜像状态失败: {}", e.getMessage());
            return ApiResponse.error("检查镜像状态失败: " + e.getMessage());
        }
    }

    /**
     * 拉取镜像
     */
    @PostMapping("/pull-image")
    @Operation(summary = "拉取镜像", description = "拉取指定的Docker镜像")
    public ApiResponse<String> pullImage(@RequestBody PullImageRequest request) {
        try {
            String result = applicationService.pullImage(request.getImageName());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("拉取镜像失败: {}", e.getMessage());
            return ApiResponse.error("拉取镜像失败: " + e.getMessage());
        }
    }

    /**
     * 解析ZIP包并返回修改后的YAML内容
     */
    @PostMapping("/parse-zip")
    @Operation(summary = "解析ZIP包", description = "解析导出的ZIP包，自动关联配置包并返回修改后的YAML")
    public ApiResponse<String> parseZipPackage(@RequestParam("file") MultipartFile zipFile) {
        try {
            log.info("开始解析ZIP包: {}", zipFile.getOriginalFilename());
            
            // 1. 解压ZIP包到临时目录
            String tempDir = extractZipToTemp(zipFile);
            
            // 2. 读取docker-compose.yml
            String yamlContent = readDockerComposeFromDir(tempDir);
            if (yamlContent == null) {
                throw new RuntimeException("ZIP包中未找到docker-compose.yml文件");
            }
            
            // 3. 扫描配置包文件
            List<String> configPackages = findConfigPackagesInDir(tempDir);
            
            // 4. 生成项目名并存储配置包
            String projectName = generateProjectName(zipFile.getOriginalFilename());
            storeConfigPackages(tempDir, configPackages, projectName);
            
            // 5. 修改YAML中的configUrl
            String modifiedYaml = updateConfigUrlsInYaml(yamlContent, configPackages, projectName);
            
            // 6. 清理临时目录
            cleanupTempDir(tempDir);
            
            log.info("ZIP包解析完成，项目: {}, 配置包数量: {}", projectName, configPackages.size());
            return ApiResponse.success(modifiedYaml);
            
        } catch (Exception e) {
            log.error("ZIP包解析失败: {}", e.getMessage(), e);
            return ApiResponse.error("ZIP包解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 解压ZIP包到临时目录
     */
    private String extractZipToTemp(MultipartFile zipFile) throws Exception {
        String tempDir = "/tmp/zip-import-" + System.currentTimeMillis();
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tempDir));
        
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(zipFile.getInputStream())) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                
                String entryPath = tempDir + "/" + entry.getName();
                java.nio.file.Path targetPath = java.nio.file.Paths.get(entryPath);
                java.nio.file.Files.createDirectories(targetPath.getParent());
                
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(targetPath.toFile())) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
        }
        
        return tempDir;
    }
    
    /**
     * 从目录读取docker-compose.yml
     */
    private String readDockerComposeFromDir(String dir) throws Exception {
        String yamlFile = dir + "/docker-compose.yml";
        if (java.nio.file.Files.exists(java.nio.file.Paths.get(yamlFile))) {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(yamlFile)));
        }
        return null;
    }
    
    /**
     * 扫描目录中的配置包文件
     */
    private List<String> findConfigPackagesInDir(String dir) throws Exception {
        List<String> packages = new ArrayList<>();
        try (java.nio.file.DirectoryStream<java.nio.file.Path> stream = 
                java.nio.file.Files.newDirectoryStream(java.nio.file.Paths.get(dir), "*.tar.gz")) {
            for (java.nio.file.Path path : stream) {
                packages.add(path.getFileName().toString());
            }
        }
        return packages;
    }
    
    /**
     * 生成项目名
     */
    private String generateProjectName(String fileName) {
        String baseName = fileName.replaceAll("\\.(zip|ZIP)$", "");
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return baseName + "-" + timestamp;
    }
    
    /**
     * 存储配置包到本地目录
     */
    private void storeConfigPackages(String tempDir, List<String> packages, String projectName) throws Exception {
        String configDir = uploadBasePath + projectName;
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(configDir));
        
        // 复制docker-compose.yml
        String yamlSource = tempDir + "/docker-compose.yml";
        String yamlTarget = configDir + "/docker-compose.yml";
        java.nio.file.Files.copy(java.nio.file.Paths.get(yamlSource), 
                                  java.nio.file.Paths.get(yamlTarget), 
                                  java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        
        // 复制配置包
        for (String packageName : packages) {
            String source = tempDir + "/" + packageName;
            String target = configDir + "/" + packageName;
            java.nio.file.Files.copy(java.nio.file.Paths.get(source), 
                                      java.nio.file.Paths.get(target), 
                                      java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    /**
     * 修改YAML中的configUrl
     */
    private String updateConfigUrlsInYaml(String yamlContent, List<String> packages, String projectName) {
        try {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = yaml.load(yamlContent);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> services = (Map<String, Object>) config.get("services");
            if (services != null) {
                for (Map.Entry<String, Object> serviceEntry : services.entrySet()) {
                    String serviceName = serviceEntry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> service = (Map<String, Object>) serviceEntry.getValue();
                    
                    // 检查是否有对应的配置包
                    String expectedPackage = serviceName + ".tar.gz";
                    if (packages.contains(expectedPackage)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> xMeta = (Map<String, Object>) service.get("x-meta");
                        if (xMeta == null) {
                            xMeta = new java.util.HashMap<>();
                            service.put("x-meta", xMeta);
                        }
                        
                        // 设置本地配置包路径
                        xMeta.put("configUrl", "local://" + projectName + "/" + expectedPackage);
                    }
                }
            }
            
            // 重新序列化为YAML
            org.yaml.snakeyaml.DumperOptions options = new org.yaml.snakeyaml.DumperOptions();
            options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK);
            org.yaml.snakeyaml.Yaml outputYaml = new org.yaml.snakeyaml.Yaml(options);
            return outputYaml.dump(config);
            
        } catch (Exception e) {
            log.warn("修改YAML失败，返回原始内容: {}", e.getMessage());
            return yamlContent;
        }
    }
    
    /**
     * 清理临时目录
     */
    private void cleanupTempDir(String tempDir) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(tempDir);
            if (java.nio.file.Files.exists(path)) {
                java.nio.file.Files.walk(path)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)
                    .forEach(java.io.File::delete);
            }
        } catch (Exception e) {
            log.warn("清理临时目录失败: {}", tempDir, e);
        }
    }

    /**
     * 解析请求DTO
     */
    @Data
    public static class ParseRequest {
        private String yamlContent;
    }
} 