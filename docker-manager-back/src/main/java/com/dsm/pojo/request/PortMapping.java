package com.dsm.pojo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 端口映射
 */
@Data
@Schema(description = "端口映射")
public class PortMapping {

    @Schema(description = "IP地址")
    private String IP;

    @Schema(description = "容器端口")
    private String PrivatePort;

    @Schema(description = "主机端口")
    private String PublicPort;

    @Schema(description = "端口类型")
    private String Type;
}
