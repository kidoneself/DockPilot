package com.dsm.service;

import com.dsm.pojo.common.PageResult;
import com.dsm.pojo.entity.Template;
import com.dsm.pojo.param.TemplateQueryParam;

public interface AppStoreService {


    PageResult<Template> getTemplates(TemplateQueryParam param);

    Template getTemplate(String id);





} 