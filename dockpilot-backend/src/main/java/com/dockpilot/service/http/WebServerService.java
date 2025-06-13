package com.dockpilot.service.http;

import com.dockpilot.model.dto.WebServerDTO;
import com.dockpilot.model.entity.WebServer;
import com.dockpilot.model.vo.WebServerVO;
import com.dockpilot.model.vo.CategoryVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Web服务服务接口
 */
public interface WebServerService {
    /**
     * 新增服务
     *
     * @param dto 服务信息
     * @return 新增的服务ID
     */
    String create(WebServerDTO dto);

    /**
     * 批量新增服务
     *
     * @param webServers 服务列表
     * @return 插入的数量
     */
    int batchInsert(List<WebServer> webServers);

    /**
     * 删除服务
     *
     * @param id 服务ID
     */
    void delete(String id);

    /**
     * 更新服务信息
     *
     * @param id  服务ID
     * @param dto 服务信息
     */
    void update(String id, WebServerDTO dto);

    /**
     * 获取服务详情
     *
     * @param id 服务ID
     * @return 服务详情
     */
    WebServerVO getById(String id);

    /**
     * 获取所有服务列表（包含分类信息）
     *
     * @return 服务列表
     */
    List<WebServerVO> listAll();

    /**
     * 根据分类ID获取服务列表
     *
     * @param categoryId 分类ID
     * @return 服务列表
     */
    List<WebServerVO> listByCategoryId(Integer categoryId);

    /**
     * 根据分类名称获取服务列表
     *
     * @param categoryName 分类名称
     * @return 服务列表
     */
    List<WebServerVO> listByCategoryName(String categoryName);

    /**
     * 更新服务排序
     *
     * @param id       服务ID
     * @param itemSort 应用排序
     */
    void updateSort(String id, Integer itemSort);

    /**
     * 批量更新服务排序
     *
     * @param servers 服务列表（包含ID和排序信息）
     */
    void batchUpdateSort(List<WebServerDTO> servers);

    /**
     * 获取所有分类（包含应用数量）
     *
     * @return 分类列表
     */
    List<CategoryVO> listAllCategories();

    /**
     * 获取分类详情
     *
     * @param id 分类ID
     * @return 分类详情
     */
    CategoryVO getCategoryById(Integer id);

    /**
     * 切换收藏状态
     *
     * @param id 服务ID
     */
    void toggleFavorite(String id);

    /**
     * 获取收藏列表
     *
     * @return 收藏的服务列表
     */
    List<WebServerVO> getFavorites();

    /**
     * 获取所有URL（用于去重检查）
     *
     * @return 所有URL的集合
     */
    Set<String> getAllUrls();

    /**
     * 获取最大排序值
     *
     * @return 最大排序值
     */
    int getMaxItemSort();

    /**
     * 批量更新图标
     *
     * @param faviconMap 服务ID到图标URL的映射
     */
    void batchUpdateFavicons(Map<String, String> faviconMap);
} 