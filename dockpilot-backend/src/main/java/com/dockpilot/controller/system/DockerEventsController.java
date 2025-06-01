package com.dockpilot.controller.system;

import com.dockpilot.service.docker.DockerEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Docker Events 监控控制器
 */
@RestController
@RequestMapping("/api/docker/events")
public class DockerEventsController {

    @Autowired
    private DockerEventService dockerEventService;

    /**
     * 获取Docker Events监听状态
     */
    @GetMapping("/status")
    public Map<String, Object> getEventListenerStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("running", dockerEventService.isRunning());
        result.put("message", dockerEventService.isRunning() ? "Docker Events监听运行中" : "Docker Events监听已停止");
        return result;
    }

    /**
     * 启动Docker Events监听
     */
    @PostMapping("/start")
    public Map<String, Object> startEventListener() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (dockerEventService.isRunning()) {
                result.put("success", false);
                result.put("message", "Docker Events监听已在运行中");
            } else {
                dockerEventService.startEventListener();
                result.put("success", true);
                result.put("message", "Docker Events监听已启动");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "启动失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 停止Docker Events监听
     */
    @PostMapping("/stop")
    public Map<String, Object> stopEventListener() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!dockerEventService.isRunning()) {
                result.put("success", false);
                result.put("message", "Docker Events监听未在运行");
            } else {
                dockerEventService.stopEventListener();
                result.put("success", true);
                result.put("message", "Docker Events监听已停止");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "停止失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 重启Docker Events监听
     */
    @PostMapping("/restart")
    public Map<String, Object> restartEventListener() {
        Map<String, Object> result = new HashMap<>();
        try {
            dockerEventService.stopEventListener();
            Thread.sleep(1000); // 等待1秒
            dockerEventService.startEventListener();
            result.put("success", true);
            result.put("message", "Docker Events监听已重启");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "重启失败: " + e.getMessage());
        }
        return result;
    }
} 