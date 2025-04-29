package com.dsm.service.impl;

import com.dsm.config.AppConfig;
import com.dsm.event.SystemSettingChangedEvent;
import com.dsm.mapper.SystemSettingMapper;
import com.dsm.service.SystemSettingService;
import com.dsm.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.net.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    @Autowired
    private SystemSettingMapper systemSettingMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AppConfig appConfig;

    @Override
    public Map<String, Long> testProxyLatency() {

        Map<String, Long> result = new HashMap<>();
        try {
            String proxyUrl = appConfig.getProxyUrl();
            Map<String, String> proxyInfo = parseProxyUrl(proxyUrl);

            // 设置代理认证（只有当用户名和密码都存在时才设置）
            if (proxyInfo.containsKey("username") && proxyInfo.containsKey("password")) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyInfo.get("username"), proxyInfo.get("password").toCharArray());
                    }
                });
                LogUtil.logSysInfo("已设置代理认证信息");
            } else {
                LogUtil.logSysInfo("未设置代理认证信息，将使用无认证代理");
            }

            // 创建HTTP代理对象
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyInfo.get("host"), Integer.parseInt(proxyInfo.get("port"))));

            // 测试HTTP连接
            long httpStartTime = System.currentTimeMillis();
            URL httpUrl = new URL("http://registry-1.docker.io/v2/");
            HttpURLConnection httpConnection = (HttpURLConnection) httpUrl.openConnection(proxy);
            httpConnection.setConnectTimeout(5000);
            httpConnection.setReadTimeout(5000);
            httpConnection.connect();
            int httpResponseCode = httpConnection.getResponseCode();
            long httpResponseTime = System.currentTimeMillis() - httpStartTime;

            // 测试HTTPS连接
            long httpsStartTime = System.currentTimeMillis();
            URL httpsUrl = new URL("https://registry-1.docker.io/v2/");
            HttpURLConnection httpsConnection = (HttpURLConnection) httpsUrl.openConnection(proxy);
            httpsConnection.setConnectTimeout(5000);
            httpsConnection.setReadTimeout(5000);
            httpsConnection.connect();
            int httpsResponseCode = httpsConnection.getResponseCode();
            long httpsResponseTime = System.currentTimeMillis() - httpsStartTime;

            // 记录结果
            result.put("httpConnectTime", httpResponseTime);
            result.put("httpStatus", (long) httpResponseCode);
            result.put("httpsConnectTime", httpsResponseTime);
            result.put("httpsStatus", (long) httpsResponseCode);
            result.put("totalTime", Math.max(httpResponseTime, httpsResponseTime));
            // 根据延迟时间给出建议
            long totalTime = result.get("totalTime");
            String suggestion;
            if (totalTime < 500) {
                suggestion = "代理速度良好，建议使用代理";
            } else if (totalTime < 1000) {
                suggestion = "代理速度一般，可以使用代理";
            } else if (totalTime < 2000) {
                suggestion = "代理速度较慢，建议检查代理配置";
            } else {
                suggestion = "代理速度很慢，建议不使用代理";
            }

            // 添加建议到返回结果
            result.put("suggestion", (long) suggestion.hashCode());
            LogUtil.logSysInfo("代理延迟测试结果: HTTP连接时间=" + httpResponseTime + "ms, HTTP状态码=" + httpResponseCode + ", HTTPS连接时间=" + httpsResponseTime + "ms, HTTPS状态码=" + httpsResponseCode);

        } catch (Exception e) {
            LogUtil.logSysError("测试代理延迟失败: " + e.getMessage());
            result.put("error", 1L);
            result.put("message", (long) e.getMessage().hashCode());
        } finally {
            // 清除代理认证
            Authenticator.setDefault(null);
        }
        return result;
    }

    @Override
    public String get(String key) {
        return systemSettingMapper.getSettingValue(key);
    }

    @Override
    public void set(String key, String value) {
        String oldValue = get(key);
        systemSettingMapper.setSettingValue(key, value);
        // 发布配置变更事件
        eventPublisher.publishEvent(new SystemSettingChangedEvent(key, oldValue, value));
    }

    /**
     * 解析代理URL，提取用户名、密码、主机和端口
     *
     * @param proxyUrl 代理URL，格式如：http://username:password@host:port 或 http://host:port
     * @return 包含代理信息的Map
     */
    private Map<String, String> parseProxyUrl(String proxyUrl) {
        Map<String, String> result = new HashMap<>();
        try {
            URL url = new URL(proxyUrl);
            String userInfo = url.getUserInfo();

            // 设置主机和端口
            result.put("host", url.getHost());
            result.put("port", String.valueOf(url.getPort()));

            // 如果有用户认证信息
            if (userInfo != null && !userInfo.isEmpty()) {
                String[] auth = userInfo.split(":");
                if (auth.length == 2) {
                    result.put("username", auth[0]);
                    result.put("password", auth[1]);
                }
            }

            LogUtil.logSysInfo("解析代理URL: " + proxyUrl + ", 结果: " + result);
            return result;
        } catch (Exception e) {
            LogUtil.logSysError("解析代理URL失败: " + e.getMessage());
            throw new RuntimeException("解析代理URL失败: " + e.getMessage());
        }
    }
}