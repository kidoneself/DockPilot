<template>
  <div class="dashboard-grid">
    <!-- 系统概览卡片 -->
    <div class="dash-card glass" :class="{ 'animate-in': dashboardVisible }">
      <div class="dash-icon system"></div>
      <div class="dash-label">系统概览</div>
      <div class="dash-info">
        <div class="info-group">
          <div class="info-item">
            <span class="info-label">操作系统</span>
            <span class="info-value">Alpine Linux v3.21</span>
          </div>
          <div class="info-item">
            <span class="info-label">架构</span>
            <span class="info-value">x64</span>
          </div>
          <div class="info-item">
            <span class="info-label">运行时间</span>
            <span class="info-value">1天 17小时 42分钟</span>
          </div>
        </div>
        <div class="info-group">
          <div class="info-item">
            <span class="info-label">CPU使用率</span>
            <span class="info-value">32%</span>
          </div>
          <div class="info-item">
            <span class="info-label">内存使用率</span>
            <span class="info-value">45%</span>
          </div>
          <div class="info-item">
            <span class="info-label">磁盘使用率</span>
            <span class="info-value">28%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 容器管理卡片 -->
    <div class="dash-card glass" :class="{ 'animate-in': dashboardVisible }">
      <div class="dash-icon container"></div>
      <div class="dash-label">容器管理</div>
      <div class="dash-info">
        <div class="info-group">
          <div class="info-item">
            <span class="info-label">容器总数</span>
            <span class="info-value">12</span>
          </div>
          <div class="info-item">
            <span class="info-label">运行中</span>
            <span class="info-value">8</span>
          </div>
          <div class="info-item">
            <span class="info-label">已停止</span>
            <span class="info-value">4</span>
          </div>
        </div>
        <div class="info-group">
          <div class="info-item">
            <span class="info-label">最近创建</span>
            <span class="info-value">nginx:latest</span>
          </div>
          <div class="info-item">
            <span class="info-label">最近更新</span>
            <span class="info-value">mysql:8.0</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 镜像管理卡片 -->
    <div class="dash-card glass" :class="{ 'animate-in': dashboardVisible }">
      <div class="dash-icon image"></div>
      <div class="dash-label">镜像管理</div>
      <div class="dash-info">
        <div class="info-group">
          <div class="info-item">
            <span class="info-label">镜像总数</span>
            <span class="info-value">24</span>
          </div>
          <div class="info-item">
            <span class="info-label">未使用镜像</span>
            <span class="info-value">5</span>
          </div>
          <div class="info-item">
            <span class="info-label">存储空间</span>
            <span class="info-value">2.4 GB</span>
          </div>
        </div>
        <div class="info-group">
          <div class="info-item">
            <span class="info-label">最近拉取</span>
            <span class="info-value">redis:latest</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 网络管理卡片 -->
    <div class="dash-card glass" :class="{ 'animate-in': dashboardVisible }">
      <div class="dash-icon network"></div>
      <div class="dash-label">网络管理</div>
      <div class="dash-info">
        <div class="info-group">
          <div class="info-item">
            <span class="info-label">网络总数</span>
            <span class="info-value">3</span>
          </div>
          <div class="info-item">
            <span class="info-label">活跃连接</span>
            <span class="info-value">8</span>
          </div>
          <div class="info-item">
            <span class="info-label">端口映射</span>
            <span class="info-value">12</span>
          </div>
        </div>
        <div class="info-group">
          <div class="info-item">
            <span class="info-label">上传速度</span>
            <span class="info-value">5.0 Mb/s</span>
          </div>
          <div class="info-item">
            <span class="info-label">下载速度</span>
            <span class="info-value">2.0 Mb/s</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 存储管理卡片 -->
    <div class="dash-card glass" :class="{ 'animate-in': dashboardVisible }">
      <div class="dash-icon storage"></div>
      <div class="dash-label">存储管理</div>
      <div class="dash-info">
        <div class="info-group">
          <div class="info-item">
            <span class="info-label">卷总数</span>
            <span class="info-value">6</span>
          </div>
          <div class="info-item">
            <span class="info-label">未使用卷</span>
            <span class="info-value">1</span>
          </div>
          <div class="info-item">
            <span class="info-label">存储空间</span>
            <span class="info-value">150.0 GiB</span>
          </div>
        </div>
        <div class="info-group">
          <div class="info-item">
            <span class="info-label">最近创建</span>
            <span class="info-value">mysql_data</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 系统日志卡片 -->
    <div class="dash-card glass" :class="{ 'animate-in': dashboardVisible }">
      <div class="dash-icon log"></div>
      <div class="dash-label">系统日志</div>
      <div class="dash-info">
        <div class="info-group">
          <div class="info-item">
            <span class="info-label">系统事件</span>
            <span class="info-value">3条新消息</span>
          </div>
          <div class="info-item">
            <span class="info-label">错误日志</span>
            <span class="info-value">1个警告</span>
          </div>
          <div class="info-item">
            <span class="info-label">操作记录</span>
            <span class="info-value">5条记录</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, defineProps } from 'vue';

