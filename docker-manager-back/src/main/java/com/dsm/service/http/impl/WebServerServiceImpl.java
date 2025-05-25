package com.dsm.service.http.impl;

import com.dsm.mapper.WebServerMapper;
import com.dsm.model.dto.WebServerDTO;
import com.dsm.model.entity.WebServer;
import com.dsm.model.vo.WebServerVO;
import com.dsm.service.http.WebServerService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Web服务服务实现类
 */
@Service
public class WebServerServiceImpl implements WebServerService {

    private final WebServerMapper webServerMapper;

    public WebServerServiceImpl(WebServerMapper webServerMapper) {
        this.webServerMapper = webServerMapper;
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
        List<WebServer> entities = webServerMapper.selectAll();
        return entities.stream()
                .map(entity -> {
                    WebServerVO vo = new WebServerVO();
                    BeanUtils.copyProperties(entity, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<WebServerVO> listByCategory(String category) {
        List<WebServer> entities = webServerMapper.selectByCategory(category);
        return entities.stream()
                .map(entity -> {
                    WebServerVO vo = new WebServerVO();
                    BeanUtils.copyProperties(entity, vo);
                    return vo;
                })
                .collect(Collectors.toList());
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
    public List<String> listAllCategories() {
        return webServerMapper.selectAllCategories();
    }
} 