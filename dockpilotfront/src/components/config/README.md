# 通用配置模态框系统

这是一个灵活、可扩展的配置模态框系统，支持多种配置类型和复用的组件架构。

## 🏗️ 架构概览

```
ConfigModal (通用模态框)
├── 插槽内容 (slot="content")
│   ├── BackgroundConfig (背景配置组件)
│   ├── FormConfig (通用表单配置组件)
│   └── 自定义配置组件...
└── 统一的确认/取消按钮
```

## 📋 组件介绍

### 1. ConfigModal - 通用配置模态框

**功能特点:**
- 统一的外观和交互
- 支持插槽传入任意内容
- 支持动态组件加载
- 内置确认前验证和确认后回调
- 响应式设计

**基本用法:**
```vue
<ConfigModal
  v-model:show="showModal"
  v-model:model-value="configData"
  :config="modalConfig"
  @confirm="handleConfirm"
  @cancel="handleCancel"
>
  <template #content="{ data, update }">
    <!-- 在这里放入具体的配置组件 -->
    <YourConfigComponent
      :model-value="data"
      @update:model-value="update"
    />
  </template>
</ConfigModal>
```

**配置对象 (ConfigModalConfig):**
```typescript
interface ConfigModalConfig {
  title: string;                          // 模态框标题
  width?: string;                         // 模态框宽度 (默认: '600px')
  component?: any;                        // 动态组件 (可选)
  componentProps?: Record<string, any>;   // 组件属性
  cancelText?: string;                    // 取消按钮文字 (默认: '取消')
  confirmText?: string;                   // 确认按钮文字 (默认: '确定')
  beforeConfirm?: (data: any) => boolean | Promise<boolean>; // 确认前验证
  afterConfirm?: (data: any) => void | Promise<void>;       // 确认后回调
}
```

### 2. BackgroundConfig - 背景配置组件

**专门用于背景图片配置的组件:**
- 当前背景预览
- 预设渐变背景选择
- 自定义背景上传和管理
- 支持删除自定义背景
- 实时预览功能

**使用示例:**
```vue
<ConfigModal
  v-model:show="showModal"
  v-model:model-value="backgroundUrl"
  :config="{
    title: '🎨 背景图片配置',
    width: '700px',
    confirmText: '应用背景'
  }"
>
  <template #content="{ data, update }">
    <BackgroundConfig
      :model-value="data"
      @update:model-value="update"
    />
  </template>
</ConfigModal>
```

### 3. FormConfig - 通用表单配置组件

**动态表单组件，支持多种字段类型:**
- 输入框 (input)
- 数字输入框 (number)
- 开关 (switch)
- 选择器 (select)
- 多选框 (checkbox)
- 时间选择器 (time)
- 日期选择器 (date)
- 文本域 (textarea)

**字段配置接口:**
```typescript
interface FormField {
  key: string;                    // 字段键名
  label: string;                  // 字段标签
  type: 'input' | 'number' | 'switch' | 'select' | 'checkbox' | 'time' | 'date' | 'textarea';
  placeholder?: string;           // 占位符
  disabled?: boolean;             // 是否禁用
  min?: number;                   // 最小值 (number类型)
  max?: number;                   // 最大值 (number类型)
  step?: number;                  // 步长 (number类型)
  rows?: number;                  // 行数 (textarea类型)
  options?: Array<{               // 选项 (select/checkbox类型)
    label: string;
    value: any;
  }>;
  required?: boolean;             // 是否必填
  validator?: (value: any) => boolean | string; // 自定义验证
}
```

**使用示例:**
```vue
<script setup>
const fields = [
  {
    key: 'name',
    label: '名称',
    type: 'input',
    placeholder: '请输入名称',
    required: true
  },
  {
    key: 'enabled',
    label: '启用',
    type: 'switch'
  },
  {
    key: 'type',
    label: '类型',
    type: 'select',
    options: [
      { label: '类型A', value: 'typeA' },
      { label: '类型B', value: 'typeB' }
    ]
  }
]
</script>

<template>
  <FormConfig
    v-model="formData"
    :fields="fields"
    description="这是一个示例表单配置"
  />
</template>
```

