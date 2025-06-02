# 支持热更新的DockPilot镜像
# 基础版本：本地构建代码 + 热更新能力

FROM alpine:3.21

# 声明构建参数
ARG VERSION=v1.0.0

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
    shadow \
    su-exec \
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

# 🔄 改为复制本地构建的代码文件（而不是下载）
# 这些文件由GitHub Actions在构建镜像前准备好
COPY frontend.tar.gz /tmp/frontend.tar.gz
COPY backend.jar /tmp/backend.jar

# 🎯 安装内置版本的前后端代码
RUN echo "📁 安装内置代码..." && \
    tar -xzf /tmp/frontend.tar.gz -C /usr/share/html-builtin/ && \
    cp /tmp/backend.jar /app/builtin/backend.jar && \
    rm -f /tmp/frontend.tar.gz /tmp/backend.jar && \
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
RUN mkdir -p /dockpilot/data && \
    cp -r /usr/share/html-builtin/* /usr/share/html/ && \
    cp /app/builtin/backend.jar /app/app.jar && \
    echo "$VERSION" > /dockpilot/data/current_version && \
    echo "✅ 版本已设置为: $VERSION"

# 🔥 设置目录权限（允许PUID用户访问）
RUN chown -R dockpilot:dockpilot /app /usr/share/html /dockpilot && \
    chmod -R 755 /app /usr/share/html && \
    chmod -R 777 /dockpilot

# 暴露端口
EXPOSE 8888

# 数据卷
VOLUME ["/var/run/docker.sock", "/mnt/host", "/dockpilot"]

# 工作目录
WORKDIR /app

# 🔥 设置PUID/PGID/UMASK环境变量
ENV PUID=0
ENV PGID=0
ENV UMASK=022
ENV DOCKPILOT_VERSION=${VERSION}
ENV DOWNLOAD_URL_BASE=https://github.com/kidoneself/DockPilot/releases/download
ENV BUILTIN_FALLBACK=true

# 启动脚本
ENTRYPOINT ["/start.sh"] 