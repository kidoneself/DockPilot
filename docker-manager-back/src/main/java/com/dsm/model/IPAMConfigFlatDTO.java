package com.dsm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dockerjava.api.model.Network;
import lombok.Data;

@Data
public class IPAMConfigFlatDTO {

    @JsonProperty("Subnet")
    private String subnet; // 子网，例如 "172.18.0.0/16"

    @JsonProperty("IPRange")
    private String ipRange; // 可选：IP 范围限制

    @JsonProperty("Gateway")
    private String gateway; // 子网网关，例如 "172.18.0.1"

    @JsonProperty("networkID")
    private String networkID; // 附加地址（键值对形式）

    public static IPAMConfigFlatDTO convert(Network.Ipam.Config config) {
        IPAMConfigFlatDTO dto = new IPAMConfigFlatDTO();
        dto.setSubnet(config.getSubnet());
        dto.setIpRange(config.getIpRange());
        dto.setGateway(config.getGateway());
        dto.setNetworkID(config.getNetworkID());
        return dto;
    }
}