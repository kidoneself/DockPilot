package com.dockpilot.dto;

import lombok.Data;
import java.util.List;

/**
 * 书签解析结果
 */
@Data
public class BookmarkParseResult {
    private List<BookmarkGroupVO> groups;
    private int totalCount;
    private String message;
} 