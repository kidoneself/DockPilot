// 热更新服务
package com.dsm.service.http;

import com.dsm.model.dto.UpdateInfoDTO;
import com.dsm.service.http.SystemSettingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
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

/**
 * 容器内热更新服务
 * 核心功能：不重启容器的情况下更新前后端代码
 */
@Slf4j
@Service
public class UpdateService {

    @Autowired
    private SystemSettingService systemSettingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // 更新状态管理
    private volatile Map<String, Object> updateProgress = new HashMap<>();
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    private volatile CompletableFuture<Void> currentUpdateTask;

    // 系统路径配置
    private static final String GITHUB_API_URL = "https://api.github.com/repos/kidoneself/DockPilot/releases/latest";
    private static final String VERSION_FILE = "/dockpilot/data/current_version";
    private static final String FRONTEND_PATH = "/usr/share/html";
    private static final String BACKEND_JAR = "/app/app.jar";
    private static final String TEMP_DIR = "/tmp/dockpilot-update";
    private static final String BACKUP_DIR = "/tmp/dockpilot-backup";

    /**
     * 检查是否有新版本
     */
    public UpdateInfoDTO checkForUpdates() throws Exception {
        log.info("🔍 开始检查新版本...");
        
        String currentVersion = getCurrentVersion();
        JsonNode latestRelease = getLatestReleaseFromGitHub();
        
        String latestVersion = latestRelease.get("tag_name").asText();
        String releaseNotes = latestRelease.get("body").asText();
        String publishedAt = latestRelease.get("published_at").asText();
        
        boolean hasUpdate = !currentVersion.equals(latestVersion) && 
                           !latestVersion.equals("unknown") && 
                           !currentVersion.equals("unknown");
        
        return UpdateInfoDTO.builder()
                .currentVersion(currentVersion)
                .latestVersion(latestVersion)
                .hasUpdate(hasUpdate)
                .releaseNotes(releaseNotes)
                .lastCheckTime(LocalDateTime.now())
                .status(hasUpdate ? "available" : "up-to-date")
                .progress(0)
                .build();
    }

    /**
     * 获取当前版本信息
     */
    public Map<String, String> getCurrentVersionInfo() {
        Map<String, String> versionInfo = new HashMap<>();
        versionInfo.put("currentVersion", getCurrentVersion());
        versionInfo.put("buildTime", getBuildTime());
        versionInfo.put("updateMethod", "hot-update");
        versionInfo.put("frontendPath", FRONTEND_PATH);
        versionInfo.put("backendJar", BACKEND_JAR);
        return versionInfo;
    }

    /**
     * 执行热更新
     */
    public String applyHotUpdate(String targetVersion) throws Exception {
        if (!isUpdating.compareAndSet(false, true)) {
            throw new RuntimeException("更新正在进行中，请稍后再试");
        }

        // 异步执行更新
        currentUpdateTask = CompletableFuture.runAsync(() -> {
            try {
                executeHotUpdate(targetVersion);
            } catch (Exception e) {
                log.error("❌ 热更新失败", e);
                updateProgress.put("status", "failed");
                updateProgress.put("error", e.getMessage());
                updateProgress.put("progress", 0);
            } finally {
                isUpdating.set(false);
            }
        });

        return "热更新已开始，请通过 /api/update/progress 查看更新进度";
    }

