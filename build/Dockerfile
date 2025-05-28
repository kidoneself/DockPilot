# 支持热更新的DockPilot镜像
# 基础版本：镜像内置代码 + 热更新能力

FROM alpine:3.21

# 安装基础工具和运行环境
RUN apk add --no-cache \
    openjdk11-jre \
    caddy \
    docker-cli \
    wget \
    curl \
    jq \
    tar \
    gzip \
    unzip \
    file \
    util-linux \
    tzdata \
    bash \
    skopeo --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community

# 下载regctl
RUN wget --tries=3 --retry-connrefused --timeout=30 \
    https://github.com/regclient/regclient/releases/latest/download/regctl-linux-amd64 \
    -O /usr/local/bin/regctl && \
    chmod +x /usr/local/bin/regctl

# 安装 Docker Compose 插件
RUN mkdir -p /usr/local/lib/docker/cli-plugins && \
    wget --tries=3 --retry-connrefused --timeout=30 \
    https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m) \
    -O /usr/local/lib/docker/cli-plugins/docker-compose && \
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 设置时区
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建应用目录结构
RUN mkdir -p /app /usr/share/html /dockpilot/data /dockpilot/logs /dockpilot/uploads \
    /app/builtin /usr/share/html-builtin

# 🔥 新增：构建时打包前后端代码作为内置版本
# 使用多阶段构建，先构建前后端代码

# Stage 1: 构建前端
FROM node:18-alpine AS frontend-builder
WORKDIR /build

# 复制前端源码
COPY dockpilot-frontend/package*.json ./
RUN npm ci

COPY dockpilot-frontend/ ./
RUN npm run build

# Stage 2: 构建后端
FROM maven:3.8-openjdk-11-slim AS backend-builder
WORKDIR /build

# 复制后端源码和pom.xml
COPY dockpilot-backend/pom.xml ./
RUN mvn dependency:go-offline -q

COPY dockpilot-backend/src ./src
RUN mvn clean package -DskipTests -q

# Stage 3: 最终镜像
FROM alpine:3.21

# 重复安装步骤（因为重新开始）
RUN apk add --no-cache \
    openjdk11-jre \
    caddy \
    docker-cli \
    wget \
    curl \
    jq \
    tar \
    gzip \
    unzip \
    file \
    util-linux \
    tzdata \
    bash \
    skopeo --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community

# 下载regctl
RUN wget --tries=3 --retry-connrefused --timeout=30 \
    https://github.com/regclient/regclient/releases/latest/download/regctl-linux-amd64 \
    -O /usr/local/bin/regctl && \
    chmod +x /usr/local/bin/regctl

# 安装 Docker Compose 插件
RUN mkdir -p /usr/local/lib/docker/cli-plugins && \
    wget --tries=3 --retry-connrefused --timeout=30 \
    https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m) \
    -O /usr/local/lib/docker/cli-plugins/docker-compose && \
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 设置时区
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建应用目录结构
RUN mkdir -p /app /usr/share/html /dockpilot/data /dockpilot/logs /dockpilot/uploads \
    /app/builtin /usr/share/html-builtin

# 🎯 复制内置版本的前后端代码
COPY --from=frontend-builder /build/dist/ /usr/share/html-builtin/
COPY --from=backend-builder /build/target/*.jar /app/builtin/backend.jar

# 复制启动脚本和配置
COPY build/start-hot-update.sh /start.sh
COPY build/Caddyfile /etc/caddy/Caddyfile  
COPY build/download-app.sh /app/download-app.sh
COPY build/loading.html /usr/share/html/index.html
COPY build/init-builtin.sh /app/init-builtin.sh

# 设置权限
RUN chmod +x /start.sh /app/download-app.sh /app/init-builtin.sh

# 🔥 初始化：复制内置版本到运行目录
RUN cp -r /usr/share/html-builtin/* /usr/share/html/ && \
    cp /app/builtin/backend.jar /app/app.jar && \
    echo "builtin-version" > /dockpilot/data/current_version

# 暴露端口
EXPOSE 8888

# 数据卷
VOLUME ["/var/run/docker.sock", "/mnt/host", "/dockpilot"]

# 工作目录
WORKDIR /app

# 设置环境变量
ENV DOCKPILOT_VERSION=${VERSION:-latest}
ENV DOWNLOAD_URL_BASE=https://github.com/kidoneself/DockPilot/releases/download
ENV BUILTIN_FALLBACK=true

# 启动脚本
ENTRYPOINT ["/start.sh"] 