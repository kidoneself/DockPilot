package com.dockpilot.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 修改用户名请求DTO
 */
@Data
@Schema(description = "修改用户名请求参数")
public class ChangeUsernameRequest {

    @Schema(description = "新用户名", required = true)
    private String newUsername;
} 