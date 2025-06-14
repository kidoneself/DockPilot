package com.dockpilot.service.http;

import com.dockpilot.common.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 代理HTTP客户端服务
 * 提供统一的代理配置管理和HttpClient创建功能
 */
@Slf4j
@Service
public class ProxyHttpClientService {

    @Autowired
    private AppConfig appConfig;

    // 缓存当前的HttpClient和代理配置
    private volatile HttpClient cachedHttpClient;
    private volatile String lastProxyUrl;

    /**
     * 获取配置了代理的HttpClient（如果有配置代理的话）
     * 支持动态代理切换
     */
    public synchronized HttpClient getHttpClient() {
        String currentProxyUrl = appConfig.getProxyUrl();
        
        // 如果HttpClient不存在，或者代理配置发生变化，重新创建
        if (cachedHttpClient == null || !Objects.equals(currentProxyUrl, lastProxyUrl)) {
            cachedHttpClient = createHttpClient(currentProxyUrl);
            lastProxyUrl = currentProxyUrl;
        }
        
        return cachedHttpClient;
    }

    /**
     * 获取配置了代理的HttpClient（自定义超时）
     */
    public HttpClient getHttpClient(Duration connectTimeout, Duration requestTimeout) {
        String currentProxyUrl = appConfig.getProxyUrl();
        // 不缓存自定义超时的客户端，每次创建新的
        return createHttpClient(currentProxyUrl, connectTimeout, requestTimeout);
    }

    /**
     * 检查是否配置了代理
     */
    public boolean isProxyConfigured() {
        String proxyUrl = appConfig.getProxyUrl();
        return proxyUrl != null && !proxyUrl.trim().isEmpty();
    }

    /**
     * 获取代理信息（用于其他服务设置环境变量等）
     */
    public Map<String, String> getProxyInfo() {
        String proxyUrl = appConfig.getProxyUrl();
        if (proxyUrl == null || proxyUrl.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return parseProxyUrl(proxyUrl.trim());
        } catch (Exception e) {
            log.warn("解析代理配置失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 获取代理环境变量（用于ProcessBuilder等）
     */
    public Map<String, String> getProxyEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();
        String proxyUrl = appConfig.getProxyUrl();
        
        if (proxyUrl != null && !proxyUrl.trim().isEmpty()) {
            envVars.put("HTTP_PROXY", proxyUrl.trim());
            envVars.put("HTTPS_PROXY", proxyUrl.trim());
            log.debug("设置代理环境变量: {}", maskProxyUrl(proxyUrl.trim()));
        }
        
        return envVars;
    }

    /**
     * 创建HTTP客户端（使用默认超时）
     */
    private HttpClient createHttpClient(String proxyUrl) {
        return createHttpClient(proxyUrl, Duration.ofSeconds(15), Duration.ofSeconds(30));
    }

    /**
     * 创建HTTP客户端（自定义超时）
     */
    private HttpClient createHttpClient(String proxyUrl, Duration connectTimeout, Duration requestTimeout) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL);
        
        // 🔍 关键：检查代理是否配置
        if (proxyUrl != null && !proxyUrl.trim().isEmpty()) {
            // 有代理配置，解析并设置代理
            try {
                Map<String, String> proxyInfo = parseProxyUrl(proxyUrl.trim());
                ProxySelector proxySelector = createProxySelector(proxyInfo);
                builder.proxy(proxySelector);
                
                // 设置代理认证（如果需要）
                if (proxyInfo.containsKey("username") && proxyInfo.containsKey("password")) {
                    builder.authenticator(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(
                                proxyInfo.get("username"), 
                                proxyInfo.get("password").toCharArray()
                            );
                        }
                    });
                    log.debug("✅ HTTP客户端已配置代理和认证: {}", maskProxyUrl(proxyUrl.trim()));
                } else {
                    log.debug("✅ HTTP客户端已配置代理: {}", maskProxyUrl(proxyUrl.trim()));
                }
            } catch (Exception e) {
                log.warn("⚠️ 代理配置解析失败，使用直连: {}", e.getMessage());
                // 代理配置失败，使用直连
            }
        } else {
            // 无代理配置，使用直连
            log.debug("📡 HTTP客户端使用直连模式（未配置代理）");
        }
        
        return builder.build();
    }

    /**
     * 创建代理选择器
     */
    private ProxySelector createProxySelector(Map<String, String> proxyInfo) {
        String host = proxyInfo.get("host");
        int port = Integer.parseInt(proxyInfo.get("port"));
        
        return new ProxySelector() {
            @Override
            public java.util.List<Proxy> select(URI uri) {
                return java.util.List.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));
            }
            
            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                log.warn("代理连接失败: {} -> {}", uri, sa);
            }
        };
    }

    /**
     * 解析代理URL，提取用户名、密码、主机和端口
     */
    private Map<String, String> parseProxyUrl(String proxyUrl) {
        Map<String, String> result = new HashMap<>();
        try {
            if (proxyUrl == null || proxyUrl.isBlank()) {
                throw new IllegalArgumentException("代理URL不能为空");
            }
            
            URL url = new URL(proxyUrl);
            String userInfo = url.getUserInfo();

            // 检查主机和端口是否有效
            String host = url.getHost();
            int port = url.getPort();
            
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("代理URL中未包含有效的主机地址");
            }
            
            if (port == -1) {
                // 如果没有指定端口，使用默认端口
                if ("http".equals(url.getProtocol())) {
                    port = 80;
                } else if ("https".equals(url.getProtocol())) {
                    port = 443;
                } else {
                    throw new IllegalArgumentException("无法确定代理端口，请在URL中指定端口号");
                }
            }

            // 设置主机和端口
            result.put("host", host);
            result.put("port", String.valueOf(port));

            // 如果有用户认证信息
            if (userInfo != null && !userInfo.isEmpty()) {
                String[] auth = userInfo.split(":");
                if (auth.length == 2) {
                    result.put("username", auth[0]);
                    result.put("password", auth[1]);
                }
            }

            log.debug("解析代理URL: {}, 结果: host={}, port={}, 有认证={}", 
                maskProxyUrl(proxyUrl), 
                result.get("host"), 
                result.get("port"), 
                result.containsKey("username"));
            return result;
        } catch (Exception e) {
            log.error("解析代理URL失败: {}", e.getMessage());
            throw new RuntimeException("解析代理URL失败: " + e.getMessage());
        }
    }

    /**
     * 屏蔽代理URL中的敏感信息用于日志
     */
    private String maskProxyUrl(String proxyUrl) {
        if (proxyUrl == null || !proxyUrl.contains("@")) {
            return proxyUrl;
        }
        
        try {
            URL url = new URL(proxyUrl);
            String userInfo = url.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] auth = userInfo.split(":");
                String maskedUserInfo = auth[0] + ":****";
                return proxyUrl.replace(userInfo, maskedUserInfo);
            }
        } catch (Exception e) {
            // 如果解析失败，返回简单的掩码
            return proxyUrl.replaceAll("://[^@]+@", "://****:****@");
        }
        
        return proxyUrl;
    }

    /**
     * 清除缓存，强制重新创建HttpClient
     * （当代理配置变更时可以调用）
     */
    public synchronized void clearCache() {
        cachedHttpClient = null;
        lastProxyUrl = null;
        log.debug("代理HttpClient缓存已清除");
    }
} 