package com.dsm.service.http.impl;

import com.dsm.mapper.TemplateMapper;
import com.dsm.model.PageResult;
import com.dsm.model.Template;
import com.dsm.model.TemplateQueryParam;
import com.dsm.service.http.AppStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 模板服务实现类
 * 管理容器模板的创建、查询、更新和删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppStoreServiceImpl implements AppStoreService {
    private final TemplateMapper templateMapper;

    @Override
    public PageResult<Template> getTemplates(TemplateQueryParam param) {
        // 查询数据
        var records = templateMapper.selectTemplates(param);
        // 查询总数
        var total = templateMapper.countTemplates(param);

        // 构建分页结果
        var result = new PageResult<Template>();
        result.setRecords(records);
        result.setTotal(total);
        result.setSize(param.getSize());
        result.setCurrent(param.getPage());
        result.setPages((total + param.getSize() - 1) / param.getSize());
        return result;
    }

    @Override
    public Template getTemplate(String id) {
        return templateMapper.selectTemplateById(id);
    }


}