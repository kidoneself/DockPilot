# DockPilot

简单易用的 Docker 管理工具，基于 Web 界面管理容器、镜像和应用。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## 功能特性

- 容器管理：启动、停止、删除、查看日志
- 镜像管理：拉取、删除、构建镜像  
- 应用商店：一键部署常用应用
- 热更新：支持在线更新，无需重新部署容器

## 快速部署

```bash
docker run -d --privileged \
   -p 8888:8888 \
   --name dockpilot \
   -v /var/run/docker.sock:/var/run/docker.sock \
   -v /:/mnt/host \
   -v /home/dockpilot:/dockpilot \
   --restart unless-stopped \
   kidself/dockpilot:latest
```

部署完成后访问：http://localhost:8888

## 热更新

系统支持在线热更新，点击界面右上角的更新按钮即可更新到最新版本，无需重新部署容器。

## 技术栈

- 前端：Vue3 + TypeScript  
- 后端：Spring Boot
- 数据库：SQLite

## 开发

```bash
# 前端开发
cd dockpilot-frontend
npm install && npm run dev

# 后端开发  
cd dockpilot-backend
mvn spring-boot:run
```

## 联系方式

- Telegram: https://t.me/dockpilot
- 问题反馈：提交 GitHub Issue

## 许可证

MIT License