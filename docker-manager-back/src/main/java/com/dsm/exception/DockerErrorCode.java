package com.dsm.exception;

public enum DockerErrorCode {
    CONTAINER_NOT_FOUND("容器不存在"),
    IMAGE_NOT_FOUND("镜像不存在"),
    CONTAINER_ALREADY_RUNNING("容器已在运行中"),
    CONTAINER_ALREADY_STOPPED("容器已停止"),
    PORT_CONFLICT("端口已经占用冲突"),
    IMAGE_PULL_FAILED("拉取镜像失败"),
    INVALID_CONFIG("容器配置无效"),
    DOCKER_DAEMON_UNAVAILABLE("Docker 守护进程不可用"),
    CONTAINER_NAME_CONFLICT("容器名称重复"),
    MOUNT_PATH_NOT_SHARED("映射宿主机路径不存在或者无权限"),
    UNKNOWN_ERROR("未知错误");

    private final String message;

    DockerErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}