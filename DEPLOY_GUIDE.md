# 🐳 DockPilot Docker部署指南

## 📋 项目信息
- **项目名称**: DockPilot 
- **Docker镜像**: `kidself/dockpilot`
- **热更新镜像**: `kidself/dockpilot-hot`
- **服务端口**: 8888
- **数据目录**: `/home/dockpilot`

## 🚀 快速部署

### 方式一：传统镜像部署
```bash
# 1. 创建数据目录
sudo mkdir -p /home/dockpilot

# 2. 停止旧容器（如果存在）
sudo docker stop dockpilot 2>/dev/null || true
sudo docker rm dockpilot 2>/dev/null || true

# 3. 启动新容器
sudo docker run -d --privileged \
  --name dockpilot \
  -p 8888:8888 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /:/mnt/host \
  -v /home/dockpilot:/dockpilot \
  --restart unless-stopped \
  kidself/dockpilot:latest
```

### 方式二：热更新镜像部署（推荐）
```bash
# 1. 创建数据目录
sudo mkdir -p /home/dockpilot

# 2. 停止旧容器（如果存在）
sudo docker stop dockpilot-hot 2>/dev/null || true
sudo docker rm dockpilot-hot 2>/dev/null || true

# 3. 启动热更新容器
sudo docker run -d --privileged \
  --name dockpilot-hot \
  -p 8888:8888 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /:/mnt/host \
  -v /home/dockpilot:/dockpilot \
  -e DOCKPILOT_VERSION=v1.0.1 \
  --restart unless-stopped \
  kidself/dockpilot-hot:latest
```

## 🔧 部署参数说明

| 参数 | 说明 |
|---|---|
| `--privileged` | 特权模式，允许容器操作Docker |
| `-p 8888:8888` | 端口映射，宿主机8888映射到容器8888 |
| `-v /var/run/docker.sock:/var/run/docker.sock` | Docker套接字映射，用于容器内操作Docker |
| `-v /:/mnt/host` | 宿主机根目录映射，用于文件系统访问 |
| `-v /home/dockpilot:/dockpilot` | 数据持久化目录 |
| `--restart unless-stopped` | 容器自动重启策略 |

## 🌐 访问地址

部署完成后，通过以下地址访问：
```
http://服务器IP:8888
```

## 📊 查看状态

```bash
# 查看容器状态
sudo docker ps | grep dockpilot

# 查看容器日志
sudo docker logs dockpilot-hot

# 查看热更新容器详细信息
sudo docker inspect dockpilot-hot
```

## 🔄 热更新使用

1. 访问管理界面: `http://服务器IP:8888`
2. 点击右上角的更新按钮（蓝色圆形图标）
3. 点击"检查更新"
4. 如有新版本，点击"开始更新"
5. 等待更新完成（2-5分钟）

## 🛠️ 故障排除

### 容器启动失败
```bash
# 检查Docker服务状态
sudo systemctl status docker

# 检查端口占用
sudo netstat -tlnp | grep 8888

# 查看容器错误日志
sudo docker logs dockpilot-hot
```

### 权限问题
```bash
# 确保数据目录权限正确
sudo chown -R 1000:1000 /home/dockpilot
sudo chmod -R 755 /home/dockpilot
```

### 网络问题
```bash
# 检查防火墙设置
sudo ufw status
sudo ufw allow 8888

# 检查Docker网络
sudo docker network ls
```

## 📋 一键部署脚本

创建 `deploy-dockpilot.sh`：
```bash
#!/bin/bash

echo "🚀 开始部署DockPilot..."

# 创建数据目录
sudo mkdir -p /home/dockpilot

# 停止旧容器
sudo docker stop dockpilot-hot 2>/dev/null || true
sudo docker rm dockpilot-hot 2>/dev/null || true

# 拉取最新镜像
sudo docker pull kidself/dockpilot-hot:latest

# 启动新容器
sudo docker run -d --privileged \
  --name dockpilot-hot \
  -p 8888:8888 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /:/mnt/host \
  -v /home/dockpilot:/dockpilot \
  -e DOCKPILOT_VERSION=v1.0.1 \
  --restart unless-stopped \
  kidself/dockpilot-hot:latest

if [ $? -eq 0 ]; then
    echo "✅ DockPilot部署成功！"
    echo "🌐 访问地址: http://$(curl -s ifconfig.me):8888"
    echo "📊 容器状态: sudo docker ps | grep dockpilot"
else
    echo "❌ DockPilot部署失败"
    exit 1
fi
```

使用方法：
```bash
chmod +x deploy-dockpilot.sh
./deploy-dockpilot.sh
``` 