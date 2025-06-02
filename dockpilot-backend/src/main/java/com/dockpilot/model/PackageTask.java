package com.dockpilot.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 打包任务信息
 */
@Data
public class PackageTask {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 任务状态：pending, processing, completed, failed
     */
    private String status;
    
    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;
    
    /**
     * 当前处理步骤描述
     */
    private String currentStep;
    
    /**
     * 项目名称
     */
    private String projectName;
    
    /**
     * 容器ID列表
     */
    private java.util.List<String> containerIds;
    
    /**
     * 选择的路径列表
     */
    private java.util.List<String> selectedPaths;
    
    /**
     * 生成的文件路径（完成后）
     */
    private String filePath;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 错误信息（失败时）
     */
    private String errorMessage;
    
    /**
     * 任务创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 任务完成时间
     */
    private LocalDateTime completeTime;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
} 