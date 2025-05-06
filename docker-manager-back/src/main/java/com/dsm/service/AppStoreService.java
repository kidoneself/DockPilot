package com.dsm.service;

import com.dsm.model.PageResult;
import com.dsm.model.Template;
import com.dsm.model.TemplateQueryParam;

public interface AppStoreService {


    PageResult<Template> getTemplates(TemplateQueryParam param);

    Template getTemplate(String id);


}