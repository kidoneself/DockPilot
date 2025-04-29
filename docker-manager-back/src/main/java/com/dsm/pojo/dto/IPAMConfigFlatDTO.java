package com.dsm.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class IPAMConfigFlatDTO {

    @JsonProperty("Subnet")
    private String subnet; // 子网，例如 "172.18.0.0/16"

    @JsonProperty("IPRange")
    private String ipRange; // 可选：IP 范围限制

    @JsonProperty("Gateway")
    private String gateway; // 子网网关，例如 "172.18.0.1"

    @JsonProperty("AuxiliaryAddresses")
    private Map<String, String> auxiliaryAddresses; // 附加地址（键值对形式）
}