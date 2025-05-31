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

# 版本号比较函数（支持语义化版本号）
# 返回0表示第一个版本大于第二个版本，返回1表示小于等于
version_compare() {
    local version1="$1"
    local version2="$2"
    
    # 移除v前缀
    version1=${version1#v}
    version2=${version2#v}
    
    # 使用sort -V进行版本比较
    if [ "$(printf '%s\n' "$version1" "$version2" | sort -V | head -n1)" != "$version1" ]; then
        return 0  # version1 > version2
    else
        return 1  # version1 <= version2
    fi
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
    local builtin_version="${DOCKPILOT_VERSION:-v1.0.0}"
    
    # 🔥 新增：智能版本管理逻辑
    # 检查持久化版本
    if [ -f "/dockpilot/data/current_version" ]; then
        current_version=$(cat /dockpilot/data/current_version)
        log_info "持久化版本: $current_version"
    else
        log_info "首次启动，未找到持久化版本信息"
        current_version="v0.0.0"  # 设置一个很低的版本号
    fi
    
    # 🔥 新增：检查内置版本是否可用
    local builtin_available=false
    if [ "$BUILTIN_FALLBACK" = "true" ] && /app/init-builtin.sh check >/dev/null 2>&1; then
        builtin_available=true
        log_info "🛡️ 内置版本可用作为备选方案"
    fi
    
    # 🎯 版本比较：如果内置版本更新，使用内置版本
    log_info "内置版本: $builtin_version"
    if version_compare "$builtin_version" "$current_version"; then
        log_info "🚀 发现内置版本($builtin_version)比持久化版本($current_version)更新，使用内置版本"
        
        # 使用内置版本
        if [ "$builtin_available" = "true" ] && /app/init-builtin.sh restore; then
            current_version="$builtin_version"
            echo "$builtin_version" > /dockpilot/data/current_version
            log_info "✅ 已更新到内置版本: $builtin_version"
            return 0  # 版本已更新，无需下载
        else
            log_warn "⚠️ 内置版本使用失败，继续使用当前版本"
        fi
    else
        log_info "当前版本已是最新或更新: $current_version"
    fi
    
    # 检查是否需要下载更新版本
    if [ ! -f "/app/app.jar" ] || [ "$current_version" != "$target_version" ]; then
        log_info "🔄 需要下载应用代码..."
        log_info "目标版本: $target_version"
        
        # 获取最新版本（如果target_version是latest）
        if [ "$target_version" = "latest" ]; then
            log_info "获取GitHub最新版本..."
            target_version=$(curl -s https://api.github.com/repos/kidoneself/DockPilot/releases/latest | jq -r '.tag_name' 2>/dev/null || echo "")
            
            if [ -z "$target_version" ] || [ "$target_version" = "null" ]; then
                log_warn "无法获取最新版本信息"
                
                # 🔥 如果有内置版本，优先使用内置版本
                if [ "$builtin_available" = "true" ]; then
                    log_info "🛡️ 使用内置版本作为fallback"
                    if /app/init-builtin.sh restore; then
                        log_info "✅ 已使用内置版本启动"
                        return 0
                    fi
                fi
                
                target_version="v1.0.0"
                log_warn "使用预设版本: $target_version"
            fi
            
            log_info "最新版本: $target_version"
        fi
        
        # 🔥 新增：如果目标版本就是内置版本，直接使用内置版本
        if [ "$target_version" = "builtin-version" ] && [ "$builtin_available" = "true" ]; then
            log_info "🎯 目标版本为内置版本，直接使用内置代码"
            if /app/init-builtin.sh restore; then
                log_info "✅ 内置版本已激活"
                return 0
            else
                log_error "❌ 内置版本激活失败"
                return 1
            fi
        fi
        
        # 调用下载脚本
        log_info "📡 开始下载应用代码..."
        if ! /app/download-app.sh "$target_version"; then
            log_error "❌ 应用代码下载失败"
            
            # 🔥 新增：下载失败时的fallback策略
            if [ -f "/app/app.jar" ]; then
                log_warn "📦 使用现有的应用代码继续启动"
                return 0
            elif [ "$builtin_available" = "true" ]; then
                log_info "🛡️ 下载失败，启用内置版本fallback"
                if /app/init-builtin.sh restore; then
                    log_info "✅ 内置版本fallback成功，继续启动"
                    return 0
                else
                    log_error "❌ 内置版本fallback也失败"
                    return 1
                fi
            else
                log_error "❌ 没有可用的应用代码，无法启动"
                return 1
            fi
        else
            # 更新版本记录
            echo "$target_version" > /dockpilot/data/current_version
            log_info "✅ 应用代码已更新到版本: $target_version"
            return 0
        fi
    else
        log_info "✅ 应用代码已是最新版本: $current_version"
        return 0
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
            if curl -s http://localhost:8080/update/version >/dev/null 2>&1; then
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
    local builtin_available="否"
    
    # 检查内置版本是否可用
    if [ "$BUILTIN_FALLBACK" = "true" ] && /app/init-builtin.sh check >/dev/null 2>&1; then
        builtin_available="是"
    fi
    
    log_info "=========================================="
    log_info "🎉 DockPilot 热更新版本启动完成！"
    log_info "=========================================="
    log_info "📋 服务信息:"
    log_info "  • 前端访问地址: http://localhost:8888"
    log_info "  • 当前版本: $current_version"
    log_info "  • 更新方式: 热更新 (容器内)"
    log_info "  • 内置版本fallback: $builtin_available"
    log_info "📊 进程信息:"
    log_info "  • Caddy PID: $CADDY_PID"
    log_info "  • Java PID: $JAVA_PID"
    log_info "📁 路径信息:"
    log_info "  • 前端目录: /usr/share/html"
    log_info "  • 后端jar: /app/app.jar"
    log_info "  • 数据目录: /dockpilot"
    if [ "$builtin_available" = "是" ]; then
        log_info "  • 内置版本: /usr/share/html-builtin, /app/builtin/"
    fi
    log_info "=========================================="
    log_info "💡 提示: 可通过右上角更新按钮检查和执行热更新"
    if [ "$current_version" = "builtin-version" ]; then
        log_info "🛡️ 当前使用内置版本，建议检查网络后尝试热更新"
    fi
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

# 检查重启信号文件并处理重启逻辑
check_restart_signal() {
    local restart_signal_file="/dockpilot/data/restart_signal"
    
    if [ -f "$restart_signal_file" ]; then
        log_info "🔄 检测到重启信号文件"
        
        # 读取重启信息
        local restart_info=$(cat "$restart_signal_file" 2>/dev/null || echo "{}")
        local new_version=$(echo "$restart_info" | jq -r '.newVersion // "unknown"' 2>/dev/null || echo "unknown")
        local reason=$(echo "$restart_info" | jq -r '.reason // "unknown"' 2>/dev/null || echo "unknown")
        local download_path=$(echo "$restart_info" | jq -r '.downloadPath // ""' 2>/dev/null || echo "")
        
        log_info "📋 重启信息:"
        log_info "  • 原因: $reason"
        log_info "  • 目标版本: $new_version"
        log_info "  • 下载路径: $download_path"
        
        # 删除重启信号文件
        rm -f "$restart_signal_file"
        log_info "✅ 重启信号文件已清理"
        
        if [ "$reason" = "update_restart" ] && [ -n "$download_path" ]; then
            log_info "🎯 检测到更新重启，使用预下载的文件"
            
            # 使用预下载的文件
            if use_downloaded_files "$download_path" "$new_version"; then
                log_info "✅ 使用下载文件 $new_version 启动成功"
                echo "$new_version" > /dockpilot/data/current_version
                return 0
            else
                log_warn "⚠️ 下载文件使用失败，回退到下载模式"
            fi
        fi
    else
        log_debug "未检测到重启信号文件，正常启动"
    fi
    
    # 正常下载流程（作为备选）
    log_info "执行正常下载流程..."
    return 1
}

# 使用下载的文件
use_downloaded_files() {
    local download_path="$1"
    local version="$2"
    
    log_info "📦 使用下载文件: $download_path"
    
    # 验证下载文件
    if [ ! -f "$download_path/frontend.tar.gz" ] || [ ! -f "$download_path/backend.jar" ]; then
        log_error "下载文件不完整"
        return 1
    fi
    
    # 备份当前文件
    local backup_dir="/tmp/backup-$(date +%s)"
    mkdir -p "$backup_dir"
    
    if [ -d "/usr/share/html" ] && [ "$(ls -A /usr/share/html 2>/dev/null)" ]; then
        cp -r /usr/share/html/* "$backup_dir/frontend/" 2>/dev/null || true
        log_info "前端已备份到: $backup_dir/frontend/"
    fi
    
    if [ -f "/app/app.jar" ]; then
        cp "/app/app.jar" "$backup_dir/backend.jar"
        log_info "后端已备份到: $backup_dir/backend.jar"
    fi
    
    # 部署前端
    log_info "🎨 部署前端文件..."
    rm -rf /usr/share/html/*
    if ! tar -xzf "$download_path/frontend.tar.gz" -C /usr/share/html/; then
        log_error "前端部署失败，开始回滚"
        if [ -d "$backup_dir/frontend" ]; then
            cp -r "$backup_dir/frontend"/* /usr/share/html/ 2>/dev/null || true
        fi
        return 1
    fi
    chmod -R 755 /usr/share/html/
    
    # 部署后端  
    log_info "⚙️ 部署后端文件..."
    if ! cp "$download_path/backend.jar" /app/app.jar; then
        log_error "后端部署失败，开始回滚"
        if [ -f "$backup_dir/backend.jar" ]; then
            cp "$backup_dir/backend.jar" /app/app.jar
        fi
        return 1
    fi
    chmod 644 /app/app.jar
    
    # 清理下载文件
    log_info "🧹 清理下载文件..."
    rm -rf "$download_path"
    
    # 🔥 新增：清理下载状态文件，重置前端状态
    log_info "🔄 清理下载状态文件，重置更新状态..."
    rm -f "/tmp/dockpilot-download-status.json"
    
    log_info "✅ 下载文件部署成功"
    return 0
}

# 主启动流程
main() {
    log_info "开始主启动流程..."
    
    # 🔥 首先检查重启信号文件
    if check_restart_signal; then
        log_info "✅ 使用下载文件启动成功，跳过下载流程"
    else
        log_info "📡 执行正常应用下载流程..."
        # 正常下载流程
        check_and_download_app
    fi
    
    # 启动服务循环
    while true; do
        # 1. 先启动Caddy（显示初始化页面）
        if [ -z "$CADDY_PID" ] || ! kill -0 "$CADDY_PID" 2>/dev/null; then
            start_caddy
        fi
        
        # 2. 启动Java应用
        log_info "☕ 启动Java后端服务..."
        if start_java; then
            log_info "✅ 应用启动完成"
        else
            log_error "❌ 应用启动失败，仅提供Web服务"
        fi
        
        # 3. 显示启动信息
        show_startup_info
        
        # 4. 监控Java进程和重启信号
        log_info "🔄 服务运行中，监控进程和重启信号..."
        
        while true; do
            # 检查Java进程是否还在运行
            if [ ! -z "$JAVA_PID" ] && ! kill -0 "$JAVA_PID" 2>/dev/null; then
                log_info "🔄 检测到Java进程退出，检查重启信号..."
                
                # 检查是否有重启信号
                if check_restart_signal; then
                    log_info "✅ 检测到重启信号，重新启动服务..."
                    break  # 跳出内层循环，重新启动
                else
                    log_warn "⚠️ Java进程意外退出，没有重启信号，重新启动..."
                    break  # 跳出内层循环，重新启动
                fi
            fi
            
            # 定期检查重启信号（即使Java进程正常运行）
            if [ -f "/dockpilot/data/restart_signal" ]; then
                log_info "🔄 检测到新的重启信号..."
                
                # 优雅停止Java进程
                if [ ! -z "$JAVA_PID" ] && kill -0 "$JAVA_PID" 2>/dev/null; then
                    log_info "🛑 优雅停止Java进程..."
                    kill -TERM "$JAVA_PID" 2>/dev/null || true
                    
                    # 等待Java进程退出
                    local wait_count=0
                    while [ $wait_count -lt 30 ] && kill -0 "$JAVA_PID" 2>/dev/null; do
                        sleep 1
                        wait_count=$((wait_count + 1))
                    done
                    
                    # 如果还没退出，强制杀死
                    if kill -0 "$JAVA_PID" 2>/dev/null; then
                        log_warn "⚠️ 强制停止Java进程..."
                        kill -KILL "$JAVA_PID" 2>/dev/null || true
                    fi
                fi
                
                # 处理重启信号
                if check_restart_signal; then
                    log_info "✅ 重启信号处理完成，重新启动服务..."
                    break  # 跳出内层循环，重新启动
                fi
            fi
            
            # 每5秒检查一次
            sleep 5
        done
        
        # 清理PID变量，准备重新启动
        JAVA_PID=""
        
        log_info "🔄 准备重新启动服务..."
        sleep 2
    done
}

# 执行主流程
main "$@" 