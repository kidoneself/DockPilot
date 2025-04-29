package com.dsm.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class JsonPlaceholderExample3 {
    public static void main(String[] args) throws IOException {
        // 创建替换器实例
        JsonPlaceholderReplacer2 replacer = new JsonPlaceholderReplacer2();

        // 准备替换值
        Map<String, String> replacements = new HashMap<>();
        replacements.put("IMAGE_NAME", "hub.naspt.de/jxxghp/moviepilot-v2:latest");
        replacements.put("ICC2022_UID", "24730");
        replacements.put("PORT", "3001");
        replacements.put("NGINX_PORT", "3000");
        replacements.put("DOCKER_PATH", "/Users/lizhiqiang/coding-my/naspt-mpv2");
        replacements.put("MEDIA_PATH", "/Users/lizhiqiang/coding-my/media");
        replacements.put("RESTART_POLICY", "always");

        // 读取 JSON 文件内容
        // 读取 JSON 文件内容
        String jsonContent = new String(Files.readAllBytes(Paths.get("docker-manager-back/src/main/java/com/dsm/test/cmd.json")));

        // 执行替换
        String replacedJson = replacer.replacePlaceholders(jsonContent, replacements);
        System.out.println("替换后的 JSON：");
        System.out.println(replacedJson);
    }
} 