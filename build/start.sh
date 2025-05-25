#!/bin/sh

# 创建容器内目录
mkdir -p /dockpilot/data /dockpilot/logs /dockpilot/uploads
chmod 700 /dockpilot/data
chmod 755 /dockpilot/logs
chmod 755 /dockpilot/uploads

# 创建宿主机目录
mkdir -p /mnt/host/dockpilot/data /mnt/host/dockpilot/logs /mnt/host/dockpilot/uploads
chmod 777 /mnt/host/dockpilot/data
chmod 777 /mnt/host/dockpilot/logs
chmod 777 /mnt/host/dockpilot/uploads

# 启动nginx
nginx -g "daemon off;" &

# 设置环境变量并启动Java应用
export SPRING_PROFILES_ACTIVE=prod
export LOG_PATH=/dockpilot/logs
java -jar app.jar &

# 等待所有后台进程
wait 