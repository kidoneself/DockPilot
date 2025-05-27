package com.dockpilot.model.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 热更新信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInfoDTO {
    
    /**
     * 当前版本
     */
    private String currentVersion;
    
    /**
     * 最新版本
     */
    private String latestVersion;
    
    /**
     * 是否有新版本可用
     */
    private boolean hasUpdate;
    
    /**
     * 更新描述
     */
    private String releaseNotes;
    
    /**
     * 发布时间
     */
    private LocalDateTime releaseTime;
    
    /**
     * 下载URL
     */
    private String downloadUrl;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 是否为强制更新
     */
    private boolean forceUpdate;
    
    /**
     * 最后检查时间
     */
    private LocalDateTime lastCheckTime;
    
    /**
     * 更新状态
     */
    private String status; // available, downloading, applying, completed, failed
    
    /**
     * 更新进度 (0-100)
     */
    private Integer progress;
    
    /**
     * 错误信息
     */
    private String errorMessage;
} 