package com.dsm.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ServiceConfig {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("template")
    private ServiceTemplate template;
}

@Data
class ServiceTemplate {
    @JsonProperty("name")
    private String name;

    @JsonProperty("image")
    private String image;

    @JsonProperty("env")
    private Map<String, String> env;

    @JsonProperty("ports")
    private Map<String, String> ports;

    @JsonProperty("volumes")
    private Map<String, String> volumes;

    @JsonProperty("restartPolicy")
    private String restartPolicy;
}
