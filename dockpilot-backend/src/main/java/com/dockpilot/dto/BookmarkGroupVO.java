package com.dockpilot.dto;

import lombok.Data;
import java.util.List;

/**
 * 书签分组VO
 */
@Data
public class BookmarkGroupVO {
    private String name;
    private List<BookmarkItemVO> items;
} 