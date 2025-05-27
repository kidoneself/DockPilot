package com.dockpilot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NetworkContainerDTO {
    @JsonProperty("Id")
    private String id; // 容器ID

    @JsonProperty("Name")
    private String name; // 容器名称

    @JsonProperty("EndpointID")
    private String endpointId; // 网络端点ID

    @JsonProperty("MacAddress")
    private String macAddress; // MAC地址

    @JsonProperty("IPv4Address")
    private String ipv4Address; // IPv4地址

    @JsonProperty("IPv6Address")
    private String ipv6Address; // IPv6地址
} 