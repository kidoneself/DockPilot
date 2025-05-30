package com.dockpilot.controller;

import com.dockpilot.service.PortCheckService;
import com.dockpilot.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Map;

/**
 * 端口检测控制器
 */
@RestController
@RequestMapping("/api/port")
@Tag(name = "端口检测", description = "宿主机端口可用性检测接口")
@RequiredArgsConstructor
public class PortController {
    
    private final PortCheckService portCheckService;
    
    /**
     * 检测单个端口是否可用
     */
    @GetMapping("/check/{port}")
    @Operation(summary = "检测端口可用性", description = "检测指定端口在宿主机上是否可用")
    public ApiResponse<Boolean> checkPort(
            @Parameter(description = "端口号", example = "8080")
            @PathVariable @Min(1) @Max(65535) int port) {
        
        boolean available = portCheckService.isPortAvailable(port);
        return ApiResponse.success(available);
    }
    
    /**
     * 批量检测端口
     */
    @PostMapping("/check-batch")
    @Operation(summary = "批量检测端口", description = "批量检测多个端口的可用性")
    public ApiResponse<Map<Integer, Boolean>> checkMultiplePorts(
            @Parameter(description = "端口列表", example = "[8080, 3306, 6379]")
            @RequestBody int[] ports) {
        
        Map<Integer, Boolean> result = portCheckService.checkMultiplePorts(ports);
        return ApiResponse.success(result);
    }
    
    /**
     * 查找可用端口
     */
    @GetMapping("/find-available")
    @Operation(summary = "查找可用端口", description = "在指定范围内查找可用端口")
    public ApiResponse<int[]> findAvailablePorts(
            @Parameter(description = "起始端口", example = "8000")
            @RequestParam @Min(1) @Max(65535) int startPort,
            
            @Parameter(description = "结束端口", example = "9000") 
            @RequestParam @Min(1) @Max(65535) int endPort,
            
            @Parameter(description = "需要的端口数量", example = "5")
            @RequestParam(defaultValue = "1") @Min(1) @Max(100) int count) {
        
        if (startPort > endPort) {
            return ApiResponse.error("起始端口不能大于结束端口");
        }
        
        int[] availablePorts = portCheckService.findAvailablePorts(startPort, endPort, count);
        return ApiResponse.success(availablePorts);
    }
    
    /**
     * 快速检测常用端口
     */
    @GetMapping("/check-common")
    @Operation(summary = "检测常用端口", description = "检测常用端口的可用性")
    public ApiResponse<Map<String, Object>> checkCommonPorts() {
        
        // 常用端口列表
        int[] commonPorts = {80, 443, 3306, 5432, 6379, 27017, 8080, 8888, 9000};
        
        Map<Integer, Boolean> portStatus = portCheckService.checkMultiplePorts(commonPorts);
        
        Map<String, Object> result = Map.of(
            "ports", portStatus,
            "availableCount", portStatus.values().stream().mapToLong(available -> available ? 1 : 0).sum(),
            "totalCount", commonPorts.length
        );
        
        return ApiResponse.success(result);
    }
} 