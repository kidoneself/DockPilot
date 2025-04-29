<template>
  <div class="xml-editor-container">
    <t-card>
      <template #header>
        <div class="card-header">
          <span>XML编辑器</span>
          <div class="header-actions">
            <t-button theme="primary" @click="handleImport">
              <template #icon><t-icon name="upload" /></template>
              导入XML
            </t-button>
            <t-button theme="primary" @click="handleExport">
              <template #icon><t-icon name="download" /></template>
              导出XML
            </t-button>
          </div>
        </div>
      </template>
      
      <div class="editor-content">
        <t-textarea
          v-model="xmlContent"
          placeholder="请输入XML内容"
          :autosize="{ minRows: 10, maxRows: 20 }"
          class="xml-textarea"
        />
        
        <div class="tree-container" v-if="treeData.length">
          <div class="tree-header">
            <span>节点操作</span>
            <div class="tree-actions" v-if="selectedNode">
              <t-button theme="primary" size="small" @click="handleAddChild">
                <template #icon><t-icon name="add" /></template>
                添加子节点
              </t-button>
              <t-button theme="primary" size="small" @click="handleEdit">
                <template #icon><t-icon name="edit" /></template>
                编辑节点
              </t-button>
              <t-button theme="primary" size="small" @click="handleCopy">
                <template #icon><t-icon name="copy" /></template>
                复制节点
              </t-button>
              <t-button theme="danger" size="small" @click="handleDelete">
                <template #icon><t-icon name="delete" /></template>
                删除节点
              </t-button>
            </div>
          </div>
          <div class="tree-content">
            <t-tree
              :data="treeData"
              :keys="{
                value: 'id',
                label: 'label',
                children: 'children'
              }"
              hover
              activable
              :default-expand-all="false"
              draggable
              @dragend="handleDragEnd"
              @click="handleNodeClick"
            />
          </div>
        </div>
      </div>
    </t-card>

    <!-- 编辑节点对话框 -->
    <t-dialog
      v-model:visible="editDialogVisible"
      :header="editDialogTitle"
      :on-confirm="handleEditConfirm"
      :on-close="handleEditClose"
    >
      <t-form :data="editFormData" ref="editForm">
        <t-form-item label="节点名称" name="name">
          <t-input v-model="editFormData.name" placeholder="请输入节点名称" />
        </t-form-item>
        <t-form-item label="节点值" name="value">
          <t-input v-model="editFormData.value" placeholder="请输入节点值" />
        </t-form-item>
        <t-form-item label="属性" name="attributes">
          <div v-for="(value, key) in editFormData.attributes" :key="key" class="attribute-item">
            <t-input v-model="editFormData.attributes[key]" :placeholder="key" />
            <t-button theme="danger" @click="removeAttribute(key)">删除</t-button>
          </div>
          <t-button theme="primary" @click="addAttribute">添加属性</t-button>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue';
import { MessagePlugin, TreeNodeModel, TreeOptionData } from 'tdesign-vue-next';

interface TreeNode {
  id: string;
  name: string;
  label: string;
  children?: TreeNode[];
  attributes?: Record<string, string>;
  value?: string;
}

interface EditFormData {
  name: string;
  value: string;
  attributes: Record<string, string>;
}

const xmlContent = ref('');
const treeData = ref<TreeNode[]>([]);
const selectedNode = ref<TreeNode | null>(null);
const editDialogVisible = ref(false);
const editDialogTitle = ref('');
const editFormData = reactive<EditFormData>({
  name: '',
  value: '',
  attributes: {}
});

// 解析XML为树形结构
const parseXML = (xmlString: string): TreeNode[] => {
  try {
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlString, 'text/xml');
    return parseNode(xmlDoc.documentElement);
  } catch (error) {
    MessagePlugin.error('XML解析错误');
    return [];
  }
};

// 递归解析XML节点
const parseNode = (node: Element): TreeNode[] => {
  const result: TreeNode[] = [];
  
  // 处理当前节点
  const currentNode: TreeNode = {
    id: Math.random().toString(36).substr(2, 9),
    name: node.nodeName,
    label: node.nodeName,
    children: [],
    attributes: {},
    value: node.textContent?.trim() || undefined
  };

  // 处理属性
  Array.from(node.attributes).forEach(attr => {
    if (currentNode.attributes) {
      currentNode.attributes[attr.name] = attr.value;
    }
  });

  // 处理子节点
  Array.from(node.children).forEach(child => {
    const childNodes = parseNode(child);
    if (currentNode.children) {
      currentNode.children.push(...childNodes);
    }
  });

  // 如果节点有值且没有子节点，则显示值
  if (currentNode.value && (!currentNode.children || currentNode.children.length === 0)) {
    currentNode.label = `${currentNode.name}: ${currentNode.value}`;
  }

  result.push(currentNode);
  return result;
};

