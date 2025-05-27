---
description: 
globs: 
alwaysApply: true
---
# DockPilot 代码提交控制规则

## 🚫 严格禁止未授权提交

### 核心原则
**除非用户明确说出以下关键词，否则绝对不允许执行任何 git 提交操作：**

- "提交打包"
- "打包"
- "发布" 
- "提交代码"
- "git commit"
- "推送代码"

### 🛑 禁止行为清单

1. **禁止自动提交**：不得在用户未明确要求的情况下执行 `git add`、`git commit`、`git push`
2. **禁止测试跳过**：不得在未进行基本验证的情况下提交代码
3. **禁止强制推送**：绝不使用 `git push --force` 等危险操作
4. **禁止标签操作**：不得在未授权情况下创建或推送 git 标签

### ✅ 允许的操作

1. **代码编辑**：可以修改文件内容和结构
2. **本地测试**：可以运行本地构建和测试命令
3. **状态查看**：可以执行 `git status`、`git diff` 等查看命令
4. **文件分析**：可以分析和搜索代码文件

### 🔍 提交前必须检查

当用户明确要求提交时，必须执行以下检查：

1. **代码验证**：
   - 检查语法错误
   - 验证关键功能（特别是热更新功能）
   - 确认没有明显的逻辑错误

2. **文件检查**：
   - 确认修改的文件路径正确
   - 验证关键配置文件：[dockpilot-backend/pom.xml](mdc:dockpilot-backend/pom.xml)
   - 检查更新服务：[dockpilot-backend/src/main/java/com/dockpilot/service/http/UpdateService.java](mdc:dockpilot-backend/src/main/java/com/dockpilot/service/http/UpdateService.java)

3. **版本管理**：
   - 确认版本号递增
   - 检查是否需要更新 [DEPLOY_GUIDE.md](mdc:DEPLOY_GUIDE.md)
   - 验证 GitHub Actions 配置：[.github/workflows/build-and-release.yml](mdc:.github/workflows/build-and-release.yml)

### 📋 DockPilot 特殊规则

1. **热更新功能**：修改更新相关代码必须特别谨慎
2. **Docker 配置**：修改 [build/](mdc:build) 目录下文件需要额外确认
3. **前端组件**：修改 [dockpilotfront/](mdc:dockpilotfront) 需要确认构建正常
4. **打包策略**：遵循 [packaging-deployment 规则](mdc:packaging-deployment)

### ⚠️ 警告信号

如果出现以下情况，必须停止并询问用户：

- 修改涉及多个重要文件
- 更改核心依赖版本
- 影响生产环境部署的修改
- 用户没有明确说明提交意图

### 🎯 标准提交流程

只有当用户明确要求时才执行：

```bash
# 1. 检查状态
git status

# 2. 添加文件
git add .

# 3. 提交代码（语义化消息）
git commit -m "类型: 描述"

# 4. 推送到远程
git push origin main

# 5. 创建标签（如果需要）
git tag v1.x.x
git push origin v1.x.x
```

### 💬 用户确认模板

在执行提交前，必须明确告知用户：

```
🔍 准备提交以下更改：
- 修改文件：[列出文件]
- 更改内容：[简要说明]
- 预期影响：[说明影响范围]

确认要执行提交操作吗？
```

## 📚 相关文档

- [部署指南](mdc:DEPLOY_GUIDE.md)
- [集成文档](mdc:INTEGRATION_GUIDE.md)
- [GitHub Actions 配置](mdc:.github/workflows/build-and-release.yml)

---

**记住：代码安全大于一切，宁可多问一句，不可擅自提交！**
---
description: 
globs: 
alwaysApply: true
---
# DockPilot 打包和发布策略规则

## 🤖 智能识别规则

### 当用户说"提交打包"、"打包"、"发布"时的自动判断逻辑：

