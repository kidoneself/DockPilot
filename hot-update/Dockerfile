# 支持热更新的DockPilot镜像
# 基础镜像保持轻量，应用代码通过运行时下载

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
RUN mkdir -p /app /usr/share/html /dockpilot/data /dockpilot/logs /dockpilot/uploads

# 复制启动脚本和配置
COPY start-hot-update.sh /start.sh
COPY Caddyfile /etc/caddy/Caddyfile
COPY download-app.sh /app/download-app.sh
COPY loading.html /usr/share/html/index.html

# 设置权限
RUN chmod +x /start.sh /app/download-app.sh

# 暴露端口
EXPOSE 8888

# 数据卷
VOLUME ["/var/run/docker.sock", "/mnt/host", "/dockpilot"]

# 工作目录
WORKDIR /app

# 设置环境变量
ENV DOCKPILOT_VERSION=${VERSION:-latest}
ENV DOWNLOAD_URL_BASE=https://github.com/kidoneself/DockPilot/releases/download

# 启动脚本
ENTRYPOINT ["/start.sh"] 