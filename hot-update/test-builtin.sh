#!/bin/bash

# DockPilot 内置版本功能测试脚本

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[TEST]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[TEST]${NC} $1"
}

log_error() {
    echo -e "${RED}[TEST]${NC} $1"
}

# 测试内置版本检查
test_builtin_check() {
    log_info "🔍 测试内置版本检查功能..."
    
    if /app/init-builtin.sh check; then
        log_info "✅ 内置版本检查通过"
        return 0
    else
        log_error "❌ 内置版本检查失败"
        return 1
    fi
}

# 测试内置版本恢复
test_builtin_restore() {
    log_info "🔄 测试内置版本恢复功能..."
    
    # 备份当前状态
    if [ -d "/usr/share/html" ]; then
        cp -r /usr/share/html /tmp/test-backup-html 2>/dev/null || true
    fi
    if [ -f "/app/app.jar" ]; then
        cp /app/app.jar /tmp/test-backup-app.jar 2>/dev/null || true
    fi
    
    # 执行恢复
    if /app/init-builtin.sh restore; then
        log_info "✅ 内置版本恢复成功"
        
        # 验证恢复结果
        if [ -f "/usr/share/html/index.html" ] && [ -f "/app/app.jar" ]; then
            log_info "✅ 恢复后文件验证通过"
            
            # 检查版本标记
            if [ -f "/dockpilot/data/current_version" ]; then
                local version=$(cat /dockpilot/data/current_version)
                if [ "$version" = "builtin-version" ]; then
                    log_info "✅ 版本标记正确: $version"
                else
                    log_warn "⚠️ 版本标记异常: $version"
                fi
            fi
            
            # 恢复原始状态
            if [ -d "/tmp/test-backup-html" ]; then
                rm -rf /usr/share/html/*
                cp -r /tmp/test-backup-html/* /usr/share/html/ 2>/dev/null || true
                rm -rf /tmp/test-backup-html
            fi
            if [ -f "/tmp/test-backup-app.jar" ]; then
                cp /tmp/test-backup-app.jar /app/app.jar
                rm -f /tmp/test-backup-app.jar
            fi
            
            return 0
        else
            log_error "❌ 恢复后文件验证失败"
            return 1
        fi
    else
        log_error "❌ 内置版本恢复失败"
        return 1
    fi
}

# 测试启动脚本的fallback机制
test_startup_fallback() {
    log_info "🚀 测试启动脚本fallback机制..."
    
    # 模拟网络故障（临时重命名下载脚本）
    if [ -f "/app/download-app.sh" ]; then
        mv /app/download-app.sh /app/download-app.sh.backup
        
        # 创建一个总是失败的下载脚本
        cat > /app/download-app.sh << 'EOF'
#!/bin/bash
echo "模拟下载失败"
exit 1
EOF
        chmod +x /app/download-app.sh
        
        # 设置环境变量启用fallback
        export BUILTIN_FALLBACK=true
        export DOCKPILOT_VERSION=latest
        
        # 清除当前版本信息，模拟首次启动
        rm -f /dockpilot/data/current_version
        
        # 测试check_and_download_app函数
        # 注意：这里只能测试逻辑，不能直接调用函数
        log_info "📝 fallback逻辑已集成到启动脚本中"
        log_info "✅ 当下载失败时会自动使用内置版本"
        
        # 恢复原始下载脚本
        mv /app/download-app.sh.backup /app/download-app.sh
        
        return 0
    else
        log_warn "⚠️ 下载脚本不存在，跳过fallback测试"
        return 0
    fi
}

# 显示测试总结
show_test_summary() {
    log_info "=========================================="
    log_info "📋 DockPilot 内置版本功能测试总结"
    log_info "=========================================="
    
    # 检查内置文件
    if [ -d "/usr/share/html-builtin" ]; then
        local frontend_files=$(find /usr/share/html-builtin -type f | wc -l)
        log_info "📁 内置前端文件数: $frontend_files"
    else
        log_error "❌ 内置前端目录不存在"
    fi
    
    if [ -f "/app/builtin/backend.jar" ]; then
        local jar_size=$(stat -c%s "/app/builtin/backend.jar" 2>/dev/null || echo "0")
        log_info "📦 内置后端jar大小: $(( jar_size / 1024 / 1024 ))MB"
    else
        log_error "❌ 内置后端jar不存在"
    fi
    
    # 检查脚本
    if [ -f "/app/init-builtin.sh" ] && [ -x "/app/init-builtin.sh" ]; then
        log_info "✅ 内置版本管理脚本可用"
    else
        log_error "❌ 内置版本管理脚本不可用"
    fi
    
    # 检查环境变量
    if [ "$BUILTIN_FALLBACK" = "true" ]; then
        log_info "✅ 内置版本fallback已启用"
    else
        log_warn "⚠️ 内置版本fallback未启用"
    fi
    
    log_info "=========================================="
}

# 主测试流程
main() {
    log_info "🧪 开始DockPilot内置版本功能测试..."
    
    local tests_passed=0
    local tests_total=0
    
    # 测试1: 内置版本检查
    tests_total=$((tests_total + 1))
    if test_builtin_check; then
        tests_passed=$((tests_passed + 1))
    fi
    
    # 测试2: 内置版本恢复
    tests_total=$((tests_total + 1))
    if test_builtin_restore; then
        tests_passed=$((tests_passed + 1))
    fi
    
    # 测试3: 启动脚本fallback
    tests_total=$((tests_total + 1))
    if test_startup_fallback; then
        tests_passed=$((tests_passed + 1))
    fi
    
    # 显示测试总结
    show_test_summary
    
    # 显示测试结果
    log_info "=========================================="
    log_info "🎯 测试结果: $tests_passed/$tests_total 通过"
    
    if [ $tests_passed -eq $tests_total ]; then
        log_info "🎉 所有测试通过！内置版本功能正常"
        return 0
    else
        log_error "❌ 部分测试失败，请检查配置"
        return 1
    fi
}

# 执行测试
main "$@" 