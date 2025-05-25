<template>
  <div class="form-config">
    <n-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-placement="left"
      label-width="auto"
      require-mark-placement="right-hanging"
    >
      <n-form-item 
        v-for="field in fields" 
        :key="field.key"
        :label="field.label"
        :path="field.key"
      >
        <!-- 输入框 -->
        <n-input
          v-if="field.type === 'input'"
          v-model:value="formData[field.key]"
          :placeholder="field.placeholder"
          :disabled="field.disabled"
        >
          <template v-if="field.suffix" #suffix>
            <n-button 
              v-if="field.suffix.type === 'button'"
              text
              size="small"
              :type="field.suffix.buttonType || 'primary'"
              :loading="field.suffix.loading"
              @click="field.suffix.onClick?.(formData[field.key])"
            >
              {{ field.suffix.text }}
            </n-button>
          </template>
        </n-input>
        
        <!-- 数字输入框 -->
        <n-input-number
          v-else-if="field.type === 'number'"
          v-model:value="formData[field.key]"
          :min="field.min"
          :max="field.max"
          :step="field.step"
          :disabled="field.disabled"
        />
        
        <!-- 开关 -->
        <n-switch
          v-else-if="field.type === 'switch'"
          v-model:value="formData[field.key]"
          :disabled="field.disabled"
        />
        
        <!-- 选择器 -->
        <n-select
          v-else-if="field.type === 'select'"
          v-model:value="formData[field.key]"
          :options="field.options"
          :disabled="field.disabled"
          :placeholder="field.placeholder"
        />
        
        <!-- 多选框 -->
        <n-checkbox-group
          v-else-if="field.type === 'checkbox'"
          v-model:value="formData[field.key]"
          :disabled="field.disabled"
        >
          <n-space>
            <n-checkbox 
              v-for="option in field.options" 
              :key="option.value" 
              :value="option.value"
            >
              {{ option.label }}
            </n-checkbox>
          </n-space>
        </n-checkbox-group>
        
        <!-- 时间选择器 -->
        <n-time-picker
          v-else-if="field.type === 'time'"
          v-model:value="formData[field.key]"
          :disabled="field.disabled"
          :placeholder="field.placeholder"
        />
        
        <!-- 日期选择器 -->
        <n-date-picker
          v-else-if="field.type === 'date'"
          v-model:value="formData[field.key]"
          :disabled="field.disabled"
          :placeholder="field.placeholder"
        />
        
        <!-- 文本域 -->
        <n-input
          v-else-if="field.type === 'textarea'"
          v-model:value="formData[field.key]"
          type="textarea"
          :placeholder="field.placeholder"
          :disabled="field.disabled"
          :rows="field.rows || 3"
        />
      </n-form-item>
    </n-form>
    
    <!-- 配置说明 -->
    <div v-if="description" class="config-description">
      <n-alert type="info" :show-icon="false">
        <template #header>
          <n-icon :component="InformationCircleOutline" />
          配置说明
        </template>
        {{ description }}
      </n-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { InformationCircleOutline } from '@vicons/ionicons5'

interface FormField {
  key: string
  label: string
  type: 'input' | 'number' | 'switch' | 'select' | 'checkbox' | 'time' | 'date' | 'textarea'
  placeholder?: string
  disabled?: boolean
  min?: number
  max?: number
  step?: number
  rows?: number
  options?: Array<{ label: string; value: any }>
  required?: boolean
  validator?: (value: any) => boolean | string
  suffix?: {
    type: 'button'
    buttonType?: 'primary' | 'success' | 'warning' | 'error'
    loading?: boolean
    text: string
    onClick?: (value: any) => void
  }
}

interface Props {
  modelValue?: Record<string, any>
  fields: FormField[]
  description?: string
}

interface Emits {
  (e: 'update:modelValue', value: Record<string, any>): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formRef = ref()
const formData = ref<Record<string, any>>({})

// 表单验证规则
const rules = computed(() => {
  const result: Record<string, any> = {}
  
  props.fields.forEach(field => {
    if (field.required || field.validator) {
      result[field.key] = []
      
      if (field.required) {
        result[field.key].push({
          required: true,
          message: `请输入${field.label}`,
          trigger: ['blur', 'input']
        })
      }
      
      if (field.validator) {
        result[field.key].push({
          validator: (rule: any, value: any) => {
            const validationResult = field.validator!(value)
            if (typeof validationResult === 'string') {
              return new Error(validationResult)
            }
            return validationResult
          },
          trigger: ['blur', 'input']
        })
      }
    }
  })
  
  return result
})

// 初始化表单数据
const initFormData = () => {
  const newFormData: Record<string, any> = {}
  
  props.fields.forEach(field => {
    if (props.modelValue && field.key in props.modelValue) {
      newFormData[field.key] = props.modelValue[field.key]
    } else {
      // 设置默认值
      switch (field.type) {
        case 'switch':
          newFormData[field.key] = false
          break
        case 'number':
          newFormData[field.key] = field.min || 0
          break
        case 'checkbox':
          newFormData[field.key] = []
          break
        default:
          newFormData[field.key] = ''
      }
    }
  })
  
  formData.value = newFormData
}

// 监听表单数据变化
watch(formData, (newData) => {
  emit('update:modelValue', { ...newData })
}, { deep: true })

// 监听props变化
watch(() => props.modelValue, () => {
  initFormData()
}, { immediate: true })

watch(() => props.fields, () => {
  initFormData()
}, { immediate: true })

// 验证表单
const validate = () => {
  return formRef.value?.validate()
}

// 暴露验证方法
defineExpose({
  validate
})
</script>

<style scoped>
.form-config {
  padding: 20px 0;
}

.config-description {
  margin-top: 24px;
}

:deep(.n-form-item-label) {
  font-weight: 500;
}

:deep(.n-input-number) {
  width: 100%;
}
</style> 