    /**
     * 执行具体的热更新流程
     */
    private void executeHotUpdate(String targetVersion) throws Exception {
        log.info("🚀 开始执行热更新...");
        updateProgress.clear();
        updateProgress.put("status", "starting");
        updateProgress.put("progress", 0);
        updateProgress.put("message", "初始化更新...");

        try {
            // 1. 准备工作
            updateProgress.put("message", "准备更新环境...");
            prepareDirs();
            updateProgress.put("progress", 10);

            // 2. 获取目标版本
            if (targetVersion == null) {
                JsonNode latestRelease = getLatestReleaseFromGitHub();
                targetVersion = latestRelease.get("tag_name").asText();
            }
            updateProgress.put("targetVersion", targetVersion);
            updateProgress.put("message", "目标版本: " + targetVersion);

            // 3. 下载更新包
            updateProgress.put("status", "downloading");
            updateProgress.put("message", "下载前后端更新包...");
            downloadReleaseFiles(targetVersion);
            updateProgress.put("progress", 40);

            // 4. 备份当前版本
            updateProgress.put("message", "备份当前版本...");
            backupCurrentVersion();
            updateProgress.put("progress", 50);

            // 5. 应用前端更新
            updateProgress.put("status", "applying");
            updateProgress.put("message", "应用前端更新...");
            applyFrontendUpdate();
            updateProgress.put("progress", 70);

            // 6. 应用后端更新
            updateProgress.put("message", "应用后端更新，重启Java服务...");
            applyBackendUpdate();
            updateProgress.put("progress", 90);

            // 7. 完成更新
            updateProgress.put("message", "更新版本记录...");
            updateVersionRecord(targetVersion);
            cleanupTempFiles();
            
            updateProgress.put("status", "completed");
            updateProgress.put("progress", 100);
            updateProgress.put("message", "热更新完成！版本: " + targetVersion);
            
            log.info("✅ 热更新完成: {}", targetVersion);

        } catch (Exception e) {
            log.error("❌ 热更新失败，开始回滚...", e);
            updateProgress.put("status", "rolling-back");
            updateProgress.put("message", "更新失败，正在回滚...");
            
            try {
                rollbackUpdate();
                updateProgress.put("message", "已回滚到之前版本");
            } catch (Exception rollbackError) {
                log.error("回滚失败", rollbackError);
                updateProgress.put("message", "回滚失败: " + rollbackError.getMessage());
            }
            
            updateProgress.put("status", "failed");
            updateProgress.put("error", e.getMessage());
            throw e;
        }
    }

    /**
     * 下载GitHub Release文件
     */
    private void downloadReleaseFiles(String version) throws Exception {
        log.info("📦 下载版本文件: {}", version);
        
        // 下载前端包
        String frontendUrl = String.format(
            "https://github.com/kidoneself/DockPilot/releases/download/%s/frontend.tar.gz", 
            version
        );
        downloadFile(frontendUrl, Paths.get(TEMP_DIR, "frontend.tar.gz"));

        // 下载后端包
        String backendUrl = String.format(
            "https://github.com/kidoneself/DockPilot/releases/download/%s/backend.jar", 
            version
        );
        downloadFile(backendUrl, Paths.get(TEMP_DIR, "backend.jar"));
        
        log.info("✅ 文件下载完成");
    }

    /**
     * 应用前端更新
     */
    private void applyFrontendUpdate() throws Exception {
        log.info("🎨 应用前端更新...");
        
        Path frontendTarGz = Paths.get(TEMP_DIR, "frontend.tar.gz");
        Path frontendPath = Paths.get(FRONTEND_PATH);
        
        // 验证前端包
        if (!Files.exists(frontendTarGz)) {
            throw new RuntimeException("前端更新包不存在");
        }
        
        // 清空当前前端目录
        deleteDirectoryContents(frontendPath);
        
        // 解压新前端
        extractTarGz(frontendTarGz, frontendPath);
        
        // 设置正确的权限
        setDirectoryPermissions(frontendPath, "755");
        
        log.info("✅ 前端更新完成，Caddy将自动服务新文件");
    }

