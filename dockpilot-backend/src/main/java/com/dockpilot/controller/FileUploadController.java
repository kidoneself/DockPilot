package com.dockpilot.controller;

import com.dockpilot.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/upload")
@Tag(name = "文件上传", description = "文件上传和访问接口")
@SecurityRequirement(name = "JWT")
public class FileUploadController {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Operation(summary = "上传图片", description = "上传图片文件，支持jpg、png、gif、webp、svg格式")
    @PostMapping("/image")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件类型
            if (!isImageFile(file)) {
                return ApiResponse.error("只支持图片文件格式 (jpg, jpeg, png, gif, webp, svg)");
            }

            // 验证文件大小
            if (file.getSize() > 10 * 1024 * 1024) { // 10MB
                return ApiResponse.error("文件大小不能超过10MB");
            }

            // 确保上传目录存在
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String newFilename = "img_" + timestamp + "_" + uniqueId + fileExtension;

            // 保存文件
            Path filePath = uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 返回文件信息（只返回文件名，让前端处理URL）
            Map<String, String> result = new HashMap<>();
            result.put("filename", newFilename);
            result.put("originalName", originalFilename);
            result.put("size", String.valueOf(file.getSize()));

            log.info("文件上传成功: {} -> {}", originalFilename, newFilename);
            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ApiResponse.error("文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取图片", description = "根据文件名获取图片文件（仅开发环境使用）")
    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // 根据文件扩展名设置Content-Type
                String contentType = getContentType(filename);
                
