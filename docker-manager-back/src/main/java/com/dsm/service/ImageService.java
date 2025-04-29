package com.dsm.service;

import com.dsm.model.dto.ImageStatusDTO;
import com.dsm.pojo.dto.image.ImageInspectDTO;

import java.util.List;
import java.util.Map;

/**
 * 镜像服务接口
 * 定义镜像管理的业务逻辑
 */
public interface ImageService {


    void removeImage(String imageId, boolean removeStatus);

    void checkAllImagesStatus();

    Map<String, Object> updateImage(String image, String tag);

    List<ImageStatusDTO> listImages();

    /**
     * 获取镜像详情
     *
     * @param imageName 镜像名称（格式：name:tag）
     * @return 镜像详情信息
     */
    ImageInspectDTO getImageDetail(String imageName);
}