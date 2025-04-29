package com.dsm.controller;

import com.dsm.model.dto.AppStoreAppDTO;
import com.dsm.pojo.common.PageResult;
import com.dsm.pojo.entity.Template;
import com.dsm.pojo.param.TemplateQueryParam;
import com.dsm.service.AppStoreService;
import com.dsm.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/app-store/apps")
@Tag(name = "应用商店", description = "Docker应用商店管理接口")
@RequiredArgsConstructor
public class AppStoreController {

    @Autowired
    private AppStoreService appStoreService;

    @Operation(summary = "获取应用商店列表", description = "分页获取应用商店列表，支持分类筛选和搜索")
    @GetMapping
    public ApiResponse<PageResult<AppStoreAppDTO>> getAppList(@Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page, @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize, @Parameter(description = "分类") @RequestParam(required = false) String category) {
        TemplateQueryParam param = new TemplateQueryParam();
        param.setPage(page);
        param.setSize(pageSize);
        param.setCategory(category);
        PageResult<Template> templates = appStoreService.getTemplates(param);

        // 转换为前端需要的格式
        List<AppStoreAppDTO> apps = templates.getRecords().stream().map(template -> {
            AppStoreAppDTO app = new AppStoreAppDTO();
            BeanUtils.copyProperties(template, app);
            return app;
        }).collect(Collectors.toList());

        PageResult<AppStoreAppDTO> result = new PageResult<>();
        result.setRecords(apps);
        result.setTotal(templates.getTotal());
        result.setSize(templates.getSize());
        result.setCurrent(templates.getCurrent());
        result.setPages(templates.getPages());

        return ApiResponse.success(result);
    }

    @Operation(summary = "获取应用详情", description = "根据ID获取应用详情，包含完整的模板配置")
    @GetMapping("/{id}")
    public ApiResponse<AppStoreAppDTO> getAppDetail(@Parameter(description = "应用ID") @PathVariable String id) {
        Template template = appStoreService.getTemplate(id);
        AppStoreAppDTO detail = new AppStoreAppDTO();
        BeanUtils.copyProperties(template, detail);
        // 设置服务和参数
        var content = template.getTemplateContent();
        if (content != null) {
            detail.setServices(content.getServices());
            detail.setParameters(content.getParameters());
        }
        return ApiResponse.success(detail);
    }

}