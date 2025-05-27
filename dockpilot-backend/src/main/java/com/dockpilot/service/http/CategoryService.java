package com.dockpilot.service.http;

import com.dockpilot.model.dto.CategoryDTO;
import com.dockpilot.model.vo.CategoryVO;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {
    /**
     * 新增分类
     *
     * @param dto 分类信息
     * @return 新增的分类ID
     */
    Integer create(CategoryDTO dto);

    /**
     * 删除分类
     *
     * @param id 分类ID
     */
    void delete(Integer id);

    /**
     * 更新分类信息
     *
     * @param id  分类ID
     * @param dto 分类信息
     */
    void update(Integer id, CategoryDTO dto);

    /**
     * 获取分类详情
     *
     * @param id 分类ID
     * @return 分类详情
     */
    CategoryVO getById(Integer id);

    /**
     * 获取所有分类列表（包含应用数量）- 只返回有应用的分类
     *
     * @return 分类列表
     */
    List<CategoryVO> listAll();

    /**
     * 获取所有分类列表（包含应用数量，包括空分类）- 用于分类管理
     *
     * @return 分类列表
     */
    List<CategoryVO> listAllIncludeEmpty();

    /**
     * 更新分类排序
     *
     * @param id        分类ID
     * @param sortOrder 排序值
     */
    void updateSort(Integer id, Integer sortOrder);

    /**
     * 批量更新分类排序
     *
     * @param categories 分类列表（包含ID和排序信息）
     */
    void batchUpdateSort(List<CategoryDTO> categories);
} 