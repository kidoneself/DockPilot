#!/bin/sh
# nginx已经在基础镜像中配置为自动启动
# 只需要启动Java后端
java -jar app.jar