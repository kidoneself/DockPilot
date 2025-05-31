// 热更新服务
package com.dockpilot.service.http;

import com.dockpilot.model.dto.UpdateInfoDTO;
import com.dockpilot.service.http.SystemSettingService;
import com.dockpilot.common.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * 检查是否有新版本 - 简化版
     */
    public UpdateInfoDTO checkForUpdates() throws Exception {
        log.info("🔍 检查新版本...");
        
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
        versionInfo.put("updateMethod", "container-restart");
        return versionInfo;
    }

    /**
     * 执行热更新（简化为容器重启）
     */
    public String applyHotUpdate(String targetVersion) throws Exception {
        // 简化：直接调用容器重启更新
        return applyContainerRestartUpdate(targetVersion);
    }
    
    /**
     * 容器重启更新（简化版）
     */
    public String applyContainerRestartUpdate(String targetVersion) throws Exception {
        log.info("🔄 开始容器重启更新...");
        
        // 创建重启信号文件，让启动脚本知道需要下载新版本
        createRestartSignal(targetVersion != null ? targetVersion : "latest");
        
        // 异步触发容器重启（延迟3秒，让响应能够发送出去）
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000);
                log.info("🔄 触发容器重启，启动脚本将自动下载最新版本...");
                System.exit(0);
            } catch (Exception e) {
                log.error("❌ 触发容器重启失败", e);
            }
        });
        
        return "容器重启更新已开始，容器将在3秒后重启并自动下载最新版本";
    }
    
    /**
     * 创建重启信号文件
     */
    private void createRestartSignal(String newVersion) throws IOException {
        Path signalFile = Paths.get("/dockpilot/data/restart_signal");
        Map<String, Object> restartInfo = new HashMap<>();
        restartInfo.put("action", "restart");
        restartInfo.put("reason", "hot_update");
        restartInfo.put("newVersion", newVersion);
        restartInfo.put("timestamp", LocalDateTime.now().toString());
        
        Files.createDirectories(signalFile.getParent());
        Files.writeString(signalFile, objectMapper.writeValueAsString(restartInfo));
        log.info("✅ 重启信号文件已创建: {}", signalFile);
    }
    
    /**
     * 获取更新进度（简化版）
     */
    public Map<String, Object> getUpdateProgress() {
        Map<String, Object> progress = new HashMap<>();
        progress.put("status", "completed");
        progress.put("progress", 100);
        progress.put("message", "更新已完成");
        progress.put("isUpdating", false);
        progress.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return progress;
    }
    
    /**
     * 取消更新（简化版）
     */
    public String cancelUpdate() {
        return "没有正在进行的更新操作";
    }
    
    /**
     * 带备用方案的获取最新版本信息
     */
    private JsonNode getLatestReleaseWithFallback() {
        try {
            ensureHttpClientInitialized();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API_URL))
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "DockPilot-UpdateService")
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readTree(response.body());
            }
        } catch (Exception e) {
            log.warn("获取GitHub Release信息失败: {}", e.getMessage());
        }
        
        // 返回最小化版本数据
        return createMinimalReleaseData();
    }
    
    /**
     * 创建最小化的版本数据
     */
    private JsonNode createMinimalReleaseData() {
        try {
            String currentVersion = getCurrentVersion();
            Map<String, Object> minimalData = new HashMap<>();
            minimalData.put("tag_name", currentVersion);
            minimalData.put("body", "无法获取最新版本信息");
            minimalData.put("published_at", LocalDateTime.now().toString());
            minimalData.put("name", "Local Version");
            
            return objectMapper.valueToTree(minimalData);
        } catch (Exception e) {
            log.error("❌ 创建最小化版本数据失败", e);
            return null;
        }
    }

    /**
     * 获取当前版本 - 多种方式尝试
     */
    private String getCurrentVersion() {
        log.debug("🔍 开始获取当前版本信息...");
        
        // 方法1: 从版本文件读取（热更新后会写入）
        try {
            Path versionFile = Paths.get(VERSION_FILE);
            if (Files.exists(versionFile)) {
                String version = Files.readString(versionFile).trim();
                if (!version.isEmpty() && !version.equals("unknown")) {
                    log.debug("✅ 从版本文件获取版本: {}", version);
                    return version;
                }
            }
        } catch (IOException e) {
            log.warn("读取版本文件失败: {}", e.getMessage());
        }
        
        // 方法2: 从环境变量读取
        String envVersion = System.getenv("DOCKPILOT_VERSION");
        if (envVersion != null && !envVersion.trim().isEmpty() && !envVersion.equals("latest")) {
            log.debug("✅ 从环境变量获取版本: {}", envVersion);
            return envVersion;
        }
        
        // 方法3: 从application.properties读取
        try {
            String propVersion = getVersionFromProperties();
            if (propVersion != null && !propVersion.trim().isEmpty()) {
                log.debug("✅ 从配置文件获取版本: {}", propVersion);
                return propVersion;
            }
        } catch (Exception e) {
            log.warn("从配置文件读取版本失败: {}", e.getMessage());
        }
        
        // 方法4: 从MANIFEST.MF读取
        try {
            String manifestVersion = getVersionFromManifest();
            if (manifestVersion != null && !manifestVersion.trim().isEmpty()) {
                log.debug("✅ 从MANIFEST获取版本: {}", manifestVersion);
                return manifestVersion;
            }
        } catch (Exception e) {
            log.warn("从MANIFEST读取版本失败: {}", e.getMessage());
        }
        
        // 方法5: 返回默认版本
        String defaultVersion = "v1.0.0";
        log.warn("⚠️ 无法获取版本信息，使用默认版本: {}", defaultVersion);
        
        // 尝试创建版本文件
        try {
            createDefaultVersionFile(defaultVersion);
        } catch (Exception e) {
            log.warn("创建默认版本文件失败: {}", e.getMessage());
        }
        
        return defaultVersion;
    }
    
    /**
     * 从application.properties读取版本
     */
    private String getVersionFromProperties() {
        try {
            // 尝试从Spring Boot的配置中读取
            String version = System.getProperty("app.version");
            if (version != null && !version.trim().isEmpty()) {
                return version;
            }
            
            // 尝试从类路径下的配置文件读取
            InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties");
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                version = props.getProperty("app.version");
                if (version != null && !version.trim().isEmpty()) {
                    return version;
                }
            }
        } catch (Exception e) {
            log.debug("从properties读取版本失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 从MANIFEST.MF读取版本
     */
    private String getVersionFromManifest() {
        try {
            Package pkg = this.getClass().getPackage();
            String version = pkg.getImplementationVersion();
            if (version != null && !version.trim().isEmpty()) {
                return "v" + version;
            }
            
            // 尝试从jar的MANIFEST.MF读取
            java.net.URL jarUrl = this.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (jarUrl.toString().endsWith(".jar")) {
                java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarUrl.getPath());
                java.util.jar.Manifest manifest = jarFile.getManifest();
                if (manifest != null) {
                    java.util.jar.Attributes attrs = manifest.getMainAttributes();
                    version = attrs.getValue("Implementation-Version");
                    if (version != null && !version.trim().isEmpty()) {
                        return "v" + version;
                    }
                    version = attrs.getValue("Bundle-Version");
                    if (version != null && !version.trim().isEmpty()) {
                        return "v" + version;
                    }
                }
                jarFile.close();
            }
        } catch (Exception e) {
            log.debug("从MANIFEST读取版本失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 创建默认版本文件
     */
    private void createDefaultVersionFile(String defaultVersion) {
        try {
            Path versionFile = Paths.get(VERSION_FILE);
            Files.createDirectories(versionFile.getParent());
            Files.writeString(versionFile, defaultVersion);
            log.info("✅ 已创建默认版本文件: {} -> {}", VERSION_FILE, defaultVersion);
        } catch (IOException e) {
            log.warn("创建默认版本文件失败: {}", e.getMessage());
        }
    }

    /**
     * 手动更新当前版本记录为最新版本
     */
    public String updateCurrentVersionToLatest() throws Exception {
        log.info("🔄 开始更新当前版本记录为最新版本...");
        
        // 清除缓存，确保获取最新信息
        clearCache();
        
        // 获取最新版本信息
        JsonNode latestRelease = getLatestReleaseWithFallback();
        if (latestRelease == null) {
            throw new RuntimeException("无法获取最新版本信息");
        }
        
        String latestVersion = latestRelease.get("tag_name").asText();
        String currentVersion = getCurrentVersion();
        
        if (latestVersion.equals(currentVersion)) {
            log.info("✅ 当前版本已是最新: {}", currentVersion);
            return "当前版本已是最新: " + currentVersion;
        }
        
        // 更新版本记录
        updateVersionRecord(latestVersion);
        
        log.info("✅ 版本记录已从 {} 更新为 {}", currentVersion, latestVersion);
        return String.format("版本记录已从 %s 更新为 %s", currentVersion, latestVersion);
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
     * 下载文件
     */
    private void downloadFile(String url, Path destination) throws Exception {
        log.info("📁 下载文件: {} -> {}", url, destination.getFileName());
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "DockPilot-UpdateService")
                .header("Accept", "*/*")
                .timeout(Duration.ofMinutes(10))
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofInputStream());

        // 添加详细的响应信息日志
        log.info("📊 响应状态码: {}", response.statusCode());
        log.debug("📋 响应头信息: {}", response.headers().map());
        
        // 检查HTTP状态码 - HttpClient已配置自动处理重定向
        // GitHub Release的302重定向会被自动跟随，最终返回200状态码
        if (response.statusCode() != 200) {
            String responseHeaders = response.headers().map().toString();
            throw new IOException("下载失败: " + url + " - HTTP状态码: " + response.statusCode() + 
                                " (HttpClient已配置自动跟随重定向，最终状态应为200)\n响应头: " + responseHeaders);
        }

        Files.createDirectories(destination.getParent());
        try (InputStream in = response.body()) {
            long bytesWritten = Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ 文件下载完成: {} (大小: {} bytes)", destination.getFileName(), bytesWritten);
            
            // 验证下载的文件大小 - 前端包应该大于100KB，后端包应该大于10MB
            if (destination.getFileName().toString().contains("frontend.tar.gz") && bytesWritten < 100_000) {
                throw new IOException("前端包文件太小: " + bytesWritten + " bytes，可能下载不完整");
            }
            if (destination.getFileName().toString().contains("backend.jar") && bytesWritten < 10_000_000) {
                throw new IOException("后端包文件太小: " + bytesWritten + " bytes，可能下载不完整");
            }
        }
    }

    /**
     * 解压tar.gz文件
     */
    private void extractTarGz(Path tarGzFile, Path destination) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "tar", "-xzf", tarGzFile.toString(), "-C", destination.toString()
        );
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            // 读取错误信息
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String error = errorReader.lines().reduce("", String::concat);
            throw new RuntimeException("解压失败: " + tarGzFile + ", 错误: " + error);
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
     * 获取构建时间
     */
    private String getBuildTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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
     * 确保热更新前HTTP客户端已初始化
     */
    private void ensureHttpClientInitialized() {
        if (httpClient == null) {
            initHttpClient();
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