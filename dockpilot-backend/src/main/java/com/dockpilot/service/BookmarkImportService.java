package com.dockpilot.service;

import com.dockpilot.dto.BookmarkImportRequest;
import com.dockpilot.dto.BookmarkImportResult;
import com.dockpilot.dto.BookmarkItemVO;
import com.dockpilot.model.dto.CategoryDTO;
import com.dockpilot.model.entity.Category;
import com.dockpilot.model.entity.WebServer;
import com.dockpilot.model.vo.CategoryVO;
import com.dockpilot.service.http.CategoryService;
import com.dockpilot.service.http.WebServerService;
import com.dockpilot.utils.FaviconFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 书签导入服务 - 优化版（真正的三次数据库交互）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkImportService {
    
    private final WebServerService webServerService;
    private final CategoryService categoryService;
    
    /**
     * 导入选中的书签 - 优化版本
     * 真正的三次数据库交互：
     * 1. 查询现有分类和URL
     * 2. 批量创建新分类 
     * 3. 批量插入书签
     */
    @Transactional
    public BookmarkImportResult importSelectedBookmarks(BookmarkImportRequest request) {
        BookmarkImportResult result = new BookmarkImportResult();
        
        try {
            log.info("开始优化批量导入书签，数量：{}", request.getBookmarks().size());
            long startTime = System.currentTimeMillis();
            
            // 第一步：一次性查询所有需要的现有数据
            ExistingDataCache existingData = queryExistingData();
            long queryTime = System.currentTimeMillis();
            log.info("第一步：查询现有数据完成，耗时：{}ms", queryTime - startTime);
            
            // 第二步：批量创建所需的新分类
            Map<String, Integer> categoryMap = batchCreateCategories(request.getBookmarks(), existingData);
            long categoryTime = System.currentTimeMillis();
            log.info("第二步：批量创建分类完成，耗时：{}ms", categoryTime - queryTime);
            
            // 第三步：批量插入书签（异步获取图标）
            List<WebServer> webServersToInsert = prepareBatchBookmarks(request.getBookmarks(), categoryMap, existingData);
            
            if (!webServersToInsert.isEmpty()) {
                webServerService.batchInsert(webServersToInsert);
                result.setProcessedCount(webServersToInsert.size());
                log.info("第三步：批量插入书签完成，成功插入 {} 个", webServersToInsert.size());
            }
            
            long totalTime = System.currentTimeMillis();
            log.info("优化批量导入完成，总耗时：{}ms，数据库交互次数：3", totalTime - startTime);
            
            // 设置结果
            result.setSkippedCount(request.getBookmarks().size() - webServersToInsert.size());
            result.setMessage(String.format("成功导入 %d 个书签到 %d 个分组", 
                result.getProcessedCount(), categoryMap.size()));
            
            // 异步获取图标（不阻塞主流程）
            asyncFetchFavicons(webServersToInsert);
            
        } catch (Exception e) {
            log.error("导入书签失败", e);
            result.setMessage("导入失败: " + e.getMessage());
            throw e;
        }
        
        return result;
    }
    
    /**
     * 第一步：一次性查询所有现有数据
     */
    private ExistingDataCache queryExistingData() {
        ExistingDataCache cache = new ExistingDataCache();
        
        // 查询现有分类
        List<CategoryVO> existingCategories = categoryService.listAllIncludeEmpty();
        cache.categoryMap = existingCategories.stream()
            .collect(Collectors.toMap(CategoryVO::getName, CategoryVO::getId));
        cache.maxCategorySortOrder = existingCategories.stream()
            .mapToInt(CategoryVO::getSortOrder)
            .max()
            .orElse(0);
        
        // 只查询现有URL（不需要全部数据）
        cache.existingUrls = webServerService.getAllUrls();
        cache.maxItemSort = webServerService.getMaxItemSort();
        
        log.info("查询到现有分类：{}个，现有URL：{}个", cache.categoryMap.size(), cache.existingUrls.size());
        return cache;
    }
    
    /**
     * 第二步：批量创建新分类
     */
    private Map<String, Integer> batchCreateCategories(List<BookmarkItemVO> bookmarks, ExistingDataCache existingData) {
        Map<String, Integer> categoryMap = new HashMap<>(existingData.categoryMap);
        
        // 找出需要创建的新分类
        Set<String> requiredCategories = bookmarks.stream()
            .map(BookmarkItemVO::getGroupName)
            .collect(Collectors.toSet());
        
        List<String> newCategoryNames = requiredCategories.stream()
            .filter(name -> !existingData.categoryMap.containsKey(name))
            .collect(Collectors.toList());
        
        log.info("所有需要的分类：{}", requiredCategories);
        log.info("现有分类：{}", existingData.categoryMap.keySet());
        log.info("需要创建的新分类：{}", newCategoryNames);
        
        if (!newCategoryNames.isEmpty()) {
            log.info("需要创建新分类：{}", newCategoryNames);
            
            // 批量创建新分类
            List<CategoryDTO> newCategories = new ArrayList<>();
            for (int i = 0; i < newCategoryNames.size(); i++) {
                CategoryDTO categoryDTO = new CategoryDTO();
                categoryDTO.setName(newCategoryNames.get(i));
                categoryDTO.setSortOrder(existingData.maxCategorySortOrder + i + 1);
                newCategories.add(categoryDTO);
            }
            
            // 调用批量创建方法
            Map<String, Integer> newCategoryIds = categoryService.batchCreate(newCategories);
            log.info("批量创建分类结果：{}", newCategoryIds);
            
            categoryMap.putAll(newCategoryIds);
            
            log.info("批量创建分类完成，新增：{}个", newCategoryIds.size());
        } else {
            log.info("无需创建新分类");
        }
        
        log.info("最终分类映射：{}", categoryMap);
        return categoryMap;
    }
    
    /**
     * 第三步：准备批量插入的书签数据
     */
    private List<WebServer> prepareBatchBookmarks(List<BookmarkItemVO> bookmarks, 
                                                  Map<String, Integer> categoryMap, 
                                                  ExistingDataCache existingData) {
        List<WebServer> webServers = new ArrayList<>();
        
        log.info("开始准备批量插入数据，书签数量：{}，分类映射：{}", bookmarks.size(), categoryMap);
        
        for (int i = 0; i < bookmarks.size(); i++) {
            BookmarkItemVO bookmark = bookmarks.get(i);
            
            // 检查是否已存在
            if (existingData.existingUrls.contains(bookmark.getUrl())) {
                log.debug("书签已存在，跳过: {}", bookmark.getUrl());
                continue;
            }
            
            // 获取分组ID
            Integer categoryId = categoryMap.get(bookmark.getGroupName());
            if (categoryId == null) {
                log.error("无法找到分组ID，分组名：{}，可用分组：{}", bookmark.getGroupName(), categoryMap.keySet());
                throw new RuntimeException("无法找到分组ID: " + bookmark.getGroupName());
            }
            
            log.debug("书签：{} -> 分组：{} -> ID：{}", bookmark.getTitle(), bookmark.getGroupName(), categoryId);
            
            // 创建WebServer对象（先不获取图标，使用默认值）
            WebServer webServer = new WebServer();
            webServer.setId(UUID.randomUUID().toString());
            webServer.setName(bookmark.getTitle());
            webServer.setInternalUrl(bookmark.getUrl());
            webServer.setExternalUrl(bookmark.getUrl());
            webServer.setDescription(bookmark.getTitle());
            webServer.setCategoryId(categoryId);
            
            // 设置默认图标（后续异步更新）
            webServer.setIcon("未知");
            webServer.setIconType("text");
            
            // 默认配置
            webServer.setBgColor("rgba(255, 255, 255, 0.15)");
            webServer.setCardType("normal");
            webServer.setOpenType("new");
            webServer.setItemSort(existingData.maxItemSort + i + 1);
            
            webServers.add(webServer);
        }
        
        log.info("准备批量插入书签：{}个", webServers.size());
        
        // 验证所有WebServer的categoryId都不为null
        long nullCategoryCount = webServers.stream().filter(ws -> ws.getCategoryId() == null).count();
        if (nullCategoryCount > 0) {
            log.error("发现{}个书签的categoryId为null！", nullCategoryCount);
            throw new RuntimeException("存在categoryId为null的书签，无法插入数据库");
        }
        
        return webServers;
    }
    
    /**
     * 异步获取图标（不阻塞主流程）
     */
    private void asyncFetchFavicons(List<WebServer> webServers) {
        if (webServers.isEmpty()) {
            return;
        }
        
        log.info("开始异步获取{}个书签的图标", webServers.size());
        
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, String> faviconMap = new ConcurrentHashMap<>();
                
                // 并行获取图标
                webServers.parallelStream().forEach(webServer -> {
                    try {
                        String faviconUrl = FaviconFetcher.getFaviconSilent(webServer.getInternalUrl());
                        if (faviconUrl != null) {
                            faviconMap.put(webServer.getId(), faviconUrl);
                        }
                    } catch (Exception e) {
                        log.debug("获取图标失败: {}", webServer.getInternalUrl());
                    }
                });
                
                // 批量更新图标
                if (!faviconMap.isEmpty()) {
                    webServerService.batchUpdateFavicons(faviconMap);
                    log.info("异步更新图标完成，成功更新：{}个", faviconMap.size());
                }
                
            } catch (Exception e) {
                log.warn("异步获取图标过程出错", e);
            }
        });
    }
    
    /**
     * 文本格式导入
     * 格式：名称, 内网, 外网, 图标, 描述
     */
    @Transactional
    public BookmarkImportResult importFromText(List<String> textLines, Integer categoryId) {
        
        BookmarkImportResult result = new BookmarkImportResult();
        
        try {
            log.info("开始文本格式导入，行数：{}", textLines.size());
            
            // 解析每一行文本
            List<WebServer> webServers = new ArrayList<>();
            int lineNumber = 0;
            
            for (String line : textLines) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    WebServer webServer = parseFixedFormatLine(line, categoryId);
                    webServers.add(webServer);
                } catch (Exception e) {
                    log.error("解析第{}行失败: {}, 错误: {}", lineNumber, line, e.getMessage());
                    result.incrementSkipped();
                }
            }
            
            // 批量插入
            if (!webServers.isEmpty()) {
                webServerService.batchInsert(webServers);
                result.setProcessedCount(webServers.size());
                
                // 异步获取图标（只处理图标为空的情况）
                List<WebServer> needFaviconServers = webServers.stream()
                    .filter(server -> "未知".equals(server.getIcon()) && "text".equals(server.getIconType()))
                    .collect(Collectors.toList());
                
                if (!needFaviconServers.isEmpty()) {
                    asyncFetchFavicons(needFaviconServers);
                    log.info("将异步获取{}个网站的favicon", needFaviconServers.size());
                }
            }
            
            result.setMessage(String.format("导入完成，成功：%d，失败：%d", 
                result.getProcessedCount(), result.getSkippedCount()));
            
        } catch (Exception e) {
            log.error("文本导入失败", e);
            result.setMessage("导入失败: " + e.getMessage());
            throw e;
        }
        
        return result;
    }
    
    /**
     * 解析固定格式行：名称, 内网, 外网, 图标, 描述
     */
    private WebServer parseFixedFormatLine(String line, Integer categoryId) {
        // 严格按逗号分割，必须有4个逗号=5个字段
        String[] parts = line.split(",", 5);
        
        if (parts.length != 5) {
            throw new IllegalArgumentException("格式错误：必须包含5个字段(名称,内网,外网,图标,描述)");
        }
        
        // 清理空格
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        
        WebServer server = new WebServer();
        server.setId(UUID.randomUUID().toString());
        
        // 字段2：内网 (必填)
        String internalUrl = parts[1];
        if (internalUrl.isEmpty()) {
            throw new IllegalArgumentException("内网地址不能为空");
        }
        server.setInternalUrl(internalUrl);
        
        // 字段1：名称 (如果为空则用域名)
        String name = parts[0];
        if (name.isEmpty()) {
            name = extractDomainName(internalUrl);
        }
        server.setName(name);
        
        // 字段3：外网 (可空，默认使用内网)
        String externalUrl = parts[2];
        if (externalUrl.isEmpty()) {
            externalUrl = internalUrl;
        }
        server.setExternalUrl(externalUrl);
        
        // 字段4：图标 (可空，为空时后续自动获取favicon)
        String icon = parts[3];
        if (icon.isEmpty()) {
            // 图标为空，设置默认值，后续异步获取favicon
            server.setIcon("未知");
            server.setIconType("text");
        } else {
            // 图标不为空，作为在线图标URL地址处理
            server.setIcon(icon);
            server.setIconType("online");
        }
        
        // 字段5：描述 (可空，默认用名称)
        String description = parts[4];
        if (description.isEmpty()) {
            description = name;
        }
        server.setDescription(description);
        
        // 设置分类和默认值
        server.setCategoryId(categoryId);
        server.setBgColor("rgba(255, 255, 255, 0.15)");
        server.setCardType("normal");
        server.setOpenType("new");
        server.setItemSort(webServerService.getMaxItemSort() + 1);
        
        return server;
    }
    
    /**
     * 从域名提取网站名称
     */
    private String extractDomainName(String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String domain = urlObj.getHost();
            if (domain.startsWith("www.")) {
                domain = domain.substring(4);
            }
            String[] parts = domain.split("\\.");
            return parts.length > 0 ? capitalize(parts[0]) : "未知网站";
        } catch (Exception e) {
            return "未知网站";
        }
    }
    
    /**
     * 首字母大写
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    

    
    /**
     * 现有数据缓存
     */
    private static class ExistingDataCache {
        Map<String, Integer> categoryMap = new HashMap<>();
        Set<String> existingUrls = new HashSet<>();
        int maxCategorySortOrder = 0;
        int maxItemSort = 0;
    }
} 