    /**
     * 应用后端更新
     */
    private void applyBackendUpdate() throws Exception {
        log.info("⚙️ 应用后端更新...");
        
        Path newJar = Paths.get(TEMP_DIR, "backend.jar");
        Path currentJar = Paths.get(BACKEND_JAR);
        
        // 验证后端包
        if (!Files.exists(newJar)) {
            throw new RuntimeException("后端更新包不存在");
        }
        
        // 获取当前Java进程PID
        String javaPid = getJavaProcessPid();
        
        // 替换jar文件
        Files.copy(newJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
        
        // 优雅停止Java进程
        if (javaPid != null) {
            log.info("停止Java进程: {}", javaPid);
            killJavaProcess(javaPid);
        }
        
        // 等待进程完全停止
        Thread.sleep(3000);
        
        // 重新启动Java应用
        restartJavaApplication();
        
        // 等待应用启动并验证
        waitForApplicationStartup();
        
        log.info("✅ 后端更新完成，Java应用已重启");
    }

    /**
     * 重启Java应用
     */
    private void restartJavaApplication() throws Exception {
        log.info("重新启动Java应用...");
        
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-jar", BACKEND_JAR
        );
        
        // 设置环境变量
        Map<String, String> env = pb.environment();
        env.put("SPRING_PROFILES_ACTIVE", "prod");
        env.put("LOG_PATH", "/dockpilot/logs");
        
        pb.directory(new File("/app"));
        pb.redirectOutput(new File("/dockpilot/logs/application-restart.log"));
        pb.redirectError(ProcessBuilder.Redirect.appendTo(new File("/dockpilot/logs/application-restart.log")));
        
        Process process = pb.start();
        log.info("Java应用已启动，PID: {}", process.pid());
    }

    /**
     * 等待应用启动
     */
    private void waitForApplicationStartup() throws Exception {
        log.info("等待应用启动...");
        
        int maxAttempts = 30; // 最多等待30秒
        for (int i = 0; i < maxAttempts; i++) {
            try {
                // 尝试访问健康检查端点
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/update/version"))
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
     * 获取更新进度
     */
    public Map<String, Object> getUpdateProgress() {
        Map<String, Object> progress = new HashMap<>(updateProgress);
        progress.put("isUpdating", isUpdating.get());
        progress.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return progress;
    }

    /**
     * 取消更新
     */
    public String cancelUpdate() {
        if (currentUpdateTask != null && !currentUpdateTask.isDone()) {
            currentUpdateTask.cancel(true);
            isUpdating.set(false);
            updateProgress.put("status", "cancelled");
            updateProgress.put("message", "更新已取消");
            return "更新已取消";
        }
        return "没有正在进行的更新操作";
    }

    /**
     * 设置自动检查更新
     */
    public void setAutoCheckEnabled(boolean enabled) {
        systemSettingService.set("auto_check_update_enabled", String.valueOf(enabled));
        log.info("自动检查更新设置: {}", enabled);
    }

    /**
     * 获取更新历史
     */
    public Map<String, Object> getUpdateHistory() {
        Map<String, Object> history = new HashMap<>();
        history.put("currentVersion", getCurrentVersion());
        history.put("lastCheckTime", LocalDateTime.now());
        history.put("autoCheckEnabled", isAutoCheckEnabled());
        history.put("updateMethod", "hot-update");
        return history;
    }

    // ==================== 私有工具方法 ====================

    /**
     * 从GitHub API获取最新版本信息
     */
    private JsonNode getLatestReleaseFromGitHub() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API_URL))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DockPilot-UpdateService")
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("GitHub API请求失败: " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
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
        String defaultVersion = "v1.0.7";
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

        // 检查HTTP状态码 - 接受200和重定向后的成功响应
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("下载失败: " + url + " - HTTP状态码: " + response.statusCode());
        }

        Files.createDirectories(destination.getParent());
        try (InputStream in = response.body()) {
            long bytesWritten = Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ 文件下载完成: {} (大小: {} bytes)", destination.getFileName(), bytesWritten);
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
        log.info("版本记录已更新: {}", newVersion);
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
     * 检查是否启用自动检查
     */
    private boolean isAutoCheckEnabled() {
        String setting = systemSettingService.get("auto_check_update_enabled");
        return "true".equals(setting);
    }
} 