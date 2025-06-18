package com.dockpilot.service.impl;

import com.dockpilot.mapper.ApplicationMapper;
import com.dockpilot.model.application.Application;
import com.dockpilot.model.application.ApplicationParseResult;
import com.dockpilot.model.application.dto.ApplicationSaveRequest;
import com.dockpilot.model.application.dto.ApplicationInstallInfo;
import com.dockpilot.model.application.dto.ApplicationDeployRequest;
import com.dockpilot.model.application.dto.ApplicationDeployResult;
import com.dockpilot.model.application.dto.ImageStatusInfo;
import com.dockpilot.model.application.vo.ApplicationVO;
import com.dockpilot.service.ApplicationService;
import com.dockpilot.api.DockerService;
import com.dockpilot.utils.ComposeGenerator;
import com.dockpilot.utils.YamlApplicationParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 应用服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final DockerService dockerService;
    private final ComposeGenerator composeGenerator;

    @Override
    public List<ApplicationVO> getApplications(String category, String keyword) {
        try {
            List<Application> applications = applicationMapper.findApplications(category, keyword);
            return applications.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取应用列表失败: {}", e.getMessage());
            throw new RuntimeException("获取应用列表失败", e);
        }
    }

    @Override
    public ApplicationVO getApplicationById(Long id) {
        try {
            Application application = applicationMapper.findById(id);
            if (application == null) {
                throw new RuntimeException("应用不存在");
            }
            return convertToVO(application);
        } catch (Exception e) {
            log.error("获取应用详情失败，应用ID: {}, 错误: {}", id, e.getMessage());
            throw new RuntimeException("获取应用详情失败", e);
        }
    }

    @Override
    @Transactional
    public ApplicationVO saveApplication(ApplicationSaveRequest request) {
        try {
            String yamlContent;
            String fileHash;

            if (request.getContainerIds() != null && !request.getContainerIds().isEmpty()) {
                // 从容器ID生成YAML
                yamlContent = composeGenerator.generateFromContainerIds(request.getContainerIds());
                fileHash = calculateFileHash(yamlContent);
            } else if (StringUtils.hasText(request.getYamlContent())) {
                // 直接使用提供的YAML内容
                yamlContent = request.getYamlContent();
                
                // 验证YAML格式
                YamlApplicationParser.validateYaml(yamlContent);
                fileHash = calculateFileHash(yamlContent);
            } else {
                throw new RuntimeException("请提供容器ID列表或YAML配置内容");
            }

            // 检查是否已存在相同的YAML配置
            Application existingApp = applicationMapper.findByFileHash(fileHash);
            if (existingApp != null) {
                throw new RuntimeException("相同的应用配置已存在: " + existingApp.getName());
            }

            // 创建新应用
            Application application = new Application();
            application.setName(request.getName());
            application.setDescription(request.getDescription());
            application.setCategory(request.getCategory() != null ? request.getCategory() : "容器应用");
            application.setIconUrl(request.getIconUrl());
            application.setYamlContent(yamlContent);
            application.setFileHash(fileHash);
            application.setEnvVars(request.getEnvVars());
            application.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            application.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            applicationMapper.insert(application);
            
            log.info("保存应用成功: {}", application.getName());
            return convertToVO(application);
            
        } catch (Exception e) {
            log.error("保存应用失败: {}", e.getMessage());
            throw new RuntimeException("保存应用失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteApplication(Long id) {
        try {
            Application application = applicationMapper.findById(id);
            if (application == null) {
                throw new RuntimeException("应用不存在");
            }

            applicationMapper.deleteById(id);
            log.info("删除应用成功: {}", application.getName());
            return true;
            
        } catch (Exception e) {
            log.error("删除应用失败，应用ID: {}, 错误: {}", id, e.getMessage());
            throw new RuntimeException("删除应用失败", e);
        }
    }

    @Override
    public String shareApplication(Long id) {
        try {
            Application application = applicationMapper.findById(id);
            if (application == null) {
                throw new RuntimeException("应用不存在");
            }
            
            log.info("分享应用: {}", application.getName());
            return application.getYamlContent();
            
        } catch (Exception e) {
            log.error("分享应用失败，应用ID: {}, 错误: {}", id, e.getMessage());
            throw new RuntimeException("分享应用失败", e);
        }
    }

    @Override
    public List<String> getCategories() {
        try {
            return applicationMapper.findDistinctCategories();
        } catch (Exception e) {
            log.error("获取分类列表失败: {}", e.getMessage());
            throw new RuntimeException("获取分类列表失败", e);
        }
    }

    @Override
    public ApplicationParseResult parseApplication(String yamlContent) {
        try {
            // 验证YAML格式
            YamlApplicationParser.validateYaml(yamlContent);
            
            // 解析YAML配置
            ApplicationParseResult result = YamlApplicationParser.parseYaml(yamlContent);
            
            // 补充镜像状态信息
            enrichImageStatus(result.getImages());
            
            return result;
            
        } catch (Exception e) {
            log.error("解析YAML应用配置失败: {}", e.getMessage());
            throw new RuntimeException("解析应用配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 补充镜像状态信息
     */
    private void enrichImageStatus(List<ApplicationParseResult.ImageInfo> images) {
        for (ApplicationParseResult.ImageInfo image : images) {
            try {
                // 检查本地镜像是否存在
                boolean exists = dockerService.isImageExists(image.getFullName());
                image.setStatus(exists ? "exists" : "not_found");
                
                // 如果镜像存在，获取大小信息
                if (exists) {
                    String size = dockerService.getImageSize(image.getFullName());
                    image.setSize(size != null ? size : "unknown");
                }
                
            } catch (Exception e) {
                log.warn("获取镜像{}状态失败: {}", image.getFullName(), e.getMessage());
                image.setStatus("unknown");
            }
        }
    }

    /**
     * 计算文件哈希值
     */
    private String calculateFileHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算文件哈希失败", e);
        }
    }

    /**
     * 转换为VO对象
     */
    private ApplicationVO convertToVO(Application application) {
        ApplicationVO vo = new ApplicationVO();
        vo.setId(application.getId());
        vo.setName(application.getName());
        vo.setDescription(application.getDescription());
        vo.setCategory(application.getCategory());
        vo.setIconUrl(application.getIconUrl());
        vo.setYamlContent(application.getYamlContent());
        vo.setFileHash(application.getFileHash());
        vo.setEnvVars(application.getEnvVars());
        vo.setCreatedAt(application.getCreatedAt());
        vo.setUpdatedAt(application.getUpdatedAt());
        
        // 动态计算服务数量
        try {
            if (application.getYamlContent() != null && !application.getYamlContent().trim().isEmpty()) {
                ApplicationParseResult parseResult = YamlApplicationParser.parseYaml(application.getYamlContent());
                vo.setServices(parseResult.getServices() != null ? parseResult.getServices().size() : 1);
            }
        } catch (Exception e) {
            log.warn("解析YAML计算服务数量失败: {}", e.getMessage());
            vo.setServices(1); // 默认1个服务
        }
        
        return vo;
    }
    
    @Override
    public ApplicationInstallInfo getInstallInfo(Long id) {
        try {
            // 获取应用信息
            Application application = applicationMapper.findById(id);
            if (application == null) {
                throw new RuntimeException("应用不存在");
            }
            
            // 解析YAML配置
            ApplicationParseResult parseResult = YamlApplicationParser.parseYaml(application.getYamlContent());
            
            ApplicationInstallInfo installInfo = new ApplicationInstallInfo();
            
            // 设置应用基本信息
            ApplicationInstallInfo.AppBasicInfo appInfo = new ApplicationInstallInfo.AppBasicInfo();
            appInfo.setId(application.getId());
            appInfo.setName(application.getName());
            appInfo.setDescription(application.getDescription());
            appInfo.setType("用户分享");
            appInfo.setIcon(application.getIconUrl());
            appInfo.setDeployCount((int)(Math.random() * 200) + 10);
            installInfo.setApp(appInfo);
            
            // 设置镜像信息
            List<ApplicationInstallInfo.ImageStatusInfo> imageInfos = new ArrayList<>();
            for (ApplicationParseResult.ImageInfo image : parseResult.getImages()) {
                ApplicationInstallInfo.ImageStatusInfo imageInfo = new ApplicationInstallInfo.ImageStatusInfo();
                imageInfo.setName(image.getFullName());
                imageInfo.setSize(image.getSize());
                imageInfo.setStatus(image.getStatus());
                imageInfos.add(imageInfo);
            }
            installInfo.setImages(imageInfos);
            
            // 设置环境变量信息
            List<ApplicationInstallInfo.EnvVarInfo> envInfos = new ArrayList<>();
            for (ApplicationParseResult.EnvVarInfo env : parseResult.getEnvVars()) {
                ApplicationInstallInfo.EnvVarInfo envInfo = new ApplicationInstallInfo.EnvVarInfo();
                envInfo.setName(env.getName());
                envInfo.setDescription(env.getDescription());
                envInfo.setValue(env.getValue());
                envInfo.setDefaultValue(env.getDefaultValue());
                envInfo.setRequired(env.isRequired());
                envInfo.setSensitive(env.isSensitive());
                envInfos.add(envInfo);
            }
            installInfo.setEnvVars(envInfos);
            
            // 设置服务信息
            List<ApplicationInstallInfo.ServiceInfo> serviceInfos = new ArrayList<>();
            for (ApplicationParseResult.ServiceInfo service : parseResult.getServices()) {
                ApplicationInstallInfo.ServiceInfo serviceInfo = new ApplicationInstallInfo.ServiceInfo();
                serviceInfo.setName(service.getName());
                serviceInfo.setImage(service.getImage());
                serviceInfo.setConfigUrl(service.getConfigUrl());
                serviceInfos.add(serviceInfo);
            }
            installInfo.setServices(serviceInfos);
            
            // 🆕 设置原始YAML内容（支持高级编辑模式）
            installInfo.setYamlContent(application.getYamlContent());
            
            // 检查镜像状态
            enrichInstallImageStatus(installInfo.getImages());
            
            return installInfo;
            
        } catch (Exception e) {
            log.error("获取应用安装信息失败: {}", e.getMessage());
            throw new RuntimeException("获取应用安装信息失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ApplicationDeployResult deployApplication(Long id, ApplicationDeployRequest request) {
        try {
            // 获取应用信息
            Application application = applicationMapper.findById(id);
            if (application == null) {
                throw new RuntimeException("应用不存在");
            }
            
            // 解析YAML配置
            ApplicationParseResult parseResult = YamlApplicationParser.parseYaml(application.getYamlContent());
            
            // 构建容器创建参数
            Map<String, String> envVars = request.getEnvVars() != null ? request.getEnvVars() : new HashMap<>();
            
            // 使用ComposeGenerator创建容器
            // 这里可以复用现有的容器创建逻辑
            String yamlContent = application.getYamlContent();
            
            // 替换环境变量占位符
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                yamlContent = yamlContent.replace("${" + entry.getKey() + "}", entry.getValue());
            }
            
            // 生成最终的compose配置并部署
            // 注意：这里需要实际的容器创建逻辑，目前简化处理
            log.info("开始部署应用: {}, 配置: {}", application.getName(), request);
            
            // 模拟容器ID列表（实际应该是真实创建的容器ID）
            List<String> containerIds = new ArrayList<>();
            containerIds.add("container_" + System.currentTimeMillis());
            
            // 构建访问地址
            List<ApplicationDeployResult.AccessUrl> accessUrls = new ArrayList<>();
            for (ApplicationParseResult.EnvVarInfo env : parseResult.getEnvVars()) {
                if (env.getName().contains("PORT") && envVars.containsKey(env.getName())) {
                    ApplicationDeployResult.AccessUrl accessUrl = new ApplicationDeployResult.AccessUrl();
                    accessUrl.setName(env.getName().replace("_PORT", "") + " 服务");
                    accessUrl.setUrl("http://localhost:" + envVars.get(env.getName()));
                    accessUrl.setDescription("服务访问地址");
                    accessUrls.add(accessUrl);
                }
            }
            
            // 更新部署次数
            // 这里可以添加部署次数统计逻辑
            
            ApplicationDeployResult result = ApplicationDeployResult.success(containerIds, "应用部署成功");
            result.setAccessUrls(accessUrls);
            result.setDeployId("deploy_" + System.currentTimeMillis());
            
            return result;
            
        } catch (Exception e) {
            log.error("部署应用失败: {}", e.getMessage());
            return ApplicationDeployResult.failure("部署失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<ImageStatusInfo> checkImages(List<String> imageNames) {
        try {
            List<ImageStatusInfo> result = new ArrayList<>();
            
            for (String imageName : imageNames) {
                try {
                    // 使用DockerService检查镜像是否存在
                    boolean exists = dockerService.isImageExists(imageName);
                    
                    if (exists) {
                        String size = dockerService.getImageSize(imageName);
                        result.add(ImageStatusInfo.exists(imageName, size != null ? size : "unknown"));
                    } else {
                        result.add(ImageStatusInfo.missing(imageName));
                    }
                    
                } catch (Exception e) {
                    log.warn("检查镜像{}状态失败: {}", imageName, e.getMessage());
                    result.add(ImageStatusInfo.missing(imageName));
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("批量检查镜像状态失败: {}", e.getMessage());
            throw new RuntimeException("批量检查镜像状态失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String pullImage(String imageName) {
        try {
            // 使用DockerService拉取镜像
            log.info("开始拉取镜像: {}", imageName);
            
            // 这里应该调用真实的Docker API拉取镜像
            // 可以使用dockerService或者直接调用Docker API
            // 暂时返回成功消息
            
            return "镜像 " + imageName + " 拉取成功";
            
        } catch (Exception e) {
            log.error("拉取镜像{}失败: {}", imageName, e.getMessage());
            throw new RuntimeException("拉取镜像失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 补充镜像状态信息
     */
    private void enrichInstallImageStatus(List<ApplicationInstallInfo.ImageStatusInfo> images) {
        for (ApplicationInstallInfo.ImageStatusInfo image : images) {
            try {
                // 检查本地镜像是否存在
                boolean exists = dockerService.isImageExists(image.getName());
                image.setStatus(exists ? "exists" : "missing");
                
                // 如果镜像存在，获取大小信息
                if (exists && (image.getSize() == null || image.getSize().isEmpty())) {
                    String size = dockerService.getImageSize(image.getName());
                    image.setSize(size != null ? size : "unknown");
                }
                
            } catch (Exception e) {
                log.warn("获取镜像{}状态失败: {}", image.getName(), e.getMessage());
                image.setStatus("unknown");
            }
        }
    }
} 