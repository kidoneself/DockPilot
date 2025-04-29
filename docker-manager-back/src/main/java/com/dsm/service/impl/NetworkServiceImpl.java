package com.dsm.service.impl;

import com.dsm.api.DockerService;
import com.dsm.pojo.dto.IPAMConfigFlatDTO;
import com.dsm.pojo.dto.NetworkInfoDTO;
import com.dsm.service.NetworkService;
import com.github.dockerjava.api.model.Network;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NetworkServiceImpl implements NetworkService {

    @Resource
    private DockerService dockerService;

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
                // IPAMConfig 转换
                List<IPAMConfigFlatDTO> ipamConfigList = new ArrayList<>();
                dto.setIpamConfig(ipamConfigList);
            }
            // 添加到结果列表
            result.add(dto);
        }
        return result;
    }

    @Override
    public Map<String, Object> getNetworkDetail(String networkId) {
        Network network = dockerService.inspectNetwork(networkId);
        Map<String, Object> networkInfo = new HashMap<>();
        networkInfo.put("id", network.getId());
        networkInfo.put("name", network.getName());
        networkInfo.put("driver", network.getDriver());
        networkInfo.put("scope", network.getScope());
        networkInfo.put("ipam", network.getIpam());
        networkInfo.put("containers", network.getContainers());

        // 添加网络模式的中文显示
        String driver = network.getDriver();
        networkInfo.put("nameStr", NETWORK_MODE_MAP.getOrDefault(driver, driver));

        return networkInfo;
    }

} 