#!/bin/bash

# DockPilot 热更新启动脚本
# 支持运行时下载应用代码，无需重新构建镜像

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# 优雅停止处理函数
cleanup() {
    log_info "收到停止信号，正在优雅关闭..."
    
    # 停止Java应用
    if [ ! -z "$JAVA_PID" ]; then
        log_info "停止Java应用进程: $JAVA_PID"
        kill -TERM "$JAVA_PID" 2>/dev/null || true
        wait "$JAVA_PID" 2>/dev/null || true
    fi
    
    # 停止Caddy
    if [ ! -z "$CADDY_PID" ]; then
        log_info "停止Caddy进程: $CADDY_PID"
        kill -TERM "$CADDY_PID" 2>/dev/null || true
        wait "$CADDY_PID" 2>/dev/null || true
    fi
    
    log_info "所有服务已停止"
    exit 0
}

# 注册信号处理
trap cleanup TERM INT

log_info "🚀 DockPilot 热更新版本启动中..."

# 创建必要的目录
log_info "📁 创建应用目录结构..."
mkdir -p /dockpilot/data /dockpilot/logs /dockpilot/uploads
mkdir -p /mnt/host/dockpilot/data /mnt/host/dockpilot/logs /mnt/host/dockpilot/uploads

# 设置权限
chmod 700 /dockpilot/data
chmod 755 /dockpilot/logs /dockpilot/uploads
chmod 777 /mnt/host/dockpilot/data /mnt/host/dockpilot/logs /mnt/host/dockpilot/uploads

# 检查是否需要下载应用代码
check_and_download_app() {
    local current_version=""
    local target_version="${DOCKPILOT_VERSION:-latest}"
    
    # 检查当前版本
    if [ -f "/dockpilot/data/current_version" ]; then
        current_version=$(cat /dockpilot/data/current_version)
        log_info "当前版本: $current_version"
    else
        log_info "首次启动，未找到版本信息"
    fi
    
    # 检查是否需要下载
    if [ ! -f "/app/app.jar" ] || [ "$current_version" != "$target_version" ]; then
        log_info "🔄 需要下载应用代码..."
        log_info "目标版本: $target_version"
        
        # 获取最新版本（如果target_version是latest）
        if [ "$target_version" = "latest" ]; then
            log_info "获取GitHub最新版本..."
            target_version=$(curl -s https://api.github.com/repos/kidoneself/DockPilot/releases/latest | jq -r '.tag_name' 2>/dev/null || echo "")
            
            if [ -z "$target_version" ] || [ "$target_version" = "null" ]; then
                log_warn "无法获取最新版本，使用预设版本"
                target_version="v1.0.0"
            fi
            
            log_info "最新版本: $target_version"
        fi
        
        # 调用下载脚本
        if ! /app/download-app.sh "$target_version"; then
            log_error "应用代码下载失败"
            
            # 如果有现有的应用，继续使用
            if [ -f "/app/app.jar" ]; then
                log_warn "使用现有的应用代码继续启动"
            else
                log_error "没有可用的应用代码，无法启动"
                exit 1
            fi
        else
            # 更新版本记录
            echo "$target_version" > /dockpilot/data/current_version
            log_info "✅ 应用代码已更新到版本: $target_version"
        fi
    else
        log_info "✅ 应用代码已是最新版本: $current_version"
    fi
}

# 启动Caddy
start_caddy() {
    log_info "🌐 启动Caddy服务..."
    caddy run --config /etc/caddy/Caddyfile &
    CADDY_PID=$!
    
    # 等待Caddy启动
    sleep 2
    
    if kill -0 "$CADDY_PID" 2>/dev/null; then
        log_info "✅ Caddy启动成功 (PID: $CADDY_PID)"
    else
        log_error "❌ Caddy启动失败"
        exit 1
    fi
}

# 启动Java应用
start_java() {
    log_info "☕ 启动Java后端服务..."
    
    # 检查jar文件是否存在
    if [ ! -f "/app/app.jar" ]; then
        log_error "后端jar文件不存在"
        return 1
    fi
    
    # 设置环境变量
    export SPRING_PROFILES_ACTIVE=prod
    export LOG_PATH=/dockpilot/logs
    
    # 启动Java应用
    java -jar /app/app.jar &
    JAVA_PID=$!
    
    # 等待Java应用启动
    log_info "⏳ 等待Java应用启动..."
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if kill -0 "$JAVA_PID" 2>/dev/null; then
            # 尝试访问健康检查端点
            if curl -s http://localhost:8080/api/update/version >/dev/null 2>&1; then
                log_info "✅ Java应用启动成功 (PID: $JAVA_PID)"
                
                # 显示完整的启动信息
                show_startup_info
                
                return 0
            fi
        else
            log_error "❌ Java应用进程意外退出"
            return 1
        fi
        
        sleep 2
        attempt=$((attempt + 1))
    done
    
    log_error "❌ Java应用启动超时"
    return 1
}

# 显示启动信息（完整版）
show_startup_info() {
    local current_version=$(cat /dockpilot/data/current_version 2>/dev/null || echo "unknown")
    
    log_info "=========================================="
    log_info "🎉 DockPilot 热更新版本启动完成！"
    log_info "=========================================="
    log_info "📋 服务信息:"
    log_info "  • 前端访问地址: http://localhost:8888"
    log_info "  • 当前版本: $current_version"
    log_info "  • 更新方式: 热更新 (容器内)"
    log_info "📊 进程信息:"
    log_info "  • Caddy PID: $CADDY_PID"
    log_info "  • Java PID: $JAVA_PID"
    log_info "📁 路径信息:"
    log_info "  • 前端目录: /usr/share/html"
    log_info "  • 后端jar: /app/app.jar"
    log_info "  • 数据目录: /dockpilot"
    log_info "=========================================="
    log_info "💡 提示: 可通过右上角更新按钮检查和执行热更新"
    log_info "=========================================="
}

# 显示初始启动信息
show_startup_info_initial() {
    log_info "=========================================="
    log_info "🌐 DockPilot Web服务已启动！"
    log_info "=========================================="
    log_info "📋 当前状态:"
    log_info "  • 前端访问地址: http://localhost:8888"
    log_info "  • Web服务: 运行中 (显示初始化页面)"
    log_info "  • 应用代码: 后台下载中..."
    log_info "📊 进程信息:"
    log_info "  • Caddy PID: $CADDY_PID"
    log_info "  • Java服务: 等待代码下载完成"
    log_info "=========================================="
    log_info "💡 您现在可以访问 http://localhost:8888 查看初始化进度"
    log_info "=========================================="
}

# 主启动流程
main() {
    log_info "开始主启动流程..."
    
    # 1. 先启动Caddy（显示初始化页面）
    start_caddy
    
    # 2. 后台检查并下载应用代码
    log_info "🔄 后台开始下载应用代码..."
    (
        check_and_download_app
        if [ $? -eq 0 ]; then
            log_info "✅ 应用代码准备完成，启动后端服务..."
            start_java
        else
            log_error "❌ 应用代码下载失败，仅提供Web服务"
        fi
    ) &
    
    # 3. 显示启动信息
    show_startup_info_initial
    
    # 4. 保持运行，等待信号
    log_info "🔄 服务运行中，等待信号..."
    
    # 支持传入的参数（如果有的话）
    if [ $# -gt 0 ]; then
        log_info "执行传入的命令: $@"
        exec "$@"
    else
        # 等待所有后台进程
        wait
    fi
}

# 执行主流程
main "$@" 