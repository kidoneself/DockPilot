package com.dockpilot.service;

import com.dockpilot.dto.IconInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class IconService {

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    // 缓存所有图标信息
    private final Map<String, IconInfo> iconCache = new ConcurrentHashMap<>();
    
    // 支持的图片格式
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".png", ".svg", ".jpg", ".jpeg", ".webp", ".ico");

    @PostConstruct
    public void init() {
        refreshIconCache();
    }

    /**
     * 刷新图标缓存
     */
    public int refreshIconCache() {
        iconCache.clear();
        int totalCount = 0;

        Path iconBaseDir = Paths.get(uploadPath, "icon");
        
        if (!Files.exists(iconBaseDir)) {
            log.warn("图标目录不存在: {}", iconBaseDir);
            return 0;
        }

        try {
            // 递归扫描整个icon目录
            totalCount = scanIconsRecursively(iconBaseDir);
            log.info("图标缓存刷新完成，共缓存 {} 个图标", totalCount);
            
        } catch (Exception e) {
            log.error("刷新图标缓存失败", e);
        }

        return totalCount;
    }

    /**
     * 递归扫描目录中的图标文件
     */
    private int scanIconsRecursively(Path directory) throws IOException {
        int count = 0;
        
        try {
            List<Path> allPaths = Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .filter(this::isIconFile)
                    .collect(Collectors.toList());

            for (Path iconFile : allPaths) {
                IconInfo iconInfo = createIconInfo(iconFile);
                iconCache.put(iconInfo.getName(), iconInfo);
                count++;
            }
        } catch (IOException e) {
            log.error("递归扫描目录失败: {}", directory, e);
            throw e;
        }

        return count;
    }

    /**
     * 判断是否为图标文件
     */
    private boolean isIconFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    /**
     * 创建图标信息对象
     */
    private IconInfo createIconInfo(Path iconFile) {
        String fileName = iconFile.getFileName().toString();
        String nameWithoutExt = getFileNameWithoutExtension(fileName);
        String displayName = formatDisplayName(nameWithoutExt);
        String extension = getFileExtension(fileName);
        
        try {
            long fileSize = Files.size(iconFile);
            
            return IconInfo.builder()
                    .name(nameWithoutExt)
                    .displayName(displayName)
                    .type("icon") // 统一类型
                    .url("/api/icons/" + nameWithoutExt)
                    .fileSize(fileSize)
                    .extension(extension)
                    .build();
        } catch (IOException e) {
            log.warn("获取文件大小失败: {}", iconFile, e);
            return IconInfo.builder()
                    .name(nameWithoutExt)
                    .displayName(displayName)
                    .type("icon")
                    .url("/api/icons/" + nameWithoutExt)
                    .fileSize(0L)
                    .extension(extension)
                    .build();
        }
    }

    /**
     * 获取图标列表
     */
    public List<IconInfo> getIconList(String search, String type) {
        return iconCache.values().stream()
                .filter(icon -> search == null || search.trim().isEmpty() || 
                        icon.getDisplayName().toLowerCase().contains(search.toLowerCase()) ||
                        icon.getName().toLowerCase().contains(search.toLowerCase()))
                .sorted(Comparator.comparing(IconInfo::getDisplayName))
                .collect(Collectors.toList());
    }

    /**
     * 获取图标资源文件
     */
    public Resource getIconResource(String iconName) {
        // 先从缓存中查找图标信息
        IconInfo iconInfo = iconCache.get(iconName);
        if (iconInfo != null) {
            // 在整个icon目录中查找文件
            Path iconPath = findIconFileRecursively(iconName);
            if (iconPath != null && Files.exists(iconPath)) {
                return new FileSystemResource(iconPath);
            }
        }

        // 缓存中没有，直接在目录中查找
        Path iconPath = findIconFileRecursively(iconName);
        if (iconPath != null && Files.exists(iconPath)) {
            return new FileSystemResource(iconPath);
        }

        return null;
    }

    /**
     * 在整个icon目录中递归查找图标文件
     */
    private Path findIconFileRecursively(String iconName) {
        Path iconBaseDir = Paths.get(uploadPath, "icon");
        
        if (!Files.exists(iconBaseDir)) {
            return null;
        }

        try {
            return Files.walk(iconBaseDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        String nameWithoutExt = getFileNameWithoutExtension(fileName);
                        return iconName.equals(nameWithoutExt);
                    })
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            log.error("查找图标文件失败: {}", iconName, e);
            return null;
        }
    }

    /**
     * 获取默认图标
     */
    public Resource getDefaultIcon() {
        // 尝试查找默认图标
        String[] defaultNames = {"default", "folder", "application"};
        
        for (String defaultName : defaultNames) {
            Resource resource = getIconResource(defaultName);
            if (resource != null && resource.exists()) {
                return resource;
            }
        }

        return null;
    }

    /**
     * 获取媒体类型
     */
    public MediaType getMediaType(String iconName) {
        String extension = "";
        
        // 从缓存中获取扩展名
        IconInfo iconInfo = iconCache.get(iconName);
        if (iconInfo != null) {
            extension = iconInfo.getExtension();
        } else {
            // 直接从文件查找扩展名
            Path iconPath = findIconFileRecursively(iconName);
            if (iconPath != null) {
                extension = getFileExtension(iconPath.getFileName().toString());
            }
        }

        switch (extension.toLowerCase()) {
            case ".png":
                return MediaType.IMAGE_PNG;
            case ".jpg":
            case ".jpeg":
                return MediaType.IMAGE_JPEG;
            case ".svg":
                return MediaType.valueOf("image/svg+xml");
            case ".webp":
                return MediaType.valueOf("image/webp");
            default:
                return MediaType.IMAGE_PNG;
        }
    }

    /**
     * 去掉文件扩展名
     */
    private String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    /**
     * 格式化显示名称
     */
    private String formatDisplayName(String name) {
        // Docker_A -> Docker
        // nginx-proxy -> Nginx Proxy
        return name.replaceAll("_[A-Z]$", "")
                   .replaceAll("[-_]", " ")
                   .trim();
    }

    /**
     * 上传单个图标文件
     */
    public IconInfo uploadIcon(MultipartFile file) throws IOException {
        // 验证文件
        validateIconFile(file);
        
        // 确保custom目录存在
        Path customDir = Paths.get(uploadPath, "icon", "custom");
        Files.createDirectories(customDir);
        
        // 生成安全的文件名
        String safeFileName = generateSafeFileName(file.getOriginalFilename(), customDir);
        Path targetPath = customDir.resolve(safeFileName);
        
        // 保存文件
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        // 创建图标信息并加入缓存
        IconInfo iconInfo = createIconInfo(targetPath);
        iconCache.put(iconInfo.getName(), iconInfo);
        
        log.info("成功上传图标: {}", safeFileName);
        return iconInfo;
    }
    
    /**
     * 上传多个图标文件
     */
    public List<IconInfo> uploadIcons(MultipartFile[] files) throws IOException {
        List<IconInfo> uploadedIcons = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                IconInfo iconInfo = uploadIcon(file);
                uploadedIcons.add(iconInfo);
            } catch (Exception e) {
                log.error("上传文件失败: {}", file.getOriginalFilename(), e);
                // 继续处理其他文件，不中断整个过程
            }
        }
        
        return uploadedIcons;
    }
    
    /**
     * 上传ZIP压缩包并解压图标
     */
    public List<IconInfo> uploadIconsFromZip(MultipartFile zipFile) throws IOException {
        // 验证ZIP文件
        if (zipFile.isEmpty() || !zipFile.getOriginalFilename().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("请上传有效的ZIP文件");
        }
        
        List<IconInfo> uploadedIcons = new ArrayList<>();
        Path customDir = Paths.get(uploadPath, "icon", "custom");
        Files.createDirectories(customDir);
        
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // 跳过目录
                if (entry.isDirectory()) {
                    continue;
                }
                
                String entryName = entry.getName();
                // 只处理图标文件
                if (isIconFileName(entryName)) {
                    try {
                        // 提取文件名（去掉路径）
                        String fileName = Paths.get(entryName).getFileName().toString();
                        String safeFileName = generateSafeFileName(fileName, customDir);
                        Path targetPath = customDir.resolve(safeFileName);
                        
                        // 保存文件
                        Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        
                        // 创建图标信息并加入缓存
                        IconInfo iconInfo = createIconInfo(targetPath);
                        iconCache.put(iconInfo.getName(), iconInfo);
                        uploadedIcons.add(iconInfo);
                        
                        log.debug("从ZIP中提取图标: {}", safeFileName);
                    } catch (Exception e) {
                        log.error("处理ZIP中的文件失败: {}", entryName, e);
                    }
                }
                zis.closeEntry();
            }
        }
        
        log.info("成功从ZIP中导入 {} 个图标", uploadedIcons.size());
        return uploadedIcons;
    }
    
    /**
     * 验证图标文件
     */
    private void validateIconFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || !isIconFileName(fileName)) {
            throw new IllegalArgumentException("不支持的文件格式，请上传 PNG、JPG、SVG、ICO 格式的图片");
        }
        
        // 限制文件大小 (2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小不能超过2MB");
        }
    }
    
    /**
     * 判断是否为图标文件名
     */
    private boolean isIconFileName(String fileName) {
        if (fileName == null) return false;
        String lowerName = fileName.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
    }
    
    /**
     * 生成安全的文件名（移除非法字符，重复文件直接覆盖）
     */
    private String generateSafeFileName(String originalFileName, Path targetDir) {
        if (originalFileName == null) {
            originalFileName = "icon.png";
        }
        
        // 移除非法字符，保留中文字符
        String safeName = originalFileName.replaceAll("[<>:\"/\\\\|?*]", "_");
        
        // 直接返回文件名，重复文件会被覆盖
        return safeName;
    }
} 