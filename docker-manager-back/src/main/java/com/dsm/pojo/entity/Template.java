package com.dsm.pojo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

/**
 * Docker容器模板实体类
 */
@Data
@Slf4j
public class Template {
    /**
     * 模板ID
     */
    private String id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板分类
     */
    private String category;

    /**
     * 模板版本
     */
    private String version;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板图标URL
     */
    private String iconUrl;

    /**
     * 模板内容
     */
    @JsonIgnore
    private String template;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 排序权重
     */
    private Integer sortWeight;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public TemplateContent getTemplateContent() {
        try {
            return objectMapper.readValue(template, TemplateContent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse template content", e);
            return null;
        }
    }

    public void setTemplateContent(TemplateContent content) {
        try {
            this.template = objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize template content", e);
        }
    }
} 