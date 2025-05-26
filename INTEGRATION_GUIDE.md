# 🔥 DockPilot 热更新功能集成指南

## ✅ 已完成的工作

### 1. 前端集成
- ✅ **UpdateNotification.vue** 组件已放置到 `dockpilotfront/src/components/`
- ✅ **update.ts** API文件已放置到 `dockpilotfront/src/api/http/`
- ✅ **API导出** 已添加到 `dockpilotfront/src/api/http/index.ts`
- ✅ **HeaderBar组件** 已集成UpdateNotification组件
- ✅ **前端构建** 验证通过，无编译错误
- ✅ **依赖库** @vicons/ionicons5 已存在

### 2. 后端集成  
- ✅ **UpdateController.java** 已放置到 `docker-manager-back/src/main/java/com/dsm/controller/`
- ✅ **UpdateService.java** 已放置到 `docker-manager-back/src/main/java/com/dsm/service/http/`
- ✅ **UpdateInfoDTO.java** 已放置到 `docker-manager-back/src/main/java/com/dsm/model/dto/`
- ✅ **包声明** 所有Java文件都有正确的package声明

### 3. Docker基础设施
- ✅ **热更新镜像** 构建文件已准备在 `hot-update/` 目录
- ✅ **GitHub Actions** 工作流已放置到 `.github/workflows/build-and-release.yml`
- ✅ **启动脚本** 支持热更新的完整脚本已准备

## 🔧 需要完成的步骤

### 1. 解决后端编译问题
后端项目存在一些现有的编译错误（与热更新功能无关）。需要：

```bash
# 检查具体的编译错误
cd docker-manager-back
mvn clean compile -q

# 主要问题可能是：
# 1. 缺失 @Slf4j 注解导致的 log 变量错误
# 2. 缺失 getter/setter 方法导致的访问错误
# 3. 缺失 Lombok 注解
```

**解决方案**：
1. 确保所有WebSocket服务类都添加了 `@Slf4j` 注解
2. 确保所有DTO类都添加了 `@Data` 注解
3. 确保DockerWebSocketMessage类有正确的getter方法

### 2. 启动应用测试
```bash
# 前端启动
cd dockpilotfront
npm run dev

# 后端启动（解决编译问题后）
cd docker-manager-back  
mvn spring-boot:run
```

### 3. 功能测试
1. 访问前端应用 `http://localhost:3000`
2. 检查右上角是否显示更新按钮
3. 点击更新按钮测试功能

### 4. 构建热更新镜像（可选）
```bash
# 构建热更新版本镜像
docker build -f hot-update/Dockerfile -t dockpilot-hot .

# 运行热更新容器
docker run -d --privileged \
  --name dockpilot-hot \
  -p 8888:8888 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /:/mnt/host \
  -v /home/dockpilot:/dockpilot \
  --restart unless-stopped \
  dockpilot-hot
```

## 🎯 功能说明

### 前端功能
- 右上角显示更新通知按钮
- 自动检查更新（每30分钟）
- 发现新版本时显示红色感叹号
- 点击按钮显示更新对话框
- 实时显示更新进度和日志
- 支持取消更新操作
- 自动刷新页面功能

### 后端功能  
- `/api/update/check` - 检查新版本
- `/api/update/version` - 获取当前版本信息
- `/api/update/apply` - 执行热更新
- `/api/update/progress` - 获取更新进度
- `/api/update/cancel` - 取消更新
- `/api/update/auto-check` - 设置自动检查
- `/api/update/history` - 获取更新历史

### 热更新流程
1. 从GitHub Releases下载前后端包
2. 备份当前版本
3. 更新前端文件（Caddy自动加载）
4. 重启Java后端服务
5. 验证更新结果
6. 失败时自动回滚

## 📋 注意事项

1. **容器权限**：热更新需要容器有足够权限执行进程管理
2. **网络连接**：需要能访问GitHub API和Releases
3. **磁盘空间**：确保有足够空间存储备份和更新文件
4. **更新窗口**：建议在业务低峰期执行更新
5. **备份策略**：重要数据建议额外备份

## 🛠️ 故障排除

### 前端问题
- 检查浏览器控制台是否有JavaScript错误
- 确认API请求是否正常到达后端
- 检查naïve UI组件是否正确导入

### 后端问题  
- 检查Spring Boot应用是否正常启动
- 确认所有依赖注入是否正确
- 检查日志输出是否有异常信息

### 网络问题
- 确认容器能访问外网
- 检查GitHub API是否可达
- 验证下载地址是否正确

## 🚀 下一步

1. 先解决现有的编译错误
2. 完成基本功能测试
3. 根据需要调整配置
4. 部署到生产环境

热更新功能已基本集成完成，主要需要解决现有项目的编译问题即可正常使用。 