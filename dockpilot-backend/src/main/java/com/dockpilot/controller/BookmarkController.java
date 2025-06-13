package com.dockpilot.controller;

import com.dockpilot.dto.BookmarkImportRequest;
import com.dockpilot.dto.BookmarkImportResult;
import com.dockpilot.service.BookmarkImportService;
import com.dockpilot.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 书签导入控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/bookmark")
@RequiredArgsConstructor
public class BookmarkController {
    
    private final BookmarkImportService bookmarkImportService;
    
    /**
     * 导入选中的书签
     */
    @PostMapping("/import")
    public ApiResponse<BookmarkImportResult> importBookmarks(
            @RequestBody BookmarkImportRequest request) {
        
        if (request.getBookmarks() == null || request.getBookmarks().isEmpty()) {
            return ApiResponse.error("没有选择要导入的书签");
        }
        
        log.info("开始导入书签，数量：{}", request.getBookmarks().size());
        
        BookmarkImportResult result = bookmarkImportService.importSelectedBookmarks(request);
        
        log.info("书签导入完成，成功：{}，跳过：{}", 
                result.getProcessedCount(), result.getSkippedCount());
        
        return ApiResponse.success(result);
    }
} 