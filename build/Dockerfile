# 使用干净的alpine基础镜像避免继承nginx镜像的80端口
FROM alpine:3.21

# 只暴露需要的端口
EXPOSE 8888

# 暴露数据卷
# /var/run/docker.sock - 用于容器内操作Docker，需要与宿主机共享
# /mnt/host - 用于访问宿主机文件系统，需要与宿主机根目录映射
# /dockpilot - 用于存储应用数据，需要与宿主机目录映射
VOLUME ["/var/run/docker.sock", "/mnt/host", "/dockpilot"]

# 创建nginx用户和用户组
RUN addgroup -g 101 -S nginx && \
    adduser -S -D -H -u 101 -h /var/cache/nginx -s /sbin/nologin -G nginx -g nginx nginx

# 安装nginx、Java、skopeo和regctl
RUN apk add --no-cache nginx openjdk11-jre && \
    apk add --no-cache skopeo --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community && \
    apk add --no-cache docker-cli && \
    apk add --no-cache wget jq tzdata

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

# 创建nginx所需目录
RUN mkdir -p /var/log/nginx /var/cache/nginx /etc/nginx/conf.d /usr/share/nginx/html && \
    chown -R nginx:nginx /var/log/nginx /var/cache/nginx /usr/share/nginx

WORKDIR /app

# 复制前端构建文件到nginx目录并设置权限
COPY dist /usr/share/nginx/html
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

# 复制后端jar包
COPY docker-manager-back-1.0.0.jar app.jar

# 复制nginx配置
COPY nginx.conf /etc/nginx/nginx.conf

# 复制启动脚本
COPY start.sh /start.sh
RUN chmod +x /start.sh

# 使用ENTRYPOINT作为固定入口点
ENTRYPOINT ["/start.sh"]