package com.dsm.test;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileDownloader {

    public static void main(String[] args) {
        // 示例：下载的文件链接
        List<String> urls = List.of(
            "https://pan.naspt.vip/d/123pan/shell/tgz/naspt-mpv2.tgz",
            "https://pan.naspt.vip/d/123pan/shell/tgz/naspt-qb.tgz",
            "https://pan.naspt.vip/d/123pan/shell/tgz/naspt-emby.tgz"
        );

        // 示例：目标目录
        String targetDir = "/Users/lizhiqiang/testDocker/new/docker";

        for (String url : urls) {
            try {
                download(url, targetDir);
            } catch (IOException e) {
                System.err.println("❌ 下载失败: " + url);
                e.printStackTrace();
            }
        }
    }

    public static void download(String fileUrl, String targetDir) throws IOException {
        URL url = new URL(fileUrl);
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        Path targetPath = Path.of(targetDir, fileName);

        // 创建目标目录（如果不存在）
        Files.createDirectories(Path.of(targetDir));

        System.out.println("⬇️ 开始下载：" + fileUrl);
        System.out.println("📂 保存到：" + targetPath);

        URLConnection conn = url.openConnection();
        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(targetPath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        System.out.println("✅ 下载完成！");
    }
}