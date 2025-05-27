package com.dockpilot.common.exception;

/**
 * 权限不足异常
 * 用于处理需要特定角色才能访问的接口
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public static UnauthorizedException needPro() {
        return new UnauthorizedException("该功能需要专业版权限，请升级到专业版");
    }
} 