package com.dockpilot.mapper;

import com.dockpilot.model.application.Application;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 应用数据访问层接口
 */
@Mapper
public interface ApplicationMapper {
    
    /**
     * 查询应用列表
     * @param category 分类筛选（可选）
     * @param keyword 关键词搜索（可选）
     * @return 应用列表
     */
    @Select("<script>" +
            "SELECT * FROM applications " +
            "WHERE 1=1 " +
            "<if test='category != null and category != \"\"'>" +
            "AND category = #{category} " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    List<Application> findApplications(@Param("category") String category, @Param("keyword") String keyword);
    
    /**
     * 根据ID查询应用
     * @param id 应用ID
     * @return 应用信息
     */
    @Select("SELECT * FROM applications WHERE id = #{id}")
    Application findById(@Param("id") Long id);
    
    /**
     * 根据文件哈希查询应用
     * @param fileHash 文件哈希值
     * @return 应用信息
     */
    @Select("SELECT * FROM applications WHERE file_hash = #{fileHash}")
    Application findByFileHash(@Param("fileHash") String fileHash);
    
    /**
     * 插入应用
     * @param application 应用信息
     */
    @Insert("INSERT INTO applications (name, description, category, icon_url, yaml_content, file_hash, env_vars, created_at, updated_at) " +
            "VALUES (#{name}, #{description}, #{category}, #{iconUrl}, #{yamlContent}, #{fileHash}, #{envVars}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Application application);
    
    /**
     * 删除应用
     * @param id 应用ID
     */
    @Delete("DELETE FROM applications WHERE id = #{id}")
    void deleteById(@Param("id") Long id);
    
    /**
     * 获取所有分类
     * @return 分类列表
     */
    @Select("SELECT DISTINCT category FROM applications WHERE category IS NOT NULL ORDER BY category")
    List<String> findDistinctCategories();
} 