<template>
  <n-card class="feature-card" :segmented="{ content: true, footer: 'soft' }">
    <template #header>
      <div class="header-row">
        <span class="feature-title">{{ title }}</span>
        <div class="switch-row">
          <n-tag
            :type="enabled ? 'success' : 'default'"
            size="small"
          >
            {{ enabled ? '已启用' : '未启用' }}
          </n-tag>
          <n-switch 
            v-model:value="switchValue" 
            style="margin-left: 8px;" 
            @update:value="onToggle" 
          />
        </div>
      </div>
    </template>
    <div class="feature-desc">
      <slot name="desc">{{ description }}</slot>
    </div>
    <template #footer>
      <n-space>
        <slot name="actions">
          <n-button size="small" @click="onConfig">配置规则</n-button>
          <n-button size="small" type="primary" @click="onRun">立即运行</n-button>
        </slot>
      </n-space>
    </template>
  </n-card>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps({
  title: String,
  description: String,
  enabled: Boolean,
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'config', 'run'])

const switchValue = ref(props.modelValue)
watch(() => props.modelValue, newValue => switchValue.value = newValue)

const onToggle = (value: boolean) => {
  emit('update:modelValue', value)
}

const onConfig = () => {
  emit('config')
}

const onRun = () => {
  emit('run')
}
</script>

<style scoped>
.feature-card {
  width: 100%;
  margin: 0;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.feature-title {
  font-weight: bold;
  font-size: 1.1rem;
}
.feature-desc {
  margin: 12px 0 0 0;
  color: #666;
  min-height: 40px;
}
.switch-row {
  display: flex;
  align-items: center;
}
</style> 