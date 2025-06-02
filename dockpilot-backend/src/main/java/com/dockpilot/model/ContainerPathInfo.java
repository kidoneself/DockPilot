package com.dockpilot.model;

import lombok.Data;
import java.util.List;

/**
 * 容器路径信息
 */
@Data
public class ContainerPathInfo {
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 容器ID
     */
    private String containerId;
    
    /**
     * 镜像名称
     */
    private String image;
    
    /**
     * 路径映射列表
     */
    private List<PathMapping> pathMappings;
    
    /**
     * 路径映射信息
     */
    @Data
    public static class PathMapping {
        
        /**
         * 唯一标识：hostPath:containerPath
         */
        private String id;
        
        /**
         * 宿主机路径
         */
        private String hostPath;
        
        /**
         * 容器内路径
         */
        private String containerPath;
        
        /**
         * 挂载类型：bind, volume, tmpfs
         */
        private String mountType;
        
        /**
         * 是否只读
         */
        private boolean readOnly;
        
        /**
         * 路径描述
         */
        private String description;
        
        /**
         * 是否为系统路径
         */
        private boolean isSystemPath;
        
        /**
         * 是否推荐打包
         */
        private boolean recommended;
    }
} 