#### 🔍 自动检测文件变更类型：
1. **代码包发布** (默认优先，90%场景)：
   - 检测到 `dockpilotfront/` 目录变更
   - 检测到 `docker-manager-back/src/` 目录变更
   - 包含 `.vue`, `.ts`, `.js`, `.java` 文件修改
   - commit 消息包含: `feat:`, `fix:`, `refactor:`, `perf:`

2. **Docker 镜像发布** (特殊场景触发)：
   - 检测到 `build/Dockerfile` 变更
   - 检测到 `build/Caddyfile` 变更
   - 检测到 `build/start.sh` 变更
   - 检测到 `docker-manager-back/pom.xml` 中 Java 版本变更
   - commit 消息包含: `[docker]`, `docker:`, `升级Java`, `基础环境`

#### 🎯 智能操作流程：
```bash
# 用户说: "提交打包" → AI自动执行：
1. git status (检查变更文件)
2. 根据变更文件类型判断打包策略
3. git add . && git commit
4. git push origin <当前分支>
5. git tag v1.x.x && git push origin v1.x.x (代码包发布)
   或 git commit -m "xxx [docker]" (镜像发布)
```

#### 📋 快速识别表：
| 文件路径模式 | 自动选择策略 | 触发方式 |
|---|---|---|
| `dockpilotfront/**` | 代码包发布 | 推送标签 |
| `docker-manager-back/src/**` | 代码包发布 | 推送标签 |
| `build/Dockerfile` | Docker镜像 | commit加[docker] |
| `build/Caddyfile` | Docker镜像 | commit加[docker] |
| `build/start.sh` | Docker镜像 | commit加[docker] |
| `pom.xml`(Java版本) | Docker镜像 | commit加[docker] |

## 🎯 项目概览
DockPilot 是一个 Docker 管理平台，采用双重发布策略：
- **前端**: Vue3 + TypeScript + TDesign ([dockpilotfront/](mdc:dockpilotfront))
- **后端**: Spring Boot + Java ([docker-manager-back/](mdc:docker-manager-back))
- **自动化**: GitHub Actions ([.github/workflows/build-and-release.yml](mdc:.github/workflows/build-and-release.yml))

## 📦 发布策略选择

### 🔥 策略1: 代码包发布 (推荐 90% 场景)

**触发方式**: 推送 Git 标签
```bash
git tag v1.2.3
git push origin v1.2.3
```

**产出物**:
- `frontend.tar.gz` - 前端构建包
- `backend.jar` - 后端应用包

**适用场景**:
- ✅ 功能更新、bug 修复
- ✅ 界面优化、逻辑改进  
- ✅ 日常迭代开发
- ✅ 紧急修复发布
- ✅ API 接口变更
- ✅ 业务逻辑调整

**优势**:
- ⚡ 构建速度快 (3-5分钟)
- 🔄 支持热更新
- 💰 节省 CI 资源
- 🎯 用户体验好

### 🐳 策略2: Docker 镜像发布 (特殊场景)

**触发方式**: 
- 手动触发 GitHub Actions
- commit 消息包含 `[docker]` 标签

**产出物**:
- `kidself/dockpilot:latest`
- `kidself/dockpilot:hot`

**适用场景**:
- ✅ Java 版本升级
- ✅ 基础依赖更新
- ✅ Dockerfile 修改 ([build/Dockerfile](mdc:build/Dockerfile))
- ✅ Caddy 配置变更 ([build/Caddyfile](mdc:build/Caddyfile))
- ✅ 系统环境变更
- ✅ 启动脚本更新 ([build/start.sh](mdc:build/start.sh))

**劣势**:
- ⏱️ 构建时间长 (8-15分钟)
- 🔄 需要用户重新部署
- 💰 消耗更多资源

## 🚀 简化指令 (AI自动执行)

### 用户常用表达 → AI自动识别执行：

#### 🎯 "提交打包" / "打包" / "发布"
```bash
# AI自动执行流程：
1. 检查 git status 分析变更文件
2. 智能判断：代码包发布 vs Docker镜像发布  
3. 提交代码并推送当前分支
4. 创建版本标签触发自动构建
5. 告知用户发布进度和预期完成时间
```

