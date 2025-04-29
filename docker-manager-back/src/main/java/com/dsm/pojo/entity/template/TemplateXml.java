package com.dsm.pojo.entity.template;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Data;
import java.util.List;

/**
 * XML模板实体类
 */
@Data
@JacksonXmlRootElement(localName = "template")
public class TemplateXml {
    @JacksonXmlProperty(localName = "meta")
    private Meta meta;

    @JacksonXmlProperty(localName = "fields")
    private List<Field> fields;

    @JacksonXmlProperty(localName = "services")
    private List<Service> services;

    @Data
    public static class Meta {
        private String name;
        private String description;
        private String logo;
        private String version;
        private String updated;
        private String author;
    }

    @Data
    public static class Field {
        @JacksonXmlProperty(isAttribute = true)
        private String key;
        
        @JacksonXmlProperty(isAttribute = true)
        private String label;
        
        @JacksonXmlProperty(isAttribute = true, localName = "default")
        private String defaultValue;
    }

    @Data
    public static class Service {
        @JacksonXmlProperty(isAttribute = true)
        private String name;
        
        private String image;
        private List<Port> ports;
        private List<Volume> volumes;
        private String restart;
    }

    @Data
    public static class Port {
        @JacksonXmlText
        private String value;
    }

    @Data
    public static class Volume {
        @JacksonXmlText
        private String value;
    }
} 