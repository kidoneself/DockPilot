package com.dockpilot.service.http.impl;

import com.dockpilot.api.DockerService;
import com.dockpilot.model.IPAMConfigFlatDTO;
import com.dockpilot.model.NetworkContainerDTO;
import com.dockpilot.model.NetworkInfoDTO;
import com.dockpilot.service.http.NetworkService;
import com.dockpilot.utils.NetworkUtil;
import com.github.dockerjava.api.model.Network;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NetworkServiceImpl implements NetworkService {

    // 网络模式映射
    private static final Map<String, String> NETWORK_MODE_MAP = new HashMap<>() {{
        put("bridge", "桥接模式");
        put("host", "主机模式");
        put("none", "禁用网络");
        put("container", "容器模式");
        put("macvlan", "Macvlan模式");
        put("ipvlan", "IPvlan模式");
        put("custom", "自定义网络");
    }};

    @Resource
    private DockerService dockerService;

    @Override
    public List<NetworkInfoDTO> listNetworks() {
        List<Network> networks = dockerService.listNetworks();

        List<NetworkInfoDTO> result = new ArrayList<>();
        for (Network network : networks) {
            NetworkInfoDTO dto = new NetworkInfoDTO();
            // 基本字段
            dto.setId(network.getId());
            dto.setName(network.getName());
            dto.setDriver(network.getDriver());
            dto.setScope(network.getScope());
            dto.setEnableIPv6(network.getEnableIPv6());
            dto.setInternal(network.getInternal());
            dto.setAttachable(network.isAttachable());
            dto.setLabels(network.getLabels());
            dto.setOptions(network.getOptions());

            // 设置网络模式的中文显示
            String driver = network.getName();
            dto.setNameStr(NETWORK_MODE_MAP.getOrDefault(driver, driver));

            // IPAM 字段
            Network.Ipam ipam = network.getIpam();
            if (ipam != null) {
                dto.setIpamDriver(ipam.getDriver());
                dto.setIpamOptions(ipam.getOptions());

                // 如果是host模式，使用宿主机网络信息
                if ("host".equals(network.getDriver())) {
                    dto.setIpamConfig(NetworkUtil.getHostNetworkInfo());
                } else {
                    // IPAMConfig 转换
                    List<Network.Ipam.Config> config = ipam.getConfig();
                    // 检查 config 是否为 null，如果是 null 则设置为空列表
                    dto.setIpamConfig(config == null ? Collections.emptyList() : config.stream().map(IPAMConfigFlatDTO::convert).collect(Collectors.toList()));
                }
            }

            // 添加到结果列表
            result.add(dto);
        }
        return result;
    }

    @Override
    public NetworkInfoDTO getNetworkDetail(String networkId) {
        Network network = dockerService.inspectNetwork(networkId);
        NetworkInfoDTO dto = new NetworkInfoDTO();
        // 基本字段
        dto.setId(network.getId());
        dto.setName(network.getName());
        dto.setDriver(network.getDriver());
        dto.setScope(network.getScope());
        dto.setEnableIPv6(network.getEnableIPv6());
        dto.setInternal(network.getInternal());
        dto.setAttachable(network.isAttachable());
        dto.setLabels(network.getLabels());
        dto.setOptions(network.getOptions());

        // 设置网络模式的中文显示
        String driver = network.getName();
        dto.setNameStr(NETWORK_MODE_MAP.getOrDefault(driver, driver));

        // IPAM 字段
        Network.Ipam ipam = network.getIpam();
        if (ipam != null) {
            dto.setIpamDriver(ipam.getDriver());
            dto.setIpamOptions(ipam.getOptions());

            // 如果是host模式，使用宿主机网络信息
            if ("host".equals(network.getDriver())) {
                dto.setIpamConfig(NetworkUtil.getHostNetworkInfo());
            } else {
                // IPAMConfig 转换
                List<Network.Ipam.Config> config = ipam.getConfig();
                // 检查 config 是否为 null，如果是 null 则设置为空列表
                dto.setIpamConfig(config == null ? Collections.emptyList() : config.stream().map(IPAMConfigFlatDTO::convert).collect(Collectors.toList()));
            }
        }

        // 设置容器信息
        Map<String, NetworkContainerDTO> containers = new HashMap<>();
        if (network.getContainers() != null) {
            // 如果是host模式，获取宿主机网络信息
            NetworkContainerDTO hostConfig = "host".equals(network.getDriver()) ?
                    NetworkUtil.getHostContainerNetworkConfig() : null;

            for (Map.Entry<String, Network.ContainerNetworkConfig> entry : network.getContainers().entrySet()) {
                NetworkContainerDTO containerDTO = new NetworkContainerDTO();
                containerDTO.setId(entry.getKey());
                Network.ContainerNetworkConfig config = entry.getValue();
                containerDTO.setEndpointId(config.getEndpointId());

                if (hostConfig != null) {
                    // 如果是host模式，使用宿主机网络信息
                    containerDTO.setMacAddress(hostConfig.getMacAddress());
                    containerDTO.setIpv4Address(hostConfig.getIpv4Address());
                    containerDTO.setIpv6Address(hostConfig.getIpv6Address());
                } else {
                    // 其他模式使用容器自己的网络信息
                    containerDTO.setMacAddress(config.getMacAddress());
                    containerDTO.setIpv4Address(config.getIpv4Address());
                    containerDTO.setIpv6Address(config.getIpv6Address());
                }
                containerDTO.setName(config.getName());
                containers.put(entry.getKey(), containerDTO);
            }
        }
        dto.setContainers(containers);

        return dto;
    }
} 