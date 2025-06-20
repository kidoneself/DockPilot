package com.dockpilot.controller;

import com.dockpilot.model.Log;
import com.dockpilot.service.http.LogService;
import com.dockpilot.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "日志管理", description = "日志管理接口")
@RestController
@RequestMapping("/logs")
@SecurityRequirement(name = "JWT")
public class LogController {
    @Autowired
    private LogService logService;


    @Operation(summary = "获取日志", description = "获取日志列表")
    @GetMapping
    public ApiResponse<List<Log>> getLogs(@RequestParam(required = false) String type, @RequestParam(required = false) String level, @RequestParam(required = false) Integer limit) {
        if (limit != null) {
            return ApiResponse.success(logService.getRecentLogs(limit));
        }
        return ApiResponse.success(logService.getLogs(type, level));
    }

    @Operation(summary = "清理旧日志", description = "清理指定天数前的日志")
    @DeleteMapping("/cleanup")
    public ApiResponse<Void> cleanupOldLogs(@RequestParam int days) {
        logService.cleanupOldLogs(days);
        return ApiResponse.success(null);
    }
}