package com.dsm.common.exception;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DockerErrorResolver {

    public static DockerOperationException resolve(String action, String identifier, Exception e) {
        String message = e.getMessage();
        DockerErrorCode errorCode = DockerErrorCode.UNKNOWN_ERROR;
        String detail = null;

        if (message != null) {
            // 提取端口冲突的具体端口
            Pattern portPattern = Pattern.compile("Bind for [^:]+:(\\d+) failed: port is already allocated");
            Matcher portMatcher = portPattern.matcher(message);
            if (portMatcher.find()) {
                errorCode = DockerErrorCode.PORT_CONFLICT;
                detail = "端口 " + portMatcher.group(1) + " 已被占用";
            }
            // 提取镜像名称
            else if (message.contains("No such image")) {
                errorCode = DockerErrorCode.IMAGE_NOT_FOUND;
                Pattern imagePattern = Pattern.compile("No such image: ([^\\s]+)");
                Matcher imageMatcher = imagePattern.matcher(message);
                if (imageMatcher.find()) {
                    detail = "镜像 " + imageMatcher.group(1) + " 不存在";
                }
            }
            // 提取容器名称
            else if (message.contains("Conflict. The container name")) {
                errorCode = DockerErrorCode.CONTAINER_NAME_CONFLICT;
                Pattern namePattern = Pattern.compile("The container name \\\"([^\\\"]+)\\\" is already in use");
                Matcher nameMatcher = namePattern.matcher(message);
                if (nameMatcher.find()) {
                    detail = "容器名称 " + nameMatcher.group(1) + " 已被使用";
                }
            }
            // 提取挂载路径
            else if (message.contains("Mounts denied")) {
                errorCode = DockerErrorCode.MOUNT_PATH_NOT_SHARED;
                Pattern mountPattern = Pattern.compile("The path ([^\\s]+) is not shared from the host");
                Matcher mountMatcher = mountPattern.matcher(message);
                if (mountMatcher.find()) {
                    detail = "挂载路径 " + mountMatcher.group(1) + " 不存在或无权限";
                }
            }
            // 提取正在使用的镜像
            else if (message.contains("image is being used by")) {
                errorCode = DockerErrorCode.IMAGE_IS_BEING_USED;
                Pattern imagePattern = Pattern.compile("image ([^\\s]+) is being used by");
                Matcher imageMatcher = imagePattern.matcher(message);
                if (imageMatcher.find()) {
                    detail = "镜像 " + imageMatcher.group(1) + " 正在被其他容器使用";
                }
            }
            // 其他错误类型
            else if (message.contains("No such container")) {
                errorCode = DockerErrorCode.CONTAINER_NOT_FOUND;
                detail = "容器 " + identifier + " 不存在";
            } else if (message.contains("is already running")) {
                errorCode = DockerErrorCode.CONTAINER_ALREADY_RUNNING;
                detail = "容器 " + identifier + " 已在运行中";
            } else if (message.contains("Status 304")) {
                errorCode = DockerErrorCode.CONTAINER_ALREADY_STOPPED;
                detail = "容器 " + identifier + " 已停止";
            } else if (message.contains("manifest for") && message.contains("not found")) {
                errorCode = DockerErrorCode.IMAGE_PULL_FAILED;
                Pattern manifestPattern = Pattern.compile("manifest for ([^\\s]+) not found");
                Matcher manifestMatcher = manifestPattern.matcher(message);
                if (manifestMatcher.find()) {
                    detail = "无法拉取镜像 " + manifestMatcher.group(1);
                }
            } else if (message.contains("Invalid container config")) {
                errorCode = DockerErrorCode.INVALID_CONFIG;
                detail = "容器 " + identifier + " 配置无效";
            } else if (message.contains("Cannot connect to the Docker daemon")) {
                errorCode = DockerErrorCode.DOCKER_DAEMON_UNAVAILABLE;
                detail = "无法连接到 Docker 守护进程";
            } else if (message.contains("Error parsing Bind")) {
                errorCode = DockerErrorCode.BIND_PARSE_ERROR;
                Pattern bindPattern = Pattern.compile("Error parsing Bind: ([^\\s]+)");
                Matcher bindMatcher = bindPattern.matcher(message);
                if (bindMatcher.find()) {
                    detail = "挂载路径解析失败: " + bindMatcher.group(1);
                }
            }
        }

        String finalMessage = detail != null ? detail : errorCode.getMessage();
        return new DockerOperationException(errorCode, String.format("%s失败：%s", action, finalMessage), e);
    }
}