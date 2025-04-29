package com.dsm.pojo.entity;

import lombok.Data;

@Data
public class DockerAuthStatus {
    private String registry;
    private boolean isAuthenticated;
    private String username;
} 