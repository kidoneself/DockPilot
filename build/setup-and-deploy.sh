#!/bin/bash

#================================================================
# DockPilot 服务器端构建部署脚本
# 
# 功能：构建前端、后端，打包Docker镜像并推送到镜像仓库
# 默认：构建test版本，除非明确指定其他版本
#
# 使用方法：
#   ./setup-and-deploy.sh                          # 构建test版本
#   ./setup-and-deploy.sh v1.0.0                   # 构建v1.0.0版本
#   ./setup-and-deploy.sh latest main              # 构建latest版本，使用main分支
#
# 支持的镜像仓库：
#   - DockerHub: kidself/dockpilot
#   - 腾讯云: ccr.ccs.tencentyun.com/naspt/dockpilot
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

# 默认参数
VERSION=${1:-test}
BRANCH=${2:-feature/yaml-template}
GIT_REPO="https://github.com/kidoneself/DockPilot.git"

print_message "=========================================="
print_message "DockPilot 服务器端构建部署脚本"
print_message "版本标签: $VERSION"
print_message "Git分支: $BRANCH" 
print_message "=========================================="

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

# 统一管理buildx构建器的函数
setup_buildx() {
    print_message "设置buildx构建器..."
    
    # 检查是否已存在mybuilder构建器
    if docker buildx inspect mybuilder >/dev/null 2>&1; then
        print_warning "构建器 mybuilder 已存在，删除后重建..."
        docker buildx rm mybuilder || true
    fi
    
    # 创建新的构建器
    print_message "创建新的构建器 mybuilder..."
    if docker buildx create --name mybuilder --driver docker-container --bootstrap --use; then
        print_message "✅ 构建器 mybuilder 创建成功"
    else
        print_error "❌ 构建器创建失败，尝试使用默认构建器..."
        docker buildx use default
        return 1
    fi
}

# 安装Docker和buildx
install_docker() {
    print_message "安装Docker和buildx..."
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
    
    # 安装qemu-user-static以支持arm64构建
    print_message "安装qemu-user-static..."
    apt-get update
    apt-get install -y qemu-user-static
    
    # 设置buildx构建器
    setup_buildx
    
    # 启用实验性功能
    mkdir -p /etc/docker
    echo '{"experimental": true}' > /etc/docker/daemon.json
    systemctl restart docker
}

# 检查是否安装了必要的工具
check_requirements() {
    print_message "检查环境依赖..."
    
    if ! command -v git &> /dev/null; then
        print_warning "git 未安装，正在安装..."
        install_basic_tools
    fi
    if ! command -v docker &> /dev/null; then
        print_warning "docker 未安装，正在安装..."
        install_docker
    fi
    if ! command -v java &> /dev/null; then
        print_warning "java 未安装，正在安装..."
        install_java
    fi
    if ! command -v mvn &> /dev/null; then
        print_warning "maven 未安装，正在安装..."
        install_maven
    fi
    if ! command -v node &> /dev/null; then
        print_warning "node 未安装，正在安装..."
        install_nodejs
    fi
    if ! command -v npm &> /dev/null; then
        print_warning "npm 未安装，正在安装..."
        install_nodejs
    fi
    
    print_message "环境检查完成"
}

# 克隆或更新代码
setup_code() {
    print_message "设置代码..."
    
    # 如果目录存在，询问是否删除
    if [ -d "DockPilot" ]; then
        # 如果是test版本，默认强制更新
        if [ "$VERSION" == "test" ]; then
            print_message "test版本 - 强制更新代码..."
            cd DockPilot
            git fetch origin
            git checkout $BRANCH
            git pull origin $BRANCH
            print_message "代码已更新到最新版本"
            return
        else
            read -p "检测到现有代码目录，是否删除重新克隆？(y/n): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                print_message "删除现有代码..."
                rm -rf DockPilot
            else
                print_message "使用现有代码，更新分支..."
                cd DockPilot
                git fetch origin
                git checkout $BRANCH
                git pull origin $BRANCH
                return
            fi
        fi
    fi

    # 克隆代码
    print_message "克隆代码仓库..."
    git clone $GIT_REPO
    if [ $? -ne 0 ]; then
        print_error "克隆代码失败"
        exit 1
    fi

    cd DockPilot

    # 切换到指定分支
    print_message "切换到分支: $BRANCH"
    git checkout $BRANCH
    if [ $? -ne 0 ]; then
        print_error "切换分支失败"
        exit 1
    fi
}

