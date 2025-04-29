package com.dsm.pojo.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ContainerInfo {
    private String name;
    private String image;
    private String status;
    private String created;
    private String ipAddress;
    private String[] ports;
    private String[] volumes;
    private Map<String, String> environment;
} 