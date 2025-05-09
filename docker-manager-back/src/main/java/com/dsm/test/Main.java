package com.dsm.test;

import com.dsm.utils.FaviconFetcher;

public class Main {
    public static void main(String[] args) {
        // 目标网站链接
        String url = "http://10.10.10.99/webGui/images";
        String faviconUrl = FaviconFetcher.getFavicon(url);
        System.out.println("获取到的 favicon URL: " + faviconUrl);

    }
}