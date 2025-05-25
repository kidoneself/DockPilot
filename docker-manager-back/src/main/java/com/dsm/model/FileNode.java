package com.dsm.model;

import java.util.List;

public class FileNode {
    private String name;
    private String path;
    private boolean isDirectory;
    private List<FileNode> children;

    public FileNode() {
    }

    public FileNode(String name, String path, boolean isDirectory, List<FileNode> children) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public void setChildren(List<FileNode> children) {
        this.children = children;
    }
} 