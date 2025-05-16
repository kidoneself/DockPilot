package com.dsm.common.config;

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


        // 然后动态添加字段（如果不存在）
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        addColumnIfNotExists(jdbcTemplate, "image_status", "image_id", "TEXT");
        addColumnIfNotExists(jdbcTemplate, "image_status", "pulling", "INTEGER DEFAULT 0");
        addColumnIfNotExists(jdbcTemplate, "image_status", "progress", "TEXT");


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