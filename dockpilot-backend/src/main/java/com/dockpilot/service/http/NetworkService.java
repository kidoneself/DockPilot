package com.dockpilot.service.http;

import com.dockpilot.model.NetworkInfoDTO;

import java.util.List;

public interface NetworkService {

    /**
     * 获取所有网络列表
     *
     * @return 网络列表
     * @ 获取失败时抛出异常
     */
    List<NetworkInfoDTO> listNetworks();

    /**
     * 获取网络详情
     *
     * @param networkId 网络ID
     * @return 网络详情
     * @ 获取失败时抛出异常
     */
    NetworkInfoDTO getNetworkDetail(String networkId);
}