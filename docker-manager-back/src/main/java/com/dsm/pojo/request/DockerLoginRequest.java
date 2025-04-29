package com.dsm.pojo.request;

import lombok.Data;

@Data
public class DockerLoginRequest {
    private String registry;
    private String username;
    private String password;
} 