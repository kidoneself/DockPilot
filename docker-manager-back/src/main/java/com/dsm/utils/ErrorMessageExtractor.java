package com.dsm.utils;

import com.dsm.common.exception.BusinessException;
import com.dsm.common.exception.DockerOperationException;

/**
 * 错误信息提取工具类
 * 用于提取用户友好的错误信息，去除技术性的异常类名和堆栈信息
 */
public class ErrorMessageExtractor {

    /**
     * 提取用户友好的错误信息，去除技术性的异常类名和堆栈信息
     */
    public static String extractUserFriendlyError(Throwable ex) {
        if (ex == null) {
            return "未知错误";
        }

        // 1. 检查是否是BusinessException（已经是用户友好的错误信息）
        Throwable current = ex;
        while (current != null) {
            if (current instanceof BusinessException) {
                return current.getMessage();
            }
            if (current instanceof DockerOperationException) {
                DockerOperationException dockerEx = (DockerOperationException) current;
                return dockerEx.getDetail();
            }
            current = current.getCause();
        }

        // 2. 如果是RuntimeException且消息包含"异步任务执行失败："，提取原始错误
        String message = ex.getMessage();
        if (message != null && message.startsWith("异步任务执行失败：")) {
            String originalError = message.substring("异步任务执行失败：".length());
            // 递归处理原始错误，可能还需要进一步清理
            return cleanErrorMessage(originalError);
        }

        // 3. 直接清理错误消息
        return cleanErrorMessage(message);
    }

    /**
     * 清理错误消息，去除技术性的异常类名
     */
    public static String cleanErrorMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "操作失败";
        }

        // 去除常见的异常类名前缀
        String cleaned = message;

        // 去除 Java 异常类名前缀
        cleaned = cleaned.replaceFirst("^java\\.[\\w.]+Exception:\\s*", "");
        cleaned = cleaned.replaceFirst("^com\\.[\\w.]+Exception:\\s*", "");
        cleaned = cleaned.replaceFirst("^org\\.[\\w.]+Exception:\\s*", "");

        // 去除 Docker 相关异常类名
        cleaned = cleaned.replaceFirst("^com\\.dsm\\.common\\.exception\\.DockerOperationException:\\s*", "");
        cleaned = cleaned.replaceFirst("^com\\.dsm\\.common\\.exception\\.BusinessException:\\s*", "");

        // 去除 "任务失败：" 前缀（如果存在）
        cleaned = cleaned.replaceFirst("^任务失败：\\s*", "");

        // 如果清理后为空，返回通用错误信息
        if (cleaned.trim().isEmpty()) {
            return "操作失败";
        }

        return cleaned.trim();
    }
} 