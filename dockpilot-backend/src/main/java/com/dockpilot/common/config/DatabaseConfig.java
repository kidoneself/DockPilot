package com.dockpilot.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl(dbUrl);

        // 初始化数据库
        initializeDatabase(dataSource);

        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    private void initializeDatabase(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/schema.sql"));
        populator.execute(dataSource);

        // 动态添加字段（兼容新旧环境）
        // 新环境：字段已存在，检查会跳过
        // 旧环境：自动添加缺失字段
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 添加收藏字段
        addColumnIfNotExists(jdbcTemplate, "web_servers", "is_favorite", "INTEGER DEFAULT 0");

        // addColumnIfNotExists(jdbcTemplate, "image_status", "image_id", "TEXT");
        // addColumnIfNotExists(jdbcTemplate, "image_status", "pulling", "INTEGER DEFAULT 0");
        // addColumnIfNotExists(jdbcTemplate, "image_status", "progress", "TEXT");

        // addColumnIfNotExists(jdbcTemplate, "container_info", "need_update", "INTEGER DEFAULT 0");
        // addColumnIfNotExists(jdbcTemplate, "container_info", "icon_url", "TEXT DEFAULT NULL");
        // addColumnIfNotExists(jdbcTemplate, "container_info", "web_url", "TEXT");

    }

    private void addColumnIfNotExists(JdbcTemplate jdbcTemplate, String table, String column, String definition) {
        String checkSql = "PRAGMA table_info(" + table + ")";
        boolean exists = jdbcTemplate.query(checkSql,
                        (rs, rowNum) -> rs.getString("name"))
                .stream()
                .anyMatch(name -> name.equalsIgnoreCase(column));

        if (!exists) {
            String alterSql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition;
            jdbcTemplate.execute(alterSql);
        }
    }
} 