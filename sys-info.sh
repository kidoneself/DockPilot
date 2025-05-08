#!/bin/bash

# 获取主机名
hostname=$(hostname)

# 获取内核版本
kernel=$(uname -r)

# 获取 OS 名称（兼容 Linux / 群晖 DSM / Alpine 等）
if [ -f /etc/os-release ]; then
    os=$(grep PRETTY_NAME /etc/os-release | cut -d= -f2 | tr -d '"')
elif [ -f /etc.defaults/VERSION ]; then
    os="Synology DSM $(grep productversion /etc.defaults/VERSION | cut -d\" -f2)"
elif [ -f /etc/alpine-release ]; then
    os="Alpine $(cat /etc/alpine-release)"
else
    os=$(uname -o)
fi

# 获取系统运行时间
uptime=$(uptime -p 2>/dev/null || uptime)

# 获取 CPU 信息
cpu_cores=$(grep -c ^processor /proc/cpuinfo)
cpu_model=$(grep "model name" /proc/cpuinfo | head -1 | cut -d: -f2 | sed 's/^ //')

# 获取内存信息（单位：MB）
mem_total=$(free -m | awk '/Mem:/ {print $2}')
mem_used=$(free -m | awk '/Mem:/ {print $3}')
mem_free=$(free -m | awk '/Mem:/ {print $4}')

# 获取磁盘信息
disk_used_percent=$(df / | awk 'NR==2 {print $5}')
disk_free=$(df -h / | awk 'NR==2 {print $4}')

# 获取 IP 地址
ip=$(ip addr show | awk '/inet / && $2 !~ /^127/ {print $2}' | cut -d/ -f1 | head -1)

# 获取默认网关
gateway=$(ip route | awk '/default/ {print $3}')

# 输出 JSON
echo "{
  \"hostname\": \"$hostname\",
  \"kernel\": \"$kernel\",
  \"os\": \"$os\",
  \"uptime\": \"${uptime}\",
  \"cpu_cores\": \"$cpu_cores\",
  \"cpu_model\": \"$cpu_model\",
  \"mem_total\": \"$mem_total\",
  \"mem_used\": \"$mem_used\",
  \"mem_free\": \"$mem_free\",
  \"disk_used_percent\": \"$disk_used_percent\",
  \"disk_free\": \"$disk_free\",
  \"ip\": \"$ip\",
  \"gateway\": \"$gateway\"
}"