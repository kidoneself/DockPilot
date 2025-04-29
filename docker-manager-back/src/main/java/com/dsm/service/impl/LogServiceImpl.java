package com.dsm.service.impl;

import com.dsm.mapper.LogMapper;
import com.dsm.pojo.entity.Log;
import com.dsm.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 日志服务实现类
 * 负责系统日志的记录和查询
 */
@Slf4j
@Service
public class LogServiceImpl implements LogService {

    @Resource
    private LogMapper logMapper;

    @Override
    public void addLog(Log logEntry) {
        try {
            logMapper.insert(logEntry);
            log.info("{}--{}--{}", logEntry.getType(), logEntry.getLevel(), logEntry.getContent());
        } catch (Exception e) {
            log.error("保存日志失败: {}", e.getMessage());
        }
    }

    @Override
    public List<Log> getLogs(String type, String level) {
        if (type != null && level != null) {
            return logMapper.findRecentLogsByType(type, 10);
        } else if (type != null) {
            return logMapper.findRecentLogsByType(type, 10);
        } else if (level != null) {
            return logMapper.findRecentLogsByLevel(level, 10);
        } else {
            return logMapper.findRecentLogs(10);
        }
    }

    @Override
    public List<Log> getRecentLogs(int limit) {
        return logMapper.findRecentLogs(limit);
    }

    @Override
    public void cleanupOldLogs(int days) {
        logMapper.deleteByCreateTimeBefore(days);
    }
} 