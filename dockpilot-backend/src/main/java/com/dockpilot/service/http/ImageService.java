package com.dockpilot.service.http;

import com.dockpilot.model.ImageInspectDTO;
import com.dockpilot.model.ImageStatusDTO;
import com.dockpilot.utils.MessageCallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    /**
     * 拉取镜像
     *
     * @param image    镜像名称
     * @param tag      镜像标签
     * @param callback 回调接口
     * @return CompletableFuture
     */
    CompletableFuture<Void> pullImage(String image, String tag, MessageCallback callback);

    /**
     * 开始拉取镜像，在数据库中记录状态
     *
     * @param imageName 镜像名称
     * @param tag       镜像标签
     */
    void startPullImage(String imageName, String tag);

    /**
     * 更新拉取进度
     *
     * @param imageName  镜像名称
     * @param tag        镜像标签
     * @param percentage 进度百分比
     * @param message    进度消息
     */
    void updatePullProgress(String imageName, String tag, int percentage, String message);

    /**
     * 拉取成功，更新状态
     *
     * @param imageName 镜像名称
     * @param tag       镜像标签
     * @param imageId   镜像ID
     */
    void completePullImage(String imageName, String tag, String imageId);

    /**
     * 拉取失败，更新状态
     *
     * @param imageName 镜像名称
     * @param tag       镜像标签
     * @param error     错误信息
     */
    void failPullImage(String imageName, String tag, String error);

    /**
     * 检查镜像是否正在拉取中
     *
     * @param imageName 镜像名称
     * @param tag       镜像标签
     * @return 是否正在拉取
     */
    boolean isPulling(String imageName, String tag);
}