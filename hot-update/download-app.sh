#!/bin/bash

# DockPilot 应用代码下载脚本
# 从GitHub Releases下载前后端代码

set -e

VERSION="$1"
DOWNLOAD_BASE_URL="${DOWNLOAD_URL_BASE:-https://github.com/kidoneself/DockPilot/releases/download}"
TEMP_DIR="/tmp/download-$$"
BACKUP_DIR="/tmp/backup-$$"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[DOWNLOAD]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[DOWNLOAD]${NC} $1"
}

log_error() {
    echo -e "${RED}[DOWNLOAD]${NC} $1"
}

# 清理函数
cleanup() {
    log_info "清理临时文件..."
    rm -rf "$TEMP_DIR" "$BACKUP_DIR" 2>/dev/null || true
}

# 注册清理函数
trap cleanup EXIT

# 验证jar文件的函数
validate_jar_file() {
    local jar_file="$1"
    
    # 检查1：文件是否存在且非空
    if [ ! -s "$jar_file" ]; then
        return 1
    fi
    
    # 检查2：使用file命令检查（如果可用）
    if command -v file >/dev/null 2>&1; then
        if file "$jar_file" | grep -q -E "(Java archive|JAR|Zip archive)"; then
            log_info "jar文件格式验证通过（file命令）"
            return 0
        fi
    fi
    
    # 检查3：检查文件头（jar文件是zip格式，以PK开头）
    if [ "$(hexdump -C "$jar_file" | head -1 | cut -d'|' -f1 | grep -c '50 4b')" -gt 0 ]; then
        log_info "jar文件格式验证通过（文件头检查）"
        return 0
    fi
    
    # 检查4：尝试使用jar命令验证（如果可用）
    if command -v jar >/dev/null 2>&1; then
        if jar tf "$jar_file" >/dev/null 2>&1; then
            log_info "jar文件格式验证通过（jar命令）"
            return 0
        fi
    fi
    
    # 检查5：使用unzip检查（jar就是zip文件）
    if command -v unzip >/dev/null 2>&1; then
        if unzip -t "$jar_file" >/dev/null 2>&1; then
            log_info "jar文件格式验证通过（unzip检查）"
            return 0
        fi
    fi
    
    log_warn "jar文件验证失败，所有检查方法都不通过"
    return 1
}

# 检查参数
if [ -z "$VERSION" ]; then
    log_error "使用方法: $0 <version>"
    log_error "示例: $0 v1.0.0"
    exit 1
fi

log_info "开始下载DockPilot应用代码，版本: $VERSION"

# 创建临时目录
mkdir -p "$TEMP_DIR" "$BACKUP_DIR"

# 下载前端代码
download_frontend() {
    local frontend_url="$DOWNLOAD_BASE_URL/$VERSION/frontend.tar.gz"
    local frontend_file="$TEMP_DIR/frontend.tar.gz"
    
    log_info "📦 下载前端代码包..."
    log_info "URL: $frontend_url"
    
    if wget --timeout=60 --tries=3 -q "$frontend_url" -O "$frontend_file"; then
        log_info "✅ 前端代码包下载成功"
        
        # 验证文件
        if [ ! -s "$frontend_file" ]; then
            log_error "前端代码包文件为空"
            return 1
        fi
        
        # 测试解压
        if ! tar -tzf "$frontend_file" >/dev/null 2>&1; then
            log_error "前端代码包格式无效"
            return 1
        fi
        
        return 0
    else
        log_error "❌ 前端代码包下载失败"
        return 1
    fi
}

# 下载后端代码
download_backend() {
    local backend_url="$DOWNLOAD_BASE_URL/$VERSION/backend.jar"
    local backend_file="$TEMP_DIR/backend.jar"
    
    log_info "📦 下载后端代码包..."
    log_info "URL: $backend_url"
    
    if wget --timeout=120 --tries=3 -q "$backend_url" -O "$backend_file"; then
        log_info "✅ 后端代码包下载成功"
        
        # 验证文件
        if [ ! -s "$backend_file" ]; then
            log_error "后端代码包文件为空"
            return 1
        fi
        
        # 验证jar文件（多重检查）
        if ! validate_jar_file "$backend_file"; then
            log_error "后端代码包不是有效的jar文件"
            return 1
        fi
        
        return 0
    else
        log_error "❌ 后端代码包下载失败"
        return 1
    fi
}