#### 💡 "测试打包" / "本地构建"
```bash
# AI执行本地构建测试：
cd build && sudo ./setup-and-deploy.sh v1.0.0-test dev
```

#### 🐳 "强制Docker打包" / "镜像打包"
```bash
# AI强制执行Docker镜像发布：
git commit -m "xxx [docker]" && git tag && git push
```

## 🚀 标准发布流程

### 日常开发发布 (自动化)
```bash
# 用户说："提交打包" → AI自动执行：
1. git status (检查变更)
2. git add . && git commit -m "智能生成的commit消息"
3. git push origin <当前分支>
4. git tag v1.x.x && git push origin v1.x.x

# ✅ GitHub Actions 自动执行:
# - 构建前端 (Vue3) + 后端 (Spring Boot)
# - 创建 GitHub Release
# - 生成可热更新的包文件 (3-5分钟)
```

### 强制 Docker 镜像发布
```bash
# 方式1: commit 消息触发
git commit -m "chore: 升级Java版本 [docker]"
git tag v1.2.3
git push origin v1.2.3

# 方式2: GitHub Actions 手动触发
# 访问 GitHub → Actions → Build and Release → Run workflow
```

## 🔧 本地开发和测试

### 前端构建
```bash
cd dockpilotfront
npm install
npm run dev     # 开发模式
npm run build   # 生产构建
```

### 后端构建
```bash
cd docker-manager-back  
mvn spring-boot:run              # 开发运行
mvn clean package -DskipTests    # 生产构建
```

### 完整本地构建
```bash
# 使用项目构建脚本
cd build
sudo ./setup-and-deploy.sh v1.0.0-test dev
```

## 📊 决策表格

| 更新内容 | 推荐策略 | 构建时间 | 用户更新方式 |
|---|---|---|---|
| 前端界面更新 | 代码包发布 | 3-5分钟 | 热更新，无需重启 |
| 后端业务逻辑 | 代码包发布 | 3-5分钟 | 服务重启 1-2分钟 |
| API 接口变更 | 代码包发布 | 3-5分钟 | 热更新 |
| 配置文件修改 | 代码包发布 | 3-5分钟 | 热更新 |
| Java 版本升级 | Docker 镜像 | 8-15分钟 | 重新部署容器 |
| 系统依赖更新 | Docker 镜像 | 8-15分钟 | 重新部署容器 |
| Dockerfile 变更 | Docker 镜像 | 8-15分钟 | 重新部署容器 |
| 紧急 Bug 修复 | 代码包发布 | 3-5分钟 | 快速热更新 |

## 🎯 热更新机制

### 用户侧部署
```bash
# 部署支持热更新的容器
docker run -d --privileged \
  --name dockpilot \
  -p 8888:8888 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /:/mnt/host \
  -v /home/dockpilot:/dockpilot \
  --restart unless-stopped \
  kidself/dockpilot:latest
```

### 热更新流程
1. 用户在管理界面点击右上角更新按钮
2. 系统检查 GitHub Release 获取最新版本
3. 自动下载 `frontend.tar.gz` 和 `backend.jar`
4. 前端无刷新更新，后端服务重启
5. 更新完成，提示用户

## ⚠️ 注意事项

### 版本标签规范
- 使用语义化版本: `v1.2.3`
- 测试版本: `v1.2.3-test`
- 预发布版本: `v1.2.3-beta`

### 发布前检查
- ✅ 前端构建无错误
- ✅ 后端测试通过
- ✅ 版本号递增
- ✅ commit 消息清晰

### 回滚策略
- 代码包发布: 热更新系统自动回滚
- Docker 镜像: 手动回滚到上一个版本标签

## 🧠 智能判断示例