// 处理节点点击
const handleNodeClick = (context: { node: TreeNodeModel<TreeOptionData>; e: MouseEvent; }) => {
  const nodeData = context.node as unknown as TreeNode;
  selectedNode.value = nodeData;
  console.log('Selected node:', nodeData); // 添加日志以便调试
};

// 处理添加子节点
const handleAddChild = () => {
  if (!selectedNode.value) return;
  console.log('Adding child to node:', selectedNode.value); // 添加日志以便调试
  editDialogTitle.value = '添加子节点';
  editFormData.name = '';
  editFormData.value = '';
  editFormData.attributes = {};
  editDialogVisible.value = true;
};

// 处理编辑节点
const handleEdit = () => {
  if (!selectedNode.value) return;
  console.log('Editing node:', selectedNode.value); // 添加日志以便调试
  editDialogTitle.value = '编辑节点';
  // 重置表单数据
  // 从label中提取名称和值
  const labelParts = selectedNode.value.label.split(': ');
  editFormData.name = labelParts[0]; // 获取节点名称
  editFormData.value = labelParts.length > 1 ? labelParts[1] : ''; // 获取节点值
  editFormData.attributes = selectedNode.value.attributes ? { ...selectedNode.value.attributes } : {};
  editDialogVisible.value = true;
};

// 处理复制节点
const handleCopy = () => {
  if (!selectedNode.value) return;
  console.log('Copying node:', selectedNode.value); // 添加日志以便调试
  const copyNode = (node: TreeNode): TreeNode => {
    return {
      id: Math.random().toString(36).substr(2, 9),
      name: node.name,
      label: node.label,
      value: node.value,
      attributes: node.attributes ? { ...node.attributes } : undefined,
      children: node.children ? node.children.map(child => copyNode(child)) : undefined
    };
  };

  const newNode = copyNode(selectedNode.value);
  if (selectedNode.value.children) {
    selectedNode.value.children.push(newNode);
  } else {
    selectedNode.value.children = [newNode];
  }
  updateXMLContent();
  MessagePlugin.success('节点复制成功');
};

// 处理删除节点
const handleDelete = () => {
  if (!selectedNode.value) return;
  console.log('Deleting node:', selectedNode.value); // 添加日志以便调试
  deleteNode(selectedNode.value);
  selectedNode.value = null;
};

// 更新节点数据
const updateNodeData = (node: TreeNode, newData: Partial<TreeNode>) => {
  // 创建一个新的节点对象
  const updatedNode: TreeNode = {
    ...node,
    ...newData
  };
  
  // 在树数据中查找并替换节点
  const updateNodeInTree = (nodes: TreeNode[]): boolean => {
    for (let i = 0; i < nodes.length; i++) {
      if (nodes[i].id === node.id) {
        nodes[i] = updatedNode;
        return true;
      }
      if (nodes[i].children) {
        if (updateNodeInTree(nodes[i].children)) {
          return true;
        }
      }
    }
    return false;
  };

  updateNodeInTree(treeData.value);
  return updatedNode;
};

// 处理编辑确认
const handleEditConfirm = () => {
  if (!selectedNode.value) return;
  console.log('Confirming edit for node:', selectedNode.value);

  if (editDialogTitle.value === '添加子节点') {
    const newNode: TreeNode = {
      id: Math.random().toString(36).substr(2, 9),
      name: editFormData.name,
      label: editFormData.value ? `${editFormData.name}: ${editFormData.value}` : editFormData.name,
      value: editFormData.value,
      attributes: { ...editFormData.attributes },
      children: []
    };

    if (!selectedNode.value.children) {
      selectedNode.value.children = [];
    }
    selectedNode.value.children.push(newNode);
  } else {
    // 更新节点属性
    const newLabel = editFormData.value ? `${editFormData.name}: ${editFormData.value}` : editFormData.name;
    const updatedNode = updateNodeData(selectedNode.value, {
      name: editFormData.name,
      label: newLabel,
      value: editFormData.value,
      attributes: { ...editFormData.attributes }
    });
    selectedNode.value = updatedNode;
  }

  // 先更新XML内容
  updateXMLContent();
  // 然后关闭对话框
  editDialogVisible.value = false;
  // 最后显示成功消息
  MessagePlugin.success('操作成功');
};

// 处理编辑关闭
const handleEditClose = () => {
  editDialogVisible.value = false;
  // 重置表单数据
  editFormData.name = '';
  editFormData.value = '';
  editFormData.attributes = {};
};

// 添加属性
const addAttribute = () => {
  const key = prompt('请输入属性名');
  if (key) {
    editFormData.attributes[key] = '';
  }
};

// 删除属性
const removeAttribute = (key: string) => {
  delete editFormData.attributes[key];
};

