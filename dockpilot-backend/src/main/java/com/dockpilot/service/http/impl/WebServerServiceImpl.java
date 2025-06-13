package com.dockpilot.service.http.impl;

import com.dockpilot.mapper.WebServerMapper;
import com.dockpilot.mapper.CategoryMapper;
import com.dockpilot.model.dto.WebServerDTO;
import com.dockpilot.model.entity.WebServer;
import com.dockpilot.model.entity.Category;
import com.dockpilot.model.vo.WebServerVO;
import com.dockpilot.model.vo.CategoryVO;
import com.dockpilot.service.http.WebServerService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Web服务服务实现类
 */
@Service
public class WebServerServiceImpl implements WebServerService {

    private final WebServerMapper webServerMapper;
    private final CategoryMapper categoryMapper;

    public WebServerServiceImpl(WebServerMapper webServerMapper, CategoryMapper categoryMapper) {
        this.webServerMapper = webServerMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional
    public String create(WebServerDTO dto) {
        WebServer entity = new WebServer();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(UUID.randomUUID().toString());
        webServerMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void delete(String id) {
        webServerMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void update(String id, WebServerDTO dto) {
        WebServer entity = webServerMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("服务不存在");
        }
        dto.setId(id);
        BeanUtils.copyProperties(dto, entity);
        webServerMapper.update(entity);
    }

    @Override
    public WebServerVO getById(String id) {
        WebServer entity = webServerMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        WebServerVO vo = new WebServerVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public List<WebServerVO> listAll() {
        return webServerMapper.selectAllWithCategory();
    }

    @Override
    public List<WebServerVO> listByCategoryId(Integer categoryId) {
        return webServerMapper.selectByCategoryIdWithCategory(categoryId);
    }

    @Override
    public List<WebServerVO> listByCategoryName(String categoryName) {
        return webServerMapper.selectByCategoryNameWithCategory(categoryName);
    }

    @Override
    @Transactional
    public void updateSort(String id, Integer itemSort) {
        webServerMapper.updateSort(id, itemSort);
    }

    @Override
    @Transactional
    public void batchUpdateSort(List<WebServerDTO> servers) {
        List<WebServer> entities = servers.stream()
                .map(dto -> {
                    WebServer entity = new WebServer();
                    BeanUtils.copyProperties(dto, entity);
                    return entity;
                })
                .collect(Collectors.toList());
        webServerMapper.batchUpdateSort(entities);
    }

    @Override
    public List<CategoryVO> listAllCategories() {
        return categoryMapper.selectAllWithAppCount();
    }

    @Override
    public CategoryVO getCategoryById(Integer id) {
        Category entity = categoryMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        
        // 查询该分类下的应用数量
        List<CategoryVO> categoriesWithCount = categoryMapper.selectAllWithAppCount();
        return categoriesWithCount.stream()
                .filter(category -> category.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public void toggleFavorite(String id) {
        webServerMapper.toggleFavorite(id);
    }

    @Override
    public List<WebServerVO> getFavorites() {
        return webServerMapper.selectFavorites();
    }

    @Override
    public Set<String> getAllUrls() {
        List<WebServerVO> allServers = webServerMapper.selectAllWithCategory();
        return allServers.stream()
            .flatMap(server -> Stream.of(server.getInternalUrl(), server.getExternalUrl()))
            .filter(Objects::nonNull)
            .filter(url -> !url.trim().isEmpty())
            .collect(Collectors.toSet());
    }

    @Override
    public int getMaxItemSort() {
        List<WebServerVO> allServers = webServerMapper.selectAllWithCategory();
        return allServers.stream()
            .mapToInt(WebServerVO::getItemSort)
            .max()
            .orElse(0);
    }

    @Override
    @Transactional
    public void batchUpdateFavicons(Map<String, String> faviconMap) {
        if (faviconMap == null || faviconMap.isEmpty()) {
            return;
        }
        webServerMapper.batchUpdateFavicons(faviconMap);
    }

    @Override
    @Transactional
    public int batchInsert(List<WebServer> webServers) {
        if (webServers == null || webServers.isEmpty()) {
            return 0;
        }
        return webServerMapper.batchInsert(webServers);
    }
} 