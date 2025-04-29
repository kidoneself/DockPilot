package com.dsm.pojo.entity;

import lombok.Data;
import java.util.List;

@Data
public class AppStoreApp {
    private String id;
    private String name;
    private String description;
    private String icon;
    private String category;
    private String version;
    private List<AppStoreService> services;
    private List<AppStoreEnvField> envFields;
}

@Data
class AppStoreService {
    private String name;
    private String displayName;
    private String description;
    private List<String> dependencies;
}

@Data
class AppStoreEnvField {
    private String key;
    private String label;
    private String description;
    private String defaultValue;
    private boolean required;
    private List<String> serviceDependencies;
} 