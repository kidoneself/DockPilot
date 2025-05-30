package com.dockpilot.service;

import com.dockpilot.model.application.ApplicationParseResult;
import com.dockpilot.model.application.dto.ApplicationSaveRequest;
import com.dockpilot.model.application.dto.ApplicationInstallInfo;
import com.dockpilot.model.application.dto.ApplicationDeployRequest;
import com.dockpilot.model.application.dto.ApplicationDeployResult;
import com.dockpilot.model.application.dto.ImageStatusInfo;
import com.dockpilot.model.application.vo.ApplicationVO;

import java.util.List;

/**
 * 应用服务接口
 */
public interface ApplicationService {
    
    /**
     * 获取应用列表
     * @param category 分类筛选（可选）
     * @param keyword 关键词搜索（可选）
     * @return 应用列表
     */
    List<ApplicationVO> getApplications(String category, String keyword);
    
    /**
     * 根据ID获取应用详情
     * @param id 应用ID
     * @return 应用详情
     */
    ApplicationVO getApplicationById(Long id);
    
    /**
     * 保存应用
     * @param request 保存请求
     * @return 保存后的应用信息
     */
    ApplicationVO saveApplication(ApplicationSaveRequest request);
    
    /**
     * 删除应用
     * @param id 应用ID
     * @return 删除结果
     */
    boolean deleteApplication(Long id);
    
    /**
     * 分享应用（获取YAML内容）
     * @param id 应用ID
     * @return YAML配置内容
     */
    String shareApplication(Long id);
    
    /**
     * 获取所有分类
     * @return 分类列表
     */
    List<String> getCategories();
    
    /**
     * 解析应用配置
     * @param yamlContent YAML配置内容
     * @return 解析结果
     */
    ApplicationParseResult parseApplication(String yamlContent);
    
    /**
     * 获取应用安装信息
     * @param id 应用ID
     * @return 安装信息
     */
    ApplicationInstallInfo getInstallInfo(Long id);
    
    /**
     * 部署应用
     * @param id 应用ID
     * @param request 部署请求
     * @return 部署结果
     */
    ApplicationDeployResult deployApplication(Long id, ApplicationDeployRequest request);
    
    /**
     * 批量检查镜像状态
     * @param imageNames 镜像名称列表
     * @return 镜像状态信息列表
     */
    List<ImageStatusInfo> checkImages(List<String> imageNames);
    
    /**
     * 拉取镜像
     * @param imageName 镜像名称
     * @return 操作结果
     */
    String pullImage(String imageName);
} 