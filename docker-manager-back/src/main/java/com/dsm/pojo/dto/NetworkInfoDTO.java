package com.dsm.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NetworkInfoDTO {

    @JsonProperty("Id")
    private String id; // 网络唯一ID

    @JsonProperty("Name")
    private String name; // 网络名称

    @JsonProperty("Driver")
    private String driver; // 驱动类型，例如 bridge、overlay

    @JsonProperty("Scope")
    private String scope; // 网络作用域（local / global）

    @JsonProperty("EnableIPv6")
    private boolean enableIPv6; // 是否启用 IPv6

    @JsonProperty("Internal")
    private boolean internal; // 是否是内部网络（容器之间可见，不能连接外部）

    @JsonProperty("Attachable")
    private boolean attachable; // 是否支持容器直接连接

    @JsonProperty("Ingress")
    private boolean ingress; // 是否是 ingress 网络（swarm 场景）

    @JsonProperty("Labels")
    private Map<String, String> labels; // 用户定义的标签

    @JsonProperty("Options")
    private Map<String, String> options; // 网络驱动相关配置

    // IPAM（地址管理）相关字段已扁平化
    @JsonProperty("IPAMDriver")
    private String ipamDriver; // IPAM 使用的驱动

    @JsonProperty("IPAMOptions")
    private Map<String, String> ipamOptions; // IPAM 选项（可为空）

    @JsonProperty("IPAMConfig")
    private List<IPAMConfigFlatDTO> ipamConfig; // 子网等配置，可能有多个

    // 来自其他网络的配置（如引用）
    @JsonProperty("ConfigFromNetwork")
    private String configFromNetwork; // 如果此网络从其他网络继承配置，则填对应网络名

    @JsonProperty("ConfigOnly")
    private boolean configOnly; // 是否仅为配置网络（Swarm 场景使用）

    @JsonProperty("nameStr")
    private String nameStr; // 英文转中文

}