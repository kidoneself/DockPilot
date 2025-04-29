package com.dsm.pojo.param;

import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

@Data
public class TemplateQueryParam {
    @Min(1)
    private Integer page = 1;
    
    @Min(1)
    @Max(100)
    private Integer size = 10;
    
    private String category;
    
    private String search;
    
    @Pattern(regexp = "^(sort_weight|created_at|updated_at)$")
    private String sortBy = "sort_weight";
    
    @Pattern(regexp = "^(asc|desc)$")
    private String sortDirection = "desc";
    
    public Integer getOffset() {
        return (page - 1) * size;
    }
} 