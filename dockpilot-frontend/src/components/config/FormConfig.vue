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
        v-for="field in (fields || [])" 
        v-show="field && field.key"
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
import { ref, watch, computed, nextTick } from 'vue'
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
  console.log('🔧 FormConfig: 计算验证规则', props.fields)
  
  // 防御性检查
  if (!props.fields || !Array.isArray(props.fields) || props.fields.length === 0) {
    console.warn('⚠️  FormConfig: 字段为空，返回空验证规则')
    return {}
  }
  
  const result: Record<string, any> = {}
  
  try {
    props.fields.forEach(field => {
      if (!field || !field.key) {
        console.warn('⚠️  FormConfig: 跳过无效字段', field)
        return
      }
      
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
            validator: (_rule: any, value: any) => {
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
    
    console.log('✅ FormConfig: 验证规则计算完成', result)
    return result
  } catch (error) {
    console.error('❌ FormConfig: 计算验证规则时发生错误', error)
    return {}
  }
})

// 防止无限循环的标志
const isInternalUpdate = ref(false)

// 比较两个对象是否相等（浅比较）
const isEqual = (obj1: Record<string, any>, obj2: Record<string, any>) => {
  const keys1 = Object.keys(obj1)
  const keys2 = Object.keys(obj2)
  
  if (keys1.length !== keys2.length) {
    return false
  }
  
  for (const key of keys1) {
    if (obj1[key] !== obj2[key]) {
      return false
    }
  }
  
  return true
}

// 初始化表单数据
const initFormData = () => {
  console.log('🔧 FormConfig: 开始初始化表单数据', { fields: props.fields, modelValue: props.modelValue })
  
  // 防御性检查
  if (!props.fields || !Array.isArray(props.fields) || props.fields.length === 0) {
    console.warn('⚠️  FormConfig: fields为空，跳过初始化')
    return
  }
  
  const newFormData: Record<string, any> = {}
  
  try {
    props.fields.forEach(field => {
      if (!field || !field.key) {
        console.warn('⚠️  FormConfig: 发现无效字段', field)
        return
      }
      
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
    
    // 检查数据是否真的发生了变化
    if (!isEqual(formData.value, newFormData)) {
      isInternalUpdate.value = true
      formData.value = newFormData
      console.log('✅ FormConfig: 表单数据初始化完成', newFormData)
      
      // 延迟重置标志
      nextTick(() => {
        isInternalUpdate.value = false
      })
    } else {
      console.log('🔄 FormConfig: 数据未变化，跳过更新')
    }
  } catch (error) {
    console.error('❌ FormConfig: 初始化表单数据时发生错误', error)
    isInternalUpdate.value = false
  }
}

// 监听表单数据变化
watch(formData, (newData) => {
  // 如果是内部更新导致的变化，不触发emit
  if (isInternalUpdate.value) {
    console.log('🔄 FormConfig: 内部更新，跳过emit')
    return
  }
  
  console.log('📤 FormConfig: 发送数据更新', newData)
  emit('update:modelValue', { ...newData })
}, { deep: true })

// 监听props变化
watch(() => props.modelValue, (newValue, oldValue) => {
  // 避免无限循环：只有当外部真正改变了modelValue时才初始化
  if (!isEqual(newValue || {}, formData.value)) {
    console.log('📥 FormConfig: modelValue变化，重新初始化', { newValue, oldValue })
    initFormData()
  }
}, { immediate: true })

watch(() => props.fields, (newFields, oldFields) => {
  // 只有当fields真正变化时才重新初始化
  if (newFields !== oldFields) {
    console.log('🔧 FormConfig: fields变化，重新初始化')
    initFormData()
  }
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