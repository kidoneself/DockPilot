package com.dsm.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 网站图标获取工具类
 * 用于获取网站的 favicon 图标
 */
public class FaviconFetcher {

    private static final String DEFAULT_PROTOCOL = "http://";

    private FaviconFetcher() {
        // 私有构造函数，防止实例化
    }

    /**
     * 获取网站的 Favicon URL
     *
     * @param urlStr 网站URL
     * @return favicon的URL，如果获取失败则返回null
     */
    public static String getFavicon(String urlStr) {
        try {
            if (urlStr == null || urlStr.trim().isEmpty()) {
                LogUtil.logSysError("URL不能为空");
                return null;
            }

            String normalizedUrl = normalizeUrl(urlStr);
            String faviconUrl = getFaviconFromSite(normalizedUrl);

            if (faviconUrl != null && isValidFavicon(faviconUrl)) {
                return faviconUrl;
            }
        } catch (Exception e) {
            LogUtil.logSysError("获取favicon失败: " + urlStr + ", error: " + e.getMessage());
        }
        return null;
    }

    /**
     * 规范化URL，确保包含协议
     */
    private static String normalizeUrl(String urlStr) {
        urlStr = urlStr.trim();
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            urlStr = DEFAULT_PROTOCOL + urlStr;
        }
        return urlStr;
    }

    /**
     * 从网站获取favicon URL
     */
    private static String getFaviconFromSite(String urlStr) {
        try {
            Document doc = Jsoup.connect(urlStr).get();
            Element link = doc.select("link[rel~=(?i)^(shortcut )?icon$]").first();

            if (link != null) {
                String href = link.attr("href");
                return new URL(new URL(urlStr), href).toString();
            }

            return new URL(new URL(urlStr), "/favicon.ico").toString();
        } catch (Exception e) {
            LogUtil.logSysError("从网站获取favicon失败: " + urlStr + ", error: " + e.getMessage());
            return null;
        }
    }

    /**
     * 验证favicon URL是否有效
     */
    private static boolean isValidFavicon(String faviconUrl) {
        try {
            URL url = new URL(faviconUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String contentType = connection.getContentType();
                return contentType != null && contentType.startsWith("image/");
            }
        } catch (Exception e) {
            LogUtil.logSysError("验证favicon失败: " + faviconUrl + ", error: " + e.getMessage());
        }
        return false;
    }
} 