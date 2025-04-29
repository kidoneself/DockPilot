package com.dsm.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis配置类
 * 用于扫描Mapper接口
 */
@Configuration
@MapperScan("com.dsm.mapper")
public class MyBatisConfig {
    // 无需具体方法，通过注解配置扫描路径
} 