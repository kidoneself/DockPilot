<template>
  <div class="category-manage-config">
    <!-- 顶部操作栏 -->
    <div class="header-actions">
      <n-button type="primary" @click="handleAddCategory">
        <template #icon>
          <n-icon><AddOutline /></n-icon>
        </template>
        添加分类
      </n-button>
      <n-text depth="3" style="margin-left: 12px;">
        共 {{ categories.length }} 个分类
      </n-text>
    </div>

    <!-- 分类列表 -->
    <div v-if="categories.length > 0" class="category-list">
      <draggable
        v-model="categories"
        group="categories"
        item-key="id"
        :animation="200"
        class="drag-container"
        @end="handleDragEnd"
      >
        <template #item="{ element: category, index }">
          <div class="category-item">
            <div class="drag-handle">
              <n-icon><ReorderThreeOutline /></n-icon>
            </div>
            
            <div class="category-info">
              <div class="category-name">
                {{ category.name }}
              </div>
              <div class="category-meta">
                <n-text depth="3">
                  {{ category.appCount }} 个应用 • 排序: {{ category.sortOrder }}
                </n-text>
              </div>
            </div>

            <div class="category-actions">
              <n-button 
                size="small" 
                tertiary 
                @click="() => handleEditCategory(category)"
              >
                <template #icon>
                  <n-icon><CreateOutline /></n-icon>
                </template>
                编辑
              </n-button>
              
              <n-button 
                size="small" 
                tertiary 
                type="error"
                @click="() => handleDeleteCategory(category)"
              >
                <template #icon>
                  <n-icon><TrashOutline /></n-icon>
                </template>
                删除
              </n-button>
            </div>
          </div>
        </template>
      </draggable>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <n-empty description="暂无分类">
        <template #extra>
          <n-button size="small" @click="handleAddCategory">创建第一个分类</n-button>
        </template>
      </n-empty>
    </div>

    <!-- 添加/编辑分类模态框 -->
    <n-modal 
      v-model:show="showEditModal" 
      :mask-closable="false"
      preset="dialog"
      :title="editingCategory?.id ? '编辑分类' : '添加分类'"
      positive-text="确定"
      negative-text="取消"
      @positive-click="handleSaveCategory"
      @negative-click="handleCancelEdit"
    >
      <n-form 
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="80px"
        class="category-form"
      >
        <n-form-item label="分类名称" path="name">
          <n-input 
            v-model:value="formData.name" 
            placeholder="请输入分类名称"
            maxlength="50"
            show-count
            clearable
          />
        </n-form-item>
        
        <n-form-item label="排序权重" path="sortOrder">
          <n-input-number 
            v-model:value="formData.sortOrder" 
            placeholder="数字越小越靠前"
            :min="1"
            :max="999"
            style="width: 100%"
          />
        </n-form-item>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { useMessage, useDialog, type FormInst, type FormRules } from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline, ReorderThreeOutline } from '@vicons/ionicons5'
import draggable from 'vuedraggable'
import { 
  getAllCategoriesForManage, 
  createCategory, 
  updateCategory, 
  deleteCategory,
  batchUpdateCategorySort,
  type CategoryVO, 
  type CategoryDTO 
} from '@/api/http/category'

// 组件Props
interface Props {
  modelValue?: any
}

// 组件Emits
interface Emits {
  (e: 'update:model-value', value: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const message = useMessage()
const dialog = useDialog()

// 状态管理
const categories = ref<CategoryVO[]>([])
const loading = ref(false)
const showEditModal = ref(false)
const editingCategory = ref<CategoryVO | null>(null)
const formRef = ref<FormInst>()

// 表单数据
const formData = reactive<CategoryDTO>({
  name: '',
  sortOrder: 1
})

// 表单验证规则
const formRules: FormRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 50, message: '分类名称长度应在1-50个字符之间', trigger: 'blur' },
    { 
      validator: (rule, value: string) => {
        if (!value) return true
        
        // 检查是否与现有分类重名（编辑时排除自己）
        const existingCategory = categories.value.find(cat => 
          cat.name === value && cat.id !== editingCategory.value?.id
        )
        return !existingCategory
      },
      message: '分类名称已存在', 
      trigger: 'blur' 
    }
  ],
  sortOrder: [
    { required: true, type: 'number', message: '请输入排序权重', trigger: 'blur' },
    { type: 'number', min: 1, max: 999, message: '排序权重应在1-999之间', trigger: 'blur' }
  ]
}

// 加载分类列表
const loadCategories = async () => {
  try {
    loading.value = true
    const data = await getAllCategoriesForManage()
    categories.value = data.sort((a, b) => a.sortOrder - b.sortOrder)
    console.log('✅ 分类列表加载成功:', categories.value.length, '(包括空分类)')
  } catch (error) {
    console.error('❌ 加载分类列表失败:', error)
    message.error('加载分类列表失败')
  } finally {
    loading.value = false
  }
}

// 处理添加分类
const handleAddCategory = () => {
  editingCategory.value = null
  formData.name = ''
  formData.sortOrder = getNextSortOrder()
  showEditModal.value = true
}

