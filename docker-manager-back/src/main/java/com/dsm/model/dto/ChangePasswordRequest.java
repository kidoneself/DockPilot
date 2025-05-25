package com.dsm.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 修改密码请求DTO
 */
@Data
@Schema(description = "修改密码请求参数")
public class ChangePasswordRequest {

    @Schema(description = "旧密码", required = true)
    private String oldPassword;

    @Schema(description = "新密码", required = true)
    private String newPassword;
} 