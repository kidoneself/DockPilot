<template>
  <div v-if="errorMessage">
    <NAlert
type="error"
:title="errorMessage"
closable
@close="errorMessage = ''" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { NAlert } from 'naive-ui'

const errorMessage = ref('')

const handleWebSocketError = (event: CustomEvent) => {
  errorMessage.value = event.detail.message
}

onMounted(() => {
  window.addEventListener('websocket-error', handleWebSocketError as EventListener)
})

onUnmounted(() => {
  window.removeEventListener('websocket-error', handleWebSocketError as EventListener)
})
</script> 