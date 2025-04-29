package com.dsm.pojo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 存储卷挂载
 */
@Data
@Schema(description = "存储卷挂载")
public class VolumeMount {

    @Schema(description = "主机路径")
    private String source;

    @Schema(description = "容器路径")
    private String destination;

    @Schema(description = "读写权限")
    private boolean rw;
} 