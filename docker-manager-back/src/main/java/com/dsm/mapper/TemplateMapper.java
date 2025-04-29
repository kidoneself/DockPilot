package com.dsm.mapper;

import com.dsm.pojo.entity.Template;
import com.dsm.pojo.param.TemplateQueryParam;
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


}