// 删除节点
const deleteNode = (node: TreeNode) => {
  const deleteFromParent = (parent: TreeNode[], target: TreeNode) => {
    const index = parent.findIndex(n => n.id === target.id);
    if (index !== -1) {
      parent.splice(index, 1);
      return true;
    }
    for (const child of parent) {
      if (child.children && deleteFromParent(child.children, target)) {
        return true;
      }
    }
    return false;
  };

  if (deleteFromParent(treeData.value, node)) {
    updateXMLContent();
    MessagePlugin.success('删除成功');
  }
};

// 更新XML内容
const updateXMLContent = () => {
  const buildXML = (node: TreeNode): string => {
    let xml = `<${node.name}`;
    
    // 添加属性
    if (node.attributes) {
      for (const [key, value] of Object.entries(node.attributes)) {
        xml += ` ${key}="${value}"`;
      }
    }
    
    if (node.children && node.children.length > 0) {
      xml += '>';
      for (const child of node.children) {
        xml += buildXML(child);
      }
      xml += `</${node.name}>`;
    } else if (node.value) {
      xml += `>${node.value}</${node.name}>`;
    } else {
      xml += '/>';
    }
    
    return xml;
  };

  // 更新XML内容
  const newXmlContent = treeData.value.map(node => buildXML(node)).join('\n');
  xmlContent.value = newXmlContent;
};

// 处理拖拽结束
const handleDragEnd = (context: { 
  e: DragEvent; 
  dragNode: TreeNodeModel<TreeOptionData>; 
  dropNode: TreeNodeModel<TreeOptionData>; 
  dropPosition: number; 
}) => {
  const { dragNode, dropNode, dropPosition } = context;
  
  // 从原位置删除节点
  const removeFromParent = (parent: TreeNode[], target: TreeNode) => {
    const index = parent.findIndex(n => n.id === target.id);
    if (index !== -1) {
      parent.splice(index, 1);
      return true;
    }
    for (const child of parent) {
      if (child.children && removeFromParent(child.children, target)) {
        return true;
      }
    }
    return false;
  };

  // 添加到新位置
  const addToNewPosition = (parent: TreeNode[], target: TreeNode, position: number) => {
    if (!parent) return;
    const index = parent.findIndex(n => n.id === target.id);
    if (index !== -1) {
      parent.splice(index + position, 0, dragNode as unknown as TreeNode);
      return true;
    }
    for (const child of parent) {
      if (child.children && addToNewPosition(child.children, target, position)) {
        return true;
      }
    }
    return false;
  };

  // 从原位置删除
  removeFromParent(treeData.value, dragNode as unknown as TreeNode);

  // 添加到新位置
  if (dropPosition === 0) {
    // 作为子节点
    const dropNodeData = dropNode as unknown as TreeNode;
    if (!dropNodeData.children) {
      dropNodeData.children = [];
    }
    dropNodeData.children.push(dragNode as unknown as TreeNode);
  } else {
    // 作为兄弟节点
    addToNewPosition(treeData.value, dropNode as unknown as TreeNode, dropPosition);
  }

  // 更新XML内容
  updateXMLContent();
  MessagePlugin.success('节点移动成功');
};

// 处理导入
const handleImport = () => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '.xml';
  input.onchange = (e) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        xmlContent.value = event.target?.result as string;
        MessagePlugin.success('XML导入成功');
      };
      reader.readAsText(file);
    }
  };
  input.click();
};

// 处理导出
const handleExport = () => {
  if (!xmlContent.value) {
    MessagePlugin.warning('请先输入XML内容');
    return;
  }
  
  const blob = new Blob([xmlContent.value], { type: 'text/xml' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'export.xml';
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(url);
  document.body.removeChild(a);
  MessagePlugin.success('XML导出成功');
};

// 监听XML内容变化，自动更新树形结构
watch(xmlContent, (newValue) => {
  if (newValue) {
    try {
      treeData.value = parseXML(newValue);
    } catch (error) {
      console.error('XML解析错误:', error);
      MessagePlugin.error('XML解析错误');
    }
  } else {
    treeData.value = [];
  }
}, { immediate: true });
</script>

<style scoped>
.xml-editor-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.editor-content {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}

.xml-textarea {
  flex: 1;
}

.tree-container {
  flex: 1;
  border: 1px solid var(--td-component-border);
  border-radius: var(--td-radius-medium);
  padding: 16px;
  display: flex;
  flex-direction: column;
  max-height: 600px;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--td-component-border);
  position: sticky;
  top: 0;
  background-color: var(--td-bg-color-container);
  z-index: 1;
}

.tree-content {
  flex: 1;
  overflow: auto;
}

.tree-actions {
  display: flex;
  gap: 8px;
}

.attribute-item {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}
</style> 