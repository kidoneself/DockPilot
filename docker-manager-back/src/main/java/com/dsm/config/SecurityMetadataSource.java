package com.dsm.config;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 安全元数据源
 * 用于动态获取URL所需的权限
 */
@Component
public class SecurityMetadataSource implements FilterInvocationSecurityMetadataSource {

    private Map<String, Collection<ConfigAttribute>> urlRoles = new HashMap<>();

    @PostConstruct
    public void init() {
        // 初始化时不需要加载URL权限，因为我们现在使用JWT认证
        // 所有需要认证的URL都会通过JWT过滤器进行验证
    }

    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        // 获取请求的URL
        HttpServletRequest request = ((FilterInvocation) object).getRequest();
        String url = request.getRequestURI();
        
        // 如果是登录相关的URL，允许匿名访问
        if (url.startsWith("/api/auth/")) {
            return Collections.emptyList();
        }
        
        // 其他URL都需要认证
        return Collections.singletonList(new SecurityConfig("ROLE_USER"));
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return null;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }
} 