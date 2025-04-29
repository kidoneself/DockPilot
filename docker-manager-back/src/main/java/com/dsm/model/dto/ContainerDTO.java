package com.dsm.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ContainerDTO implements Serializable {

    private String command;
    private Long created;
    private String id;
    private String image;
    private String imageId;
    private String[] names;
    private ContainerPortDTO[] ports;
    private Map<String, String> labels;
    private String status;
    private String state;
    private Long sizeRw;
    private Long sizeRootFs;
    private ContainerHostConfigDTO hostConfig;
    private ContainerNetworkSettingsDTO networkSettings;
    private List<ContainerMountDTO> mounts;


    // 转换方法：将 DockerJava 的 Container 转换为自定义 DTO
    public static ContainerDTO convertToDTO(com.github.dockerjava.api.model.Container container) {
        ContainerDTO containerDTO = new ContainerDTO();

        containerDTO.setId(container.getId());
        containerDTO.setCommand(container.getCommand());
        containerDTO.setCreated(container.getCreated());
        containerDTO.setImage(container.getImage());
        containerDTO.setImageId(container.getImageId());
        containerDTO.setNames(container.getNames());

        // Convert ContainerPort to ContainerPortDTO
        if (container.getPorts() != null) {
            ContainerPortDTO[] portsDTO = new ContainerPortDTO[container.getPorts().length];
            for (int i = 0; i < container.getPorts().length; i++) {
                ContainerPortDTO portDTO = new ContainerPortDTO();
                portDTO.setPrivatePort(String.valueOf(container.getPorts()[i].getPrivatePort()));
                portDTO.setPublicPort(String.valueOf(container.getPorts()[i].getPublicPort()));
                portDTO.setType(container.getPorts()[i].getType());
                portsDTO[i] = portDTO;
            }
            containerDTO.setPorts(portsDTO);
        }

        containerDTO.setLabels(container.getLabels());
        containerDTO.setStatus(container.getStatus());
        containerDTO.setState(container.getState());
        containerDTO.setSizeRw(container.getSizeRw());
        containerDTO.setSizeRootFs(container.getSizeRootFs());

        // Convert HostConfig to ContainerHostConfigDTO
        if (container.getHostConfig() != null) {
            ContainerHostConfigDTO hostConfigDTO = new ContainerHostConfigDTO();
            hostConfigDTO.setNetworkMode(container.getHostConfig().getNetworkMode());
//            hostConfigDTO.setRestartPolicy(container.getHostConfig().getRestartPolicy());
            containerDTO.setHostConfig(hostConfigDTO);
        }

        // Convert NetworkSettings to ContainerNetworkSettingsDTO
        if (container.getNetworkSettings() != null) {
            ContainerNetworkSettingsDTO networkSettingsDTO = new ContainerNetworkSettingsDTO();
//            networkSettingsDTO.setIpAddress(container.getNetworkSettings().getIpAddress());
            containerDTO.setNetworkSettings(networkSettingsDTO);
        }

        // Convert Mounts to ContainerMountDTO
        if (container.getMounts() != null) {
            List<ContainerMountDTO> mountsDTO = new ArrayList<>();
            for (com.github.dockerjava.api.model.ContainerMount mount : container.getMounts()) {
                ContainerMountDTO mountDTO = new ContainerMountDTO();
                mountDTO.setSource(mount.getSource());
                mountDTO.setDestination(mount.getDestination());
//                mountDTO.setReadOnly(mount.isReadOnly());
                mountsDTO.add(mountDTO);
            }
            containerDTO.setMounts(mountsDTO);
        }

        return containerDTO;
    }

    // 内部类：ContainerPortDTO
    @Data
    public static class ContainerPortDTO {
        private String privatePort;
        private String publicPort;
        private String type;

        public String getPrivatePort() {
            return privatePort;
        }

        public void setPrivatePort(String privatePort) {
            this.privatePort = privatePort;
        }

        public String getPublicPort() {
            return publicPort;
        }

        public void setPublicPort(String publicPort) {
            this.publicPort = publicPort;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    // 内部类：ContainerHostConfigDTO
    @Data
    public static class ContainerHostConfigDTO {
        private String networkMode;
        private String restartPolicy;

    }

    // 内部类：ContainerNetworkSettingsDTO
    @Data
    public static class ContainerNetworkSettingsDTO {
        private String ipAddress;

    }

    // 内部类：ContainerMountDTO
    @Data
    public static class ContainerMountDTO {
        private String source;
        private String destination;
        private boolean readOnly;

    }
}