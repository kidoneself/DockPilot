package com.dockpilot.mapper;

import com.dockpilot.model.Template;
import com.dockpilot.model.TemplateQueryParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TemplateMapper {
    /**
     * 分页查询模板列表
     */
    List<Template> selectTemplates(@Param("param") TemplateQueryParam param);

    /**
     * 查询模板总数
     */
    long countTemplates(@Param("param") TemplateQueryParam param);

    /**
     * 根据ID查询模板
     */
    Template selectTemplateById(@Param("id") String id);

    /**
     * 插入模板
     */
    int insert(Template template);

    /**
     * 根据ID删除模板
     */
    int deleteById(@Param("id") String id);
}