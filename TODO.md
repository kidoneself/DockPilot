# 📋 DockPilot 多节点管理功能 TODO

## 🎯 需求背景
- **现状**：多个NAS部署DockPilot，需要手动输入不同地址访问
- **目标**：一个前端界面切换管理所有NAS的Docker环境
- **核心**：操作便利性优先，去中心化架构

## 🏗️ 架构设计

### 总体方案
- **去中心化**：所有节点平等，任意节点都能管理其他节点
- **节点发现**：手动添加一个节点后，自动发现其他相关节点
- **动态切换**：前端可以切换连接到不同后端服务

## 📝 开发任务清单

### 🔧 后端开发

#### 1. 数据库设计
- [ ] 创建节点信息表
```sql
CREATE TABLE nodes (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100),
    host VARCHAR(100), 
    port INT,
    status VARCHAR(20),
    created_time TIMESTAMP,
    updated_time TIMESTAMP
);
```

#### 2. 节点管理API
- [ ] `GET /api/nodes/list` - 获取节点列表
- [ ] `POST /api/nodes/add` - 添加节点
- [ ] `POST /api/nodes/notify` - 节点间通知接口
- [ ] `DELETE /api/nodes/{id}` - 删除节点
- [ ] `GET /api/nodes/{id}/health` - 节点健康检查

#### 3. 节点服务层
- [ ] `NodeService` - 节点CRUD操作
- [ ] `NodeDiscoveryService` - 节点发现和通知逻辑
- [ ] `NodeHealthService` - 节点状态检查（定时任务）

#### 4. 节点通信机制
- [ ] HTTP客户端封装，用于节点间通信
- [ ] 节点注册握手协议实现
- [ ] 节点信息同步逻辑

### 🎨 前端开发

#### 1. 节点管理界面
- [ ] 节点列表显示组件
- [ ] 添加节点对话框
- [ ] 节点状态展示（在线/离线）
- [ ] 节点删除确认

#### 2. 节点切换功能
- [ ] 顶部节点选择器组件
- [ ] 当前节点状态指示
- [ ] 节点切换时的loading状态

#### 3. 动态连接管理
- [ ] 修改 `request.ts` 支持动态baseURL切换
- [ ] 修改 `websocketClient.ts` 支持动态连接切换
- [ ] 切换节点时清理当前状态并重新加载

#### 4. 用户体验优化
- [ ] 节点连接失败提示
- [ ] 切换节点时的平滑过渡
- [ ] 节点连接状态实时显示

### 🔧 系统功能

#### 1. 节点发现流程
- [ ] 手动添加节点A时，A自动注册回当前节点
- [ ] A告知当前节点它已知的其他节点列表
- [ ] 当前节点告知A自己已知的其他节点
- [ ] 实现完整的节点网络同步

#### 2. 健康检查机制
- [ ] 定时ping所有已知节点
- [ ] 更新节点在线状态
- [ ] 自动清理长期离线节点

#### 3. 错误处理
- [ ] 网络连接失败重试机制
- [ ] 节点不可达时的降级处理
- [ ] WebSocket断线重连逻辑

## 🧪 测试计划

### 功能测试
- [ ] 单节点正常运行测试
- [ ] 两节点相互发现测试
- [ ] 多节点（3+）网络同步测试
- [ ] 节点上下线场景测试

### 用户体验测试  
- [ ] 节点切换流畅性测试
- [ ] 界面响应性测试
- [ ] 错误情况用户友好性测试

## 📚 文档更新

- [ ] 多节点部署说明文档
- [ ] 节点管理使用手册
- [ ] API接口文档更新
- [ ] 架构设计文档

## 🎯 开发优先级

### Phase 1 - MVP功能（1-2周）
1. 后端节点管理API
2. 前端基础切换界面
3. 简单的手动添加节点功能

### Phase 2 - 自动发现（1周）
1. 节点间通信协议
2. 自动节点发现机制
3. 节点健康检查

### Phase 3 - 用户体验优化（1周）
1. 界面美化和交互优化
2. 错误处理完善
3. 性能优化

## 💡 技术要点

- **前端状态管理**：切换节点时需要清理并重建所有状态
- **WebSocket管理**：动态重连到不同节点的WebSocket
- **错误处理**：网络分区、节点故障等异常情况
- **数据一致性**：节点列表在各节点间的同步

## ✅ 验收标准

- [ ] 用户可以在任意节点前端添加其他节点
- [ ] 添加一个节点后，所有相关节点自动互相发现
- [ ] 前端可以无缝切换管理不同节点的Docker
- [ ] 节点状态实时更新，离线节点正确标识
- [ ] 整个系统去中心化，无单点故障

## 📁 相关文件

### 前端文件
- `dockpilot-frontend/src/utils/request.ts` - HTTP请求配置
- `dockpilot-frontend/src/utils/websocketClient.ts` - WebSocket客户端
- `dockpilot-frontend/src/api/http/` - API接口定义
- `dockpilot-frontend/src/components/` - 节点管理组件
- `dockpilot-frontend/src/views/` - 节点管理页面

### 后端文件
- `dockpilot-backend/src/main/java/com/dockpilot/controller/` - 节点管理控制器
- `dockpilot-backend/src/main/java/com/dockpilot/service/` - 节点服务层
- `dockpilot-backend/src/main/java/com/dockpilot/model/` - 节点数据模型
- `dockpilot-backend/src/main/resources/application.yml` - 配置文件

---

**开发开始时间**：待定  
**预计完成时间**：3-4周  
**负责人**：待分配  
**优先级**：中等 