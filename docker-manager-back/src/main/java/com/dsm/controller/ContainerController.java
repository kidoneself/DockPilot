package com.dsm.controller;

import com.dsm.model.dto.ContainerDTO;
import com.dsm.pojo.dto.ContainerCreateDTO;
import com.dsm.model.dto.JsonContainerRequest;
import com.dsm.model.dto.ContainerStaticInfoDTO;
import com.dsm.model.dto.ResourceUsageDTO;
import com.dsm.model.dockerApi.ContainerCreateRequest;
import com.dsm.service.ContainerService;
import com.dsm.utils.ApiResponse;
import com.dsm.utils.ContainerCreateDTOConverterV2;
import com.dsm.utils.ContainerRequestAdapter;
import com.dsm.utils.JsonContainerRequestToContainerCreateRequestConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@RestController
@RequestMapping("/containers")
@RequiredArgsConstructor
@Tag(name = "容器相关的操作", description = "Docker容器")
public class ContainerController {

    @Resource
    ContainerService containerService;


    @Operation(summary = "列出容器", description = "获取所有容器的列表")
    @GetMapping()
    public ApiResponse<List<ContainerDTO>> listContainers() {
        return ApiResponse.success(containerService.listContainers());
    }


    @Operation(summary = "删除容器", description = "根据ID删除容器")
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteContainer(@PathVariable String id) {
        containerService.removeContainer(id);
        return ApiResponse.success("容器删除成功");

    }


    @Operation(summary = "获取容器配置", description = "根据ID获取容器的配置信息")
    @GetMapping("/{id}/config")
    public ApiResponse<ContainerStaticInfoDTO> getContainerConfig(@PathVariable String id) {
        return ApiResponse.success(containerService.getContainerConfig(id));
    }


    @Operation(summary = "创建容器", description = "根据请求创建一个新容器")
    @PostMapping()
    public ApiResponse<String> createContainer(@RequestBody JsonContainerRequest json) {
        ContainerCreateRequest request = JsonContainerRequestToContainerCreateRequestConverter.convert(json);
        String containerId = containerService.createContainer(request);
        return ApiResponse.success(containerId);
    }

    @Operation(summary = "更新容器", description = "根据ID更新容器配置并重新创建")
    @PostMapping("/update/{id}")
    public ApiResponse<String> updateContainer(@PathVariable String id, @RequestBody JsonContainerRequest json) {
        ContainerCreateRequest request = JsonContainerRequestToContainerCreateRequestConverter.convert(json);
        String newContainerId = containerService.updateContainer(id, request);
        return ApiResponse.success(newContainerId);
    }



    @Operation(summary = "获取容器统计信息", description = "根据ID获取容器的统计信息")
    @GetMapping("/{id}/stats")
    public ApiResponse<ResourceUsageDTO> getContainerStats(@PathVariable String id) {
        return ApiResponse.success(containerService.getContainerStats(id));
    }


    @Operation(summary = "启动容器", description = "根据ID启动容器")
    @PostMapping("/start/{id}")
    public ApiResponse<String> startContainer(@PathVariable String id) {
        containerService.startContainer(id);
        return ApiResponse.success(null, "启动成功");

    }


    @Operation(summary = "停止容器", description = "根据ID停止容器")
    @PostMapping("/stop/{id}")
    public ApiResponse<String> stopContainer(@PathVariable String id) {
        containerService.stopContainer(id);
        return ApiResponse.success("停止成功");
    }


    @Operation(summary = "重启容器", description = "根据ID重启容器")
    @PostMapping("/restart/{id}")
    public ApiResponse<String> restartContainer(@PathVariable String id) {
        containerService.restartContainer(id);
        return ApiResponse.success("重启成功");
    }

    @Operation(summary = "获取容器日志", description = "根据ID获取容器的日志")
    @GetMapping("/{id}/logs")
    public ApiResponse<String> getContainerLogs(@PathVariable String id, @RequestParam(defaultValue = "100") int tail, @RequestParam(defaultValue = "false") boolean follow, @RequestParam(defaultValue = "false") boolean timestamps) {
        return ApiResponse.success(containerService.getContainerLogs(id, tail, follow, timestamps));
    }






}