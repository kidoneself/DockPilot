<template>
  <NFloatButton
    v-if="showButton"
    type="primary"
    :right="20"
    :bottom="20"
    @click="scrollToTop"
  >
    <NIcon>
      <ArrowUpOutline />
    </NIcon>
  </NFloatButton>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { NFloatButton, NIcon } from 'naive-ui'
import { ArrowUpOutline } from '@vicons/ionicons5'

const showButton = ref(false)

const handleScroll = () => {
  // 当页面垂直滚动超过 200px 时显示按钮
  showButton.value = window.scrollY > 200
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
/* 可以在这里添加一些额外的样式，比如过渡效果 */
</style> 