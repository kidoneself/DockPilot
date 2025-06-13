package com.dockpilot.mapper;

import com.dockpilot.model.entity.Category;
import com.dockpilot.model.vo.CategoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类数据访问层
 */
@Mapper
public interface CategoryMapper {
    /**
     * 新增分类
     */
    int insert(Category category);

    /**
     * 批量新增分类
     */
    int batchInsert(@Param("categories") List<Category> categories);

    /**
     * 根据ID删除分类
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 更新分类信息
     */
    int update(Category category);

    /**
     * 根据ID查询分类
     */
    Category selectById(@Param("id") Integer id);

    /**
     * 查询所有分类（按排序权重排序）
     */
    List<Category> selectAll();

    /**
     * 查询所有分类及应用数量（按排序权重排序）- 只返回有应用的分类
     */
    List<CategoryVO> selectAllWithAppCount();

    /**
     * 查询所有分类及应用数量（包括空分类）- 用于分类管理
     */
    List<CategoryVO> selectAllWithAppCountIncludeEmpty();

    /**
     * 更新分类排序
     */
    int updateSort(@Param("id") Integer id, @Param("sortOrder") Integer sortOrder);

    /**
     * 批量更新分类排序
     */
    int batchUpdateSort(@Param("categories") List<Category> categories);

    /**
     * 根据名称查询分类
     */
    Category selectByName(@Param("name") String name);
} 