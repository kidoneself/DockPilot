package com.dsm.exception;

import com.dsm.utils.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理系统中抛出的各类异常，确保返回格式一致的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return ApiResponse.success(null, e.getMessage());
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return ApiResponse.error("系统内部错误");
    }


    @ExceptionHandler(DockerOperationException.class)
    public ApiResponse<Map<String, Object>> handleDockerException(DockerOperationException ex) {
        log.error("Docker异常", ex);
        return ApiResponse.error(ex.getDetail());
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error("系统异常，请稍后重试");
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException e) {
        log.error("参数校验异常", e);
        return ApiResponse.error("参数校验失败：" + e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }
}