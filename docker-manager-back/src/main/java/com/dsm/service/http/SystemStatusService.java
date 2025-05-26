package com.dsm.service.http;

import com.dsm.model.SystemStatusDTO;

/**
 * 系统状态服务接口
 */
public interface SystemStatusService {
    
    /**
     * 获取系统状态信息
     * @return 系统状态DTO
     */
    SystemStatusDTO getSystemStatus();
} 