package com.dockpilot.utils;

/**
 * 回调接口
 */
public interface MessageCallback {
    /**
     * 进度回调
     *
     * @param progress 进度百分比
     */
    void onProgress(int progress);

    /**
     * 日志回调
     *
     * @param log 日志信息
     */
    void onLog(String log);

    /**
     * 完成回调
     */
    void onComplete();

    /**
     * 错误回调
     *
     * @param error 错误信息
     */
    void onError(String error);
}