//package com.dsm.controller;
//
//import com.dsm.model.FileNode;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//public class FileTreeControllerTest {
//
//    private final FileTreeController fileTreeController = new FileTreeController();
//
//    @Test
//    public void testGetFileTree() {
//        // 测试获取指定目录的直接子文件和子目录
//        String testPath = "/usr";
//        List<FileNode> result = fileTreeController.getFileTree(testPath, 50);
//
//        // 验证结果
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//
//        // 打印文件树结构
//        System.out.println("目录: " + testPath);
//        printFileTree(result, 0);
//    }
//
//    @Test
//    public void testGetFileTreeWithDifferentPaths() {
//        // 测试不同目录的文件树
//        String[] testPaths = {
//            "/Users/lizhiqiang/testDocker/new/docker/config",
//            "/Users/lizhiqiang/testDocker/new/docker"
//        };
//
//        for (String path : testPaths) {
//            System.out.println("\n测试目录: " + path);
//            List<FileNode> result = fileTreeController.getFileTree(path, 20);
//            printFileTree(result, 0);
//        }
//    }
//
//    private void printFileTree(List<FileNode> nodes, int level) {
//        String indent = "  ".repeat(level);
//        for (FileNode node : nodes) {
//            System.out.println(indent + (node.isDirectory() ? "📁 " : "📄 ") + node.getName());
//            // 注意：这里不再递归打印子节点，因为子节点是懒加载的
//        }
//    }
//}