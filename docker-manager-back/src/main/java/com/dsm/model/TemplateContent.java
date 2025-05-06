package com.dsm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TemplateContent {
    @JsonProperty("services")
    private List<ServiceConfig> services;

    @JsonProperty("parameters")
    private List<ParameterConfig> parameters;

    @JsonProperty("configs")
    private List<Configs> configs;
} 