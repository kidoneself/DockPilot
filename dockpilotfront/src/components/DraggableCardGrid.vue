<template>
  <div class="draggable-grid">
    <draggable
      v-model="innerList"
      item-key="id"
      class="grid-container"
      @end="onEnd"
    >
      <template #item="{ element }">
        <div class="grid-item">
          <AppCard
            :name="element.name"
            :description="element.description"
            :icon="element.icon"
            :icon-url="element.iconUrl"
            :bg-color="element.bgColor"
            :text-color="element.textColor"
            @click="onCardClick(element)"
          />
        </div>
      </template>
    </draggable>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import draggable from 'vuedraggable'
import AppCard from '@/components/AppCard.vue'

const props = defineProps<{
  list: any[],
}>()
const emit = defineEmits(['update:list', 'card-click'])

const innerList = ref([...props.list])

watch(() => props.list, (newList) => {
  innerList.value = [...newList]
})

const onEnd = () => {
  emit('update:list', innerList.value)
}

const onCardClick = (item: any) => {
  emit('card-click', item)
}
</script>

<style scoped>
.draggable-grid {
  width: 100%;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  padding: 16px;
}

.grid-item {
  width: 100%;
}
</style> 