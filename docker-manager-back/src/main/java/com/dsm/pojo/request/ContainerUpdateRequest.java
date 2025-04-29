package com.dsm.pojo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 容器更新请求参数
 */
@Data
@Schema(description = "容器更新请求参数")
public class ContainerUpdateRequest {

    @Schema(description = "容器ID")
    private String id;

    @Schema(description = "容器名称")
    private String name;

    @Schema(description = "镜像名称")
    private String image;

    @Schema(description = "容器状态")
    private String status;

    @Schema(description = "创建时间")
    private Long created;

    @Schema(description = "启动命令")
    private String command;

    @Schema(description = "重启策略")
    private String restart_policy;

    @Schema(description = "网络模式")
    private String network_mode;

    @Schema(description = "IP地址")
    private String ip_address;

    @Schema(description = "网关")
    private String gateway;

    @Schema(description = "IP前缀长度")
    private Integer ip_prefix_len;

    @Schema(description = "MAC地址")
    private String mac_address;

    @Schema(description = "端口映射列表")
    private List<PortMapping> ports;

    @Schema(description = "存储卷挂载列表")
    private List<VolumeMount> mounts;

    @Schema(description = "环境变量列表")
    private List<String> env;
} 