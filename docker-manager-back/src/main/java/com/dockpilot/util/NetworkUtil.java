package com.dockpilot.util;

import com.dockpilot.model.IPAMConfigFlatDTO;
import com.dockpilot.model.NetworkContainerDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtil {

    /**
     * 获取宿主机的网络配置信息
     *
     * @return 网络配置列表，只包含合法的IPv4配置
     */
    public static List<IPAMConfigFlatDTO> getHostNetworkInfo() {
        List<IPAMConfigFlatDTO> hostNetworkInfo = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface nif = interfaces.nextElement();
                // 跳过回环接口、未启用的接口和虚拟接口
                if (nif.isLoopback() || !nif.isUp() || nif.isVirtual() || nif.isPointToPoint()) {
                    continue;
                }

                for (InterfaceAddress addr : nif.getInterfaceAddresses()) {
                    if (addr.getAddress() instanceof java.net.Inet4Address) {
                        String subnet = addr.getAddress().getHostAddress() + "/" + addr.getNetworkPrefixLength();
                        String gateway = getDefaultGateway(nif.getName(), true);

                        // 验证网关地址是否合法
                        if (isValidIPv4Gateway(gateway)) {
                            IPAMConfigFlatDTO config = new IPAMConfigFlatDTO();
                            config.setSubnet(subnet);
                            config.setGateway(gateway);
                            hostNetworkInfo.add(config);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hostNetworkInfo;
    }

    /**
     * 获取宿主机的网络配置信息
     *
     * @return 宿主机的网络配置
     */
    public static NetworkContainerDTO getHostContainerNetworkConfig() {
        NetworkContainerDTO config = new NetworkContainerDTO();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface nif = interfaces.nextElement();
                // 跳过回环接口、未启用的接口和虚拟接口
                if (nif.isLoopback() || !nif.isUp() || nif.isVirtual() || nif.isPointToPoint()) {
                    continue;
                }

                for (InterfaceAddress addr : nif.getInterfaceAddresses()) {
                    if (addr.getAddress() instanceof java.net.Inet4Address) {
                        String gateway = getDefaultGateway(nif.getName(), true);
                        if (isValidIPv4Gateway(gateway)) {
                            config.setId("host");
                            config.setEndpointId("host");
                            config.setMacAddress(nif.getHardwareAddress() != null ?
                                    formatMacAddress(nif.getHardwareAddress()) : "N/A");
                            config.setIpv4Address(addr.getAddress().getHostAddress());
                            config.setIpv6Address("N/A");
                            config.setName("宿主机");
                            return config;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }

    /**
     * 将字节数组格式的MAC地址转换为字符串格式
     */
    private static String formatMacAddress(byte[] mac) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02x%s", mac[i], (i < mac.length - 1) ? ":" : ""));
        }
        return sb.toString();
    }

    /**
     * 验证网关地址是否为合法的IPv4地址
     *
     * @param gateway 网关地址
     * @return 是否合法
     */
    private static boolean isValidIPv4Gateway(String gateway) {
        if (gateway == null || gateway.isEmpty()) {
            return false;
        }

        // 检查是否以link#开头
        if (gateway.startsWith("link#")) {
            return false;
        }

        // 验证是否是合法的IPv4地址
        try {
            String[] parts = gateway.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 获取指定网络接口的默认网关
     *
     * @param interfaceName 网络接口名称
     * @param isIPv4        是否是IPv4
     * @return 网关地址
     */
    private static String getDefaultGateway(String interfaceName, boolean isIPv4) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            Process process;
            if (os.contains("linux")) {
                process = Runtime.getRuntime().exec("ip route show default");
            } else if (os.contains("mac")) {
                process = Runtime.getRuntime().exec("netstat -nr | grep default");
            } else if (os.contains("windows")) {
                process = Runtime.getRuntime().exec("route print");
            } else {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (os.contains("linux")) {
                        if (line.contains("default") && line.contains(interfaceName)) {
                            String[] parts = line.trim().split("\\s+");
                            for (int i = 0; i < parts.length; i++) {
                                if (parts[i].equals("via")) {
                                    return parts[i + 1];
                                }
                            }
                        }
                    } else if (os.contains("mac")) {
                        if (line.contains("default") && line.contains(interfaceName)) {
                            String[] parts = line.trim().split("\\s+");
                            if (parts.length > 1) {
                                return parts[1];
                            }
                        }
                    } else if (os.contains("windows")) {
                        if (line.contains("0.0.0.0") && line.contains(interfaceName)) {
                            String[] parts = line.trim().split("\\s+");
                            for (int i = 0; i < parts.length; i++) {
                                if (parts[i].equals("0.0.0.0")) {
                                    return parts[i + 2];
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
} 