// 处理编辑分类
const handleEditCategory = (category: CategoryVO) => {
  editingCategory.value = category
  formData.name = category.name
  formData.sortOrder = category.sortOrder
  showEditModal.value = true
}

// 处理保存分类
const handleSaveCategory = async (e?: MouseEvent) => {
  // 安全处理事件对象，如果存在则阻止默认行为
  if (e && e.preventDefault) {
    e.preventDefault()
  }
  
  try {
    await formRef.value?.validate()
    
    loading.value = true
    
    if (editingCategory.value?.id) {
      // 更新分类
      await updateCategory(editingCategory.value.id, formData)
      message.success('分类更新成功')
      console.log('✅ 分类更新成功:', formData.name)
    } else {
      // 创建分类
      await createCategory(formData)
      message.success('分类创建成功')
      console.log('✅ 分类创建成功:', formData.name)
    }
    
    showEditModal.value = false
    await loadCategories()
    
  } catch (error) {
    console.error('❌ 保存分类失败:', error)
    const errorMessage = error instanceof Error ? error.message : String(error)
    if (errorMessage.includes('已存在')) {
      message.error('分类名称已存在')
    } else {
      message.error('保存分类失败')
    }
  } finally {
    loading.value = false
  }
  
  return false // 阻止模态框自动关闭
}

// 处理取消编辑
const handleCancelEdit = () => {
  showEditModal.value = false
  editingCategory.value = null
}

// 处理删除分类
const handleDeleteCategory = (category: CategoryVO) => {
  const hasApps = category.appCount > 0
  
  dialog.warning({
    title: '确认删除',
    content: hasApps 
      ? `分类"${category.name}"下有 ${category.appCount} 个应用，删除分类后这些应用将移动到"未分类"。确定要删除吗？`
      : `确定要删除分类"${category.name}"吗？此操作无法撤销。`,
    positiveText: '确定删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteCategory(category.id)
        
        if (hasApps) {
          message.success(`分类"${category.name}"已删除，${category.appCount} 个应用已移动到未分类`)
        } else {
          message.success('分类删除成功')
        }
        
        console.log('✅ 分类删除成功:', category.name, hasApps ? `(包含${category.appCount}个应用)` : '(空分类)')
        await loadCategories()
      } catch (error) {
        console.error('❌ 删除分类失败:', error)
        const errorMessage = error instanceof Error ? error.message : String(error)
        if (errorMessage.includes('外键约束') || errorMessage.includes('关联')) {
          message.error('删除失败：该分类下还有关联的应用，请先清空分类或联系管理员')
        } else {
          message.error('删除分类失败')
        }
      }
    }
  })
}

// 处理拖拽排序结束
const handleDragEnd = async () => {
  try {
    // 更新排序权重
    const updateData = categories.value.map((category, index) => ({
      id: category.id,
      name: category.name,
      sortOrder: (index + 1) * 10 // 间隔10，便于插入新项
    }))
    
    await batchUpdateCategorySort(updateData)
    message.success('分类排序已更新')
    console.log('✅ 分类排序更新成功')
    
    // 重新加载以获取最新状态
    await loadCategories()
    
  } catch (error) {
    console.error('❌ 更新分类排序失败:', error)
    message.error('更新排序失败')
    // 重新加载恢复原状态
    await loadCategories()
  }
}

// 获取下一个排序值
const getNextSortOrder = () => {
  if (categories.value.length === 0) return 10
  const maxSort = Math.max(...categories.value.map(c => c.sortOrder || 0))
  return maxSort + 10
}

// 组件挂载时加载数据
onMounted(() => {
  loadCategories()
})

// 监听数据变化，通知父组件
defineExpose({
  refresh: loadCategories
})
</script>

<style scoped>
.category-manage-config {
  padding: 0;
  min-height: 400px;
}

.header-actions {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.category-list {
  max-height: 500px;
  overflow-y: auto;
}

.drag-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.category-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: var(--card-color);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  transition: all 0.2s ease;
  cursor: default;
}

.category-item:hover {
  border-color: var(--primary-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.drag-handle {
  display: flex;
  align-items: center;
  margin-right: 12px;
  color: var(--text-color-3);
  cursor: grab;
  padding: 4px;
}

.drag-handle:active {
  cursor: grabbing;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-1);
  margin-bottom: 4px;
}

.category-meta {
  font-size: 12px;
  color: var(--text-color-3);
}

.category-actions {
  display: flex;
  gap: 8px;
  margin-left: 12px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.category-form {
  margin-top: 16px;
}

/* 拖拽时的样式 */
.sortable-ghost {
  opacity: 0.5;
}

.sortable-chosen {
  transform: scale(1.02);
}

/* 响应式样式 */
@media (max-width: 768px) {
  .category-item {
    padding: 8px;
  }
  
  .category-actions {
    flex-direction: column;
    gap: 4px;
  }
  
  .category-actions .n-button {
    font-size: 12px;
  }
}
</style> 