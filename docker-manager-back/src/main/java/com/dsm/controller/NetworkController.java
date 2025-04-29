package com.dsm.controller;

import com.dsm.pojo.dto.NetworkInfoDTO;
import com.dsm.service.NetworkService;
import com.dsm.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Tag(name = "Docker网络管理", description = "Docker网络相关的API接口")
@RestController
@RequestMapping("/networks")
public class NetworkController {

    @Resource
    private NetworkService networkService;

    @Operation(summary = "获取网络列表", description = "获取所有Docker网络的列表")
    @GetMapping("/list")
    public ApiResponse<List<NetworkInfoDTO>> listNetworks() {
        List<NetworkInfoDTO> networks = networkService.listNetworks();
        return ApiResponse.success(networks);
    }

    @Operation(summary = "获取网络详情", description = "根据网络ID获取网络的详细信息")
    @GetMapping("/{networkId}")
    public ApiResponse<Map<String, Object>> getNetworkDetail(@PathVariable String networkId) {
        Map<String, Object> network = networkService.getNetworkDetail(networkId);
        return ApiResponse.success(network);
    }

} 