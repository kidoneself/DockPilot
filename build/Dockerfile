# 使用nginx作为基础镜像
FROM nginx:stable-alpine

# 安装Java、skopeo和regctl
RUN apk add --no-cache openjdk11-jre && \
    apk add --no-cache skopeo --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community && \
    apk add --no-cache docker-cli && \
    apk add --no-cache wget jq tzdata && \
    wget --tries=3 --retry-connrefused --timeout=30 https://github.com/regclient/regclient/releases/latest/download/regctl-linux-amd64 -O /usr/local/bin/regctl && \
    chmod +x /usr/local/bin/regctl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建应用目录结构并设置权限
RUN mkdir -p /dockpilot/data /dockpilot/logs && \
    chmod 700 /dockpilot/data && \
    chmod 755 /dockpilot/logs

WORKDIR /app

# 复制前端构建文件到nginx目录并设置权限
COPY dist /usr/share/nginx/html
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

# 复制后端jar包
COPY docker-manager-back-1.0.0.jar app.jar

# 复制nginx配置
COPY nginx.conf /etc/nginx/nginx.conf

# 使用默认的nginx配置
# 不复制自定义的nginx.conf

# 暴露端口
EXPOSE 8888

# 启动命令
CMD ["sh", "-c", "nginx -g 'daemon off;' & java -jar app.jar"]