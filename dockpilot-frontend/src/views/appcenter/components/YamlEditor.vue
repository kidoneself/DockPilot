<template>
  <div class="yaml-editor">
    <!-- 编辑器工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <n-space align="center">
          <n-tag v-if="isValidYaml" type="success" size="small">
            <template #icon><n-icon><CheckmarkCircleOutline /></n-icon></template>
            语法正确
          </n-tag>
          <n-tag v-else-if="yamlError" type="error" size="small">
            <template #icon><n-icon><CloseCircleOutline /></n-icon></template>
            语法错误
          </n-tag>
          <n-tag v-else type="default" size="small">
            <template #icon><n-icon><CodeOutline /></n-icon></template>
            YAML编辑器
          </n-tag>
        </n-space>
      </div>
      
      <div class="toolbar-right">
        <n-space>
          <n-button size="small" @click="formatYaml" :disabled="!isValidYaml">
            <template #icon><n-icon><CodeOutline /></n-icon></template>
            格式化
          </n-button>
          <n-button size="small" @click="resetToOriginal" v-if="originalYaml">
            <template #icon><n-icon><RefreshOutline /></n-icon></template>
            重置
          </n-button>
          <n-button size="small" @click="expandEditor" v-if="!isFullscreen">
            <template #icon><n-icon><ExpandOutline /></n-icon></template>
            全屏
          </n-button>
          <n-button size="small" @click="contractEditor" v-else>
            <template #icon><n-icon><ContractOutline /></n-icon></template>
            退出全屏
          </n-button>
        </n-space>
      </div>
    </div>
    
    <!-- Monaco编辑器容器 -->
    <div ref="editorContainer" class="editor-container" :class="{ 'fullscreen': isFullscreen }">
      <div ref="monacoEditor" class="monaco-wrapper"></div>
    </div>
    
    <!-- 错误提示 -->
    <div v-if="yamlError" class="error-message">
      <n-alert type="error" :show-icon="false">
        <template #header>
          <n-icon><CloseCircleOutline /></n-icon>
          YAML语法错误
        </template>
        <div class="error-detail">
          <p>{{ yamlError.message }}</p>
          <p v-if="yamlError.line" class="error-location">
            行 {{ yamlError.line }}{{ yamlError.column ? `，列 ${yamlError.column}` : '' }}
          </p>
        </div>
      </n-alert>
    </div>
    
    <!-- 快速帮助 -->
    <div class="yaml-help">
      <n-collapse>
        <n-collapse-item title="YAML编辑提示" name="help">
          <div class="help-content">
            <p><strong>快捷键：</strong></p>
            <ul>
              <li><code>Ctrl+/</code> - 注释/取消注释</li>
              <li><code>Ctrl+F</code> - 查找</li>
              <li><code>Ctrl+H</code> - 替换</li>
              <li><code>Ctrl+Z</code> - 撤销</li>
              <li><code>Ctrl+Y</code> - 重做</li>
            </ul>
            <p><strong>注意事项：</strong></p>
            <ul>
              <li>缩进使用空格，不要使用Tab</li>
              <li>注意冒号后面要有空格</li>
              <li>字符串包含特殊字符时请使用引号</li>
            </ul>
          </div>
        </n-collapse-item>
      </n-collapse>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import * as monaco from 'monaco-editor'
import {
  CheckmarkCircleOutline,
  CloseCircleOutline,
  CodeOutline,
  RefreshOutline,
  ExpandOutline,
  ContractOutline
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import yaml from 'js-yaml'

// 组件属性定义
interface Props {
  content: string
  originalYaml?: string
  readonly?: boolean
  height?: string
}

// 组件事件定义
interface Emits {
  'update:content': [content: string]
  change: [content: string]
  'syntax-error': [error: any]
  'syntax-valid': []
}

// 定义props和emits
const props = withDefaults(defineProps<Props>(), {
  content: '',
  originalYaml: '',
  readonly: false,
  height: '600px'
})

const emit = defineEmits<Emits>()

// 组件状态
const message = useMessage()
const editorContainer = ref<HTMLElement | null>(null)
const monacoEditor = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)
const yamlError = ref<any>(null)

// Monaco编辑器实例
let editor: monaco.editor.IStandaloneCodeEditor | null = null

// 计算属性
const isValidYaml = computed(() => {
  return !yamlError.value && props.content.trim() !== ''
})

// 本地内容状态
const localContent = computed({
  get: () => props.content,
  set: (value: string) => {
    emit('update:content', value)
    emit('change', value)
  }
})

// 验证YAML语法
const validateYaml = (content: string) => {
  try {
    if (content.trim()) {
      yaml.load(content)
    }
    yamlError.value = null
    emit('syntax-valid')
    return true
  } catch (error: any) {
    const yamlErr = {
      message: error.message || 'YAML语法错误',
      line: error.mark?.line ? error.mark.line + 1 : null,
      column: error.mark?.column ? error.mark.column + 1 : null
    }
    yamlError.value = yamlErr
    emit('syntax-error', yamlErr)
    return false
  }
}

// 格式化YAML
const formatYaml = () => {
  if (!editor || !isValidYaml.value) return
  
  try {
    const content = editor.getValue()
    const parsed = yaml.load(content)
    const formatted = yaml.dump(parsed, {
      indent: 2,
      lineWidth: 120,
      noRefs: true,
      sortKeys: false
    })
    
    editor.setValue(formatted)
    message.success('YAML格式化完成')
  } catch (error) {
    message.error('格式化失败，请检查YAML语法')
  }
}

