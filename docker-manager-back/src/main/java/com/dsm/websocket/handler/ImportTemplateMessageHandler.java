package com.dsm.websocket.handler;

import com.dsm.mapper.TemplateMapper;
import com.dsm.pojo.entity.Template;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 导入模板消息处理器
 */
@Slf4j
@Component
public class ImportTemplateMessageHandler extends BaseMessageHandler {

    private final TemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    public ImportTemplateMessageHandler(TemplateMapper templateMapper, ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public MessageType getType() {
        return MessageType.IMPORT_TEMPLATE;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
            String templateContent = (String) data.get("content");
            
            // 解析完整的模板内容
            JsonNode rootNode = objectMapper.readTree(templateContent);
            
            // 创建新的JSON对象，只包含services和parameters节点
            ObjectNode newTemplate = objectMapper.createObjectNode();
            newTemplate.set("services", rootNode.get("services"));
            newTemplate.set("parameters", rootNode.get("parameters"));
            if (rootNode.get("configs") != null) {
                newTemplate.set("configs", rootNode.get("configs"));
            }
            
            // 将新的JSON对象转换为字符串
            String processedContent = objectMapper.writeValueAsString(newTemplate);

            // 创建模板实体
            Template template = new Template();
            template.setId(UUID.randomUUID().toString());
            template.setName(rootNode.get("name").asText());
            template.setCategory(rootNode.get("category").asText());
            template.setVersion(rootNode.get("version").asText());
            template.setDescription(rootNode.get("description").asText());
            template.setIconUrl(rootNode.get("iconUrl").asText());
            template.setTemplate(processedContent);
            template.setCreatedAt(LocalDateTime.now());
            template.setUpdatedAt(LocalDateTime.now());
            template.setSortWeight(0);

            // 保存到数据库
            templateMapper.insert(template);

            // 发送成功响应
            sendResponse(session, MessageType.IMPORT_TEMPLATE_RESULT, wsMessage.getTaskId(), 
                Map.of("success", true, "message", "模板导入成功"));
        } catch (Exception e) {
            log.error("导入模板失败", e);
            sendResponse(session, MessageType.IMPORT_TEMPLATE_RESULT, ((DockerWebSocketMessage) message).getTaskId(),
                Map.of("success", false, "message", "导入模板失败: " + e.getMessage()));
        }
    }
} 