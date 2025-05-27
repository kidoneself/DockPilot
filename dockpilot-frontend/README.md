# Docker 管理后台

基于 Vue 3 + Vite + Naive UI 的 Docker 管理后台项目。

## 功能特性

- 容器管理：查看、创建、编辑、删除容器
- 镜像管理：查看、拉取、删除镜像
- 网络管理：查看、创建、删除网络
- 暗黑模式支持
- 响应式布局

## 技术栈

- Vue 3
- TypeScript
- Vite
- Naive UI
- Pinia
- Vue Router

## 开发环境要求

- Node.js >= 16.0.0
- npm >= 7.0.0

## 快速开始

1. 克隆项目

```bash
git clone https://github.com/yourusername/docker-management-ui.git
cd docker-management-ui
```

2. 安装依赖

```bash
npm install
```

3. 启动开发服务器

```bash
npm run dev
```

4. 构建生产版本

```bash
npm run build
```

## 项目结构

```
docker-management-ui/
├── public/
├── src/
│   ├── assets/          # 静态资源
│   ├── components/      # 通用组件
│   ├── views/          # 页面组件
│   ├── router/         # 路由配置
│   ├── store/          # 状态管理
│   ├── utils/          # 工具函数
│   ├── App.vue         # 根组件
│   └── main.ts         # 入口文件
├── .gitignore
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 开发规范

- 使用 TypeScript 进行开发
- 遵循 Vue 3 组合式 API 风格
- 组件命名采用 PascalCase
- 变量命名采用 camelCase
- 使用 ESLint 进行代码规范检查

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 许可证

MIT License 