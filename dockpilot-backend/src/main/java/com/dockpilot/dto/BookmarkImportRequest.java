package com.dockpilot.dto;

import lombok.Data;
import java.util.List;

/**
 * 书签导入请求
 */
@Data
public class BookmarkImportRequest {
    private List<BookmarkItemVO> bookmarks;
} 