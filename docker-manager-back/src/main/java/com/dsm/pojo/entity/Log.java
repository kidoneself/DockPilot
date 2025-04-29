package com.dsm.pojo.entity;

import lombok.Data;

@Data
public class Log {
    private Long id;
    private String type;      // 日志类型：OPERATION-操作日志，SYSTEM-系统日志
    private String level;     // 日志级别：INFO, ERROR, WARN
    private String content;   // 日志内容
    private String createTime;  // 创建时间
} 