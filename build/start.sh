#!/bin/sh

# 优雅停止处理函数
cleanup() {
    echo "收到停止信号，正在优雅关闭..."
    # 停止Java应用
    if [ ! -z "$JAVA_PID" ]; then
        kill -TERM "$JAVA_PID"
        wait "$JAVA_PID"
    fi
    # 停止Caddy
    if [ ! -z "$CADDY_PID" ]; then
        kill -TERM "$CADDY_PID"
        wait "$CADDY_PID"
    fi
    echo "所有服务已停止"
    exit 0
}

# 注册信号处理
trap cleanup TERM INT

echo "DockPilot 容器启动中..."

# 创建容器内目录
echo "创建应用目录..."
mkdir -p /dockpilot/data /dockpilot/logs /dockpilot/uploads
chmod 700 /dockpilot/data
chmod 755 /dockpilot/logs
chmod 755 /dockpilot/uploads

# 创建宿主机目录
echo "创建宿主机映射目录..."
mkdir -p /mnt/host/dockpilot/data /mnt/host/dockpilot/logs /mnt/host/dockpilot/uploads
chmod 777 /mnt/host/dockpilot/data
chmod 777 /mnt/host/dockpilot/logs
chmod 777 /mnt/host/dockpilot/uploads

# 启动Caddy
echo "启动Caddy服务..."
caddy run --config /etc/caddy/Caddyfile &
CADDY_PID=$!

# 等待Caddy启动
sleep 2

# 设置环境变量并启动Java应用
echo "启动后端服务..."
export SPRING_PROFILES_ACTIVE=prod
export LOG_PATH=/dockpilot/logs
java -jar app.jar &
JAVA_PID=$!

echo "DockPilot 容器启动完成！"
echo "前端访问地址: http://localhost:8888"
echo "Caddy PID: $CADDY_PID"
echo "Java PID: $JAVA_PID"

# 支持传入的参数（如果有的话）
if [ $# -gt 0 ]; then
    echo "执行传入的命令: $@"
    exec "$@"
else
    # 等待所有后台进程
    wait
fi 