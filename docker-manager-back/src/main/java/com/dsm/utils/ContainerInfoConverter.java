//package com.dsm.utils;
//
//import com.dsm.model.dto.ContainerStaticInfoDTO;
//import com.github.dockerjava.api.command.InspectContainerResponse;
//import com.github.dockerjava.api.model.*;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class ContainerInfoConverter {
//
//    public static ContainerStaticInfoDTO toStaticInfoDTO(InspectContainerResponse containerInfo) {
//        if (containerInfo == null) return null;
//
//        ContainerStaticInfoDTO dto = new ContainerStaticInfoDTO();
//
//        dto.setId(containerInfo.getId());
//        dto.setName(safe(containerInfo.getName()).replaceFirst("^/", ""));
//        dto.setImage(containerInfo.getImage());
//        dto.setCreated(containerInfo.getCreated());
//
//        if (containerInfo.getState() != null) {
//            dto.setState(containerInfo.getState().getStatus());
//            dto.setStatus(containerInfo.getState().getRunning() != null && containerInfo.getState().getRunning()
//                    ? "running"
//                    : containerInfo.getState().getStatus());
//        }
//
//        if (containerInfo.getConfig() != null) {
//            dto.setCommand(safe(containerInfo.getConfig().getCmd()).toString());
//
//            dto.setEnvs(parseEnvList(containerInfo.getConfig().getEnv()));
//            dto.setLabels(Optional.ofNullable(containerInfo.getConfig().getLabels())
//                    .orElse(Collections.emptyMap()));
//        }
//
//        if (containerInfo.getHostConfig() != null) {
//            ContainerStaticInfoDTO.ContainerConfigExtra configExtra = new ContainerStaticInfoDTO.ContainerConfigExtra();
//
//            configExtra.setCapAdd(safeList(containerInfo.getHostConfig().getCapAdd()));
//            configExtra.setCapDrop(safeList(containerInfo.getHostConfig().getCapDrop()));
//            configExtra.setDns(safeList(containerInfo.getHostConfig().getDns()));
//
//            RestartPolicy restartPolicy = containerInfo.getHostConfig().getRestartPolicy();
//            configExtra.setRestartPolicy(restartPolicy != null ? restartPolicy.getName() : null);
//
//            dto.setNetworkMode(containerInfo.getHostConfig().getNetworkMode());
//            dto.setConfigExtra(configExtra);
//        }
//
//        // Mounts
//        List<ContainerMount> mounts = safeList(containerInfo.getMounts()).stream()
//                .map(m -> {
//                    ContainerStaticInfoDTO.VolumeMount vm = new ContainerStaticInfoDTO.VolumeMount();
//                    vm.setSource(m.getSource());
//                    vm.setDestination(m.getDestination());
//                    vm.setReadOnly(Boolean.TRUE.equals(m.getRW()) ? false : true);
//                    return vm;
//                })
//                .collect(Collectors.toList());
//        dto.setMounts(mounts);
//
//        // Ports
//        Ports portBindings = Optional.ofNullable(containerInfo.getNetworkSettings())
//                .map(NetworkSettings::getPorts)
//                .orElse(null);
//
//        if (portBindings != null && portBindings.getBindings() != null) {
//            List<ContainerStaticInfoDTO.PortMapping> portList = new ArrayList<>();
//            for (Map.Entry<ExposedPort, Ports.Binding[]> entry : portBindings.getBindings().entrySet()) {
//                ExposedPort containerPort = entry.getKey();
//                Ports.Binding[] bindings = entry.getValue();
//                if (bindings != null) {
//                    for (Ports.Binding b : bindings) {
//                        ContainerStaticInfoDTO.PortMapping port = new ContainerStaticInfoDTO.PortMapping();
//                        port.setProtocol(containerPort.getProtocol().toString());
//                        port.setContainerPort(containerPort.getPort());
//                        try {
//                            port.setHostPort(Integer.parseInt(b.getHostPortSpec()));
//                        } catch (Exception ignored) {
//                            port.setHostPort(null);
//                        }
//                        portList.add(port);
//                    }
//                }
//            }
//            dto.setPorts(portList);
//        }
//
//        return dto;
//    }
//
//    // 处理 ENV 环境变量数组 -> Map<String, String>
//    private static Map<String, String> parseEnvList(String[] envArray) {
//        if (envArray == null) return Collections.emptyMap();
//        Map<String, String> envs = new LinkedHashMap<>();
//        for (String env : envArray) {
//            String[] kv = env.split("=", 2);
//            if (kv.length == 2) {
//                envs.put(kv[0], kv[1]);
//            } else {
//                envs.put(kv[0], "");
//            }
//        }
//        return envs;
//    }
//
//    // 空安全 list 处理
//    private static <T> List<T> safeList(List<T> list) {
//        return list != null ? list : Collections.emptyList();
//    }
//
//    // 空安全字符串
//    private static String safe(String str) {
//        return str != null ? str : "";
//    }
//
//    // 空安全数组
//    private static List<String> safe(String[] arr) {
//        return arr != null ? Arrays.asList(arr) : Collections.emptyList();
//    }
//}