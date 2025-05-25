package com.dsm.controller;

import com.dsm.model.FileNode;
import com.dsm.utils.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/file-tree")
public class FileTreeController {

    private static final int MAX_FILES_PER_DIR = 100;  // 每个目录最大文件数

    @GetMapping("/list")
    public ApiResponse<List<FileNode>> getFileTree(
            @RequestParam(defaultValue = "/") String path,
            @RequestParam(defaultValue = "100") int maxFilesPerDir) {
        try {
            List<FileNode> nodes = buildFileTree(path, Math.min(maxFilesPerDir, MAX_FILES_PER_DIR));
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
                node.setPath(file.getAbsolutePath());
                node.setDirectory(file.isDirectory());
                node.setChildren(new ArrayList<>());  // 初始化为空列表，表示未加载子节点

                result.add(node);
                count++;
            }
        }

        return result;
    }
} 