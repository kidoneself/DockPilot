#!/bin/bash

#================================================================
# Buildx构建器清理脚本
# 
# 功能：清理所有自定义buildx构建器并重新初始化
# 用途：解决buildx构建器问题
#================================================================

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m'

print_message() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_message "=========================================="
print_message "Buildx构建器清理脚本"
print_message "=========================================="

# 显示当前构建器列表
print_message "当前buildx构建器列表："
docker buildx ls

# 删除mybuilder构建器
print_message "删除mybuilder构建器..."
if docker buildx inspect mybuilder >/dev/null 2>&1; then
    docker buildx rm mybuilder
    print_message "✅ mybuilder构建器已删除"
else
    print_warning "mybuilder构建器不存在"
fi

# 删除所有自定义构建器（除了default）
print_message "清理所有自定义构建器..."
BUILDERS=$(docker buildx ls | grep -v "^NAME" | grep -v "^default" | awk '{print $1}')
for builder in $BUILDERS; do
    if [ ! -z "$builder" ]; then
        print_message "删除构建器: $builder"
        docker buildx rm $builder 2>/dev/null || true
    fi
done

# 设置使用默认构建器
print_message "切换到默认构建器..."
docker buildx use default

# 重新创建mybuilder构建器
print_message "重新创建mybuilder构建器..."
if docker buildx create --name mybuilder --driver docker-container --bootstrap --use; then
    print_message "✅ mybuilder构建器创建成功"
else
    print_error "❌ 构建器创建失败"
    print_message "回退到默认构建器..."
    docker buildx use default
fi

# 显示最终状态
print_message "清理完成！当前构建器列表："
docker buildx ls

print_message "==========================================" 