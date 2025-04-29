package com.dsm.pojo.request;

import lombok.Data;

@Data
public class DockerRegistryRequest {
    private String registry;
    private String username;
    private String password;
} 