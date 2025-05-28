#!/bin/bash

# DockPilot 内置版本初始化脚本
# 当热更新失败时，恢复到镜像内置的代码版本

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[BUILTIN]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[BUILTIN]${NC} $1"
}

log_error() {
    echo -e "${RED}[BUILTIN]${NC} $1"
}

# 恢复内置版本
restore_builtin() {
    log_info "🔄 开始恢复到内置版本..."
    
    # 检查内置文件是否存在
    if [ ! -d "/usr/share/html-builtin" ] || [ ! -f "/app/builtin/backend.jar" ]; then
        log_error "❌ 内置版本文件不存在，无法恢复"
        return 1
    fi
    
    # 备份当前状态（如果存在）
    if [ -d "/usr/share/html" ] && [ "$(ls -A /usr/share/html 2>/dev/null)" ]; then
        log_info "📦 备份当前前端文件..."
        mkdir -p /tmp/backup-frontend
        cp -r /usr/share/html/* /tmp/backup-frontend/ 2>/dev/null || true
    fi
    
    if [ -f "/app/app.jar" ]; then
        log_info "📦 备份当前后端文件..."
        cp /app/app.jar /tmp/backup-backend.jar 2>/dev/null || true
    fi
    
    # 恢复前端
    log_info "🎨 恢复内置前端版本..."
    rm -rf /usr/share/html/*
    cp -r /usr/share/html-builtin/* /usr/share/html/
    chmod -R 755 /usr/share/html/
    
    # 恢复后端
    log_info "⚙️ 恢复内置后端版本..."
    cp /app/builtin/backend.jar /app/app.jar
    chmod 644 /app/app.jar
    
    # 更新版本标记
    echo "builtin-version" > /dockpilot/data/current_version
    
    log_info "✅ 内置版本恢复完成"
    return 0
}

# 检查内置版本完整性
check_builtin() {
    log_info "🔍 检查内置版本完整性..."
    
    local errors=0
    
    # 检查前端内置版本
    if [ ! -d "/usr/share/html-builtin" ]; then
        log_error "❌ 内置前端目录不存在"
        errors=$((errors + 1))
    elif [ ! -f "/usr/share/html-builtin/index.html" ]; then
        log_error "❌ 内置前端index.html不存在"
        errors=$((errors + 1))
    else
        local frontend_files=$(find /usr/share/html-builtin -type f | wc -l)
        log_info "📁 内置前端文件数: $frontend_files"
    fi
    
    # 检查后端内置版本
    if [ ! -f "/app/builtin/backend.jar" ]; then
        log_error "❌ 内置后端jar不存在"
        errors=$((errors + 1))
    else
        local jar_size=$(stat -c%s "/app/builtin/backend.jar" 2>/dev/null || echo "0")
        if [ "$jar_size" -lt 1048576 ]; then
            log_error "❌ 内置后端jar文件太小 (${jar_size} bytes)"
            errors=$((errors + 1))
        else
            log_info "📦 内置后端jar大小: $(( jar_size / 1024 / 1024 ))MB"
        fi
    fi
    
    if [ $errors -eq 0 ]; then
        log_info "✅ 内置版本完整性检查通过"
        return 0
    else
        log_error "❌ 内置版本完整性检查失败 ($errors 个错误)"
        return 1
    fi
}

# 获取内置版本信息
get_builtin_info() {
    log_info "📋 内置版本信息:"
    
    if [ -d "/usr/share/html-builtin" ]; then
        local frontend_files=$(find /usr/share/html-builtin -type f | wc -l)
        log_info "  • 前端文件数: $frontend_files"
    fi
    
    if [ -f "/app/builtin/backend.jar" ]; then
        local jar_size=$(stat -c%s "/app/builtin/backend.jar" 2>/dev/null || echo "0")
        log_info "  • 后端jar大小: $(( jar_size / 1024 / 1024 ))MB"
    fi
    
    log_info "  • 版本标识: builtin-version"
    log_info "  • 构建来源: Docker镜像内置"
}

# 主函数
main() {
    local action="${1:-restore}"
    
    case "$action" in
        "restore")
            log_info "=========================================="
            log_info "🔄 DockPilot 内置版本恢复"
            log_info "=========================================="
            
            if check_builtin && restore_builtin; then
                get_builtin_info
                log_info "🎉 内置版本恢复成功！"
            else
                log_error "❌ 内置版本恢复失败"
                exit 1
            fi
            ;;
            
        "check")
            log_info "=========================================="
            log_info "🔍 DockPilot 内置版本检查"
            log_info "=========================================="
            
            if check_builtin; then
                get_builtin_info
            else
                exit 1
            fi
            ;;
            
        "info")
            get_builtin_info
            ;;
            
        *)
            log_error "❌ 未知操作: $action"
            log_info "使用方法: $0 [restore|check|info]"
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@" 