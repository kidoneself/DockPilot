# Docker 管理平台前端 WebSocket 全局通知与页面集成规范

## 1. 全局 WebSocket 服务说明

- 项目已实现全局唯一的 WebSocket 服务（`DockerWebSocketService`），在 `@/api/websocket/DockerWebSocketService.ts`。
- WebSocket 连接在 `App.vue` 启动时全局初始化，任何页面/组件均可通过单例 `dockerWebSocketService` 进行消息收发。
- 所有后端推送的通知消息，均可通过 WebSocket 实时接收。

## 2. 页面如何接入 WebSocket

页面/组件如需监听 WebSocket 消息，推荐如下方式：

```ts
import { onMounted, onUnmounted } from 'vue';
import { dockerWebSocketService } from '@/api/websocket/DockerWebSocketService';

onMounted(() => {
  dockerWebSocketService.on('PULL_PROGRESS', handler);
  // ...可注册多个类型
});

onUnmounted(() => {
  dockerWebSocketService.off('PULL_PROGRESS', handler);
});

function handler(message) {
  // 处理消息
}
```

- 推荐在 `onMounted` 注册，在 `onUnmounted` 注销，避免内存泄漏。
- `on(type, handler)`/`off(type, handler)` 支持多类型、多回调。
- 发送消息用 `dockerWebSocketService.send(msg)`，参数为 WebSocketMessage 类型。

## 3. 通知中心集成与用法

- 全局通知中心基于 Pinia 的 `notification` store（`@/store/modules/notification.ts`）和 `Notice.vue` 组件实现。
- 后端推送的通知类消息（如 `TEST_NOTIFY_RESPONSE`），会自动写入通知中心，无需页面手动处理。
- 页面如需自定义通知，可调用 `useNotificationStore().addNotification(item)`。
- 通知数据结构见 `NotificationItem` 类型：

```ts
export interface NotificationItem {
  id: string;
  content: string;
  type: string;      // 通知类型，如"系统通知"
  status: boolean;   // true=未读，false=已读
  collected: boolean;// 是否收藏
  date: string;      // 时间
  quality: string;   // 重要性
}
```

## 4. 典型页面用法示例

以镜像拉取页面为例：

```ts
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { dockerWebSocketService } from '@/api/websocket/DockerWebSocketService';

const pullProgress = ref(0);
function onProgress(msg) {
  pullProgress.value = msg.data.progress || 0;
}

onMounted(() => {
  dockerWebSocketService.on('PULL_PROGRESS', onProgress);
});
onUnmounted(() => {
  dockerWebSocketService.off('PULL_PROGRESS', onProgress);
});
</script>
```

## 5. 约定与注意事项

- WebSocket 服务全局唯一，页面无需重复创建连接。
- 通知中心只处理后端推送的"通知"类消息，业务消息请按需自行处理。
- 注册/注销 handler 时，务必传递同一个函数引用。
- store 中通知数据已持久化，刷新页面不会丢失。
- 如需扩展通知类型、国际化、权限等，可在现有框架基础上迭代。

---
> 文档维护：如有变更请同步更新本文件及相关模块注释。 