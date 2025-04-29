package com.dsm.exception;

/**
 * 业务异常类
 * 用于封装业务逻辑异常，便于全局异常处理器统一处理
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}