// 热更新服务
package com.dockpilot.service.http;

import com.dockpilot.model.dto.UpdateInfoDTO;
import com.dockpilot.service.http.SystemSettingService;
import com.dockpilot.common.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.scheduling.annotation.Scheduled;
import javax.annotation.PostConstruct;

/**
 * 容器内热更新服务
 * 核心功能：不重启容器的情况下更新前后端代码
 * 新增功能：缓存机制 + 完善容错处理
 */
@Slf4j
@Service
public class UpdateService {

    @Autowired
    private SystemSettingService systemSettingService;
    
    @Autowired
    private AppConfig appConfig;

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Autowired
    private ApplicationContext applicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    // HTTP客户端（会根据代理配置动态创建）
    private volatile HttpClient httpClient;
    private volatile String lastProxyUrl; // 缓存最后使用的代理URL

    // 更新状态管理
    private volatile Map<String, Object> updateProgress = new HashMap<>();
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    private volatile CompletableFuture<Void> currentUpdateTask;

    // 📦 缓存机制相关
    private volatile UpdateInfoDTO cachedUpdateInfo;
    private volatile LocalDateTime lastCheckTime;
    private volatile JsonNode lastReleaseData;
    private static final int CACHE_DURATION_MINUTES = 10; // 缓存10分钟
    private static final String CACHE_FILE = "/dockpilot/data/update_cache.json";
    private static final String FALLBACK_FILE = "/dockpilot/data/fallback_update.json";

    // 系统路径配置
    private static final String GITHUB_API_URL = "https://api.github.com/repos/kidoneself/DockPilot/releases/latest";
    private static final String VERSION_FILE = "/dockpilot/data/current_version";
    private static final String FRONTEND_PATH = "/usr/share/html";
    private static final String BACKEND_JAR = "/app/app.jar";
    private static final String TEMP_DIR = "/tmp/dockpilot-update";
    private static final String BACKUP_DIR = "/tmp/dockpilot-backup";
    private static final String DOWNLOAD_STATUS_FILE = "/tmp/dockpilot-download-status.json";
    private static final String DOWNLOAD_DIR = "/tmp/dockpilot-download";
    private static final String RESTART_SIGNAL_FILE = "/dockpilot/data/restart_signal";

