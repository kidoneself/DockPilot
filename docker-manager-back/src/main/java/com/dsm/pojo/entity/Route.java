package com.dsm.pojo.entity;

import lombok.Data;

import java.util.List;

@Data
public class Route {
    private String path;
    private String name;
    private String component;
    private String redirect;
    private Meta meta;
    private List<Route> children;

    // getter 和 setter 省略
}

class Meta {
    private String title;
    private String icon;

    // getter 和 setter 省略
}