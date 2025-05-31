package com.dockpilot.mapper;

import com.dockpilot.model.entity.WebServer;
import com.dockpilot.model.vo.WebServerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Web服务数据访问层
 */
@Mapper
public interface WebServerMapper {
    /**
     * 新增服务
     */
    int insert(WebServer webServer);

    /**
     * 根据ID删除服务
     */
    int deleteById(@Param("id") String id);

    /**
     * 更新服务信息
     */
    int update(WebServer webServer);

    /**
     * 根据ID查询服务
     */
    WebServer selectById(@Param("id") String id);

    /**
     * 查询所有服务
     */
    List<WebServer> selectAll();

    /**
     * 查询所有服务（包含分类信息）
     */
    List<WebServerVO> selectAllWithCategory();

    /**
     * 根据分类ID查询服务
     */
    List<WebServer> selectByCategoryId(@Param("categoryId") Integer categoryId);

    /**
     * 根据分类ID查询服务（包含分类信息）
     */
    List<WebServerVO> selectByCategoryIdWithCategory(@Param("categoryId") Integer categoryId);

    /**
     * 根据分类名称查询服务（包含分类信息）
     */
    List<WebServerVO> selectByCategoryNameWithCategory(@Param("categoryName") String categoryName);

    /**
     * 更新服务排序
     */
    int updateSort(@Param("id") String id, @Param("itemSort") Integer itemSort);

    /**
     * 批量更新服务排序
     */
    int batchUpdateSort(@Param("servers") List<WebServer> servers);

    /**
     * 切换收藏状态
     */
    int toggleFavorite(@Param("id") String id);

    /**
     * 查询收藏列表
     */
    List<WebServerVO> selectFavorites();
} 