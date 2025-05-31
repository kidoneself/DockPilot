package com.dockpilot.controller;

import com.dockpilot.common.annotation.Anonymous;
import com.dockpilot.model.dto.WebServerDTO;
import com.dockpilot.model.vo.WebServerVO;
import com.dockpilot.model.vo.CategoryVO;
import com.dockpilot.service.http.WebServerService;
import com.dockpilot.utils.ApiResponse;
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

    @Anonymous
    @Operation(summary = "获取所有Web服务")
    @GetMapping
    public ApiResponse<List<WebServerVO>> listAll() {
        List<WebServerVO> list = webServerService.listAll();
        return ApiResponse.success(list);
    }

    @Anonymous
    @Operation(summary = "根据分类ID获取Web服务")
    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<WebServerVO>> listByCategoryId(
            @Parameter(description = "分类ID") @PathVariable Integer categoryId) {
        List<WebServerVO> list = webServerService.listByCategoryId(categoryId);
        return ApiResponse.success(list);
    }

    @Anonymous
    @Operation(summary = "根据分类名称获取Web服务")
    @GetMapping("/category/name/{categoryName}")
    public ApiResponse<List<WebServerVO>> listByCategoryName(
            @Parameter(description = "分类名称") @PathVariable String categoryName) {
        List<WebServerVO> list = webServerService.listByCategoryName(categoryName);
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

    @Anonymous
    @Operation(summary = "获取所有分类")
    @GetMapping("/categories")
    public ApiResponse<List<CategoryVO>> listAllCategories() {
        List<CategoryVO> categories = webServerService.listAllCategories();
        return ApiResponse.success(categories);
    }

    @Anonymous
    @Operation(summary = "获取分类详情")
    @GetMapping("/categories/{id}")
    public ApiResponse<CategoryVO> getCategoryById(@Parameter(description = "分类ID") @PathVariable Integer id) {
        CategoryVO category = webServerService.getCategoryById(id);
        return category != null ? ApiResponse.success(category) : ApiResponse.error("分类不存在");
    }

    @Operation(summary = "切换收藏状态")
    @PutMapping("/{id}/favorite")
    public ApiResponse<Void> toggleFavorite(@Parameter(description = "服务ID") @PathVariable String id) {
        webServerService.toggleFavorite(id);
        return ApiResponse.success();
    }

    @Anonymous
    @Operation(summary = "获取收藏列表")
    @GetMapping("/favorites")
    public ApiResponse<List<WebServerVO>> getFavorites() {
        List<WebServerVO> favorites = webServerService.getFavorites();
        return ApiResponse.success(favorites);
    }
} 