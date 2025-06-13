package com.dockpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IconInfo {
    private String name;        // 图标名称：Docker_A
    private String displayName; // 显示名称：Docker
    private String type;        // 类型：border-radius/circle/svg
    private String url;         // 访问URL
    private Long fileSize;      // 文件大小
    private String extension;   // 文件扩展名
} 