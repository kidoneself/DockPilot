package com.dsm.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * 允许匿名访问的注解
 * 用于标记不需要登录就可以访问的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("permitAll()")
public @interface Anonymous {
} 