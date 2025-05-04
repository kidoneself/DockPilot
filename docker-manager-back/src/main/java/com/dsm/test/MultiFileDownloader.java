package com.dsm.test;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MultiFileDownloader {

    public static void main(String[] args) {
        String targetDir = "/Users/lizhiqiang/testDocker/new/docker";

        List<String> urls = List.of(
            "https://docker-template.oss-cn-shanghai.aliyuncs.com/Downloads/naspt-mpv2.tgz",
            "https://docker-template.oss-cn-shanghai.aliyuncs.com/Downloads/naspt-qb.tgz",
            "https://docker-template.oss-cn-shanghai.aliyuncs.com/Downloads/naspt-emby.tgz"
        );

        for (String url : urls) {
            try {
                downloadWithProgress(url, targetDir);
            } catch (IOException e) {
                System.err.println("❌ 下载失败: " + url);
                e.printStackTrace();
            }
        }
    }

    public static void downloadWithProgress(String fileUrl, String targetDir) throws IOException {
        URL url = new URL(fileUrl);
        URLConnection conn = url.openConnection();
        long contentLength = conn.getContentLengthLong();

        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        Path targetPath = Path.of(targetDir, fileName);
        Files.createDirectories(targetPath.getParent());

        try (
            InputStream in = conn.getInputStream();
            OutputStream out = Files.newOutputStream(targetPath)
        ) {
            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int bytesRead;
            long lastPrintedPercent = -1;

            System.out.println("⬇️ 开始下载: " + fileName);
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;

                if (contentLength > 0) {
                    long percent = downloaded * 100 / contentLength;
                    if (percent != lastPrintedPercent) {
                        System.out.print("\r📦 下载进度: " + percent + "%");
                        lastPrintedPercent = percent;
                    }
                }
            }
            System.out.println("\n✅ 下载完成: " + targetPath);
        }
    }
}