## 🎯 使用场景示例

### 1. 背景配置

```vue
<script setup>
const backgroundConfig = {
  title: '🎨 背景图片配置',
  width: '700px',
  confirmText: '应用背景',
  afterConfirm: (backgroundUrl) => {
    applyBackground(backgroundUrl)
  }
}
</script>
```

### 2. ALIST同步配置

```vue
<script setup>
const alistFields = [
  {
    key: 'interval',
    label: '同步间隔',
    type: 'number',
    min: 1,
    max: 86400,
    placeholder: '秒',
    required: true
  },
  {
    key: 'path',
    label: '目标路径',
    type: 'input',
    placeholder: '请输入目标路径',
    required: true
  },
  {
    key: 'enableSign',
    label: '启用签名验证',
    type: 'switch'
  }
]

const alistConfig = {
  title: '⚙️ ALIST同步配置',
  width: '600px',
  confirmText: '保存配置'
}
</script>
```

### 3. 危险操作配置

```vue
<script setup>
const dangerousFields = [
  {
    key: 'confirmText',
    label: '确认文本',
    type: 'input',
    placeholder: '请输入"我确认删除"',
    required: true,
    validator: (value) => {
      if (value !== '我确认删除') {
        return '请输入正确的确认文本'
      }
      return true
    }
  }
]

const dangerousConfig = {
  title: '⚠️ 危险操作确认',
  width: '500px',
  confirmText: '执行操作',
  beforeConfirm: (data) => {
    if (data.confirmText !== '我确认删除') {
      message.error('请输入正确的确认文本')
      return false
    }
    return true
  }
}
</script>
```

## 🚀 扩展新的配置类型

### 1. 创建专用配置组件

```vue
<!-- MyCustomConfig.vue -->
<template>
  <div class="my-custom-config">
    <!-- 你的自定义配置界面 -->
    <n-form>
      <n-form-item label="自定义设置">
        <n-input v-model:value="localData.customValue" />
      </n-form-item>
    </n-form>
  </div>
</template>

<script setup lang="ts">
interface Props {
  modelValue?: any
}

interface Emits {
  (e: 'update:modelValue', value: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const localData = ref(props.modelValue || {})

watch(localData, (newData) => {
  emit('update:modelValue', newData)
}, { deep: true })
</script>
```

### 2. 在主组件中使用

```vue
<template>
  <ConfigModal
    v-model:show="showModal"
    v-model:model-value="configData"
    :config="customConfig"
  >
    <template #content="{ data, update }">
      <MyCustomConfig
        v-if="configType === 'custom'"
        :model-value="data"
        @update:model-value="update"
      />
    </template>
  </ConfigModal>
</template>
```

## 💡 最佳实践

1. **配置类型分类**: 使用`configType`来区分不同的配置类型
2. **数据验证**: 利用`beforeConfirm`进行数据验证
3. **错误处理**: 在验证失败时返回`false`并显示错误信息
4. **组件复用**: 使用`FormConfig`来快速创建表单类配置
5. **响应式设计**: 确保配置组件在不同屏幕尺寸下都能正常使用
6. **类型安全**: 使用TypeScript定义清晰的接口和类型

## 🔧 高级功能

### 动态组件加载

```vue
<ConfigModal
  :config="{
    title: '动态配置',
    component: resolveComponent('DynamicConfigComponent'),
    componentProps: { someProps: 'value' }
  }"
/>
```

### 异步验证

```vue
const config = {
  beforeConfirm: async (data) => {
    try {
      await validateConfigOnServer(data)
      return true
    } catch (error) {
      message.error('配置验证失败')
      return false
    }
  }
}
```

这个系统提供了强大的扩展性和灵活性，可以轻松适应各种配置需求。 