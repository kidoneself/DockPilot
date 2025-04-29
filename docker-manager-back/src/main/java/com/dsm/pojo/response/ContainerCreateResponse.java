package com.dsm.pojo.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerCreateResponse {
    private boolean success;
    private String containerId;
    private String message;
    private String error;
    private ContainerInfo containerInfo;
}