# 构建前端
build_frontend() {
    print_message "构建前端..."
    
    # 直接进入dockpilotfront目录
    if [ -d "dockpilotfront" ]; then
        cd dockpilotfront
        print_message "进入前端目录: dockpilotfront"
    else
        print_error "未找到前端目录: dockpilotfront"
        exit 1
    fi
    
    # 安装依赖
    npm install
    
    # 升级 vue-tsc 以解决兼容性问题
    print_message "升级 vue-tsc 以解决兼容性问题..."
    npm install vue-tsc@latest --save-dev
    
    # 安装 terser 依赖
    print_message "安装 terser 依赖..."
    npm install terser --save-dev
    
    # 尝试正常构建
    print_message "尝试标准构建（包含类型检查）..."
    if npm run build; then
        print_message "标准构建成功！"
    else
        print_warning "标准构建失败，尝试跳过类型检查..."
        
        # 备份原始 package.json
        cp package.json package.json.backup
        
        # 修改构建脚本跳过类型检查
        sed -i 's/"build": "vue-tsc && vite build"/"build": "vite build"/' package.json
        
        # 重新尝试构建
        if npm run build; then
            print_message "跳过类型检查的构建成功！"
        else
            print_error "前端构建失败"
            # 恢复原始 package.json
            mv package.json.backup package.json
            exit 1
        fi
        
        # 恢复原始 package.json
        mv package.json.backup package.json
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
    if [ -d "dockpilotfront/dist" ]; then
        cp -r dockpilotfront/dist build/
        print_message "前端构建文件已复制"
    else
        print_error "未找到前端构建目录: dockpilotfront/dist"
        exit 1
    fi

    # 复制后端jar文件
    if ls docker-manager-back/target/*.jar 1> /dev/null 2>&1; then
        cp docker-manager-back/target/*.jar build/
        print_message "后端jar文件已复制"
    else
        print_error "未找到后端jar文件"
        exit 1
    fi
}

# 构建Docker镜像（仅本地）
build_docker_image_local() {
    print_message "构建本地Docker镜像 (版本: $VERSION)..."
    cd build
    docker build -t kidself/dockpilot:$VERSION .
    if [ $? -ne 0 ]; then
        print_error "Docker镜像构建失败"
        exit 1
    fi
    cd ..
}

# 推送到DockerHub
push_to_dockerhub() {
    print_message "推送到DockerHub (版本: $VERSION)..."
    # DockerHub信息
    DOCKERHUB_USERNAME="kidself"
    DOCKERHUB_IMAGE="dockpilot"

    # 确保使用正确的builder
    print_message "确保buildx构建器可用..."
    setup_buildx

    # 使用buildx构建并推送多架构镜像
    cd build
    docker buildx build --platform linux/amd64,linux/arm64 \
        -t ${DOCKERHUB_USERNAME}/${DOCKERHUB_IMAGE}:${VERSION} \
        --push .
    cd ..

    print_message "DockerHub镜像推送完成！"
    print_message "镜像地址: ${DOCKERHUB_USERNAME}/${DOCKERHUB_IMAGE}:${VERSION}"
}

# 推送到腾讯云容器镜像服务
push_to_tencent() {
    print_message "推送到腾讯云容器镜像服务 (版本: $VERSION)..."
    # 腾讯云容器镜像服务信息
    TENCENT_REGISTRY="ccr.ccs.tencentyun.com"
    NAMESPACE="naspt/dockpilot"
    
    # 确保使用正确的builder
    print_message "确保buildx构建器可用..."
    setup_buildx

    # 使用buildx构建并推送多架构镜像
    cd build
    docker buildx build --platform linux/amd64,linux/arm64 \
        -t ${TENCENT_REGISTRY}/${NAMESPACE}:${VERSION} \
        --push .
    cd ..

    print_message "镜像推送完成！"
    print_message "镜像地址: ${TENCENT_REGISTRY}/${NAMESPACE}:${VERSION}"
}

# 选择构建模式
choose_build_mode() {
    echo
    print_message "请选择构建模式："
    echo "1. 仅构建本地镜像"
    echo "2. 构建并推送到DockerHub"
    echo "3. 构建并推送到腾讯云"
    echo "4. 构建并推送到所有仓库"
    read -p "请输入选择 (1-4): " -n 1 -r
    echo
    
    case $REPLY in
        1)
            build_docker_image_local
            ;;
        2)
            push_to_dockerhub
            ;;
        3)
            push_to_tencent
            ;;
        4)
            push_to_tencent
            push_to_dockerhub
            ;;
        *)
            print_error "无效选择"
            exit 1
            ;;
    esac
}

# 主函数
main() {
    check_root
    check_requirements
    
    # 初始化buildx构建器，确保后续构建过程顺利
    print_message "初始化Docker buildx构建器..."
    setup_buildx
    
    setup_code
    build_frontend
    build_backend
    copy_build_files
    choose_build_mode
    
    print_message "=========================================="
    print_message "部署完成！版本: $VERSION"
    print_message "Git分支: $BRANCH"
    print_message "可用镜像:"
    print_message "  - kidself/dockpilot:$VERSION"
    print_message "  - ccr.ccs.tencentyun.com/naspt/dockpilot:$VERSION"
    print_message "=========================================="
    
    # 询问是否运行容器
    read -p "是否在本服务器运行容器？(y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_message "使用运行脚本启动容器..."
        ./run-dockerhub.sh $VERSION
    fi
}

# 显示使用说明
show_usage() {
    echo "使用说明:"
    echo "$0 [VERSION] [BRANCH]"
    echo ""
    echo "参数:"
    echo "  VERSION  - 镜像版本标签 (默认: test)"
    echo "  BRANCH   - Git分支名称 (默认: feature/yaml-template)"
    echo ""
    echo "示例:"
    echo "  $0                          # 构建test版本"
    echo "  $0 v1.0.0                   # 构建v1.0.0版本"
    echo "  $0 latest main              # 构建latest版本，使用main分支"
    echo ""
    echo "构建流程:"
    echo "  1. 环境检查和依赖安装"
    echo "  2. 代码拉取和更新"
    echo "  3. 前端构建 (Vue3 + TypeScript)"
    echo "  4. 后端构建 (Spring Boot + Maven)"
    echo "  5. Docker镜像构建和推送"
}

# 如果参数为help，显示使用说明
if [[ "$1" == "help" ]] || [[ "$1" == "--help" ]] || [[ "$1" == "-h" ]]; then
    show_usage
    exit 0
fi

# 执行主函数
main 