package com.dockpilot.controller;

import com.dockpilot.model.FileNode;
import com.dockpilot.utils.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/file-tree")
public class FileTreeController {

    private static final int MAX_FILES_PER_DIR = 100;  // 每个目录最大文件数

    @GetMapping("/list")
    public ApiResponse<List<FileNode>> getFileTree(
            @RequestParam(defaultValue = "/") String path,
            @RequestParam(defaultValue = "100") int maxFilesPerDir) {
        try {
            // 将所有路径映射到宿主机挂载目录
            String actualPath = "/".equals(path) ? "/mnt/host" : "/mnt/host" + path;
            List<FileNode> nodes = buildFileTree(actualPath, Math.min(maxFilesPerDir, MAX_FILES_PER_DIR));
            return ApiResponse.success(nodes);
        } catch (Exception e) {
            return ApiResponse.error("获取文件列表失败: " + e.getMessage());
        }
    }

    private List<FileNode> buildFileTree(String rootPath, int maxFilesPerDir) {
        List<FileNode> result = new ArrayList<>();
        File root = new File(rootPath);

        if (!root.exists()) {
            return result;
        }

        File[] files = root.listFiles();
        if (files != null) {
            int count = 0;
            for (File file : files) {
                if (count >= maxFilesPerDir) {
                    break;
                }

                FileNode node = new FileNode();
                node.setName(file.getName());
                // 将容器内路径转换回前端路径（去掉 /mnt/host 前缀）
                String absolutePath = file.getAbsolutePath();
                String frontendPath = absolutePath.startsWith("/mnt/host") ? 
                    absolutePath.substring(9) : absolutePath;  // 去掉 "/mnt/host" (9个字符)
                if (frontendPath.isEmpty()) {
                    frontendPath = "/";
                }
                node.setPath(frontendPath);
                node.setDirectory(file.isDirectory());
                node.setChildren(new ArrayList<>());  // 初始化为空列表，表示未加载子节点

                result.add(node);
                count++;
            }
        }

        return result;
    }
} 