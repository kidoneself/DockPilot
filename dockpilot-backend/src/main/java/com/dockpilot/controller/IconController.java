package com.dockpilot.controller;

import com.dockpilot.dto.IconInfo;
import com.dockpilot.service.IconService;
import com.dockpilot.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/icons")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IconController {

    private final IconService iconService;

    /**
     * 获取所有可用图标列表
     */
    @GetMapping("/list")
    public ApiResponse<List<IconInfo>> getIconList(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type) {
        try {
            List<IconInfo> icons = iconService.getIconList(search, type);
            return ApiResponse.success(icons);
        } catch (Exception e) {
            log.error("获取图标列表失败", e);
            return ApiResponse.error("获取图标列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取图标文件
     */
    @GetMapping("/{iconName}")
    public ResponseEntity<Resource> getIcon(@PathVariable String iconName) {
        try {
            Resource iconResource = iconService.getIconResource(iconName);
            if (iconResource != null && iconResource.exists()) {
                // 确定内容类型
                MediaType mediaType = iconService.getMediaType(iconName);
                
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .body(iconResource);
            } else {
                // 返回默认图标
                Resource defaultIcon = iconService.getDefaultIcon();
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(defaultIcon);
            }
        } catch (Exception e) {
            log.error("获取图标文件失败: {}", iconName, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 上传单个图标文件
     */
    @PostMapping("/upload")
    public ApiResponse<IconInfo> uploadIcon(@RequestParam("file") MultipartFile file) {
        try {
            IconInfo iconInfo = iconService.uploadIcon(file);
            return ApiResponse.success(iconInfo, "图标上传成功");
        } catch (Exception e) {
            log.error("上传图标失败", e);
            return ApiResponse.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传多个图标文件
     */
    @PostMapping("/upload-multiple")
    public ApiResponse<List<IconInfo>> uploadIcons(@RequestParam("files") MultipartFile[] files) {
        try {
            List<IconInfo> iconInfos = iconService.uploadIcons(files);
            return ApiResponse.success(iconInfos, "成功上传 " + iconInfos.size() + " 个图标");
        } catch (Exception e) {
            log.error("批量上传图标失败", e);
            return ApiResponse.error("批量上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传ZIP压缩包图标
     */
    @PostMapping("/upload-zip")
    public ApiResponse<List<IconInfo>> uploadIconsFromZip(@RequestParam("file") MultipartFile zipFile) {
        try {
            List<IconInfo> iconInfos = iconService.uploadIconsFromZip(zipFile);
            return ApiResponse.success(iconInfos, "成功从压缩包中导入 " + iconInfos.size() + " 个图标");
        } catch (Exception e) {
            log.error("从ZIP上传图标失败", e);
            return ApiResponse.error("ZIP上传失败：" + e.getMessage());
        }
    }

    /**
     * 刷新图标缓存
     */
    @PostMapping("/refresh")
    public ApiResponse<String> refreshIconCache() {
        try {
            int count = iconService.refreshIconCache();
            return ApiResponse.success("刷新成功，共扫描到 " + count + " 个图标");
        } catch (Exception e) {
            log.error("刷新图标缓存失败", e);
            return ApiResponse.error("刷新失败: " + e.getMessage());
        }
    }
} 