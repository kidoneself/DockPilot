package com.dsm.pojo.entity;

import lombok.Data;

@Data
public class DockerResource {
    private String type; // container, network, volume
    private String id;
    private String name;
} 