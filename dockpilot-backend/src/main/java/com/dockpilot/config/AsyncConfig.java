package com.dockpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步处理配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "packageTaskExecutor")
    public Executor packageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);  // 核心线程数
        executor.setMaxPoolSize(5);   // 最大线程数
        executor.setQueueCapacity(10); // 队列容量
        executor.setThreadNamePrefix("PackageTask-");
        executor.initialize();
        return executor;
    }
} 