package com.dsm.websocket.callback;

/**
 * 拉取镜像回调接口
 */
public interface PullImageCallback {
    /**
     * 进度回调
     * @param progress 进度百分比
     * @param status 状态信息
     */
    void onProgress(int progress, String status);
    
    /**
     * 完成回调
     */
    void onComplete();
    
    /**
     * 错误回调
     * @param error 错误信息
     */
    void onError(String error);
} 