                // 🔥 修复可能的中文文件名编码问题
                String encodedFilename = encodeFilename(filename);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "inline; filename=\"" + sanitizeFilename(filename) + "\"; " +
                                "filename*=UTF-8''" + encodedFilename)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("获取图片文件失败: {}", filename, e);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "删除文件", description = "删除指定的上传文件")
    @DeleteMapping("/file/{filename}")
    public ApiResponse<Void> deleteFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(filename);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功: {}", filename);
                return ApiResponse.success(null);
            } else {
                return ApiResponse.error("文件不存在");
            }
        } catch (IOException e) {
            log.error("文件删除失败: {}", filename, e);
            return ApiResponse.error("文件删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取所有上传的图片", description = "扫描上传目录中的所有图片文件")
    @GetMapping("/images")
    public ApiResponse<List<Map<String, String>>> getAllImages() {
        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                return ApiResponse.success(new ArrayList<>());
            }

            List<Map<String, String>> images = new ArrayList<>();
            
            try (var stream = Files.list(uploadDir)) {
                stream.filter(Files::isRegularFile)
                      .filter(this::isImageFile)
                      .sorted((a, b) -> {
                          try {
                              // 按修改时间倒序排列（最新的在前面）
                              return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                          } catch (IOException e) {
                              return 0;
                          }
                      })
                      .forEach(path -> {
                          String filename = path.getFileName().toString();
                          Map<String, String> imageInfo = new HashMap<>();
                          imageInfo.put("filename", filename);
                          imageInfo.put("name", getImageDisplayName(filename));
                          
                          try {
                              long size = Files.size(path);
                              imageInfo.put("size", String.valueOf(size));
                              imageInfo.put("lastModified", Files.getLastModifiedTime(path).toString());
                          } catch (IOException e) {
                              log.warn("获取文件信息失败: {}", filename, e);
                          }
                          
                          images.add(imageInfo);
                      });
            }

            log.info("扫描到 {} 个图片文件", images.size());
            return ApiResponse.success(images);
        } catch (IOException e) {
            log.error("扫描图片文件失败", e);
            return ApiResponse.error("扫描图片文件失败: " + e.getMessage());
        }
    }

    @Operation(summary = "从URL下载图片", description = "从指定URL下载图片并保存到本地")
    @PostMapping("/image/download")
    public ApiResponse<Map<String, String>> downloadImageFromUrl(@RequestBody Map<String, String> request) {
        try {
            String imageUrl = request.get("url");
            String customName = request.get("name"); // 可选的自定义名称
            
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return ApiResponse.error("图片URL不能为空");
            }

            // 验证URL格式
            URL url;
            try {
                url = new URL(imageUrl);
            } catch (Exception e) {
                return ApiResponse.error("无效的URL格式");
            }

            // 确保上传目录存在
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 建立连接并下载
            URLConnection connection = url.openConnection();
            
            // 设置User-Agent避免被某些网站拒绝
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            // 设置超时时间
            connection.setConnectTimeout(10000); // 10秒连接超时
            connection.setReadTimeout(30000);    // 30秒读取超时

            // 获取文件扩展名
            String contentType = connection.getContentType();
            String fileExtension = getExtensionFromContentType(contentType);
            
            // 如果无法从Content-Type获取扩展名，尝试从URL获取
            if (fileExtension.isEmpty()) {
                fileExtension = getExtensionFromUrl(imageUrl);
            }
            
            // 如果还是没有扩展名，默认为jpg
            if (fileExtension.isEmpty()) {
                fileExtension = ".jpg";
            }

            // 验证是否为支持的图片格式
            if (!isSupportedImageExtension(fileExtension)) {
                return ApiResponse.error("不支持的图片格式，只支持 jpg, jpeg, png, gif, webp, svg");
            }

            // 生成文件名
            String filename;
            if (customName != null && !customName.trim().isEmpty()) {
                // 使用自定义名称
                filename = sanitizeFilename(customName.trim()) + fileExtension;
            } else {
                // 生成唯一文件名
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String uniqueId = UUID.randomUUID().toString().substring(0, 8);
                filename = "download_" + timestamp + "_" + uniqueId + fileExtension;
            }

            // 检查文件是否已存在，如果存在则添加后缀
            Path filePath = uploadDir.resolve(filename);
            int counter = 1;
            while (Files.exists(filePath)) {
                String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
                filename = nameWithoutExt + "_" + counter + fileExtension;
                filePath = uploadDir.resolve(filename);
                counter++;
            }

            // 下载文件
            try (InputStream inputStream = connection.getInputStream()) {
                long fileSize = Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                
                // 验证文件大小
                if (fileSize > 10 * 1024 * 1024) { // 10MB
                    Files.deleteIfExists(filePath);
                    return ApiResponse.error("下载的文件大小超过10MB限制");
                }

                // 返回结果
                Map<String, String> result = new HashMap<>();
                result.put("filename", filename);
                result.put("originalName", customName != null ? customName : extractFilenameFromUrl(imageUrl));
                result.put("size", String.valueOf(fileSize));
                result.put("url", imageUrl);

                log.info("图片下载成功: {} -> {}", imageUrl, filename);
                return ApiResponse.success(result);
            }

        } catch (IOException e) {
            log.error("下载图片失败", e);
            return ApiResponse.error("下载图片失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理下载请求时发生错误", e);
            return ApiResponse.error("处理请求时发生错误: " + e.getMessage());
        }
    }

    /**
     * 从Content-Type获取文件扩展名
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) return "";
        
        switch (contentType.toLowerCase()) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/webp":
                return ".webp";
            case "image/svg+xml":
                return ".svg";
            default:
                return "";
        }
    }

    /**
     * 从URL获取文件扩展名
     */
    private String getExtensionFromUrl(String url) {
        try {
            // 移除查询参数
            String cleanUrl = url.split("\\?")[0];
            int lastDot = cleanUrl.lastIndexOf('.');
            if (lastDot > 0 && lastDot < cleanUrl.length() - 1) {
                String ext = cleanUrl.substring(lastDot).toLowerCase();
                if (isSupportedImageExtension(ext)) {
                    return ext;
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return "";
    }

    /**
     * 检查扩展名是否为支持的图片格式
     */
    private boolean isSupportedImageExtension(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || 
               ext.equals(".gif") || ext.equals(".webp") || ext.equals(".svg");
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
     * 清理文件名，移除不安全字符
     */
    private String sanitizeFilename(String filename) {
        // 🔥 改进：移除或替换不安全的字符，包括中文字符的处理
        if (filename == null) {
            return "file";
        }
        
        // 替换中文和特殊字符为安全字符
        String sanitized = filename
                .replaceAll("[^a-zA-Z0-9._-]", "_")  // 替换非安全字符为下划线
                .replaceAll("_{2,}", "_");          // 多个连续下划线合并为一个
        
        // 确保文件名不为空且不以点或下划线开头
        if (sanitized.isEmpty() || sanitized.startsWith(".") || sanitized.startsWith("_")) {
            sanitized = "file_" + sanitized;
        }
        
        return sanitized;
    }

    /**
     * 从URL中提取文件名
     */
    private String extractFilenameFromUrl(String url) {
        try {
            String cleanUrl = url.split("\\?")[0];
            int lastSlash = cleanUrl.lastIndexOf('/');
            if (lastSlash > 0 && lastSlash < cleanUrl.length() - 1) {
                return cleanUrl.substring(lastSlash + 1);
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return "下载的图片";
    }

    /**
     * 检查文件是否为图片（基于文件路径）
     */
    private boolean isImageFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".jpg") || 
               filename.endsWith(".jpeg") || 
               filename.endsWith(".png") || 
               filename.endsWith(".gif") || 
               filename.endsWith(".webp") ||
               filename.endsWith(".svg");
    }

    /**
     * 从文件名生成显示名称
     */
    private String getImageDisplayName(String filename) {
        // 移除扩展名
        String nameWithoutExt = filename.replaceAll("\\.[^.]+$", "");
        
        // 如果是生成的文件名格式（img_20250101_123456_abc123），提取有意义的部分
        if (nameWithoutExt.startsWith("img_") && nameWithoutExt.length() > 20) {
            // 提取日期时间部分
            String[] parts = nameWithoutExt.split("_");
            if (parts.length >= 3) {
                String date = parts[1]; // 20250101
                String time = parts[2]; // 123456
                
                // 格式化为可读的名称
                if (date.length() == 8 && time.length() == 6) {
                    String formattedDate = date.substring(0, 4) + "-" + 
                                         date.substring(4, 6) + "-" + 
                                         date.substring(6, 8);
                    String formattedTime = time.substring(0, 2) + ":" + 
                                         time.substring(2, 4) + ":" + 
                                         time.substring(4, 6);
                    return "图片 " + formattedDate + " " + formattedTime;
                }
            }
        }
        
        // 默认返回文件名（去除扩展名）
        return nameWithoutExt;
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp") ||
                contentType.equals("image/svg+xml")
        );
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String getContentType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        switch (extension) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".webp":
                return "image/webp";
            case ".svg":
                return "image/svg+xml";
            default:
                return "application/octet-stream";
        }
    }
} 