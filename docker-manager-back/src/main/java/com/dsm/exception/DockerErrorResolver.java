package com.dsm.exception;

public class DockerErrorResolver {

    public static DockerOperationException resolve(String action, String identifier, Exception e) {
        String message = e.getMessage();
        DockerErrorCode errorCode = DockerErrorCode.UNKNOWN_ERROR;

        if (message != null) {
            if (message.contains("No such container")) {
                errorCode = DockerErrorCode.CONTAINER_NOT_FOUND;
            } else if (message.contains("No such image")) {
                errorCode = DockerErrorCode.IMAGE_NOT_FOUND;
            } else if (message.contains("is already running")) {
                errorCode = DockerErrorCode.CONTAINER_ALREADY_RUNNING;
            } else if (message.contains("Status 304")) {
                errorCode = DockerErrorCode.CONTAINER_ALREADY_STOPPED;
            } else if (message.contains("port is already allocated")) {
                errorCode = DockerErrorCode.PORT_CONFLICT;
            } else if (message.contains("manifest for") && message.contains("not found")) {
                errorCode = DockerErrorCode.IMAGE_PULL_FAILED;
            } else if (message.contains("Invalid container config")) {
                errorCode = DockerErrorCode.INVALID_CONFIG;
            } else if (message.contains("Cannot connect to the Docker daemon")) {
                errorCode = DockerErrorCode.DOCKER_DAEMON_UNAVAILABLE;
            } else if (message.contains("Conflict. The container name") && message.contains("is already in use")) {
                errorCode = DockerErrorCode.CONTAINER_NAME_CONFLICT;
            } else if (message.contains("Mounts denied") && message.contains("is not shared from the host")) {
                errorCode = DockerErrorCode.MOUNT_PATH_NOT_SHARED;
            }
        }

        return new DockerOperationException(errorCode, String.format("%s失败：%s", action, errorCode.getMessage()), e);
    }
}