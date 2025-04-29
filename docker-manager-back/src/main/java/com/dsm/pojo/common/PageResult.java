package com.dsm.pojo.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long size;
    private long current;
    private long pages;
} 