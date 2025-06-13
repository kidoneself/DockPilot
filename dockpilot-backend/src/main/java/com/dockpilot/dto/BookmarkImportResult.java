package com.dockpilot.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 书签导入结果
 */
@Data
public class BookmarkImportResult {
    private int processedCount = 0;
    private int skippedCount = 0;
    private List<String> createdCategories = new ArrayList<>();
    private String message;
    
    public void incrementProcessed() {
        this.processedCount++;
    }
    
    public void incrementSkipped() {
        this.skippedCount++;
    }
    
    public void addCreatedCategory(String categoryName) {
        if (!createdCategories.contains(categoryName)) {
            createdCategories.add(categoryName);
        }
    }
} 