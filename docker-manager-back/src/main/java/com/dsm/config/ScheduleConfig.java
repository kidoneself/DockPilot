package com.dsm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 * 启用Spring定时任务支持
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
    // 启用定时任务即可，无需其他配置
} 