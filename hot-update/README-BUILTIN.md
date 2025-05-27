# DockPilot 内置版本 + 热更新 Hybrid 方案

## 🎯 方案概述

这是一个 **双保险机制**，结合了镜像内置代码和热更新的优势：

- 🏗️ **镜像构建时**: 打包当前最新的前后端代码作为基础版本
- 🚀 **容器启动时**: 尝试下载更新的代码版本  
- 🛡️ **下载失败时**: 自动fallback到镜像内置的基础版本

## 🔄 工作流程

### 1. 镜像构建阶段
```dockerfile
# 多阶段构建
FROM node:18-alpine AS frontend-builder
# 构建前端代码

FROM maven:3.8-openjdk-11-slim AS backend-builder  
# 构建后端代码

FROM alpine:3.21
# 复制内置版本到镜像
COPY --from=frontend-builder /build/dist/ /usr/share/html-builtin/
COPY --from=backend-builder /build/target/*.jar /app/builtin/backend.jar

# 初始化：复制内置版本到运行目录
RUN cp -r /usr/share/html-builtin/* /usr/share/html/ && \
    cp /app/builtin/backend.jar /app/app.jar && \
    echo "builtin-version" > /dockpilot/data/current_version
```

### 2. 容器启动阶段
```bash
# 启动流程
1. 检查内置版本是否可用 ✅
2. 尝试下载最新版本代码
   ├─ 成功 → 使用最新版本 🎉
   └─ 失败 → fallback到内置版本 🛡️
3. 启动服务
```

### 3. 运行时热更新
```bash
# 用户可以随时热更新
1. 点击更新按钮
2. 下载新版本代码
   ├─ 成功 → 热更新完成 🔥
   └─ 失败 → 保持当前版本 ⚡
```

## 📁 目录结构

```
/app/
├── app.jar                    # 当前运行的后端jar
├── builtin/
│   └── backend.jar           # 内置的后端jar（镜像构建时打包）
├── init-builtin.sh           # 内置版本管理脚本
├── download-app.sh           # 热更新下载脚本
└── start-hot-update.sh       # 启动脚本

/usr/share/
├── html/                     # 当前运行的前端文件
└── html-builtin/             # 内置的前端文件（镜像构建时打包）

/dockpilot/data/
└── current_version           # 当前版本标识文件
```

## 🛡️ Fallback 机制

### 触发条件
- 🌐 网络连接失败
- 📡 GitHub API 访问失败  
- 📦 代码包下载失败
- 🔍 版本信息获取失败

### 处理策略
```bash
# 优先级顺序
1. 尝试下载最新版本
2. 使用现有版本（如果存在）
3. 启用内置版本fallback
4. 报错退出（最后手段）
```

### 版本标识
- `v1.2.3` - 正常的GitHub Release版本
- `builtin-version` - 内置版本标识
- `unknown` - 版本信息丢失

## 🔧 管理命令

### 内置版本管理
```bash
# 检查内置版本完整性
/app/init-builtin.sh check

# 恢复到内置版本
/app/init-builtin.sh restore

# 查看内置版本信息
/app/init-builtin.sh info
```

### 测试功能
```bash
# 运行完整测试
/app/test-builtin.sh

# 手动测试fallback
export BUILTIN_FALLBACK=true
export DOCKPILOT_VERSION=latest
```

## 🚀 部署方式

### 方式一：直接使用（推荐）
```bash
docker run -d --privileged \
  --name dockpilot \
  -p 8888:8888 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /:/mnt/host \
  -v /home/dockpilot:/dockpilot \
  --restart unless-stopped \
  kidself/dockpilot:latest
```

### 方式二：指定版本
```bash
docker run -d --privileged \
  --name dockpilot \
  -p 8888:8888 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /:/mnt/host \
  -v /home/dockpilot:/dockpilot \
  -e DOCKPILOT_VERSION=v1.2.3 \
  --restart unless-stopped \
  kidself/dockpilot:hot
```

### 方式三：强制使用内置版本
```bash
docker run -d --privileged \
  --name dockpilot \
  -p 8888:8888 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /:/mnt/host \
  -v /home/dockpilot:/dockpilot \
  -e DOCKPILOT_VERSION=builtin-version \
  -e BUILTIN_FALLBACK=true \
  --restart unless-stopped \
  kidself/dockpilot:hot
```

## 📊 优势对比

| 特性 | 纯热更新 | 纯镜像 | Hybrid方案 |
|------|----------|--------|------------|
| 启动速度 | 慢（需下载） | 快 | 快 |
| 网络依赖 | 强 | 无 | 弱 |
| 更新灵活性 | 高 | 低 | 高 |
| 可用性保障 | 低 | 高 | 高 |
| 镜像大小 | 小 | 大 | 中等 |
| 运维复杂度 | 高 | 低 | 中等 |

## 🔍 监控和诊断

### 检查当前状态
```bash
# 查看当前版本
cat /dockpilot/data/current_version

# 检查内置版本
/app/init-builtin.sh info

# 查看启动日志
docker logs dockpilot
```

### 常见问题排查
```bash
# 问题1: 启动时显示"使用内置版本"
# 原因: 网络问题或GitHub访问失败
# 解决: 检查网络，稍后可手动热更新

# 问题2: 热更新失败
# 原因: 下载超时或文件损坏
# 解决: 重试更新，或重启容器

# 问题3: 内置版本损坏
# 原因: 镜像构建问题
# 解决: 重新拉取镜像
```

## 🎯 最佳实践

### 1. 版本管理
- ✅ 使用语义化版本号
- ✅ 定期更新内置版本（重新构建镜像）
- ✅ 保持热更新包的及时发布

### 2. 监控建议
- 📊 监控容器启动时间
- 📊 监控热更新成功率
- 📊 监控fallback触发频率

### 3. 运维建议
- 🔄 定期测试热更新功能
- 🛡️ 在网络不稳定环境优先使用内置版本
- 📦 重要更新时同时更新镜像和热更新包

## 🚫 注意事项

### 构建要求
- 需要在有网络的环境构建镜像
- 前后端代码必须能正常编译
- 构建时间会增加（约5-10分钟）

### 运行要求
- 内置版本占用额外存储空间（约50-100MB）
- 需要设置 `BUILTIN_FALLBACK=true` 启用fallback
- 首次启动可能需要更长时间

### 兼容性
- 支持 linux/amd64 和 linux/arm64
- 需要 Docker 18.09+ 支持多阶段构建
- 前端需要支持静态文件部署

## 📝 更新日志

### v1.0.0 (2024-01-XX)
- ✨ 新增内置版本功能
- 🛡️ 新增自动fallback机制
- 🔧 优化启动流程
- 📚 完善文档和测试

---

**这个方案完美解决了你提到的问题：既保证了热更新的灵活性，又确保了服务的可用性！** 🎉 