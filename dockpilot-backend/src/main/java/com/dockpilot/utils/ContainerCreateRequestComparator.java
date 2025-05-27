package com.dockpilot.utils;

import com.dockpilot.model.ContainerCreateRequest;
import com.github.dockerjava.api.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 比较两个ContainerCreateRequest对象的工具类
 */
public class ContainerCreateRequestComparator {

    public static ComparisonResult compare(ContainerCreateRequest request1, ContainerCreateRequest request2) {
        ComparisonResult result = new ComparisonResult();

        // 比较基本字段
        compareField(result, "name", request1.getName(), request2.getName());
        compareField(result, "hostName", request1.getHostName(), request2.getHostName());
        compareField(result, "image", request1.getImage(), request2.getImage());
        compareField(result, "workingDir", request1.getWorkingDir(), request2.getWorkingDir());
        compareField(result, "networkMode", request1.getNetworkMode(), request2.getNetworkMode());
        compareField(result, "ipv4Address", request1.getIpv4Address(), request2.getIpv4Address());
        compareField(result, "privileged", request1.getPrivileged(), request2.getPrivileged());

        // 比较列表类型字段
        compareListField(result, "cmd", request1.getCmd(), request2.getCmd());
        compareListField(result, "entrypoint", request1.getEntrypoint(), request2.getEntrypoint());
        compareListField(result, "env", request1.getEnv(), request2.getEnv());
        compareListField(result, "exposedPorts", request1.getExposedPorts(), request2.getExposedPorts());

        // 比较Map类型字段
        compareMapField(result, "labels", request1.getLabels(), request2.getLabels());

        // 比较特殊类型字段
        comparePorts(result, "portBindings", request1.getPortBindings(), request2.getPortBindings());
        compareVolumes(result, "volumes", request1.getVolumes(), request2.getVolumes());
        compareBinds(result, "binds", request1.getBinds(), request2.getBinds());
        compareDevices(result, "devices", request1.getDevices(), request2.getDevices());
        compareRestartPolicy(result, "restartPolicy", request1.getRestartPolicy(), request2.getRestartPolicy());

        return result;
    }

    private static void compareField(ComparisonResult result, String fieldName, Object value1, Object value2) {
        if (!Objects.equals(value1, value2)) {
            result.addDifference(fieldName, value1, value2);
        } else {
            result.addField(fieldName, value1, value2);
        }
    }

    private static void compareListField(ComparisonResult result, String fieldName, List<?> list1, List<?> list2) {
        if (list1 == null && list2 == null) {
            result.addField(fieldName, null, null);
            return;
        }
        if (list1 == null || list2 == null) {
            result.addDifference(fieldName, list1, list2);
            return;
        }
        if (list1.size() != list2.size()) {
            result.addDifference(fieldName, list1, list2);
            return;
        }
        boolean hasDifference = false;
        for (int i = 0; i < list1.size(); i++) {
            if (!Objects.equals(list1.get(i), list2.get(i))) {
                hasDifference = true;
                break;
            }
        }
        if (hasDifference) {
            result.addDifference(fieldName, list1, list2);
        } else {
            result.addField(fieldName, list1, list2);
        }
    }

    private static void compareMapField(ComparisonResult result, String fieldName, Map<?, ?> map1, Map<?, ?> map2) {
        if (map1 == null && map2 == null) {
            result.addField(fieldName, null, null);
            return;
        }
        if (map1 == null || map2 == null) {
            result.addDifference(fieldName, map1, map2);
            return;
        }
        if (!map1.equals(map2)) {
            result.addDifference(fieldName, map1, map2);
        } else {
            result.addField(fieldName, map1, map2);
        }
    }

    private static void comparePorts(ComparisonResult result, String fieldName, Ports ports1, Ports ports2) {
        if (ports1 == null && ports2 == null) {
            result.addField(fieldName, null, null);
            return;
        }
        if (ports1 == null || ports2 == null) {
            result.addDifference(fieldName, ports1, ports2);
            return;
        }
        if (!ports1.getBindings().equals(ports2.getBindings())) {
            result.addDifference(fieldName, ports1, ports2);
        } else {
            result.addField(fieldName, ports1, ports2);
        }
    }

