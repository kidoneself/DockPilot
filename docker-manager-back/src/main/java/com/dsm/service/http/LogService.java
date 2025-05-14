package com.dsm.service.http;

import com.dsm.model.Log;

import java.util.List;

public interface LogService {
    void addLog(Log log);

    List<Log> getLogs(String type, String level);

    List<Log> getRecentLogs(int limit);

    void cleanupOldLogs(int days);
} 