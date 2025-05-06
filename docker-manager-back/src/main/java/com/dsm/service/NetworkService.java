package com.dsm.service;

import com.dsm.model.NetworkInfoDTO;

import java.util.List;

public interface NetworkService {

    /**
     * 获取所有网络列表
     *
     * @return 网络列表
     * @ 获取失败时抛出异常
     */
    List<NetworkInfoDTO> listNetworks();

}