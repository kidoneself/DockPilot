package com.dsm.utils;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerComposeUtils {
    private final Map<String, String> envVariables;

    public DockerComposeUtils() {
        this.envVariables = new HashMap<>();
    }

    public void loadEnvFile(String envFilePath) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        Map<String, Object> envData = yaml.load(new FileInputStream(envFilePath));
        
        if (envData != null && envData.containsKey("fields")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fields = (List<Map<String, Object>>) envData.get("fields");
            for (Map<String, Object> field : fields) {
                String key = (String) field.get("key");
                Object defaultValue = field.get("default");
                String value = defaultValue != null ? defaultValue.toString() : "";
                envVariables.put(key, value);
            }
        }
    }

    public Map<String, ServiceConfig> parseDockerCompose(String composeFilePath) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        Map<String, Object> composeData = yaml.load(new FileInputStream(composeFilePath));
        Map<String, ServiceConfig> serviceConfigs = new HashMap<>();
        
        if (composeData != null && composeData.containsKey("services")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> services = (Map<String, Object>) composeData.get("services");
            
            for (Map.Entry<String, Object> serviceEntry : services.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> service = (Map<String, Object>) serviceEntry.getValue();
                String serviceName = serviceEntry.getKey();
                
                ServiceConfig config = new ServiceConfig();
                config.setImage(replaceEnvVariables((String) service.get("image")));
                config.setContainerName(replaceEnvVariables((String) service.get("container_name")));
                
                // 解析端口映射
                @SuppressWarnings("unchecked")
                List<String> ports = (List<String>) service.get("ports");
                List<PortBinding> portBindings = new ArrayList<>();
                if (ports != null) {
                    for (String port : ports) {
                        String[] parts = replaceEnvVariables(port).split(":");
                        if (parts.length == 2) {
                            ExposedPort exposedPort = ExposedPort.tcp(Integer.parseInt(parts[1]));
                            PortBinding portBinding = new PortBinding(
                                Ports.Binding.bindPort(Integer.parseInt(parts[0])),
                                exposedPort
                            );
                            portBindings.add(portBinding);
                        }
                    }
                }
                config.setPortBindings(portBindings);
                
                // 解析卷映射
                @SuppressWarnings("unchecked")
                List<String> volumeStrings = (List<String>) service.get("volumes");
                List<Volume> volumes = new ArrayList<>();
                if (volumeStrings != null) {
                    for (String volumeStr : volumeStrings) {
                        String[] parts = replaceEnvVariables(volumeStr).split(":");
                        if (parts.length == 2) {
                            volumes.add(new Volume(parts[1]));
                        }
                    }
                }
                config.setVolumes(volumes);
                
                serviceConfigs.put(serviceName, config);
            }
        }
        
        return serviceConfigs;
    }

    private String replaceEnvVariables(String input) {
        if (input == null) return "";
        String result = input;
        for (Map.Entry<String, String> entry : envVariables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public static class ServiceConfig {
        private String image;
        private String containerName;
        private List<PortBinding> portBindings;
        private List<Volume> volumes;

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getContainerName() {
            return containerName;
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }

        public List<PortBinding> getPortBindings() {
            return portBindings;
        }

        public void setPortBindings(List<PortBinding> portBindings) {
            this.portBindings = portBindings;
        }

        public List<Volume> getVolumes() {
            return volumes;
        }

        public void setVolumes(List<Volume> volumes) {
            this.volumes = volumes;
        }
    }
} 