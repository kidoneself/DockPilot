package com.dockpilot.utils;

import com.dockpilot.model.application.ApplicationParseResult;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YAML应用配置解析工具
 */
public class YamlApplicationParser {
    
    /**
     * 解析YAML配置
     */
    public static ApplicationParseResult parseYaml(String yamlContent) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(yamlContent);
            
            ApplicationParseResult result = new ApplicationParseResult();
            
            // 解析x-meta信息
            result.setMeta(parseMetadata(config));
            
            // 解析services信息
            result.setServices(parseServices(config));
            
            // 解析镜像列表
            result.setImages(parseImages(config));
            
            // 解析环境变量
            result.setEnvVars(parseEnvVars(config));
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("解析YAML配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证YAML格式
     */
    public static void validateYaml(String yamlContent) {
        try {
            Yaml yaml = new Yaml();
            yaml.load(yamlContent);
        } catch (Exception e) {
            throw new RuntimeException("YAML格式错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析应用元数据
     */
    private static ApplicationParseResult.ApplicationMeta parseMetadata(Map<String, Object> config) {
        ApplicationParseResult.ApplicationMeta meta = new ApplicationParseResult.ApplicationMeta();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> xMeta = (Map<String, Object>) config.get("x-meta");
        if (xMeta != null) {
            meta.setName((String) xMeta.get("name"));
            meta.setDescription((String) xMeta.get("description"));
            meta.setVersion((String) xMeta.get("version"));
            meta.setAuthor((String) xMeta.get("author"));
            meta.setCategory((String) xMeta.get("category"));
        }
        
        return meta;
    }
    
    /**
     * 解析服务列表
     */
    @SuppressWarnings("unchecked")
    private static List<ApplicationParseResult.ServiceInfo> parseServices(Map<String, Object> config) {
        List<ApplicationParseResult.ServiceInfo> services = new ArrayList<>();
        
        Map<String, Object> servicesMap = (Map<String, Object>) config.get("services");
        if (servicesMap != null) {
            for (Map.Entry<String, Object> entry : servicesMap.entrySet()) {
                String serviceName = entry.getKey();
                Map<String, Object> serviceConfig = (Map<String, Object>) entry.getValue();
                
                ApplicationParseResult.ServiceInfo service = new ApplicationParseResult.ServiceInfo();
                service.setName(serviceName);
                service.setImage((String) serviceConfig.get("image"));
                
                // 解析x-meta
                Map<String, Object> serviceMeta = (Map<String, Object>) serviceConfig.get("x-meta");
                if (serviceMeta != null) {
                    service.setDescription((String) serviceMeta.get("description"));
                    service.setConfigUrl((String) serviceMeta.get("configUrl"));
                }
                
                // 解析端口
                service.setPorts(parseServicePorts(serviceConfig));
                
                // 解析卷挂载
                service.setVolumes(parseServiceVolumes(serviceConfig));
                
                services.add(service);
            }
        }
        
        return services;
    }
    
    /**
     * 解析镜像列表
     */
    @SuppressWarnings("unchecked")
    private static List<ApplicationParseResult.ImageInfo> parseImages(Map<String, Object> config) {
        Set<String> imageSet = new HashSet<>();
        List<ApplicationParseResult.ImageInfo> images = new ArrayList<>();
        
        Map<String, Object> servicesMap = (Map<String, Object>) config.get("services");
        if (servicesMap != null) {
            for (Map.Entry<String, Object> entry : servicesMap.entrySet()) {
                Map<String, Object> serviceConfig = (Map<String, Object>) entry.getValue();
                String imageName = (String) serviceConfig.get("image");
                
                if (imageName != null && !imageSet.contains(imageName)) {
                    imageSet.add(imageName);
                    
                    ApplicationParseResult.ImageInfo image = new ApplicationParseResult.ImageInfo();
                    
                    // 解析镜像名和标签
                    if (imageName.contains(":")) {
                        String[] parts = imageName.split(":");
                        image.setName(parts[0]);
                        image.setTag(parts[1]);
                    } else {
                        image.setName(imageName);
                        image.setTag("latest");
                    }
                    
                    image.setFullName(imageName);
                    image.setStatus("unknown"); // 需要后续检查
                    image.setSize(""); // 需要后续获取
                    
                    images.add(image);
                }
            }
        }
        
        return images;
    }
    
    /**
     * 解析环境变量
     */
    @SuppressWarnings("unchecked")
    private static List<ApplicationParseResult.EnvVarInfo> parseEnvVars(Map<String, Object> config) {
        List<ApplicationParseResult.EnvVarInfo> envVars = new ArrayList<>();
        
        Map<String, Object> xMeta = (Map<String, Object>) config.get("x-meta");
        if (xMeta != null) {
            Map<String, Object> env = (Map<String, Object>) xMeta.get("env");
            if (env != null) {
                for (Map.Entry<String, Object> entry : env.entrySet()) {
                    String key = entry.getKey();
                    Object valueObj = entry.getValue();
                    
                    ApplicationParseResult.EnvVarInfo envVar = new ApplicationParseResult.EnvVarInfo();
                    envVar.setName(key);
                    
                    String value = "";
                    String description = "";
                    
                    // 处理新的对象格式：{value: "...", description: "..."}
                    if (valueObj instanceof Map) {
                        Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                        value = String.valueOf(valueMap.get("value"));
                        Object descObj = valueMap.get("description");
                        if (descObj != null) {
                            description = String.valueOf(descObj);
                        }
                    } else {
                        // 处理旧的字符串格式
                        value = String.valueOf(valueObj);
                    }
                    
                    envVar.setDefaultValue(value);
                    envVar.setValue(value);
                    
                    // 如果有描述就使用描述，否则根据命名规则生成
                    if (description != null && !description.trim().isEmpty() && !"null".equals(description)) {
                        envVar.setDescription(description);
                    } else {
                        // 根据命名规则判断描述和是否必需
                        if (key.contains("PORT")) {
                            envVar.setDescription(extractPortDescription(key));
                            envVar.setRequired(true);
                        } else if (key.contains("PATH")) {
                            envVar.setDescription(extractPathDescription(key));
                            envVar.setRequired(true);
                        } else if (key.contains("PASSWORD")) {
                            envVar.setDescription("密码配置");
                            envVar.setRequired(true);
                            envVar.setSensitive(true);
                        } else {
                            envVar.setDescription(key + " 配置");
                            envVar.setRequired(false);
                        }
                    }
                    
                    // 设置是否必需（可以从描述或命名规则推断）
                    if (key.contains("PORT") || key.contains("PATH") || key.contains("PASSWORD")) {
                        envVar.setRequired(true);
                    }
                    
                    if (key.contains("PASSWORD") || key.contains("SECRET") || key.contains("TOKEN")) {
                        envVar.setSensitive(true);
                    }
                    
                    envVars.add(envVar);
                }
            }
        }
        
        return envVars;
    }
    
    /**
     * 解析服务端口
     */
    @SuppressWarnings("unchecked")
    private static List<String> parseServicePorts(Map<String, Object> serviceConfig) {
        List<String> ports = new ArrayList<>();
        
        Object portsObj = serviceConfig.get("ports");
        if (portsObj instanceof List) {
            List<String> portsList = (List<String>) portsObj;
            ports.addAll(portsList);
        }
        
        return ports;
    }
    
    /**
     * 解析服务卷挂载
     */
    @SuppressWarnings("unchecked")
    private static List<String> parseServiceVolumes(Map<String, Object> serviceConfig) {
        List<String> volumes = new ArrayList<>();
        
        Object volumesObj = serviceConfig.get("volumes");
        if (volumesObj instanceof List) {
            List<String> volumesList = (List<String>) volumesObj;
            volumes.addAll(volumesList);
        }
        
        return volumes;
    }
    
    /**
     * 提取端口描述
     */
    private static String extractPortDescription(String key) {
        // 例如: NGINX_PORT_80 -> "Nginx Web端口"
        Pattern pattern = Pattern.compile("([A-Z]+)_PORT_([0-9]+)");
        Matcher matcher = pattern.matcher(key);
        
        if (matcher.matches()) {
            String serviceName = matcher.group(1).toLowerCase();
            String port = matcher.group(2);
            
            // 根据服务名和端口推断描述
            switch (serviceName) {
                case "nginx":
                    return "Nginx Web端口";
                case "mysql":
                    return "MySQL数据库端口";
                case "redis":
                    return "Redis缓存端口";
                case "postgres":
                    return "PostgreSQL数据库端口";
                default:
                    return serviceName + " 服务端口";
            }
        }
        
        return "端口配置";
    }
    
    /**
     * 提取路径描述
     */
    private static String extractPathDescription(String key) {
        // 例如: BASE_PATH_1 -> "数据存储基础路径"
        if (key.startsWith("BASE_PATH")) {
            return "数据存储基础路径";
        }
        return "路径配置";
    }
} 