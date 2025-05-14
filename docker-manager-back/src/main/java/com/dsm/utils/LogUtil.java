package com.dsm.utils;

import com.dsm.model.Log;
import com.dsm.service.http.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Slf4j
@Component
public class LogUtil {
    private static LogService logService;

    // 记录操作日志
    public static void logOpe(String content) {
        logOpe("OPERATION", "INFO", content);
    }

    // 记录系统日志
    public static void logSysError(String content) {
        logOpe("SYSTEM", "ERROR", content);
    }

    public static void logSysInfo(String content) {
        logOpe("SYSTEM", "INFO", content);
    }

    // 统一的日志记录方法
    private static void logOpe(String type, String level, String content) {
        try {
            Log logEntry = new Log();
            logEntry.setType(type);
            logEntry.setLevel(level);
            logEntry.setContent(content);

            if (logService != null) {
                logService.addLog(logEntry);
            } else {
                log.error("LogService未初始化，无法保存日志");
            }
        } catch (Exception e) {
            log.error("记录日志失败: {}", e.getMessage());
        }
    }

    @Resource
    public void setLogService(LogService logService) {
        LogUtil.logService = logService;
    }

    @PostConstruct
    public void init() {
        if (logService == null) {
            log.error("LogService未初始化，无法保存日志");
        } else {
            log.info("LogService初始化成功");
        }
    }
} 