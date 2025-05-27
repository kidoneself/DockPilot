package com.dockpilot.service.http.impl;

import com.dockpilot.mapper.ContainerInfoMapper;
import com.dockpilot.model.ContainerInfo;
import com.dockpilot.service.http.ContainerInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContainerInfoServiceImpl implements ContainerInfoService {

    @Autowired
    private ContainerInfoMapper containerInfoMapper;

    @Override
    @Transactional
    public void createContainerInfo(ContainerInfo containerInfo) {
        containerInfoMapper.insert(containerInfo);
    }

    @Override
    @Transactional
    public void updateContainerInfo(ContainerInfo containerInfo) {
        containerInfoMapper.update(containerInfo);
    }

    @Override
    @Transactional
    public void deleteContainerInfo(Integer id) {
        containerInfoMapper.deleteById(id);
    }

    @Override
    public ContainerInfo getContainerInfoById(Integer id) {
        return containerInfoMapper.selectById(id);
    }

    @Override
    public ContainerInfo getContainerInfoByContainerId(String containerId) {
        return containerInfoMapper.selectByContainerId(containerId);
    }

    @Override
    public List<ContainerInfo> getAllContainerInfo() {
        return containerInfoMapper.selectAll();
    }

    @Override
    @Transactional
    public void updateContainerStatus(String containerId, String status, String operationStatus) {
        containerInfoMapper.updateStatus(containerId, status, operationStatus);
    }

    @Override
    @Transactional
    public void updateContainerError(String containerId, String error) {
        containerInfoMapper.updateError(containerId, error);
    }
} 