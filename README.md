# DockPilot

<div align="center">

[![Github](https://img.shields.io/badge/Github-123456?logo=github&labelColor=242424)](https://github.com/kidoneself/dockpilot)
[![Docker](https://img.shields.io/badge/docker-123456?logo=docker&logoColor=fff&labelColor=1c7aed)](https://hub.docker.com/r/kidself/dockpilot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

</div>

一个现代化的 Docker 管理平台，提供容器管理、镜像管理、应用商店等功能。支持代理热更新、镜像加速等特性，方便用户快速部署和管理 Docker 应用。

## 😎 主要特性

- 🐳 完整的容器生命周期管理
- 🖼️ 强大的镜像管理功能
- 🏪 丰富的应用商店
- 🔧 灵活的系统设置
- 📊 详细的日志管理
- 🚀 支持代理热更新
- ⚡ 镜像加速支持
- 🎨 美观的现代化界面

## 🛠️ 技术栈

- 前端：Vue 3 + TypeScript + TDesign
- 后端：Spring Boot + Java
- 数据库：SQLite
- 容器：Docker

## 🚀 快速开始

### 环境要求
- Docker 20.10.0 或更高版本
- JDK 17 或更高版本
- Node.js 16 或更高版本

### 方法一：使用运行脚本（推荐）
```bash
# 进入build目录
cd build

# 运行脚本（默认使用test测试版本）
./run-dockerhub.sh

# 指定其他版本
./run-dockerhub.sh latest      # 使用最新版本
./run-dockerhub.sh v1.0.0      # 使用指定版本
./run-dockerhub.sh test 9999   # 使用test版本，自定义端口
```

### 方法二：手动运行
```bash
# 拉取测试版镜像（默认）
docker pull kidself/dockpilot:test

# 运行容器
docker run -d --privileged \
   -p 8888:8888 \
   --name dockpilot \
   -v /var/run/docker.sock:/var/run/docker.sock \
   -v /:/mnt/host \
   -v dockpilot-data:/dockpilot \
   --restart unless-stopped \
   kidself/dockpilot:test
```

### 访问系统
打开浏览器访问：http://localhost:8888

### 常用命令
```bash
# 查看日志
docker logs -f dockpilot

# 停止容器
docker stop dockpilot

# 重启容器
docker restart dockpilot
```

## 🏗️ 开发部署

### 热更新发布（推荐）
项目采用热更新机制，通过 GitHub Actions 自动构建发布：

- **自动触发**: 推送版本标签时自动构建
- **构建产物**: `frontend.tar.gz` + `backend.jar`
- **发布速度**: 3-5分钟快速构建
- **用户体验**: 容器内一键热更新，无需重新部署

**发布流程**:
```bash
# 开发完成后创建版本标签
git tag v1.x.x
git push origin v1.x.x

# GitHub Actions 自动构建发布
# 用户通过管理界面一键热更新
```

### 本地开发环境

#### 前端开发
```bash
cd dockpilotfront
npm install
npm run dev
```

#### 后端开发
```bash
cd docker-manager-back
mvn spring-boot:run
```

## 📋 开发计划

开发计划和未来功能可以直接查看[开发任务](开发任务.md)文档。

## 🤝 贡献指南
欢迎提交 Issue 和 Pull Request 来帮助改进项目。

Telegram 群组：https://t.me/+hGFa3joV-TNhNDc1

微信群：

![微信二维码](./wechat-qr.png)

## 📄 许可证
本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。