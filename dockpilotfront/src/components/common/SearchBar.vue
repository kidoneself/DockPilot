<template>
  <NInputGroup>
    <NInput
      v-model:value="inputValue"
      :placeholder="placeholder"
      clearable
      @keyup.enter="onSearch"
    />
    <NButton type="primary" @click="onSearch">
      <template #icon>
        <NIcon><SearchOutline /></NIcon>
      </template>
    </NButton>
  </NInputGroup>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { SearchOutline } from '@vicons/ionicons5'
import { NInput, NInputGroup, NButton, NIcon } from 'naive-ui'

const props = defineProps<{
  modelValue: string
  placeholder?: string
}>()
const emit = defineEmits(['update:modelValue', 'search'])

const inputValue = ref(props.modelValue)
watch(() => props.modelValue, v => inputValue.value = v)
watch(inputValue, v => emit('update:modelValue', v))

function onSearch() {
  emit('search', inputValue.value)
}
</script> 