package com.dockpilot.service.http.impl;

import com.dockpilot.mapper.CategoryMapper;
import com.dockpilot.model.dto.CategoryDTO;
import com.dockpilot.model.entity.Category;
import com.dockpilot.model.vo.CategoryVO;
import com.dockpilot.service.http.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

/**
 * 分类服务实现类
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

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

    @Override
    @Transactional
    public Map<String, Integer> batchCreate(List<CategoryDTO> categories) {
        Map<String, Integer> result = new HashMap<>();
        
        if (categories == null || categories.isEmpty()) {
            return result;
        }
        
        // SQLite批量插入时useGeneratedKeys可能不工作，改为逐个插入
        for (CategoryDTO categoryDTO : categories) {
            try {
                // 检查分类名称是否已存在
                Category existing = categoryMapper.selectByName(categoryDTO.getName());
                if (existing != null) {
                    // 如果已存在，直接使用现有ID
                    result.put(categoryDTO.getName(), existing.getId());
                    log.info("分类已存在，跳过创建: {}, ID: {}", categoryDTO.getName(), existing.getId());
                    continue;
                }
                
                // 创建新分类
                Category entity = new Category();
                BeanUtils.copyProperties(categoryDTO, entity);
                
                // 执行插入操作
                int insertCount = categoryMapper.insert(entity);
                if (insertCount > 0 && entity.getId() != null) {
                    result.put(entity.getName(), entity.getId());
                    log.info("成功创建分类: {}, ID: {}", entity.getName(), entity.getId());
                } else {
                    log.error("插入分类失败或未获取到ID: {}", categoryDTO.getName());
                    throw new RuntimeException("创建分类失败: " + categoryDTO.getName());
                }
                
            } catch (Exception e) {
                log.error("创建分类时出错: {}", categoryDTO.getName(), e);
                throw new RuntimeException("创建分类失败: " + categoryDTO.getName() + ", 错误: " + e.getMessage());
            }
        }
        
        log.info("批量创建分类完成，成功创建: {}个", result.size());
        return result;
    }
} 