package com.dockpilot.controller;

import com.dockpilot.model.application.*;
import com.dockpilot.model.application.dto.ApplicationSaveRequest;
import com.dockpilot.model.application.dto.ApplicationInstallInfo;
import com.dockpilot.model.application.dto.ApplicationDeployRequest;
import com.dockpilot.model.application.dto.ApplicationDeployResult;
import com.dockpilot.model.application.dto.ImageStatusInfo;
import com.dockpilot.model.application.dto.PullImageRequest;
import com.dockpilot.model.application.vo.ApplicationVO;
import com.dockpilot.model.application.vo.ApplicationMarketVO;
import com.dockpilot.service.ApplicationService;
import com.dockpilot.service.http.SystemSettingService;
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
    
    @Autowired
    private SystemSettingService systemSettingService;
    
    @Value("${file.upload.path:uploads/}")
    private String uploadBasePath;
    
    @Value("${file.config.path}")
    private String configPath;

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
            // 添加调试日志，检查中文编码
            log.info("接收到保存应用请求，应用名称: {}", request.getName());
            log.info("应用名称字符数: {}, 字符编码: {}", 
                request.getName() != null ? request.getName().length() : 0,
                request.getName() != null ? java.util.Arrays.toString(request.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8)) : "null");
            log.info("应用描述: {}", request.getDescription());
            log.info("应用分类: {}", request.getCategory());
            
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
     * 从URL获取YAML或ZIP文件
     */
    @PostMapping("/fetch-from-url")
    @Operation(summary = "从URL获取文件", description = "从指定URL下载YAML或ZIP文件并处理")
    public ApiResponse<String> fetchFromUrl(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        if (url == null || url.trim().isEmpty()) {
            return ApiResponse.error("URL不能为空");
        }
        
        try {
            log.info("开始从URL获取文件: {}", url);
            
            // 根据URL判断文件类型
            String lowerUrl = url.toLowerCase();
            
            if (lowerUrl.endsWith(".yml") || lowerUrl.endsWith(".yaml")) {
                // YAML文件：直接下载文本内容
                return downloadYamlFromUrl(url);
                
            } else if (lowerUrl.endsWith(".zip")) {
                // ZIP文件：下载并调用现有ZIP解析逻辑
                return downloadZipFromUrl(url);
                
            } else {
                return ApiResponse.error("不支持的文件类型，仅支持 .yml、.yaml 或 .zip 文件");
            }
            
        } catch (Exception e) {
            log.error("从URL获取文件失败: {}", e.getMessage(), e);
            return ApiResponse.error("从URL获取文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 从URL下载YAML文件
     */
    private ApiResponse<String> downloadYamlFromUrl(String url) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();
            
        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .timeout(java.time.Duration.ofSeconds(60))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7")
            .header("Cache-Control", "no-cache")
            .GET()
            .build();
            
        java.net.http.HttpResponse<String> response = client.send(httpRequest, 
            java.net.http.HttpResponse.BodyHandlers.ofString());
            
        if (response.statusCode() != 200) {
            return ApiResponse.error("HTTP请求失败，状态码: " + response.statusCode());
        }
        
        String content = response.body();
        log.info("YAML文件下载完成，内容长度: {}", content.length());
        return ApiResponse.success(content);
    }
    
    /**
     * 从URL下载ZIP文件并解析
     */
    private ApiResponse<String> downloadZipFromUrl(String url) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();
            
        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .timeout(java.time.Duration.ofSeconds(60))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept", "application/zip,application/octet-stream,*/*")
            .header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7")
            .header("Cache-Control", "no-cache")
            .GET()
            .build();
            
        // 创建临时文件
        String tempFileName = "/tmp/url-download-" + System.currentTimeMillis() + ".zip";
        java.nio.file.Path tempFilePath = java.nio.file.Paths.get(tempFileName);
        
        try {
            // 流式下载到临时文件，不占用内存
            java.net.http.HttpResponse<java.nio.file.Path> response = client.send(httpRequest, 
                java.net.http.HttpResponse.BodyHandlers.ofFile(tempFilePath));
                
            if (response.statusCode() != 200) {
                // 提供更详细的错误信息
                String errorMsg = String.format("HTTP请求失败，状态码: %d", response.statusCode());
                if (response.statusCode() == 302 || response.statusCode() == 301) {
                    errorMsg += "，重定向失败，请检查URL是否需要特殊访问权限";
                } else if (response.statusCode() == 403) {
                    errorMsg += "，访问被拒绝，可能需要登录或特殊权限";
                } else if (response.statusCode() == 404) {
                    errorMsg += "，文件不存在";
                }
                log.warn("下载失败: {}, URL: {}", errorMsg, url);
                return ApiResponse.error(errorMsg);
            }
            
            long fileSize = java.nio.file.Files.size(tempFilePath);
            log.info("ZIP文件下载完成，大小: {} bytes，临时文件: {}", fileSize, tempFileName);
            
            // 检查文件大小限制（比如限制500MB）
            if (fileSize > 500 * 1024 * 1024) {
                return ApiResponse.error("文件过大，限制500MB以内");
            }
            
            // 验证下载的文件是否真的是ZIP格式
            if (!isValidZipFile(tempFilePath)) {
                return ApiResponse.error("下载的文件不是有效的ZIP格式，可能是网页重定向页面");
            }
            
            // 从URL提取文件名
            String fileName = extractFileNameFromUrl(url);
            
            // 创建基于临时文件的MultipartFile实现
            MultipartFile zipFile = new TempFileMultipartFile(fileName, tempFilePath);
            
            // 调用现有的ZIP解析方法
            return parseZipPackage(zipFile);
            
        } finally {
            // 确保临时文件被删除
            try {
                java.nio.file.Files.deleteIfExists(tempFilePath);
                log.debug("临时下载文件已删除: {}", tempFileName);
            } catch (Exception e) {
                log.warn("删除临时文件失败: {}", tempFileName, e);
            }
        }
    }
    
    /**
     * 验证文件是否为有效的ZIP格式
     */
    private boolean isValidZipFile(java.nio.file.Path filePath) {
        try {
            // 检查文件头，ZIP文件以"PK"开头
            byte[] header = new byte[4];
            try (java.io.InputStream is = java.nio.file.Files.newInputStream(filePath)) {
                int bytesRead = is.read(header);
                if (bytesRead >= 2) {
                    return header[0] == 0x50 && header[1] == 0x4B; // "PK"
                }
            }
        } catch (Exception e) {
            log.warn("验证ZIP文件格式失败: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * 基于临时文件的MultipartFile实现（不占用内存）
     */
    private static class TempFileMultipartFile implements MultipartFile {
        private final String filename;
        private final java.nio.file.Path filePath;
        
        public TempFileMultipartFile(String filename, java.nio.file.Path filePath) {
            this.filename = filename;
            this.filePath = filePath;
        }
        
        @Override
        public String getName() { return "file"; }
        
        @Override
        public String getOriginalFilename() { return filename; }
        
        @Override
        public String getContentType() { return "application/zip"; }
        
        @Override
        public boolean isEmpty() { 
            try {
                return java.nio.file.Files.size(filePath) == 0;
            } catch (Exception e) {
                return true;
            }
        }
        
        @Override
        public long getSize() { 
            try {
                return java.nio.file.Files.size(filePath);
            } catch (Exception e) {
                return 0;
            }
        }
        
        @Override
        public byte[] getBytes() throws java.io.IOException {
            return java.nio.file.Files.readAllBytes(filePath);
        }
        
        @Override
        public java.io.InputStream getInputStream() throws java.io.IOException {
            return java.nio.file.Files.newInputStream(filePath);
        }
        
        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException {
            java.nio.file.Files.copy(filePath, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    /**
     * 从URL提取文件名
     */
    private String extractFileNameFromUrl(String url) {
        try {
            String path = new java.net.URL(url).getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            return fileName.isEmpty() ? "downloaded-file.zip" : fileName;
        } catch (Exception e) {
            return "downloaded-file.zip";
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
        String configDir = configPath + projectName;
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
     * 获取应用市场数据
     */
    @GetMapping("/marketplace")
    @Operation(summary = "获取应用市场数据", description = "从配置的市场源获取应用列表")
    public ApiResponse<List<ApplicationMarketVO>> getMarketApplications() {
        try {
            // 1. 获取应用市场源配置
            String marketSources = systemSettingService.get("app_market_sources");
            if (marketSources == null || marketSources.trim().isEmpty()) {
                log.info("未配置应用市场源，返回空列表");
                return ApiResponse.success(new ArrayList<>());
            }
            
            // 2. 解析多个URL (按行分割)
            String[] urls = marketSources.split("\n");
            List<ApplicationMarketVO> allApplications = new ArrayList<>();
            
            // 3. 并行获取各个市场源的数据
            for (String url : urls) {
                String trimmedUrl = url.trim();
                if (trimmedUrl.isEmpty() || trimmedUrl.startsWith("#")) {
                    continue; // 跳过空行和注释行
                }
                
                try {
                    List<ApplicationMarketVO> apps = fetchApplicationsFromMarketSource(trimmedUrl);
                    allApplications.addAll(apps);
                    log.info("从市场源 {} 获取到 {} 个应用", trimmedUrl, apps.size());
                } catch (Exception e) {
                    log.warn("从市场源 {} 获取数据失败: {}", trimmedUrl, e.getMessage());
                    // 继续处理下一个源，不因为单个源失败而中断
                }
            }
            
            log.info("应用市场数据获取完成，总计 {} 个应用", allApplications.size());
            return ApiResponse.success(allApplications);
            
        } catch (Exception e) {
            log.error("获取应用市场数据失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取应用市场数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 从单个市场源获取应用数据
     */
    private List<ApplicationMarketVO> fetchApplicationsFromMarketSource(String url) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();
            
        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .timeout(java.time.Duration.ofSeconds(30))
            .header("User-Agent", "DockPilot/1.0")
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .GET()
            .build();
            
        java.net.http.HttpResponse<String> response = client.send(httpRequest, 
            java.net.http.HttpResponse.BodyHandlers.ofString());
            
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP请求失败，状态码: " + response.statusCode());
        }
        
        // 解析JSON数据
        String jsonData = response.body();
        return parseMarketApplicationsJson(jsonData);
    }
    
    /**
     * 解析应用市场JSON数据
     */
    private List<ApplicationMarketVO> parseMarketApplicationsJson(String jsonData) throws Exception {
        List<ApplicationMarketVO> applications = new ArrayList<>();
        
        try {
            // 使用Jackson或者手动解析JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(jsonData);
            
            // 支持多种JSON格式
            com.fasterxml.jackson.databind.JsonNode appsNode;
            if (rootNode.has("applications")) {
                appsNode = rootNode.get("applications");
            } else if (rootNode.has("data")) {
                appsNode = rootNode.get("data");
            } else if (rootNode.isArray()) {
                appsNode = rootNode;
            } else {
                throw new RuntimeException("不支持的JSON格式，需要包含 'applications' 或 'data' 字段");
            }
            
            if (appsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode appNode : appsNode) {
                    ApplicationMarketVO app = parseApplicationNode(appNode);
                    if (app != null) {
                        applications.add(app);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("解析应用市场JSON数据失败: {}", e.getMessage());
            throw new RuntimeException("JSON数据格式错误: " + e.getMessage());
        }
        
        return applications;
    }
    
    /**
     * 解析单个应用节点
     */
    private ApplicationMarketVO parseApplicationNode(com.fasterxml.jackson.databind.JsonNode appNode) {
        try {
            ApplicationMarketVO app = new ApplicationMarketVO();
            
            // 必需字段
            if (!appNode.has("name") || !appNode.has("downloadUrl")) {
                log.warn("应用缺少必需字段 name 或 downloadUrl，跳过");
                return null;
            }
            
            app.setName(appNode.get("name").asText());
            app.setDownloadUrl(appNode.get("downloadUrl").asText());
            
            // 可选字段
            if (appNode.has("description")) {
                app.setDescription(appNode.get("description").asText());
            }
            if (appNode.has("category")) {
                app.setCategory(appNode.get("category").asText());
            } else {
                app.setCategory("其他");
            }
            if (appNode.has("iconUrl")) {
                app.setIconUrl(appNode.get("iconUrl").asText());
            }
            if (appNode.has("version")) {
                app.setVersion(appNode.get("version").asText());
            } else {
                app.setVersion("1.0.0");
            }
            if (appNode.has("author")) {
                app.setAuthor(appNode.get("author").asText());
            } else {
                app.setAuthor("应用市场");
            }
            
            // 如果有services字段，设置服务数量
            if (appNode.has("services")) {
                app.setServices(appNode.get("services").asInt(1));
            }
            
            return app;
            
        } catch (Exception e) {
            log.warn("解析应用节点失败: {}", e.getMessage());
            return null;
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