# 备份当前代码
backup_current() {
    log_info "🔒 备份当前应用代码..."
    
    # 备份前端
    if [ -d "/usr/share/html" ] && [ "$(ls -A /usr/share/html 2>/dev/null)" ]; then
        mkdir -p "$BACKUP_DIR/frontend"
        cp -r /usr/share/html/* "$BACKUP_DIR/frontend/" 2>/dev/null || true
        log_info "前端代码已备份"
    fi
    
    # 备份后端
    if [ -f "/app/app.jar" ]; then
        cp "/app/app.jar" "$BACKUP_DIR/backend.jar"
        log_info "后端代码已备份"
    fi
    
    log_info "✅ 当前代码备份完成"
}

# 部署前端代码
deploy_frontend() {
    local frontend_file="$TEMP_DIR/frontend.tar.gz"
    
    log_info "🎨 部署前端代码..."
    
    # 清空现有前端目录
    rm -rf /usr/share/html/*
    
    # 解压新前端代码
    if tar -xzf "$frontend_file" -C /usr/share/html/; then
        # 设置权限
        chmod -R 755 /usr/share/html/
        log_info "✅ 前端代码部署成功"
        return 0
    else
        log_error "❌ 前端代码部署失败"
        return 1
    fi
}

# 部署后端代码
deploy_backend() {
    local backend_file="$TEMP_DIR/backend.jar"
    
    log_info "⚙️ 部署后端代码..."
    
    # 复制新的jar文件
    if cp "$backend_file" "/app/app.jar"; then
        chmod 644 "/app/app.jar"
        log_info "✅ 后端代码部署成功"
        return 0
    else
        log_error "❌ 后端代码部署失败"
        return 1
    fi
}

# 回滚代码
rollback() {
    log_warn "🔄 开始回滚到之前版本..."
    
    # 回滚前端
    if [ -d "$BACKUP_DIR/frontend" ]; then
        rm -rf /usr/share/html/*
        cp -r "$BACKUP_DIR/frontend"/* /usr/share/html/ 2>/dev/null || true
        log_info "前端代码已回滚"
    fi
    
    # 回滚后端
    if [ -f "$BACKUP_DIR/backend.jar" ]; then
        cp "$BACKUP_DIR/backend.jar" "/app/app.jar"
        log_info "后端代码已回滚"
    fi
    
    log_warn "代码已回滚到之前版本"
}

# 验证部署
verify_deployment() {
    log_info "🔍 验证部署结果..."
    
    # 检查前端文件
    if [ ! -f "/usr/share/html/index.html" ]; then
        log_error "前端index.html文件不存在"
        return 1
    fi
    
    # 检查后端jar
    if [ ! -f "/app/app.jar" ]; then
        log_error "后端jar文件不存在"
        return 1
    fi
    
    # 检查jar文件是否可执行
    if ! java -jar /app/app.jar --help >/dev/null 2>&1; then
        log_warn "后端jar文件可能存在问题（无法执行help命令）"
    fi
    
    log_info "✅ 部署验证通过"
    return 0
}

# 主函数
main() {
    log_info "=========================================="
    log_info "DockPilot 应用代码下载器"
    log_info "版本: $VERSION"
    log_info "=========================================="
    
    # 1. 下载代码包
    if ! download_frontend || ! download_backend; then
        log_error "代码包下载失败"
        exit 1
    fi
    
    # 2. 备份当前代码
    backup_current
    
    # 3. 部署新代码
    if ! deploy_frontend || ! deploy_backend; then
        log_error "代码部署失败，开始回滚..."
        rollback
        exit 1
    fi
    
    # 4. 验证部署
    if ! verify_deployment; then
        log_error "部署验证失败，开始回滚..."
        rollback
        exit 1
    fi
    
    log_info "=========================================="
    log_info "🎉 应用代码下载和部署完成！"
    log_info "版本: $VERSION"
    log_info "前端路径: /usr/share/html"
    log_info "后端路径: /app/app.jar"
    log_info "=========================================="
    
    return 0
}

# 执行主函数
main 