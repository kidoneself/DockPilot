package com.dsm.websocket.service;

import com.alibaba.fastjson.JSON;
import com.dsm.api.DockerService;
import com.dsm.mapper.TemplateMapper;
import com.dsm.pojo.entity.Template;
import com.dsm.utils.JsonPlaceholderReplacerUtil;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.DockerWebSocketMessageSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.CreateContainerCmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

@Slf4j
@Service
public class DockerInstallService {

    @Autowired
    private TemplateMapper templateMapper;

    @Autowired
    private DockerWebSocketMessageSender messageSender;

    @Autowired
    private DockerService dockerService;

    public void handleInstallStart(WebSocketSession session, DockerWebSocketMessage message) {
        try {
            @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) message.getData();
            String appId = (String) data.get("appId");
            @SuppressWarnings("unchecked") Map<String, String> params = (Map<String, String>) data.get("params");

            // 发送开始日志
            messageSender.sendLog(session, "info", "开始处理安装请求...");

            // 从数据库获取应用模板
            messageSender.sendLog(session, "info", "正在获取应用模板...");
            String templateJson = getApplicationTemplate(appId);
            if (templateJson == null) {
                messageSender.sendLog(session, "error", "应用模板不存在");
                messageSender.sendErrorMessage(session, "应用模板不存在");
                return;
            }
            messageSender.sendLog(session, "success", "成功获取应用模板");

            // 替换模板中的参数
            messageSender.sendLog(session, "info", "正在替换模板参数...");
            String processedTemplate = JsonPlaceholderReplacerUtil.replacePlaceholders(templateJson, params);
            messageSender.sendLog(session, "success", "参数替换完成");
            ObjectMapper mapper = new ObjectMapper();
            messageSender.sendLog(session, "info", "开始解析容器配置...");
            JsonNode jsonNode = mapper.readTree(processedTemplate);
            messageSender.sendLog(session, "success", "解析容器配置完成");

            // 获取 services 节点
            JsonNode servicesNode = jsonNode.get("services");
            if (servicesNode == null) {
                messageSender.sendLog(session, "error", "模板中未找到 services 配置");
                messageSender.sendErrorMessage(session, "模板中未找到 services 配置");
                return;
            }

            if (!servicesNode.isArray()) {
                messageSender.sendLog(session, "error", "services 配置格式错误，应为对象类型");
                messageSender.sendErrorMessage(session, "services 配置格式错误，应为对象类型");
                return;
            }

            if (servicesNode.isEmpty()) {
                messageSender.sendLog(session, "error", "services 配置为空，未定义任何服务");
                messageSender.sendErrorMessage(session, "services 配置为空，未定义任何服务");
                return;
            }

            messageSender.sendLog(session, "info", "开始处理服务配置...");
            
            // 使用for循环遍历services节点
            for (JsonNode serviceConfig : servicesNode) {
                try {
                    String serviceName = serviceConfig.get("name").asText();
                    messageSender.sendLog(session, "info", String.format("正在处理服务 [%s] 的配置...", serviceName));

                    JsonNode template = serviceConfig.get("template");


                    // 生成容器启动命令
                    CreateContainerCmd containerCmd = dockerService.getCmdByTempJson(template);
                    if (containerCmd == null) {
                        messageSender.sendLog(session, "error", String.format("服务 [%s] 的容器启动命令生成失败", serviceName));
                        continue;
                    }
                    
                    messageSender.sendLog(session, "success", String.format("服务 [%s] 的容器启动命令生成成功", serviceName));
                    
                    String containerId = dockerService.startContainerWithCmd(containerCmd);
                    messageSender.sendLog(session, "success", String.format("服务 [%s] 的容器创建成功", serviceName));
                    
                } catch (Exception e) {
                    log.error("处理服务配置时发生错误", e);
                    messageSender.sendLog(session, "error", String.format("处理服务配置时发生错误: %s", e.getMessage()));
                }
            }

            // 发送处理后的模板
            messageSender.sendMessage(session, new DockerWebSocketMessage("INSTALL_START_RESULT", message.getTaskId(), Map.of("success", true, "message", "模板处理成功", "template", JSON.parseObject(processedTemplate))));
            messageSender.sendLog(session, "success", "所有服务配置处理完成");

        } catch (Exception e) {
            log.error("处理安装请求失败", e);
            messageSender.sendLog(session, "error", "处理安装请求失败: " + e.getMessage());
            messageSender.sendErrorMessage(session, "处理安装请求失败: " + e.getMessage());
        }
    }

    /**
     * 从数据库获取应用模板
     *
     * @param appId 应用ID
     * @return 模板JSON字符串
     */
    private String getApplicationTemplate(String appId) {
        Template template = templateMapper.selectTemplateById(appId);
        return template.getTemplate();
    }
} 