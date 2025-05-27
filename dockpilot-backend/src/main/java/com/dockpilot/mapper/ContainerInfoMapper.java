package com.dockpilot.mapper;

import com.dockpilot.model.ContainerInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContainerInfoMapper {
    int insert(ContainerInfo containerInfo);

    int update(ContainerInfo containerInfo);

    int deleteById(Integer id);

    ContainerInfo selectById(Integer id);

    ContainerInfo selectByContainerId(String containerId);

    List<ContainerInfo> selectAll();

    int updateStatus(@Param("containerId") String containerId,
                     @Param("status") String status,
                     @Param("operationStatus") String operationStatus);

    int updateError(@Param("containerId") String containerId,
                    @Param("lastError") String lastError);
} 