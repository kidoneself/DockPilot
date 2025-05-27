package com.dockpilot.service.http.impl;

import com.dockpilot.mapper.CategoryMapper;
import com.dockpilot.model.dto.CategoryDTO;
import com.dockpilot.model.entity.Category;
import com.dockpilot.model.vo.CategoryVO;
import com.dockpilot.service.http.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类服务实现类
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional
    public Integer create(CategoryDTO dto) {
        // 检查分类名称是否已存在
        Category existing = categoryMapper.selectByName(dto.getName());
        if (existing != null) {
            throw new RuntimeException("分类名称已存在");
        }

        Category entity = new Category();
        BeanUtils.copyProperties(dto, entity);
        
        // 如果没有设置排序值，设置为最大值+1
        if (entity.getSortOrder() == null) {
            List<Category> allCategories = categoryMapper.selectAll();
            int maxSort = allCategories.stream()
                    .mapToInt(cat -> cat.getSortOrder() != null ? cat.getSortOrder() : 0)
                    .max()
                    .orElse(0);
            entity.setSortOrder(maxSort + 1);
        }
        
        categoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        // 这里可以添加业务逻辑检查，比如是否有关联的应用
        categoryMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void update(Integer id, CategoryDTO dto) {
        Category entity = categoryMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("分类不存在");
        }

        // 检查分类名称是否已被其他分类使用
        if (!entity.getName().equals(dto.getName())) {
            Category existing = categoryMapper.selectByName(dto.getName());
            if (existing != null && !existing.getId().equals(id)) {
                throw new RuntimeException("分类名称已存在");
            }
        }

        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        categoryMapper.update(entity);
    }

    @Override
    public CategoryVO getById(Integer id) {
        // 获取包含应用数量的分类信息（包括空分类）
        List<CategoryVO> categoriesWithCount = categoryMapper.selectAllWithAppCountIncludeEmpty();
        return categoriesWithCount.stream()
                .filter(category -> category.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<CategoryVO> listAll() {
        return categoryMapper.selectAllWithAppCount();
    }

    @Override
    public List<CategoryVO> listAllIncludeEmpty() {
        return categoryMapper.selectAllWithAppCountIncludeEmpty();
    }

    @Override
    @Transactional
    public void updateSort(Integer id, Integer sortOrder) {
        categoryMapper.updateSort(id, sortOrder);
    }

    @Override
    @Transactional
    public void batchUpdateSort(List<CategoryDTO> categories) {
        List<Category> entities = categories.stream()
                .map(dto -> {
                    Category entity = new Category();
                    BeanUtils.copyProperties(dto, entity);
                    return entity;
                })
                .collect(Collectors.toList());
        categoryMapper.batchUpdateSort(entities);
    }
} 