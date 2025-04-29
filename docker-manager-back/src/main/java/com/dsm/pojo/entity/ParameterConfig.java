package com.dsm.pojo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ParameterConfig {
    @JsonProperty("key")
    private String key;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("value")
    private String value;
} 