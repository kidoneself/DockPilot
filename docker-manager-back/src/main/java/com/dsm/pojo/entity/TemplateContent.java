package com.dsm.pojo.entity;

import com.dsm.model.dto.ServiceConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class TemplateContent {
    @JsonProperty("services")
    private List<ServiceConfig> services;
    
    @JsonProperty("parameters")
    private List<ParameterConfig> parameters;
} 