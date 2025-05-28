# 支持热更新的DockPilot镜像
# 基础版本：下载已构建的代码包 + 热更新能力

FROM alpine:3.21

# 声明构建参数
ARG VERSION=latest

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

# 🚀 下载已构建的前后端代码包
RUN echo "📦 下载DockPilot ${VERSION} 代码包..." && \
    if [ "$VERSION" = "latest" ]; then \
        echo "获取最新版本号..."; \
        LATEST_VERSION=$(wget -qO- https://api.github.com/repos/kidoneself/DockPilot/releases/latest | grep '"tag_name"' | cut -d'"' -f4); \
        if [ -n "$LATEST_VERSION" ]; then \
            VERSION=$LATEST_VERSION; \
            echo "最新版本: $VERSION"; \
        else \
            echo "⚠️ 无法获取最新版本，使用 v1.3.0"; \
            VERSION="v1.3.0"; \
        fi; \
    fi && \
    echo "下载版本: $VERSION" && \
    wget --tries=3 --retry-connrefused --timeout=60 \
        "https://github.com/kidoneself/DockPilot/releases/download/${VERSION}/frontend.tar.gz" \
        -O /tmp/frontend.tar.gz && \
    wget --tries=3 --retry-connrefused --timeout=60 \
        "https://github.com/kidoneself/DockPilot/releases/download/${VERSION}/backend.jar" \
        -O /tmp/backend.jar && \
    echo "✅ 代码包下载完成"

# 🎯 安装内置版本的前后端代码
RUN echo "📁 安装内置代码..." && \
    tar -xzf /tmp/frontend.tar.gz -C /usr/share/html-builtin/ && \
    cp /tmp/backend.jar /app/builtin/backend.jar && \
    echo "✅ 内置代码安装完成"

# 复制启动脚本和配置
COPY build/start-hot-update.sh /start.sh
COPY build/Caddyfile /etc/caddy/Caddyfile  
COPY build/download-app.sh /app/download-app.sh
COPY build/loading.html /usr/share/html/index.html
COPY build/init-builtin.sh /app/init-builtin.sh

# 设置权限
RUN chmod +x /start.sh /app/download-app.sh /app/init-builtin.sh

# 🔥 初始化：复制内置版本到运行目录
RUN echo "🔄 初始化运行环境..." && \
    cp -r /usr/share/html-builtin/* /usr/share/html/ && \
    cp /app/builtin/backend.jar /app/app.jar && \
    echo "builtin-${VERSION:-latest}" > /dockpilot/data/current_version && \
    echo "✅ 运行环境初始化完成"

# 清理临时文件
RUN rm -f /tmp/frontend.tar.gz /tmp/backend.jar && \
    echo "🗑️ 临时文件已清理"

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