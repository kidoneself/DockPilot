//package com.dsm.utils;
//
//import com.dsm.model.ContainerCreateRequest;
//import com.dsm.model.ContainerStaticInfoDTO;
//import com.dsm.model.VolumeMapping;
//import com.github.dockerjava.api.model.*;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//
///**
// * 将ContainerStaticInfoDTO转换为ContainerCreateRequest的工具类
// */
//public class ContainerStaticInfoDTOToContainerCreateRequestConverter {
//
//    public static ContainerCreateRequest convert(ContainerStaticInfoDTO dto) {
//        ContainerCreateRequest request = new ContainerCreateRequest();
//        request.setName(dto.getContainerName());
//        request.setImage(dto.getImageName());
//        request.setCmd(Collections.singletonList(dto.getCommand()));
//        request.setEnv(dto.getEnvs());
//        request.setVolumes(convertVolumes(dto.getVolumes()));
//        request.setBinds(convertBinds(dto.getVolumes()));
//        request.setPortBindings(convertPorts(dto.getPorts()));
//        request.setExposedPorts(convertExposedPorts(dto.getExposedPorts()));
//        request.setNetworkMode(dto.getNetworkMode());
//        request.setIpv4Address(dto.getIpAddress());
//        request.setRestartPolicy(convertRestartPolicy(dto.getRestartPolicyName(), dto.getRestartPolicyMaxRetry()));
//        request.setPrivileged(dto.getPrivileged());
//        return request;
//    }
//
//    private static List<Volume> convertVolumes(List<VolumeMapping> volumes) {
//        if (volumes == null) return new ArrayList<>();
//        List<Volume> result = new ArrayList<>();
//        for (VolumeMapping volume : volumes) {
//            if (volume.getContainerPath() != null) {
//                result.add(new Volume(volume.getContainerPath()));
//            }
//        }
//        return result;
//    }
//
//    private static List<Bind> convertBinds(List<VolumeMapping> volumes) {
//        if (volumes == null) return new ArrayList<>();
//        List<Bind> result = new ArrayList<>();
//        for (VolumeMapping volume : volumes) {
//            if (volume.getHostPath() != null && volume.getContainerPath() != null) {
//                String mode = volume.getReadOnly() != null && volume.getReadOnly() ? "ro" : "rw";
//                String bindStr = volume.getHostPath() + ":" + volume.getContainerPath() + ":" + mode;
//                result.add(Bind.parse(bindStr));
//            }
//        }
//        return result;
//    }
//
//    private static Ports convertPorts(List<String> ports) {
//        if (ports == null) return new Ports();
//        Ports result = new Ports();
//        for (String port : ports) {
//            String[] parts = port.split(":");
//            if (parts.length == 2) {
//                String[] containerPortParts = parts[0].split("/");
//                int containerPort = Integer.parseInt(containerPortParts[0]);
//                String protocol = containerPortParts.length > 1 ? containerPortParts[1] : "tcp";
//                ExposedPort exposedPort = new ExposedPort(containerPort, InternetProtocol.parse(protocol));
//                result.bind(exposedPort, Ports.Binding.bindPort(Integer.parseInt(parts[1])));
//            }
//        }
//        return result;
//    }
//
//    private static List<ExposedPort> convertExposedPorts(List<String> exposedPorts) {
//        if (exposedPorts == null) return new ArrayList<>();
//        List<ExposedPort> result = new ArrayList<>();
//        for (String port : exposedPorts) {
//            String[] portParts = port.split("/");
//            int portNumber = Integer.parseInt(portParts[0]);
//            String protocol = portParts.length > 1 ? portParts[1] : "tcp";
//            result.add(new ExposedPort(portNumber, InternetProtocol.parse(protocol)));
//        }
//        return result;
//    }
//
//    private static Device[] convertDevices(List<String> devices) {
//        if (devices == null) return new Device[0];
//        List<Device> result = new ArrayList<>();
//        for (String device : devices) {
//            String[] parts = device.split(":");
//            if (parts.length == 3) {
//                result.add(new Device(parts[0], parts[1], parts[2]));
//            }
//        }
//        return result.toArray(new Device[0]);
//    }
//
//    private static RestartPolicy convertRestartPolicy(String policyName, Integer maxRetry) {
//        if (policyName == null) return RestartPolicy.noRestart();
//        switch (policyName.toLowerCase()) {
//            case "always":
//                return RestartPolicy.alwaysRestart();
//            case "on-failure":
//                return RestartPolicy.onFailureRestart(maxRetry != null ? maxRetry : 0);
//            case "unless-stopped":
//                return RestartPolicy.unlessStoppedRestart();
//            default:
//                return RestartPolicy.noRestart();
//        }
//    }
//}