// 重置为原始内容
const resetToOriginal = () => {
  if (!editor || !props.originalYaml) return
  
  editor.setValue(props.originalYaml)
  localContent.value = props.originalYaml
  message.info('已重置为原始配置')
}

// 全屏切换
const expandEditor = () => {
  isFullscreen.value = true
  nextTick(() => {
    if (editor) {
      editor.layout()
    }
  })
}

const contractEditor = () => {
  isFullscreen.value = false
  nextTick(() => {
    if (editor) {
      editor.layout()
    }
  })
}

// 处理ESC键退出全屏
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && isFullscreen.value) {
    contractEditor()
  }
}

// 初始化Monaco编辑器
const initEditor = () => {
  if (!monacoEditor.value) return

  // 配置YAML语言支持
  monaco.languages.register({ id: 'yaml' })
  
  // 设置YAML语法高亮
  monaco.languages.setMonarchTokensProvider('yaml', {
    tokenizer: {
      root: [
        [/^(\s*)([\w\-]+)(\s*)(:)/, ['white', 'key', 'white', 'delimiter']],
        [/^\s*-/, 'string'],
        [/".*?"/, 'string'],
        [/'.*?'/, 'string'],
        [/\d+/, 'number'],
        [/true|false/, 'keyword'],
        [/null/, 'keyword'],
        [/#.*$/, 'comment'],
      ]
    }
  })

  // 创建编辑器实例
  editor = monaco.editor.create(monacoEditor.value, {
    value: props.content,
    language: 'yaml',
    theme: 'vs-dark',
    automaticLayout: true,
    readOnly: props.readonly,
    fontSize: 13,
    lineNumbers: 'on',
    minimap: { enabled: true },
    scrollBeyondLastLine: false,
    wordWrap: 'on',
    tabSize: 2,
    insertSpaces: true,
    folding: true,
    lineDecorationsWidth: 10,
    lineNumbersMinChars: 3,
    renderWhitespace: 'boundary',
    // 🔧 禁用可能导致意外格式化的功能
    formatOnType: false,
    formatOnPaste: false,
    autoIndent: 'none',
    autoClosingBrackets: 'never',
    autoClosingQuotes: 'never',
    autoSurround: 'never',
    scrollbar: {
      vertical: 'auto',
      horizontal: 'auto',
      useShadows: false,
      verticalHasArrows: false,
      horizontalHasArrows: false,
      verticalScrollbarSize: 10,
      horizontalScrollbarSize: 10
    }
  })

  // 监听内容变化
  editor.onDidChangeModelContent(() => {
    if (editor) {
      const value = editor.getValue()
      localContent.value = value
      
      // 延迟验证，避免频繁验证
      setTimeout(() => {
        validateYaml(value)
      }, 300)
    }
  })

  // 🔧 完全禁用Ctrl+S - 阻止任何格式化行为
  editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => {
    // 只显示提示，不执行任何其他操作
    message.info('YAML内容已保存（无需手动保存）')
    // 返回false阻止任何默认行为
    return false
  })
  
  // 🔧 额外：监听键盘事件，确保Ctrl+S被完全拦截
  editor.onKeyDown((e) => {
    if ((e.ctrlKey || e.metaKey) && e.code === 'KeyS') {
      e.preventDefault()
      e.stopPropagation()
      message.info('YAML内容已保存（无需手动保存）')
      return false
    }
  })

  // 初始验证
  validateYaml(props.content)
}

// 监听内容变化
watch(() => props.content, (newContent) => {
  if (editor && editor.getValue() !== newContent) {
    editor.setValue(newContent)
  }
})

// 监听全屏状态变化
watch(isFullscreen, () => {
  if (editor) {
    setTimeout(() => {
      if (editor) {
        editor.layout()
      }
    }, 100)
  }
})

// 生命周期
onMounted(() => {
  nextTick(() => {
    initEditor()
  })
  
  // 添加键盘事件监听
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  // 销毁编辑器实例
  if (editor) {
    editor.dispose()
    editor = null
  }
  
  // 移除键盘事件监听
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.yaml-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--card-color-hover);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.toolbar-left {
  flex: 1;
}

.toolbar-right {
  flex-shrink: 0;
}

.editor-container {
  flex: 1;
  min-height: v-bind(height);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  background: #1e1e1e;
  position: relative;
  transition: all 0.3s ease;
}

.editor-container.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  min-height: 100vh;
  border-radius: 0;
  background: #1e1e1e;
}

.monaco-wrapper {
  width: 100%;
  height: 100%;
  min-height: inherit;
}

.error-message {
  margin-top: 8px;
}

.error-detail {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
}

.error-detail p {
  margin: 4px 0;
}

.error-location {
  color: var(--text-color-3);
  font-size: 11px;
}

.yaml-help {
  margin-top: 16px;
}

.help-content {
  font-size: 12px;
  line-height: 1.5;
}

.help-content ul {
  margin: 8px 0;
  padding-left: 20px;
}

.help-content li {
  margin: 4px 0;
}

.help-content code {
  background: var(--code-color);
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 11px;
}

/* 深色模式增强 */
[data-theme="dark"] .editor-toolbar {
  background: rgba(255, 255, 255, 0.02);
  border-color: rgba(255, 255, 255, 0.06);
}

[data-theme="dark"] .editor-container {
  border-color: rgba(255, 255, 255, 0.06);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .editor-toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .toolbar-left,
  .toolbar-right {
    justify-content: center;
  }
  
  .editor-container {
    min-height: 300px;
  }
}

@media (max-width: 640px) {
  .yaml-editor {
    gap: 8px;
  }
  
  .editor-toolbar {
    padding: 8px 12px;
  }
  
  .editor-container {
    min-height: 250px;
  }
}
</style> 