    private static void compareVolumes(ComparisonResult result, String fieldName, List<Volume> volumes1, List<Volume> volumes2) {
        if (volumes1 == null && volumes2 == null) {
            result.addField(fieldName, null, null);
            return;
        }
        if (volumes1 == null || volumes2 == null) {
            result.addDifference(fieldName, volumes1, volumes2);
            return;
        }
        if (volumes1.size() != volumes2.size()) {
            result.addDifference(fieldName, volumes1, volumes2);
            return;
        }
        boolean hasDifference = false;
        for (int i = 0; i < volumes1.size(); i++) {
            if (!Objects.equals(volumes1.get(i).getPath(), volumes2.get(i).getPath())) {
                hasDifference = true;
                break;
            }
        }
        if (hasDifference) {
            result.addDifference(fieldName, volumes1, volumes2);
        } else {
            result.addField(fieldName, volumes1, volumes2);
        }
    }

    private static void compareDevices(ComparisonResult result, String fieldName, Device[] devices1, Device[] devices2) {
        if (devices1 == null && devices2 == null) {
            result.addField(fieldName, null, null);
            return;
        }
        if (devices1 == null || devices2 == null) {
            result.addDifference(fieldName, devices1, devices2);
            return;
        }
        if (devices1.length != devices2.length) {
            result.addDifference(fieldName, devices1, devices2);
            return;
        }
        boolean hasDifference = false;
        for (int i = 0; i < devices1.length; i++) {
            if (!Objects.equals(devices1[i].getPathOnHost(), devices2[i].getPathOnHost()) ||
                    !Objects.equals(devices1[i].getPathInContainer(), devices2[i].getPathInContainer())) {
                hasDifference = true;
                break;
            }
        }
        if (hasDifference) {
            result.addDifference(fieldName, devices1, devices2);
        } else {
            result.addField(fieldName, devices1, devices2);
        }
    }

    private static void compareRestartPolicy(ComparisonResult result, String fieldName, RestartPolicy policy1, RestartPolicy policy2) {
        if (policy1 == null && policy2 == null) {
            result.addField(fieldName, null, null);
            return;
        }
        if (policy1 == null || policy2 == null) {
            result.addDifference(fieldName, policy1, policy2);
            return;
        }
        if (!Objects.equals(policy1.getName(), policy2.getName()) ||
                !Objects.equals(policy1.getMaximumRetryCount(), policy2.getMaximumRetryCount())) {
            result.addDifference(fieldName, policy1, policy2);
        } else {
            result.addField(fieldName, policy1, policy2);
        }
    }

    private static void compareBinds(ComparisonResult result, String fieldName, List<Bind> binds1, List<Bind> binds2) {
        if (binds1 == null && binds2 == null) {
            result.addField(fieldName, null, null);
            return;
        }
        if (binds1 == null || binds2 == null) {
            result.addDifference(fieldName, binds1, binds2);
            return;
        }
        if (binds1.size() != binds2.size()) {
            result.addDifference(fieldName, binds1, binds2);
            return;
        }
        boolean hasDifference = false;
        for (int i = 0; i < binds1.size(); i++) {
            if (!Objects.equals(binds1.get(i).getPath(), binds2.get(i).getPath()) ||
                    !Objects.equals(binds1.get(i).getVolume().getPath(), binds2.get(i).getVolume().getPath())) {
                hasDifference = true;
                break;
            }
        }
        if (hasDifference) {
            result.addDifference(fieldName, binds1, binds2);
        } else {
            result.addField(fieldName, binds1, binds2);
        }
    }

    public static class ComparisonResult {
        private final Map<String, FieldDifference> differences = new HashMap<>();
        private final Map<String, FieldDifference> allFields = new HashMap<>();

