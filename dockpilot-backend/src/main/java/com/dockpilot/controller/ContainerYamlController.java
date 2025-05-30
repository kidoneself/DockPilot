package com.dockpilot.controller;

import com.dockpilot.api.DockerService;
import com.dockpilot.model.ContainerYamlRequest;
import com.dockpilot.model.ContainerYamlResponse;
import com.dockpilot.utils.ApiResponse;
import com.dockpilot.utils.ComposeGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;

/**
 * 容器YAML生成控制器
 */
@RestController
@RequestMapping("/api/containers")
@Tag(name = "容器YAML生成", description = "容器YAML配置生成接口")
@Validated
public class ContainerYamlController {
    
    @Autowired
    private ComposeGenerator composeGenerator;
    
    @Autowired
    private DockerService dockerService;
    
    /**
     * 根据容器ID列表生成YAML配置
     */
    @PostMapping("/generate-yaml")
    @Operation(summary = "生成YAML配置", description = "根据选中的容器ID列表生成Docker Compose YAML配置")
    public ApiResponse<ContainerYamlResponse> generateYaml(@Valid @RequestBody ContainerYamlRequest request) {
        try {
            // 生成YAML内容
            String yamlContent = composeGenerator.generateFromContainerIds(request.getContainerIds());
            
            // 如果用户提供了环境变量描述配置，更新YAML内容
            if (request.getEnvDescriptions() != null && !request.getEnvDescriptions().isEmpty()) {
                yamlContent = updateEnvDescriptions(yamlContent, request.getEnvDescriptions());
            }
            
            // 确定项目名称
            String projectName = request.getProjectName();
            if (projectName == null || projectName.trim().isEmpty()) {
                projectName = "容器项目-" + System.currentTimeMillis();
            }
            
            // 构建响应
            ContainerYamlResponse response = ContainerYamlResponse.success(
                yamlContent, 
                request.getContainerIds().size(), 
                projectName
            );
            
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            ContainerYamlResponse response = ContainerYamlResponse.error("生成YAML配置失败: " + e.getMessage());
            return ApiResponse.success(response);  // 返回业务错误，仍然是成功的HTTP响应
        }
    }
    
    /**
     * 更新YAML内容中的环境变量描述
     */
    private String updateEnvDescriptions(String yamlContent, java.util.Map<String, String> envDescriptions) {
        StringBuilder result = new StringBuilder();
        String[] lines = yamlContent.split("\n");
        boolean inEnvSection = false;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 检测env段落
            if (trimmedLine.equals("env:")) {
                inEnvSection = true;
                result.append(line).append("\n");
                continue;
            }
            
            // 检测env段落结束
            if (inEnvSection && !line.startsWith("    ") && !line.trim().isEmpty()) {
                inEnvSection = false;
            }
            
            // 如果在env段落中，且是环境变量定义行
            if (inEnvSection && trimmedLine.contains(":")) {
                String envName = trimmedLine.split(":")[0].trim();
                String userDescription = envDescriptions.get(envName);
                
                if (userDescription != null && !userDescription.trim().isEmpty()) {
                    // 如果是对象格式，更新description字段
                    if (line.contains("value:")) {
                        result.append(line).append("\n");
                        // 查找下一行是否是description行
                        boolean hasDescriptionLine = false;
                        for (int i = 0; i < lines.length; i++) {
                            if (lines[i].equals(line) && i + 1 < lines.length) {
                                String nextLine = lines[i + 1];
                                if (nextLine.trim().startsWith("description:")) {
                                    hasDescriptionLine = true;
                                    break;
                                }
                            }
                        }
                        if (!hasDescriptionLine) {
                            // 添加description行
                            String indent = line.substring(0, line.indexOf(line.trim()));
                            result.append(indent).append("description: \"").append(userDescription).append("\"").append("\n");
                        }
                    } else if (line.contains("description:")) {
                        // 更新现有的description行
                        String indent = line.substring(0, line.indexOf(line.trim()));
                        result.append(indent).append("description: \"").append(userDescription).append("\"").append("\n");
                    } else {
                        result.append(line).append("\n");
                    }
                } else {
                    result.append(line).append("\n");
                }
            } else {
                result.append(line).append("\n");
            }
        }
        
        return result.toString();
    }
    
    /**
     * 预览容器YAML配置（不包含敏感信息）
     */
    @PostMapping("/preview-yaml")
    @Operation(summary = "预览YAML配置", description = "预览容器的YAML配置，不包含敏感信息")
    public ApiResponse<ContainerYamlResponse> previewYaml(@Valid @RequestBody ContainerYamlRequest request) {
        try {
            // 设置排除字段（排除敏感信息）
            HashSet<String> excludeFields = new HashSet<>();
            if (request.getExcludeFields() != null) {
                excludeFields.addAll(request.getExcludeFields());
            }
            // 默认排除一些敏感字段
            excludeFields.add("environment");
            
            // 获取容器详细信息
            String yamlContent = composeGenerator.generateComposeContent(
                dockerService.listContainers().stream()
                    .filter(container -> request.getContainerIds().contains(container.getId()))
                    .map(container -> dockerService.inspectContainerCmd(container.getId()))
                    .collect(java.util.stream.Collectors.toList()),
                excludeFields
            );
            
            // 确定项目名称
            String projectName = request.getProjectName();
            if (projectName == null || projectName.trim().isEmpty()) {
                projectName = "预览项目";
            }
            
            // 构建响应
            ContainerYamlResponse response = ContainerYamlResponse.success(
                yamlContent, 
                request.getContainerIds().size(), 
                projectName
            );
            response.setMessage("YAML预览生成成功（已排除敏感信息）");
            
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            ContainerYamlResponse response = ContainerYamlResponse.error("预览YAML配置失败: " + e.getMessage());
            return ApiResponse.success(response);  // 返回业务错误，仍然是成功的HTTP响应
        }
    }
} 