const props = defineProps({
  dashboardVisible: {
    type: Boolean,
    default: false
  }
});
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 2rem;
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  width: 100%;
}

.dash-card {
  padding: 2rem 1.5rem;
  border-radius: 1.5rem;
  min-height: 180px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  position: relative;
  transition: all 0.3s ease;
  cursor: default;
}

.dash-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.2);
}

.glass {
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(12px);
  border: 1.5px solid rgba(255,255,255,0.18);
}

.dash-icon {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.8rem;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  position: relative;
  backdrop-filter: blur(4px);
}

.dash-icon::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.2);
  border-radius: inherit;
  z-index: -1;
}

.dash-icon.system { 
  background: linear-gradient(45deg, rgba(110, 231, 183, 0.8), rgba(110, 231, 183, 0.4));
  border: 1px solid rgba(110, 231, 183, 0.6); 
}

.dash-icon.container { 
  background: linear-gradient(45deg, rgba(252, 165, 165, 0.8), rgba(252, 165, 165, 0.4));
  border: 1px solid rgba(252, 165, 165, 0.6); 
}

.dash-icon.image { 
  background: linear-gradient(45deg, rgba(165, 180, 252, 0.8), rgba(165, 180, 252, 0.4));
  border: 1px solid rgba(165, 180, 252, 0.6); 
}

.dash-icon.network { 
  background: linear-gradient(45deg, rgba(253, 230, 138, 0.8), rgba(253, 230, 138, 0.4));
  border: 1px solid rgba(253, 230, 138, 0.6); 
}

.dash-icon.storage { 
  background: linear-gradient(45deg, rgba(167, 139, 250, 0.8), rgba(167, 139, 250, 0.4));
  border: 1px solid rgba(167, 139, 250, 0.6); 
}

.dash-icon.log { 
  background: linear-gradient(45deg, rgba(251, 191, 36, 0.8), rgba(251, 191, 36, 0.4));
  border: 1px solid rgba(251, 191, 36, 0.6); 
}

.dash-label {
  font-size: 1.1rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 0.8rem;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.dash-info {
  width: 100%;
}

.info-group {
  margin-bottom: 1rem;
}

.info-group:last-child {
  margin-bottom: 0;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.9);
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  color: rgba(255, 255, 255, 0.7);
}

.info-value {
  font-weight: 500;
}

/* 仪表盘卡片动画 */
.dash-card {
  opacity: 0;
  transform: translateY(20px);
  animation-fill-mode: forwards;
}

.dash-card.animate-in {
  animation: cardFadeIn 0.6s ease forwards;
}

@keyframes cardFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式布局调整 */
@media screen and (max-width: 768px) {
  .dashboard-grid {
    gap: 1.5rem;
    padding: 1.5rem;
  }
  
  .dash-card {
    padding: 1.5rem 1.2rem;
  }
}

@media screen and (max-width: 480px) {
  .dashboard-grid {
    gap: 1rem;
    padding: 1rem;
  }
  
  .dash-card {
    padding: 1.2rem 1rem;
  }
  
  .info-item {
    font-size: 0.85rem;
  }
}
</style> 