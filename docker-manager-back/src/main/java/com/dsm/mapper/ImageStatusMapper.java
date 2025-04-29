package com.dsm.mapper;

import com.dsm.pojo.entity.ImageStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 镜像状态Mapper接口
 */
@Mapper
public interface ImageStatusMapper {

    /**
     * 获取所有镜像状态记录
     *
     * @return 镜像状态列表
     */
    List<ImageStatus> selectAll();

    /**
     * 根据ID查询镜像状态
     *
     * @param id 主键ID
     * @return 镜像状态
     */
    ImageStatus selectById(Long id);

    /**
     * 根据镜像名称和标签查询状态
     *
     * @param name 镜像名称
     * @param tag  镜像标签
     * @return 镜像状态
     */
    ImageStatus selectByNameAndTag(@Param("name") String name, @Param("tag") String tag);

    /**
     * 插入镜像状态记录
     *
     * @param imageStatus 镜像状态
     * @return 影响行数
     */
    int insert(ImageStatus imageStatus);

    /**
     * 更新镜像状态记录
     *
     * @param imageStatus 镜像状态
     * @return 影响行数
     */
    int update(ImageStatus imageStatus);

    /**
     * 更新镜像的远程创建时间和更新状态
     *
     * @param id               主键ID
     * @param remoteCreateTime 远程创建时间
     * @param needUpdate       是否需要更新
     * @param lastChecked      检查时间（时间戳字符串）
     * @return 影响行数
     */
    int updateRemoteCreateTime(@Param("id") Long id,
                               @Param("remoteCreateTime") String remoteCreateTime,
                               @Param("needUpdate") Boolean needUpdate,
                               @Param("lastChecked") String lastChecked);

    /**
     * 更新本地创建时间
     *
     * @param id              主键ID
     * @param localCreateTime 本地创建时间
     * @return 影响行数
     */
    int updateLocalCreateTime(@Param("id") Long id, @Param("localCreateTime") String localCreateTime);

    /**
     * 根据ID删除镜像状态记录
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 根据镜像名称和标签删除状态记录
     *
     * @param name 镜像名称
     * @param tag  镜像标签
     * @return 影响行数
     */
    int deleteByNameAndTag(@Param("name") String name, @Param("tag") String tag);
} 