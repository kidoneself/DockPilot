package com.dsm.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
@Schema(description = "登录请求参数")
public class LoginRequest {
    
    @Schema(description = "用户名", required = true, example = "admin")
    private String username;
    
    @Schema(description = "密码", required = true, example = "123456")
    private String password;
} 