### 场景1: 前端组件更新
```bash
# git status 显示:
modified: dockpilotfront/src/components/UpdateNotification.vue
modified: dockpilotfront/package.json

# AI判断: 代码包发布 ✅
# 执行: git tag v1.x.x && git push origin v1.x.x
# 结果: frontend.tar.gz + backend.jar (3-5分钟)
```

### 场景2: 后端业务逻辑
```bash
# git status 显示:
modified: docker-manager-back/src/main/java/com/dsm/service/UpdateService.java

# AI判断: 代码包发布 ✅  
# 执行: git tag v1.x.x && git push origin v1.x.x
# 结果: frontend.tar.gz + backend.jar (3-5分钟)
```

### 场景3: Docker基础环境
```bash
# git status 显示:
modified: build/Dockerfile
modified: build/start.sh

# AI判断: Docker镜像发布 🐳
# 执行: git commit -m "xxx [docker]" && git tag && git push
# 结果: kidself/dockpilot:latest 镜像 (8-15分钟)
```

### 场景4: 混合变更
```bash
# git status 显示:
modified: dockpilotfront/src/App.vue
modified: build/Dockerfile

# AI提示: 检测到混合变更，询问用户选择
# 建议: 分别提交，优先代码包发布
```

## 🤖 AI 行为准则

### 当用户说"提交打包"时，AI必须：

1. **🔍 首先检查**: `git status` 分析变更文件
2. **🧠 智能判断**: 根据文件路径自动选择策略
3. **💬 明确告知**: "检测到XX变更，将执行XX发布策略"
4. **⚡ 自动执行**: 完整的提交+打包流程
5. **📊 进度反馈**: 告知构建时间和查看链接

### 优先级规则：
- 🥇 **代码包发布**: 默认优先选择
- 🥈 **Docker镜像**: 仅在检测到基础环境变更时
- 🥉 **询问用户**: 混合变更或不确定时

### 安全检查：
- ✅ 确认当前分支状态
- ✅ 检查版本号递增
- ✅ 验证构建文件存在  
- ✅ 提示预计完成时间

## 🚫 避免的反模式

❌ **错误做法**:
- 小功能更新使用 Docker 镜像发布
- 频繁重建 Docker 镜像
- 不使用语义化版本标签
- 跳过本地测试直接发布

✅ **正确做法**:  
- 优先使用代码包发布
- 合理利用热更新机制
- 遵循发布流程规范
- 充分测试后再发布

## 📋 相关文件

- [前端项目](mdc:dockpilotfront) - Vue3 前端代码
- [后端项目](mdc:docker-manager-back) - Spring Boot 后端代码
- [构建脚本](mdc:build/setup-and-deploy.sh) - 本地构建部署
- [GitHub Actions](mdc:.github/workflows/build-and-release.yml) - 自动化 CI/CD
- [部署指南](mdc:DEPLOY_GUIDE.md) - 用户部署文档
- [集成指南](mdc:INTEGRATION_GUIDE.md) - 开发集成文档

## 📝 总结

### 🎯 核心原则
- **智能识别**: 根据文件变更自动判断打包策略
- **简化操作**: 用户说"提交打包"即可，AI自动执行完整流程  
- **安全优先**: 代码包发布为默认选择，Docker镜像仅特殊场景
- **用户友好**: 明确反馈进度，告知预期完成时间

### 🔮 典型对话示例
```
用户: "我修改了前端组件，提交打包吧"
AI: "🔍 检测到前端组件变更，将执行代码包发布策略
     ⚡ 正在提交代码并创建版本标签 v1.0.9
     📦 GitHub Actions 将在3-5分钟内构建完成
     🔗 查看进度: https://github.com/xxx/actions"

用户: "Dockerfile改了，需要打包"  
AI: "🐳 检测到Docker基础环境变更，将执行镜像发布策略
     ⏱️ 预计8-15分钟完成镜像构建
     🔗 构建进度: https://github.com/xxx/actions"
```

遵循这些智能化规则，可以确保高效、稳定的发布流程，为用户提供最佳的更新体验。
