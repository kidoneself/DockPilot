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

### 安装步骤

```bash
docker run -d --privileged \
   -p 8888:80 \
   --name dockpilot \
   -v /var/run/docker.sock:/var/run/docker.sock \
   -v /:/mnt/host \
   kidself/dockpilot:latest

```

4. 访问系统
打开浏览器访问：http://IP:8888

## 📋 开发计划

- [ ] 完善导航栏的地址添加
- [ ] 应用商店增加接口导入
- [ ] 自定义背景图
- [ ] 添加分组，系统UI，APP UI
- [ ] 备份Docker文件夹
- [ ] 保存的文件放到data文件夹映射出来
- [ ] 发散中...

## 🤝 贡献指南
欢迎提交 Issue 和 Pull Request 来帮助改进项目。

Telegram 群组：https://t.me/+hGFa3joV-TNhNDc1

微信群：
![微信二维码](https://raw.githubusercontent.com/kidoneself/dmc/refs/heads/feature/websocket/docs/wechat-qr.png)

## 📄 许可证
本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。