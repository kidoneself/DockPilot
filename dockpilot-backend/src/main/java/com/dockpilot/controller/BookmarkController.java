package com.dockpilot.controller;

import com.dockpilot.dto.BookmarkImportRequest;
import com.dockpilot.dto.BookmarkImportResult;
import com.dockpilot.service.BookmarkImportService;
import com.dockpilot.utils.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * 文本格式导入
     */
    @PostMapping("/import-text")
    public ApiResponse<BookmarkImportResult> importFromText(
            @RequestBody TextImportRequest request) {
        
        if (request.getTextLines() == null || request.getTextLines().isEmpty()) {
            return ApiResponse.error("导入内容不能为空");
        }
        
        if (request.getCategoryId() == null) {
            return ApiResponse.error("必须指定分类");
        }
        
        log.info("开始文本格式导入，行数：{}", request.getTextLines().size());
        
        BookmarkImportResult result = bookmarkImportService.importFromText(
            request.getTextLines(), 
            request.getCategoryId()
        );
        
        log.info("文本导入完成，成功：{}，失败：{}", 
                result.getProcessedCount(), result.getSkippedCount());
        
        return ApiResponse.success(result);
    }
}

/**
 * 文本导入请求
 */
@Data
class TextImportRequest {
    private List<String> textLines;
    private Integer categoryId;
} 