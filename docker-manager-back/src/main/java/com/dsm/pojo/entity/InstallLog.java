package com.dsm.pojo.entity;

import lombok.Data;

@Data
public class InstallLog {
    private String type; // info, success, warning, error
    private String message;
    private String time;
}