package com.dsm.controller;

import com.dsm.dto.WebServerDTO;
import com.dsm.service.WebServerService;
import com.dsm.utils.ApiResponse;
import com.dsm.vo.WebServerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Web服务管理接口
 */
@Tag(name = "Web服务管理", description = "Web服务的增删改查和排序管理")
@RestController
@RequestMapping("/web-servers")
public class WebServerController {

    private final WebServerService webServerService;

    public WebServerController(WebServerService webServerService) {
        this.webServerService = webServerService;
    }

    @Operation(summary = "创建Web服务")
    @PostMapping
    public ApiResponse<String> create(@Valid @RequestBody WebServerDTO dto) {
        String id = webServerService.create(dto);
        return ApiResponse.success(id);
    }

    @Operation(summary = "删除Web服务")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "服务ID") @PathVariable String id) {
        webServerService.delete(id);
        return ApiResponse.success();
    }

        @Operation(summary = "更新Web服务")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @Parameter(description = "服务ID") @PathVariable String id,
            @Valid @RequestBody WebServerDTO dto) {
        webServerService.update(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "获取Web服务详情")
    @GetMapping("/{id}")
    public ApiResponse<WebServerVO> getById(@Parameter(description = "服务ID") @PathVariable String id) {
        WebServerVO vo = webServerService.getById(id);
        return vo != null ? ApiResponse.success(vo) : ApiResponse.error("服务不存在");
    }

    @Operation(summary = "获取所有Web服务")
    @GetMapping
    public ApiResponse<List<WebServerVO>> listAll() {
        List<WebServerVO> list = webServerService.listAll();
        return ApiResponse.success(list);
    }

    @Operation(summary = "根据分类获取Web服务")
    @GetMapping("/category/{category}")
    public ApiResponse<List<WebServerVO>> listByCategory(
            @Parameter(description = "分类名称") @PathVariable String category) {
        List<WebServerVO> list = webServerService.listByCategory(category);
        return ApiResponse.success(list);
    }

    @Operation(summary = "更新Web服务排序")
    @PutMapping("/{id}/sort")
    public ApiResponse<Void> updateSort(
            @Parameter(description = "服务ID") @PathVariable String id,
            @Parameter(description = "应用排序") @RequestParam Integer itemSort) {
        webServerService.updateSort(id, itemSort);
        return ApiResponse.success();
    }

    @Operation(summary = "批量更新Web服务排序")
    @PutMapping("/batch-sort")
    public ApiResponse<Void> batchUpdateSort(@Valid @RequestBody List<WebServerDTO> servers) {
        webServerService.batchUpdateSort(servers);
        return ApiResponse.success();
    }

    @Operation(summary = "获取所有分类")
    @GetMapping("/categories")
    public ApiResponse<List<String>> listAllCategories() {
        List<String> categories = webServerService.listAllCategories();
        return ApiResponse.success(categories);
    }
} 