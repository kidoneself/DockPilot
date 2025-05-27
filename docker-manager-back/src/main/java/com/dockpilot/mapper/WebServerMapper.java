package com.dockpilot.mapper;

import com.dockpilot.model.entity.WebServer;
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
     * 根据分类查询服务
     */
    List<WebServer> selectByCategory(@Param("category") String category);

    /**
     * 更新服务排序
     */
    int updateSort(@Param("id") String id, @Param("itemSort") Integer itemSort);

    /**
     * 批量更新服务排序
     */
    int batchUpdateSort(@Param("servers") List<WebServer> servers);

    /**
     * 获取所有分类
     */
    List<String> selectAllCategories();
} 