    /**
     * 检查是否有新版本
     */
    public UpdateInfoDTO checkForUpdates() throws Exception {
        log.info("🔍 检查新版本...");
        log.info("🎯 [测试标记] 当前运行版本: v1.0.4 - 热更新功能测试版本！");
        
        ensureHttpClientInitialized();
        
        String currentVersion = getCurrentVersion();
        String latestVersion = currentVersion;
        boolean hasUpdate = false;
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API_URL))
                    .header("Accept", "application/vnd.github.v3+json")
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode release = objectMapper.readTree(response.body());
                latestVersion = release.get("tag_name").asText();
                hasUpdate = !currentVersion.equals(latestVersion);
            }
        } catch (Exception e) {
            log.warn("获取最新版本失败: {}", e.getMessage());
        }
        
        log.info("✅ 版本检查完成: {} -> {} (有更新: {})", currentVersion, latestVersion, hasUpdate);
        
        return UpdateInfoDTO.builder()
                .currentVersion(currentVersion)
                .latestVersion(latestVersion)
                .hasUpdate(hasUpdate)
                .lastCheckTime(LocalDateTime.now())
                .status(hasUpdate ? "available" : "up-to-date")
                .build();
    }

    /**
     * 获取当前版本信息
     */
    public Map<String, String> getCurrentVersionInfo() {
        Map<String, String> versionInfo = new HashMap<>();
        versionInfo.put("currentVersion", getCurrentVersion());
        versionInfo.put("updateMethod", "download-then-restart");
        return versionInfo;
    }

    /**
     * 开始下载新版本（不重启）
     */
    public String startDownload(String targetVersion) throws Exception {
        if (isUpdating.get()) {
            return "已有下载任务在进行中";
        }

        log.info("📡 开始下载版本: {}", targetVersion);
        
        // 异步下载
        CompletableFuture.runAsync(() -> {
            try {
                isUpdating.set(true);
                executeDownload(targetVersion);
            } catch (Exception e) {
                log.error("下载失败", e);
                updateDownloadStatus("failed", 0, "下载失败: " + e.getMessage(), targetVersion);
            } finally {
                isUpdating.set(false);
            }
        });

        return "开始下载新版本，请等待完成后重启";
    }

    /**
     * 确认重启应用
     */
    public String confirmRestart() throws Exception {
        // 检查下载状态
        Map<String, Object> downloadStatus = getDownloadStatus();
        if (!"completed".equals(downloadStatus.get("status"))) {
            throw new RuntimeException("请先完成下载再重启");
        }

        String downloadedVersion = (String) downloadStatus.get("version");
        log.info("🔄 确认重启，使用下载的版本: {}", downloadedVersion);

        // 创建重启信号
        createRestartSignal(downloadedVersion);

        // 优雅关闭Spring Boot应用，让启动脚本来处理重启
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(2000);
                log.info("🔄 执行优雅关闭，让启动脚本处理重启...");
                
                // 使用Spring Boot的优雅关闭机制
                ConfigurableApplicationContext configurableContext = (ConfigurableApplicationContext) applicationContext;
                configurableContext.close();
                
                log.info("✅ Spring Boot应用已关闭，等待启动脚本重启...");
        } catch (Exception e) {
                log.error("应用关闭失败", e);
                // 如果优雅关闭失败，使用System.exit作为备选
                System.exit(1);
            }
        });

        return "应用正在重启更新，请等待30秒后刷新页面";
    }

    /**
     * 获取下载状态
     */
    public Map<String, Object> getDownloadStatus() {
        try {
            if (Files.exists(Paths.get(DOWNLOAD_STATUS_FILE))) {
                String content = Files.readString(Paths.get(DOWNLOAD_STATUS_FILE));
                return objectMapper.readValue(content, Map.class);
            }
        } catch (Exception e) {
            log.warn("读取下载状态失败", e);
        }

        // 默认状态
        Map<String, Object> defaultStatus = new HashMap<>();
        defaultStatus.put("status", "idle");
        defaultStatus.put("progress", 0);
        defaultStatus.put("message", "就绪");
        defaultStatus.put("version", "");
        return defaultStatus;
    }

    /**
     * 取消下载
     */
    public String cancelDownload() {
        if (!isUpdating.get()) {
            return "没有正在进行的下载任务";
        }

        isUpdating.set(false);
        updateDownloadStatus("cancelled", 0, "下载已取消", "");
        
        // 清理下载文件
        try {
            if (Files.exists(Paths.get(DOWNLOAD_DIR))) {
                Files.walk(Paths.get(DOWNLOAD_DIR))
                     .map(Path::toFile)
                     .forEach(File::delete);
            }
        } catch (Exception e) {
            log.warn("清理下载文件失败", e);
        }

        return "下载已取消";
    }

    /**
     * 获取当前版本 - 统一从BuildProperties获取
     */
    private String getCurrentVersion() {
        // 从BuildProperties获取（推荐方式）
        if (buildProperties != null) {
            String version = buildProperties.getVersion();
            log.debug("✅ 从BuildProperties获取版本: v{}", version);
            return "v" + version;
        }

        // 备选：从环境变量获取
        String envVersion = System.getenv("DOCKPILOT_VERSION");
        if (envVersion != null && !envVersion.trim().isEmpty() && !envVersion.equals("latest")) {
            log.debug("✅ 从环境变量获取版本: {}", envVersion);
            return envVersion;
        }
        
        // 默认版本
        String defaultVersion = "v1.0.0";
        log.warn("⚠️ 无法获取版本信息，使用默认版本: {}", defaultVersion);
        return defaultVersion;
    }

    /**
     * 执行下载任务
     */
    private void executeDownload(String version) throws Exception {
        log.info("🚀 开始下载任务，版本: {}", version);
        
        // 创建下载目录
        Files.createDirectories(Paths.get(DOWNLOAD_DIR));
        
        updateDownloadStatus("downloading", 10, "开始下载前端包...", version);
        
        // 下载前端包
        String frontendUrl = String.format("https://github.com/kidoneself/DockPilot/releases/download/%s/frontend.tar.gz", version);
        downloadFile(frontendUrl, DOWNLOAD_DIR + "/frontend.tar.gz");
        
        updateDownloadStatus("downloading", 50, "开始下载后端包...", version);
        
        // 下载后端包
        String backendUrl = String.format("https://github.com/kidoneself/DockPilot/releases/download/%s/backend.jar", version);
        downloadFile(backendUrl, DOWNLOAD_DIR + "/backend.jar");
        
        updateDownloadStatus("verifying", 80, "验证下载文件...", version);
        
        // 验证文件
        validateDownloadedFiles();
        
        updateDownloadStatus("completed", 100, "下载完成，可以重启更新", version);
        log.info("✅ 下载任务完成，版本: {}", version);
    }

    /**
     * 下载单个文件
     */
    private void downloadFile(String url, String targetPath) throws Exception {
        ensureHttpClientInitialized();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("下载失败，HTTP状态码: " + response.statusCode());
        }

        try (InputStream inputStream = response.body();
             FileOutputStream outputStream = new FileOutputStream(targetPath)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        log.info("✅ 文件下载完成: {}", targetPath);
    }

    /**
     * 验证下载的文件
     */
    private void validateDownloadedFiles() throws Exception {
        Path frontendFile = Paths.get(DOWNLOAD_DIR + "/frontend.tar.gz");
        Path backendFile = Paths.get(DOWNLOAD_DIR + "/backend.jar");
        
        if (!Files.exists(frontendFile) || Files.size(frontendFile) < 1024) {
            throw new RuntimeException("前端文件下载不完整");
        }
        
        if (!Files.exists(backendFile) || Files.size(backendFile) < 1024 * 1024) {
            throw new RuntimeException("后端文件下载不完整");
        }
        
        log.info("✅ 文件验证通过");
    }

    /**
     * 更新下载状态
     */
    private void updateDownloadStatus(String status, int progress, String message, String version) {
        try {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("status", status);
            statusMap.put("progress", progress);
            statusMap.put("message", message);
            statusMap.put("version", version);
            statusMap.put("timestamp", LocalDateTime.now().toString());
            
            String statusJson = objectMapper.writeValueAsString(statusMap);
            Files.writeString(Paths.get(DOWNLOAD_STATUS_FILE), statusJson);
            
            log.info("📊 下载状态: {} ({}%) - {}", status, progress, message);
        } catch (Exception e) {
            log.warn("更新下载状态失败", e);
        }
    }

    /**
     * 创建重启信号文件
     */
    private void createRestartSignal(String newVersion) throws IOException {
        Map<String, Object> restartInfo = new HashMap<>();
        restartInfo.put("action", "restart");
        restartInfo.put("reason", "update_restart");
        restartInfo.put("newVersion", newVersion);
        restartInfo.put("downloadPath", DOWNLOAD_DIR);
        restartInfo.put("timestamp", LocalDateTime.now().toString());
        
        Files.createDirectories(Paths.get(RESTART_SIGNAL_FILE).getParent());
        Files.writeString(Paths.get(RESTART_SIGNAL_FILE), objectMapper.writeValueAsString(restartInfo));
        log.info("✅ 重启信号文件已创建");
    }

    /**
     * 初始化HTTP客户端
     */
    private synchronized void ensureHttpClientInitialized() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            log.debug("✅ HTTP客户端已初始化");
        }
    }

    /**
     * 获取构建时间
     */
    private String getBuildTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 获取Java进程PID
     */
    private String getJavaProcessPid() {
        try {
            ProcessBuilder pb = new ProcessBuilder("pgrep", "-f", "java.*app.jar");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String pid = reader.readLine();
            process.waitFor();
            return pid;
        } catch (Exception e) {
            log.warn("获取Java进程PID失败", e);
            return null;
        }
    }

    /**
     * 杀死Java进程
     */
    private void killJavaProcess(String pid) throws Exception {
        // 先尝试优雅停止
        ProcessBuilder pb = new ProcessBuilder("kill", "-TERM", pid);
        Process process = pb.start();
        process.waitFor();
        
        // 等待3秒
        Thread.sleep(3000);
        
        // 检查进程是否还存在，如果存在则强制杀死
        pb = new ProcessBuilder("kill", "-0", pid);
        process = pb.start();
        if (process.waitFor() == 0) {
            // 进程还存在，强制杀死
            pb = new ProcessBuilder("kill", "-KILL", pid);
            pb.start().waitFor();
        }
    }

    /**
     * 准备目录
     */
    private void prepareDirs() throws IOException {
        // 创建临时目录
        Path tempDir = Paths.get(TEMP_DIR);
        if (Files.exists(tempDir)) {
            deleteDirectory(tempDir);
        }
        Files.createDirectories(tempDir);
        
        // 创建备份目录
        Path backupDir = Paths.get(BACKUP_DIR);
        if (Files.exists(backupDir)) {
            deleteDirectory(backupDir);
        }
        Files.createDirectories(backupDir);
    }

    /**
     * 备份当前版本
     */
    private void backupCurrentVersion() throws IOException {
        // 备份前端
        Path frontendBackup = Paths.get(BACKUP_DIR, "frontend");
        Files.createDirectories(frontendBackup);
        copyDirectory(Paths.get(FRONTEND_PATH), frontendBackup);
        
        // 备份后端
        Path backendBackup = Paths.get(BACKUP_DIR, "backend.jar");
        Files.copy(Paths.get(BACKEND_JAR), backendBackup, StandardCopyOption.REPLACE_EXISTING);
        
        // 备份版本文件
        Path versionBackup = Paths.get(BACKUP_DIR, "current_version");
        Files.copy(Paths.get(VERSION_FILE), versionBackup, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 回滚更新
     */
    private void rollbackUpdate() throws Exception {
        log.info("🔄 开始回滚更新...");
        
        // 回滚前端
        Path frontendBackup = Paths.get(BACKUP_DIR, "frontend");
        if (Files.exists(frontendBackup)) {
            deleteDirectoryContents(Paths.get(FRONTEND_PATH));
            copyDirectory(frontendBackup, Paths.get(FRONTEND_PATH));
        }
        
        // 回滚后端
        Path backendBackup = Paths.get(BACKUP_DIR, "backend.jar");
        if (Files.exists(backendBackup)) {
            Files.copy(backendBackup, Paths.get(BACKEND_JAR), StandardCopyOption.REPLACE_EXISTING);
            
            // 重启Java应用
            String javaPid = getJavaProcessPid();
            if (javaPid != null) {
                killJavaProcess(javaPid);
            }
            Thread.sleep(3000);
            restartJavaApplication();
            waitForApplicationStartup();
        }
        
        // 回滚版本文件
        Path versionBackup = Paths.get(BACKUP_DIR, "current_version");
        if (Files.exists(versionBackup)) {
            Files.copy(versionBackup, Paths.get(VERSION_FILE), StandardCopyOption.REPLACE_EXISTING);
        }
        
        log.info("✅ 回滚完成");
    }

    /**
     * 更新版本记录
     */
    private void updateVersionRecord(String newVersion) throws IOException {
        Files.createDirectories(Paths.get(VERSION_FILE).getParent());
        Files.writeString(Paths.get(VERSION_FILE), newVersion);
        log.info("✅ 版本记录已更新: {} -> {}", VERSION_FILE, newVersion);
        
        // 清除缓存，强制下次重新读取
        clearCache();
        log.info("🗑️ 版本缓存已清除，下次将读取新版本");
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFiles() {
        try {
            deleteDirectory(Paths.get(TEMP_DIR));
            deleteDirectory(Paths.get(BACKUP_DIR));
            log.info("临时文件已清理");
        } catch (IOException e) {
            log.warn("清理临时文件失败", e);
        }
    }

    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path, e);
                        }
                    });
        }
    }

    /**
     * 删除目录内容但保留目录本身
     */
    private void deleteDirectoryContents(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .filter(path -> !path.equals(directory))
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path, e);
                        }
                    });
        }
    }

    /**
     * 复制目录
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                log.error("复制文件失败: {} -> {}", sourcePath, target, e);
            }
        });
    }

    /**
     * 设置目录权限
     */
    private void setDirectoryPermissions(Path directory, String permissions) {
        try {
            ProcessBuilder pb = new ProcessBuilder("chmod", "-R", permissions, directory.toString());
            pb.start().waitFor();
        } catch (Exception e) {
            log.warn("设置目录权限失败: {}", directory, e);
        }
    }

    /**
     * 重启Java应用（完全修复版本 - 解决进程管理和日志问题）
     */
    private void restartJavaApplication() throws Exception {
        log.info("🚀 重新启动Java应用...");
        
        // 检查环境和文件
        Path jarFile = Paths.get(BACKEND_JAR);
        if (!Files.exists(jarFile)) {
            throw new RuntimeException("Backend JAR文件不存在: " + BACKEND_JAR);
        }
        
        log.info("📍 当前工作目录: {}", System.getProperty("user.dir"));
        log.info("📦 JAR文件路径: {}", jarFile.toAbsolutePath());
        log.info("📊 JAR文件大小: {} bytes", Files.size(jarFile));
        
        // 创建启动脚本，确保进程正确管理
        String startScript = createStartupScript();
        
        try {
            // 使用exec替换当前进程，这样新进程会继承容器的进程树
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", startScript);
            
            // 设置工作目录
            pb.directory(new File("/app"));
            
            // 关键：继承父进程的IO，确保日志输出到容器标准输出
            pb.inheritIO();
            
            // 设置环境变量
            Map<String, String> env = pb.environment();
            env.put("JAVA_OPTS", "-Xmx512m -Xms256m");
            env.put("SPRING_PROFILES_ACTIVE", "prod");
            env.put("SERVER_PORT", "8080");
            
            log.info("🔧 使用启动脚本: {}", startScript);
            
            // 启动新进程
            Process process = pb.start();
            
            // 检查进程是否立即失败
            try {
                Thread.sleep(3000); // 等待3秒检查
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("启动检查被中断", ie);
            }
            
            if (!process.isAlive()) {
                int exitCode = process.exitValue();
                log.error("❌ Java进程启动后立即退出，退出码: {}", exitCode);
                throw new RuntimeException("Java应用启动失败，退出码: " + exitCode);
            }
            
            log.info("✅ 新的Java应用已启动，PID: {}", process.pid());
            log.info("📝 日志将正常输出到容器标准输出");
            
        } catch (Exception e) {
            log.error("❌ 启动Java应用时发生异常", e);
            throw new RuntimeException("启动Java应用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建正确的启动脚本
     */
    private String createStartupScript() throws IOException {
        String scriptPath = "/tmp/restart_app.sh";
        
        // 创建启动脚本内容
        String scriptContent = String.format(
            "#!/bin/bash\n" +
            "set -e\n" +
            "echo \"🚀 启动新的DockPilot应用...\"\n" +
            "cd /app\n" +
            "\n" +
            "# 确保日志目录存在\n" +
            "mkdir -p /dockpilot/logs\n" +
            "\n" +
            "# 设置Java参数\n" +
            "export JAVA_OPTS=\"-Xmx512m -Xms256m -XX:+UseG1GC\"\n" +
            "export SPRING_PROFILES_ACTIVE=prod\n" +
            "\n" +
            "# 启动应用 - 关键：不使用nohup，让进程正确继承容器环境\n" +
            "exec java $JAVA_OPTS -jar %s \\\n" +
            "  --spring.profiles.active=prod \\\n" +
            "  --server.port=8080 \\\n" +
            "  --logging.file.path=/dockpilot/logs \\\n" +
            "  --logging.level.com.dockpilot=INFO \\\n" +
            "  --logging.pattern.console='%%d{yyyy-MM-dd HH:mm:ss.SSS} [%%thread] %%level %%logger{50} - %%msg%%n'\n",
            BACKEND_JAR
        );
        
        // 写入脚本文件
        Files.writeString(Paths.get(scriptPath), scriptContent);
        
        // 设置执行权限
        ProcessBuilder chmod = new ProcessBuilder("chmod", "+x", scriptPath);
        try {
            chmod.start().waitFor();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("设置脚本权限被中断", ie);
        }
        
        log.info("✅ 启动脚本已创建: {}", scriptPath);
        return scriptPath;
    }

    /**
     * 等待应用启动（原始方法，保持兼容性）
     */
    private void waitForApplicationStartup() throws Exception {
        log.info("等待应用启动...");
        
        int maxAttempts = 30; // 最多等待30秒
        for (int i = 0; i < maxAttempts; i++) {
            try {
                // 尝试访问健康检查端点
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/update/version"))
                        .timeout(Duration.ofSeconds(2))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    log.info("✅ 应用启动成功");
                    return;
                }
            } catch (Exception e) {
                // 继续等待
            }
            
            Thread.sleep(1000);
        }
        
        throw new RuntimeException("应用启动超时，可能启动失败");
    }

    /**
     * 等待指定端口的应用启动
     */
    private boolean waitForApplicationStartupOnPort(int port, int maxSeconds) {
        log.info("等待端口{}上的应用启动...", port);
        
        // 确保HTTP客户端已初始化
        ensureHttpClientInitialized();
        
        for (int i = 0; i < maxSeconds; i++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/update/version"))
                        .timeout(Duration.ofSeconds(5))
                        .build();
                
                log.debug("尝试第{}次检查新应用是否启动 (端口: {})", i+1, port);
                
                try {
                    HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        log.info("✅ 端口{}上的应用启动成功，收到200响应", port);
                        return true;
                    } else {
                        log.debug("收到非200响应：{}", response.statusCode());
                    }
                } catch (ConnectException e) {
                    log.debug("连接被拒绝，新应用可能尚未启动完成");
                } catch (Exception e) {
                    log.debug("检查失败: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                }
                
                // 打印倒计时信息
                if (i % 5 == 0) {
                    log.info("⏳ 继续等待新应用启动，已等待{}秒，最多等待{}秒", i, maxSeconds);
                }
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            } catch (Exception e) {
                log.warn("检查应用启动状态异常: {}", e.getMessage());
                // 继续等待
            }
        }
        
        log.error("❌ 端口{}上的应用启动超时 ({}秒)", port, maxSeconds);
        return false;
    }

    // 📦 缓存机制相关
    
    /**
     * 获取缓存的更新信息
     */
    private UpdateInfoDTO getCachedUpdateInfo() {
        // 1. 检查内存缓存
        if (cachedUpdateInfo != null && lastCheckTime != null && 
            LocalDateTime.now().isBefore(lastCheckTime.plusMinutes(CACHE_DURATION_MINUTES))) {
            log.debug("✅ 使用内存缓存 (剩余{}分钟)", 
                java.time.Duration.between(LocalDateTime.now(), lastCheckTime.plusMinutes(CACHE_DURATION_MINUTES)).toMinutes());
            return cachedUpdateInfo;
        }
        
        // 2. 尝试从文件缓存加载
        try {
            UpdateInfoDTO fileCache = loadCacheFromFile();
            if (fileCache != null) {
                log.debug("✅ 使用文件缓存");
                // 更新内存缓存
                cachedUpdateInfo = fileCache;
                lastCheckTime = fileCache.getLastCheckTime();
                return fileCache;
            }
        } catch (Exception e) {
            log.warn("⚠️ 加载文件缓存失败: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * 缓存更新信息（内存+文件）
     */
    private void cacheUpdateInfo(UpdateInfoDTO updateInfo, JsonNode releaseData) {
        // 内存缓存
        cachedUpdateInfo = updateInfo;
        lastCheckTime = LocalDateTime.now();
        lastReleaseData = releaseData;
        
        // 文件缓存
        try {
            saveCacheToFile(updateInfo);
        } catch (Exception e) {
            log.warn("⚠️ 保存缓存到文件失败: {}", e.getMessage());
        }
    }

    /**
     * 保存缓存到文件
     */
    private void saveCacheToFile(UpdateInfoDTO updateInfo) throws Exception {
        Path cacheFile = Paths.get(CACHE_FILE);
        Files.createDirectories(cacheFile.getParent());
        
        Map<String, Object> cacheData = new HashMap<>();
        cacheData.put("updateInfo", updateInfo);
        cacheData.put("timestamp", LocalDateTime.now().toString());
        cacheData.put("cacheVersion", "1.0");
        
        Files.writeString(cacheFile, objectMapper.writeValueAsString(cacheData));
        log.debug("💾 已保存缓存到文件: {}", CACHE_FILE);
    }

    /**
     * 从文件加载缓存
     */
    private UpdateInfoDTO loadCacheFromFile() throws Exception {
        Path cacheFile = Paths.get(CACHE_FILE);
        if (!Files.exists(cacheFile)) {
            return null;
        }
        
        String content = Files.readString(cacheFile);
        JsonNode cacheJson = objectMapper.readTree(content);
        
        // 检查缓存是否过期
        String timestampStr = cacheJson.get("timestamp").asText();
        LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
        if (timestamp.isBefore(LocalDateTime.now().minusMinutes(CACHE_DURATION_MINUTES))) {
            log.debug("⏰ 文件缓存已过期，删除缓存文件");
            Files.deleteIfExists(cacheFile);
            return null;
        }
        
        // 解析更新信息
        JsonNode updateInfoNode = cacheJson.get("updateInfo");
        return objectMapper.treeToValue(updateInfoNode, UpdateInfoDTO.class);
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        // 简化版本不需要缓存
    }

    /**
     * 定时检查更新（每2小时执行一次）
     * 后台静默更新缓存，用户界面从缓存快速读取
     */
    @Scheduled(fixedRate = 2 * 60 * 60 * 1000) // 2小时 = 2 * 60 * 60 * 1000 毫秒
    public void scheduledUpdateCheck() {
        try {
            log.debug("⏰ 定时检查更新开始...");
            
            // 强制清除缓存，确保获取最新信息
            cachedUpdateInfo = null;
            lastCheckTime = null;
            lastReleaseData = null;
            
            // 执行检查并更新缓存
            UpdateInfoDTO updateInfo = checkForUpdates();
            
            log.info("✅ 定时检查更新完成: {} -> {} (有更新: {})", 
                    updateInfo.getCurrentVersion(), 
                    updateInfo.getLatestVersion(), 
                    updateInfo.isHasUpdate());
                    
        } catch (Exception e) {
            log.warn("⚠️ 定时检查更新失败，但不影响系统运行: {}", e.getMessage());
            // 定时任务失败不抛出异常，避免影响其他功能
        }
    }

    /**
     * 服务启动后初始化
     */
    @PostConstruct
    public void initializeService() {
        // 🔥 启动时清理旧的下载状态，避免重启后状态错乱
        cleanupOldDownloadStatus();
        
        // 初始化HTTP客户端，支持重定向
        initHttpClient();
        
        // 延迟30秒后执行首次检查，避免启动时网络未就绪
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(30000); // 等待30秒
                log.info("🚀 应用启动后首次检查更新...");
                scheduledUpdateCheck();
            } catch (Exception e) {
                log.warn("⚠️ 启动后首次检查更新失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 清理旧的下载状态文件
     */
    private void cleanupOldDownloadStatus() {
        try {
            // 检查下载状态文件是否存在
            if (Files.exists(Paths.get(DOWNLOAD_STATUS_FILE))) {
                log.info("🧹 发现旧的下载状态文件，正在清理...");
                Files.delete(Paths.get(DOWNLOAD_STATUS_FILE));
                log.info("✅ 下载状态文件已清理，避免重启后状态错乱");
            }
            
            // 同时清理下载目录中的残留文件
            if (Files.exists(Paths.get(DOWNLOAD_DIR))) {
                log.info("🧹 清理下载目录中的残留文件...");
                Files.walk(Paths.get(DOWNLOAD_DIR))
                     .sorted((a, b) -> b.compareTo(a)) // 先删除文件再删除目录
                     .forEach(path -> {
                         try {
                             Files.deleteIfExists(path);
                         } catch (IOException e) {
                             log.warn("清理下载文件失败: {}", path);
                         }
                     });
                log.info("✅ 下载目录已清理");
            }
        } catch (Exception e) {
            log.warn("⚠️ 清理旧下载状态失败，但不影响正常运行: {}", e.getMessage());
        }
    }

    /**
     * 初始化HTTP客户端
     */
    private synchronized void initHttpClient() {
        if (httpClient == null) {
            log.info("初始化HTTP客户端...");
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL) // 支持HTTP重定向
                    .build();
            log.info("✅ HTTP客户端已初始化（支持重定向）");
        }
    }

    /**
     * 创建fallback更新信息
     */
    private UpdateInfoDTO createFallbackUpdateInfo(String currentVersion) {
        return UpdateInfoDTO.builder()
                .currentVersion(currentVersion)
                .latestVersion("unknown")
                .hasUpdate(false)
                .releaseNotes("无法获取最新版本信息，请检查网络连接")
                .lastCheckTime(LocalDateTime.now())
                .status("network-error")
                .progress(0)
                .build();
    }

} 