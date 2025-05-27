package com.dockpilot.service.http;

import com.dockpilot.model.PageResult;
import com.dockpilot.model.Template;
import com.dockpilot.model.TemplateQueryParam;

public interface AppStoreService {


    PageResult<Template> getTemplates(TemplateQueryParam param);

    Template getTemplate(String id);


}