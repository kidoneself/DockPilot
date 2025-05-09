#!/bin/bash

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

print_message() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否为root用户
check_root() {
    if [ "$EUID" -ne 0 ]; then
        print_error "请使用root权限运行此脚本"
        exit 1
    fi
}

# 安装基础工具
install_basic_tools() {
    print_message "安装基础工具..."
    apt-get update
    apt-get install -y curl wget git
}

# 安装Java
install_java() {
    print_message "安装Java..."
    apt-get install -y openjdk-11-jdk
}

# 安装Maven
install_maven() {
    print_message "安装Maven..."
    apt-get install -y maven
}

# 安装Node.js和npm
install_nodejs() {
    print_message "安装Node.js和npm..."
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
    apt-get install -y nodejs
}

# 安装Docker
install_docker() {
    print_message "安装Docker..."
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
}

# 检查是否安装了必要的工具
check_requirements() {
    if ! command -v git &> /dev/null; then
        print_error "git 未安装"
        install_basic_tools
    fi
    if ! command -v docker &> /dev/null; then
        print_error "docker 未安装"
        install_docker
    fi
    if ! command -v java &> /dev/null; then
        print_error "java 未安装"
        install_java
    fi
    if ! command -v mvn &> /dev/null; then
        print_error "maven 未安装"
        install_maven
    fi
    if ! command -v node &> /dev/null; then
        print_error "node 未安装"
        install_nodejs
    fi
    if ! command -v npm &> /dev/null; then
        print_error "npm 未安装"
        install_nodejs
    fi
}

# 克隆或更新代码
setup_code() {
    print_message "设置代码..."
    if [ ! -d "DockPilot" ]; then
        print_message "克隆代码仓库..."
        git clone https://github.com/kidoneself/DockPilot.git
        if [ $? -ne 0 ]; then
            print_error "克隆代码失败"
            exit 1
        fi
        cd DockPilot
    else
        print_message "更新代码仓库..."
        cd DockPilot
        git fetch origin
        if [ $? -ne 0 ]; then
            print_error "获取远程代码失败"
            exit 1
        fi
    fi

    # 切换到指定分支
    BRANCH_NAME="feature/websocket"
    print_message "切换到分支: $BRANCH_NAME"
    git checkout $BRANCH_NAME
    if [ $? -ne 0 ]; then
        print_error "切换分支失败"
        exit 1
    fi

    # 拉取最新代码
    git pull origin $BRANCH_NAME
    if [ $? -ne 0 ]; then
        print_error "更新代码失败"
        exit 1
    fi
}

# 构建前端
build_frontend() {
    print_message "构建前端..."
    cd docker-manager-front
    npm install
    npm run build
    if [ $? -ne 0 ]; then
        print_error "前端构建失败"
        exit 1
    fi
    cd ..
}

# 构建后端
build_backend() {
    print_message "构建后端..."
    cd docker-manager-back
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        print_error "后端构建失败"
        exit 1
    fi
    cd ..
}

# 复制构建文件到build目录
copy_build_files() {
    print_message "复制构建文件到build目录..."
    rm -rf build/dist
    rm -rf build/*.jar

    # 复制前端构建文件
    cp -r docker-manager-front/dist build/

    # 复制后端jar文件
    cp docker-manager-back/target/*.jar build/
}

# 构建Docker镜像
build_docker_image() {
    print_message "构建Docker镜像..."
    cd build
    ./build-and-run.sh
    cd ..
}

# 推送到DockerHub
push_to_dockerhub() {
    print_message "推送到DockerHub..."
    # DockerHub信息
    DOCKERHUB_USERNAME="kidself"  # 请替换为你的DockerHub用户名
    DOCKERHUB_IMAGE="dockpilot"

    # 获取最新镜像ID
    get_latest_image_id

    # 给镜像打标签
    print_message "给镜像打标签..."
    docker tag ${IMAGE_ID} ${DOCKERHUB_USERNAME}/${DOCKERHUB_IMAGE}:latest

    # 推送镜像
    print_message "推送镜像..."
    docker push ${DOCKERHUB_USERNAME}/${DOCKERHUB_IMAGE}:latest

    print_message "DockerHub镜像推送完成！"
    print_message "镜像地址: ${DOCKERHUB_USERNAME}/${DOCKERHUB_IMAGE}:latest"
}

# 获取最新构建的镜像ID
get_latest_image_id() {
    print_message "获取最新构建的镜像ID..."
    # 获取最近构建的dockpilot镜像ID
    IMAGE_ID=$(docker images dockpilot --format "{{.ID}}" | head -n 1)
    if [ -z "$IMAGE_ID" ]; then
        print_error "未找到dockpilot镜像"
        exit 1
    fi
    print_message "找到镜像ID: $IMAGE_ID"
}

# 推送到腾讯云容器镜像服务
push_to_tencent() {
    print_message "推送到腾讯云容器镜像服务..."
    # 腾讯云容器镜像服务信息
    TENCENT_REGISTRY="ccr.ccs.tencentyun.com"
    NAMESPACE="naspt/dockpilot"
    
    # 获取最新镜像ID
    get_latest_image_id
    
    # 给镜像打标签
    print_message "给镜像打标签..."
    docker tag ${IMAGE_ID} ${TENCENT_REGISTRY}/${NAMESPACE}:latest
    
    # 推送镜像
    print_message "推送镜像..."
    docker push ${TENCENT_REGISTRY}/${NAMESPACE}:latest
    
    print_message "镜像推送完成！"
    print_message "镜像地址: ${TENCENT_REGISTRY}/${NAMESPACE}:latest"
}

# 主函数
main() {
    check_root
    check_requirements
    setup_code
    build_frontend
    build_backend
    copy_build_files
    build_docker_image
    push_to_tencent
    push_to_dockerhub
    
    print_message "部署完成！"
}

# 执行主函数
main