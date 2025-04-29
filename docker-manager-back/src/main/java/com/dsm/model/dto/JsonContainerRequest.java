package com.dsm.model.dto;

/**
 * 前端请求容器的参数
 * 通过JsonContainerRequestToContainerCreateRequestConverter转环成dockerClient能识别的容器创建参数
 */

import lombok.Data;

import java.util.List;

@Data
public class JsonContainerRequest {
    private String image;
    private String tag;
    private Boolean autoPull;
    private String name;
    private Boolean autoRemove;
    private String restartPolicy;
    private List<PortMapping> portMappings;
    private String networkMode;
    private String ipAddress;
    private List<String> dns;
    private List<String> dnsSearch;
    private List<String> extraHosts;
    private List<VolumeMount> volumeMounts;
    private List<EnvVar> environmentVariables;
    private String command;
    private Boolean privileged;

    @Data
    public static class PortMapping {
        private Integer hostPort;
        private Integer containerPort;
        private String protocol;
        private String ip;
    }

    @Data
    public static class VolumeMount {
        private String hostPath;
        private String containerPath;
        private String mode;
        private Boolean readOnly;
    }

    @Data
    public static class EnvVar {
        private String key;
        private String value;
    }
}