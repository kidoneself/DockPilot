//package com.dsm.utils;
//
//import java.util.*;
//
//public class PathMappingTest {
//
//    public static void main(String[] args) {
//        List<String> paths = Arrays.asList(
//                "/Users/lizhiqiang/testDocker/new/docker/config",
//                "/Users/lizhiqiang/testDocker/new/docker/115/naspt-115-emby/config",
//                "/Users/lizhiqiang/testDocker/new/docker/115/naspt-115-cms/cache",
//                "/Users/lizhiqiang/testDocker/new/docker/115/naspt-115-cms/logs",
//                "/Users/lizhiqiang/testDocker/new/docker/115/naspt-115-cms/config",
//                "/Users/lizhiqiang/testDocker/new/docker/115/naspt-cloudsaver/data",
//                "/Users/lizhiqiang/testDocker/new/docker/115/naspt-cloudsaver/config",
//                "/Users/lizhiqiang2/testDocker2/media",                           // 不同用户目录
//                "/",                                             // 根路径
//                "/var/lib/docker/volumes/abc123/_data",
//                "/var/lib/docker/volumes/def456/_data",
//                "/var/lib/docker/volumes/xyz789/_data",
//                "/srv/containers/emby/cache",                                    // 新分支路径
//                "/Users/lizhiqiang/testDocker/new/docker/media",                // 跟已有路径接近
//                "/Users/lizhiqiang/testDocker/new/media"                        // 与 common 有一定前缀
//        );
//
//        Map<String, String> baseEnv = new LinkedHashMap<>();
//        Map<String, String> pathToEnv = new LinkedHashMap<>();
//        int baseCount = 1;
//
//        for (String path : paths) {
//            boolean matched = false;
//            for (Map.Entry<String, String> entry : baseEnv.entrySet()) {
//                String base = entry.getValue();
//                if (path.equals(base) || path.startsWith(base + "/")) {
//                    String envName = entry.getKey();
//                    String relative = path.substring(base.length());
//                    // 去掉相对路径开头的斜杠，避免双斜杠
//                    if (relative.startsWith("/")) {
//                        relative = relative.substring(1);
//                    }
//                    pathToEnv.put(path, String.format("${%s}/%s", envName, relative.isEmpty() ? "" : relative));
//                    matched = true;
//                    break;
//                }
//            }
//
//            if (!matched) {
//                String base;
//                if (path.equals("/")) {
//                    base = "/";
//                } else {
//                    String[] segments = path.split("/");
//                    int endIndex = Math.min(4, segments.length);
//                    // 防止 subList 越界
//                    if (endIndex <= 1) {
//                        endIndex = 2;
//                    }
//                    base = "/" + String.join("/", Arrays.asList(segments).subList(1, endIndex));
//                }
//                String envName = "BASE_" + baseCount++;
//                baseEnv.put(envName, base);
//
//                String relative = path.substring(base.length());
//                if (relative.startsWith("/")) {
//                    relative = relative.substring(1);
//                }
//                pathToEnv.put(path, String.format("${%s}/%s", envName, relative.isEmpty() ? "" : relative));
//            }
//        }
//
//        System.out.println("=== ENV Mappings ===");
//        baseEnv.forEach((k, v) -> System.out.println(k + "=" + v));
//
//        System.out.println("\n=== Path Remapping ===");
//        pathToEnv.forEach((k, v) -> System.out.println(k + " -> " + v));
//    }
//}