        public void addDifference(String fieldName, Object value1, Object value2) {
            differences.put(fieldName, new FieldDifference(value1, value2));
            allFields.put(fieldName, new FieldDifference(value1, value2));
        }

        public void addField(String fieldName, Object value1, Object value2) {
            allFields.put(fieldName, new FieldDifference(value1, value2));
        }

        public boolean hasDifferences() {
            return !differences.isEmpty();
        }

        public Map<String, FieldDifference> getDifferences() {
            return differences;
        }

        public Map<String, FieldDifference> getAllFields() {
            return allFields;
        }

        private String formatValue(Object value) {
            if (value == null) {
                return "null";
            }
            if (value instanceof Ports) {
                Ports ports = (Ports) value;
                Map<ExposedPort, Ports.Binding[]> bindings = ports.getBindings();
                if (bindings == null || bindings.isEmpty()) {
                    return "{}";
                }
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                bindings.forEach((exposedPort, bindingArray) -> {
                    if (bindingArray != null && bindingArray.length > 0) {
                        for (Ports.Binding binding : bindingArray) {
                            sb.append(exposedPort.toString())
                                    .append(" -> ")
                                    .append(binding.getHostPortSpec())
                                    .append(", ");
                        }
                    }
                });
                if (sb.length() > 1) {
                    sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
                }
                sb.append("}");
                return sb.toString();
            }
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (list.isEmpty()) {
                    return "[]";
                }
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (Object item : list) {
                    sb.append(formatValue(item)).append(", ");
                }
                sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
                sb.append("]");
                return sb.toString();
            }
            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                if (map.isEmpty()) {
                    return "{}";
                }
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                map.forEach((k, v) -> sb.append(formatValue(k))
                        .append(": ")
                        .append(formatValue(v))
                        .append(", "));
                sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
                sb.append("}");
                return sb.toString();
            }
            if (value instanceof Device[]) {
                Device[] devices = (Device[]) value;
                if (devices.length == 0) {
                    return "[]";
                }
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (Device device : devices) {
                    sb.append(device.getPathOnHost())
                            .append(":")
                            .append(device.getPathInContainer())
                            .append(", ");
                }
                sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
                sb.append("]");
                return sb.toString();
            }
            if (value instanceof RestartPolicy) {
                RestartPolicy policy = (RestartPolicy) value;
                return String.format("%s(maxRetry=%d)",
                        policy.getName(),
                        policy.getMaximumRetryCount());
            }
            if (value instanceof Bind[]) {
                Bind[] binds = (Bind[]) value;
                if (binds.length == 0) {
                    return "[]";
                }
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (Bind bind : binds) {
                    sb.append(bind.getPath())
                            .append(":")
                            .append(bind.getVolume().getPath())
                            .append(", ");
                }
                sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
                sb.append("]");
                return sb.toString();
            }
            if (value instanceof Volume[]) {
                Volume[] volumes = (Volume[]) value;
                if (volumes.length == 0) {
                    return "[]";
                }
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (Volume volume : volumes) {
                    sb.append(volume.getPath()).append(", ");
                }
                sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
                sb.append("]");
                return sb.toString();
            }
            return value.toString();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("比较结果:\n");
            for (Map.Entry<String, FieldDifference> entry : allFields.entrySet()) {
                sb.append(String.format("字段: %s\n", entry.getKey()));
                sb.append(String.format("  原值: %s\n", formatValue(entry.getValue().value1)));
                sb.append(String.format("  新值: %s\n", formatValue(entry.getValue().value2)));
                if (differences.containsKey(entry.getKey())) {
                    sb.append("  [已修改]\n");
                } else {
                    sb.append("  [未修改]\n");
                }
            }
            return sb.toString();
        }
    }

    public static class FieldDifference {
        public final Object value1;
        public final Object value2;

        public FieldDifference(Object value1, Object value2) {
            this.value1 = value1;
            this.value2 = value2;
        }
    }
} 