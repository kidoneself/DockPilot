<template>
  <div class="main-title-row">
    <div class="main-title">DockPilot</div>
    <div class="main-time">
      {{ currentTime }}<span class="main-date"> {{ currentDate }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

// 时钟数据
const currentTime = ref('');
const currentDate = ref('');
let timer: number;

// 更新时间函数
function updateTime() {
  const now = new Date();
  currentTime.value = now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
  currentDate.value = `${now.getMonth() + 1}-${now.getDate()} 星期${'日一二三四五六'[now.getDay()]}`;
}

// 生命周期钩子
onMounted(() => {
  updateTime();
  timer = window.setInterval(updateTime, 1000);
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
  }
});
</script>

<style scoped>
.main-title-row {
  display: flex;
  align-items: flex-end;
  gap: 2rem;
  margin-bottom: 2.5rem;
}

.main-title {
  font-size: 3rem;
  font-weight: 800;
  color: #fff;
  letter-spacing: 2px;
  text-shadow: 0 4px 24px rgba(0, 0, 0, 0.18);
}

.main-time {
  font-size: 1.5rem;
  color: #fff;
  font-weight: 400;
  opacity: 0.85;
}

.main-date {
  font-size: 1rem;
  margin-left: 0.5rem;
  opacity: 0.7;
}

/* 响应式布局 */
@media screen and (max-width: 992px) {
  .main-title {
    font-size: 2.5rem;
  }
  
  .main-time {
    font-size: 1.3rem;
  }
}

@media screen and (max-width: 768px) {
  .main-title-row {
    flex-direction: column;
    align-items: center;
    gap: 1rem;
    text-align: center;
  }
  
  .main-title {
    font-size: 2rem;
  }
  
  .main-time {
    font-size: 1.1rem;
  }
}

@media screen and (max-width: 480px) {
  .main-title {
    font-size: 1.8rem;
  }
  
  .main-time {
    font-size: 1rem;
  }
}
</style> 