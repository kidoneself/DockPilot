package com.dockpilot.controller;

import com.dockpilot.model.dto.CategoryDTO;
import com.dockpilot.model.vo.CategoryVO;
import com.dockpilot.service.http.CategoryService;
import com.dockpilot.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 分类管理接口
 */
@Tag(name = "分类管理", description = "应用分类的增删改查和排序管理")
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "创建分类")
    @PostMapping
    public ApiResponse<Integer> create(@Valid @RequestBody CategoryDTO dto) {
        Integer id = categoryService.create(dto);
        return ApiResponse.success(id);
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @Parameter(description = "分类ID") @PathVariable Integer id,
            @Valid @RequestBody CategoryDTO dto) {
        categoryService.update(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "分类ID") @PathVariable Integer id) {
        categoryService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "更新分类排序")
    @PutMapping("/{id}/sort")
    public ApiResponse<Void> updateSort(
            @Parameter(description = "分类ID") @PathVariable Integer id,
            @Parameter(description = "排序值") @RequestParam Integer sortOrder) {
        categoryService.updateSort(id, sortOrder);
        return ApiResponse.success();
    }

    @Operation(summary = "批量更新分类排序")
    @PutMapping("/batch-sort")
    public ApiResponse<Void> batchUpdateSort(@Valid @RequestBody List<CategoryDTO> categories) {
        categoryService.batchUpdateSort(categories);
        return ApiResponse.success();
    }
} 