package com.dockpilot.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class AsyncApiConfig implements WebMvcConfigurer {

    @Value("classpath:asyncapi.yaml")
    private Resource asyncApiResource;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/asyncapi/**")
                .addResourceLocations("classpath:/static/asyncapi/");
    }

    @Bean
    public String asyncApiSpec() throws IOException {
        // 使用InputStream读取jar包内的资源文件
        return StreamUtils.copyToString(asyncApiResource.getInputStream(), StandardCharsets.UTF_8);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Docker Manager API")
                        .version("1.0.0")
                        .description("Docker容器管理系统API文档"))
                // 添加安全配置
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")));
    }

    @Bean
    public OpenApiCustomiser asyncApiCustomiser() {
        return openApi -> {
            openApi.getInfo().setTitle("Docker Manager API");
            openApi.getInfo().setVersion("1.0.0");
            openApi.getInfo().setDescription("Docker容器管理系统的API文档");
        };
    }
} 