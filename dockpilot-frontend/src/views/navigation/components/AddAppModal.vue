<template>
  <n-modal 
    v-model:show="visible" 
    preset="card" 
    style="max-width: 900px;"
    :title="editMode ? '编辑应用' : (currentTab === 'import' ? '导入浏览器书签' : '添加应用')"
    size="huge"
    :bordered="false"
    :segmented="false"
    :mask-closable="false"
    :close-on-esc="false"
    :closable="true"
    @update:show="handleVisibleChange"
  >

    <!-- 选项卡 -->
    <n-tabs v-if="!editMode" v-model:value="currentTab" type="line" animated>
      <n-tab-pane name="manual" tab="手动添加">
        <div class="add-app-form">
      <!-- 效果预览 -->
      <div class="preview-section">
        <div class="preview-options">
          <h4 style="margin: 0; color: #ffffff; font-size: 14px;">实时预览</h4>
          <span class="preview-tip">预览使用当前系统背景，效果更真实</span>
        </div>
        
        <!-- 预览区域 -->
        <div 
          class="preview-area real-background"
          :style="{ 
            backgroundImage: currentBackgroundImage ? `url(${currentBackgroundImage})` : 'none'
          }"
        >
          <!-- 透明度控制 - 简洁进度条 -->
          <div class="preview-opacity-slider">
            <n-slider
              v-model:value="opacityValue"
              :min="0"
              :max="100"
              :step="1"
              :tooltip="true"
              :format-tooltip="(value: number) => `透明度: ${value}%`"
              style="width: 150px;"
              @update:value="handleOpacityChange"
            />
          </div>
          
          <div 
            class="preview-card" 
            :class="{ 
              'preview-text': newApp.cardType === 'text',
              'preview-transparent': newApp.bgColor === 'transparent' || newApp.bgColor === 'rgba(0, 0, 0, 0)'
            }"
            :style="{ 
              backgroundColor: newApp.bgColor || 'rgba(42, 42, 42, 0.42)'
            }"
          >
            <div v-if="newApp.cardType !== 'text'" class="preview-icon">
              <!-- 文字图标 -->
              <span v-if="newApp.iconType === 'text'" class="preview-text-icon">
                {{ newApp.textContent || (newApp.title || 'A').charAt(0).toUpperCase() }}
              </span>
              <!-- 本地图标 -->
              <img 
                v-else-if="newApp.iconType === 'local' && newApp.iconUrl && !previewImageError" 
                :src="`/api/icons/${newApp.iconUrl}`" 
                alt=""
                @error="previewImageError = true"
                @load="previewImageError = false"
              >
              <div 
                v-else-if="newApp.iconType === 'local' && newApp.iconUrl && previewImageError"
                class="preview-fallback-icon"
                title="本地图标加载失败，显示文字图标"
              >
                {{ (newApp.title || 'A').charAt(0).toUpperCase() }}
              </div>
              <div 
                v-else-if="newApp.iconType === 'local' && !newApp.iconUrl"
                class="preview-placeholder-icon"
                title="请选择本地图标"
              >
                <n-icon :size="24" :component="ImageOutline" />
              </div>
              <!-- 在线图标 -->
              <img 
                v-else-if="newApp.iconType === 'online' && newApp.iconUrl && !previewImageError" 
                :src="newApp.iconUrl" 
                alt=""
                @error="previewImageError = true"
                @load="previewImageError = false"
              >
              <div 
                v-else-if="newApp.iconType === 'online' && newApp.iconUrl && previewImageError"
                class="preview-fallback-icon"
                title="在线图标加载失败，显示文字图标"
              >
                {{ (newApp.title || 'A').charAt(0).toUpperCase() }}
              </div>
              <div 
                v-else-if="newApp.iconType === 'online' && !newApp.iconUrl"
                class="preview-placeholder-icon"
                title="请输入网站地址获取图标"
              >
                <n-icon :size="24" :component="GlobeOutline" />
              </div>
            </div>
            <div class="preview-content">
              <div class="preview-title">{{ newApp.title || '应用标题' }}</div>
              <div class="preview-desc">{{ newApp.description || '应用描述' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 表单内容 -->
      <n-form :model="newApp" label-placement="top">
        <div class="form-row">
          <div class="form-item">
            <n-form-item label="分组" required>
              <div class="category-input-container">
                <n-select 
                  v-model:value="newApp.category" 
                  :options="categoryOptions"
                  placeholder="选择分组"
                  style="flex: 1;"
                />
                <n-button 
                  quaternary
                  type="primary"
                  @click="showAddCategoryModal = true"
                  style="margin-left: 8px;"
                >
                  <template #icon>
                    <n-icon><AddOutline /></n-icon>
                  </template>
                  添加分类
                </n-button>
              </div>
            </n-form-item>
          </div>
          <div class="form-item">
            <n-form-item label="卡片类型">
              <n-select 
                v-model:value="newApp.cardType" 
                :options="cardTypeOptions"
                placeholder="选择卡片类型"
              />
            </n-form-item>
          </div>
          <div class="form-item">
            <n-form-item label="卡片背景色">
              <div class="color-picker-section">
                <n-color-picker 
                  v-model:value="newApp.bgColor" 
                  :show-alpha="false"
                  :modes="['hex']"
                />
              </div>
            </n-form-item>
          </div>
        </div>

        <div class="form-row">
          <div class="form-item">
            <n-form-item label="标题" required>
              <n-input 
                v-model:value="newApp.title" 
                placeholder="请输入标题"
                :maxlength="20"
                show-count
              />
            </n-form-item>
          </div>
          <div class="form-item">
            <n-form-item label="描述信息">
              <n-input 
                v-model:value="newApp.description" 
                placeholder="请输入描述"
                :maxlength="100"
                show-count
              />
            </n-form-item>
          </div>
        </div>

        <n-form-item label="图标风格">
          <n-radio-group v-model:value="newApp.iconType">
            <n-radio value="text">文字</n-radio>
            <!-- <n-radio value="image">图片</n-radio> -->
            <n-radio value="local">本地图标</n-radio>
            <n-radio value="online">在线图标</n-radio>
          </n-radio-group>
        </n-form-item>

        <!-- 文字图标 - 输入文本内容 -->
        <n-form-item v-if="newApp.iconType === 'text'" label="文本内容">
          <div class="icon-input-container">
            <n-input 
              v-model:value="newApp.textContent" 
              placeholder="最多2个字符"
              :maxlength="2"
              show-count
            />
          </div>
        </n-form-item>

        <!-- 本地图标 - 选择图标 -->
        <n-form-item v-if="newApp.iconType === 'local'" label="选择图标">
          <div class="icon-input-container">
            <IconSelector @change="handleIconSelected" />
          </div>
        </n-form-item>

        <!-- 图片图标 - 输入地址或上传 -->
        <!-- <n-form-item v-if="newApp.iconType === 'image'" label="图像地址">
          <div class="icon-input-container">
            <n-input 
              v-model:value="newApp.iconUrl" 
              placeholder="输入图标地址或上传"
            />
            <n-button>本地上传</n-button>
          </div>
        </n-form-item> -->

        <!-- 在线图标 - 输入网站地址或图标地址 -->
        <n-form-item v-if="newApp.iconType === 'online'" label="网站地址">
          <div class="icon-input-container">
            <n-input 
              v-model:value="newApp.iconUrl" 
              placeholder="输入网站地址或直接输入图标地址"
            />
            <n-button quaternary @click="getWebsiteIcon">获取图标</n-button>
          </div>
        </n-form-item>

        <n-form-item label="外网地址">
          <n-input 
            v-model:value="newApp.url" 
            placeholder="http(s)://"
            @blur="handleUrlBlur('url')"
          />
        </n-form-item>

        <n-form-item label="内网地址">
          <n-input 
            v-model:value="newApp.internalUrl" 
            placeholder="http(s):// (内网环境，会跳转该地址)"
            @blur="handleUrlBlur('internalUrl')"
          />
        </n-form-item>

        <n-form-item label="打开方式">
          <n-radio-group v-model:value="newApp.openType">
            <!-- <n-radio value="current">当前窗口</n-radio> -->
            <n-radio value="new">新窗口</n-radio>
          </n-radio-group>
        </n-form-item>
              </n-form>
        </div>
      </n-tab-pane>
      
      <!-- 书签导入选项卡 -->
      <n-tab-pane name="import" tab="书签导入">
        <div class="bookmark-import">
          
          <!-- 第一步：文件上传 -->
          <div v-if="importStep === 1" class="upload-step">
            <div class="step-header">
              <h3>📂 步骤一：上传书签文件</h3>
              <p>支持Chrome、Edge等浏览器导出的HTML格式书签文件</p>
            </div>
            
            <n-upload
              :custom-request="handleBookmarkUpload"
              accept=".html"
              :show-file-list="false"
              :max="1"
            >
              <n-upload-dragger>
                <div style="margin-bottom: 12px">
                  <n-icon size="48" :depth="3">
                    <svg viewBox="0 0 24 24">
                      <path fill="currentColor" d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z"/>
                    </svg>
                  </n-icon>
                </div>
                <n-text style="font-size: 16px">
                  点击或者拖动文件到该区域来上传
                </n-text>
                <n-p depth="3" style="margin: 8px 0 0 0">
                  请上传浏览器导出的书签文件（.html格式）
                </n-p>
              </n-upload-dragger>
            </n-upload>
            
            <div class="upload-tips">
              <n-alert type="info" title="如何导出书签文件？">
                <div>
                  <p><strong>Chrome浏览器：</strong></p>
                  <p>1. 点击右上角三点菜单 → 书签 → 书签管理器</p>
                  <p>2. 点击右上角三点菜单 → 导出书签</p>
                  <p>3. 保存为HTML文件</p>
                  
                  <p style="margin-top: 12px;"><strong>Edge浏览器：</strong></p>
                  <p>1. 点击右上角三点菜单 → 收藏夹 → 管理收藏夹</p>
                  <p>2. 点击导出收藏夹 → 保存为HTML文件</p>
                </div>
              </n-alert>
            </div>
          </div>
          
          <!-- 第二步：选择导入 -->
          <div v-else-if="importStep === 2" class="selection-step">
            <div class="step-header">
              <h3>📋 步骤二：选择要导入的书签</h3>
              <p>解析到 {{ bookmarkParseResult?.totalCount || 0 }} 个书签，请选择要导入的内容</p>
            </div>
            
            <div class="bookmark-selection">
              <div class="selection-header">
                <div class="selection-left">
                  <n-checkbox 
                    :checked="isAllSelected" 
                    :indeterminate="isIndeterminate"
                    @update:checked="handleSelectAll"
                  >
                    全选
                  </n-checkbox>
                  <n-button 
                    text 
                    size="small" 
                    @click="toggleAllGroups"
                    style="margin-left: 12px;"
                  >
                    {{ allGroupsExpanded ? '全部收起' : '全部展开' }}
                  </n-button>
                </div>
                <span class="selection-count">
                  已选择 {{ selectedBookmarksCount }} / {{ totalBookmarksCount }} 个书签
                </span>
              </div>
              
              <div class="bookmark-tree">
                <div 
                  v-for="(group, index) in bookmarkParseResult?.groups" 
                  :key="group.name"
                  class="bookmark-group"
                >
                  <!-- 分组标题 -->
                  <div class="group-header" @click="toggleGroupExpanded(group.name)">
                    <div class="group-header-content">
                      <!-- 展开/收起图标 -->
                      <n-icon 
                        :size="16" 
                        class="expand-icon"
                        :class="{ 'expanded': isGroupExpanded(group.name) }"
                      >
                        <ChevronDownOutline v-if="isGroupExpanded(group.name)" />
                        <ChevronForwardOutline v-else />
                      </n-icon>
                      
                      <!-- 分组复选框 -->
                      <n-checkbox 
                        :checked="isGroupAllSelected(group)" 
                        :indeterminate="isGroupIndeterminate(group)"
                        @update:checked="(checked: boolean) => handleGroupSelect(group, checked)"
                        @click.stop
                      >
                        📁 {{ group.name }} ({{ group.items.length }})
                      </n-checkbox>
                    </div>
                  </div>
                  
                  <!-- 书签列表 - 可折叠 -->
                  <div 
                    v-show="isGroupExpanded(group.name)"
                    class="group-items"
                  >
                    <div 
                      v-for="item in group.items" 
                      :key="item.url"
                      class="bookmark-item"
                    >
                      <div class="item-indent"></div>
                      <n-checkbox 
                        v-model:checked="item.selected"
                        @update:checked="updateSelectionState"
                      >
                        <div class="item-content">
                          <div class="item-title">{{ item.title }}</div>
                          <div class="item-url">{{ item.url }}</div>
                        </div>
                      </n-checkbox>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 第三步：导入结果 -->
          <div v-else-if="importStep === 3" class="result-step">
            <div class="step-header">
              <h3>✅ 导入完成</h3>
            </div>
            
            <div class="import-result">
              <n-result 
                status="success" 
                title="书签导入成功"
                :description="importResult?.message || ''"
              >
                <template #footer>
                  <div class="result-stats">
                    <div class="stat-item">
                      <span class="stat-label">成功导入：</span>
                      <span class="stat-value">{{ importResult?.processedCount || 0 }} 个</span>
                    </div>
                    <div class="stat-item">
                      <span class="stat-label">跳过重复：</span>
                      <span class="stat-value">{{ importResult?.skippedCount || 0 }} 个</span>
                    </div>
                    <div class="stat-item">
                      <span class="stat-label">创建分组：</span>
                      <span class="stat-value">{{ importResult?.createdCategories?.length || 0 }} 个</span>
                    </div>
                  </div>
                  
                  <!-- 图标获取提示 -->
                  <div class="icon-fetch-notice">
                    <n-alert type="info" show-icon>
                      <template #icon>
                        <n-icon :component="GlobeOutline" />
                      </template>
                      📡 书签图标正在后台获取中，请稍后刷新页面查看完整图标
                    </n-alert>
                  </div>
                  
                  <div class="result-actions">
                    <n-button type="primary" @click="handleImportComplete">
                      完成
                    </n-button>
                    <n-button @click="resetImport">
                      继续导入
                    </n-button>
                  </div>
                </template>
              </n-result>
            </div>
          </div>
          
        </div>
      </n-tab-pane>
    </n-tabs>
    
    <!-- 编辑模式下直接显示表单 -->
    <div v-else class="add-app-form">
      <!-- 编辑模式的表单内容 -->
      <div class="preview-section">
        <div class="preview-options">
          <h4 style="margin: 0; color: #ffffff; font-size: 14px;">实时预览</h4>
          <span class="preview-tip">预览使用当前系统背景，效果更真实</span>
        </div>
        
        <!-- 预览区域 -->
        <div 
          class="preview-area real-background"
          :style="{ 
            backgroundImage: currentBackgroundImage ? `url(${currentBackgroundImage})` : 'none'
          }"
        >
          <!-- 透明度控制 -->
          <div class="preview-opacity-slider">
            <n-slider
              v-model:value="opacityValue"
              :min="0"
              :max="100"
              :step="1"
              :tooltip="true"
              :format-tooltip="(value: number) => `透明度: ${value}%`"
              style="width: 150px;"
              @update:value="handleOpacityChange"
            />
          </div>
          
          <div 
            class="preview-card" 
            :class="{ 
              'preview-text': newApp.cardType === 'text',
              'preview-transparent': newApp.bgColor === 'transparent' || newApp.bgColor === 'rgba(0, 0, 0, 0)'
            }"
            :style="{ 
              backgroundColor: newApp.bgColor || 'rgba(42, 42, 42, 0.42)'
            }"
          >
            <div v-if="newApp.cardType !== 'text'" class="preview-icon">
              <span v-if="newApp.iconType === 'text'" class="preview-text-icon">
                {{ newApp.textContent || (newApp.title || 'A').charAt(0).toUpperCase() }}
              </span>
              <!-- 本地图标 -->
              <img 
                v-else-if="newApp.iconType === 'local' && newApp.iconUrl && !previewImageError" 
                :src="`/api/icons/${newApp.iconUrl}`" 
                alt=""
                @error="previewImageError = true"
                @load="previewImageError = false"
              >
              <div 
                v-else-if="newApp.iconType === 'local' && newApp.iconUrl && previewImageError"
                class="preview-fallback-icon"
                title="本地图标加载失败，显示文字图标"
              >
                {{ (newApp.title || 'A').charAt(0).toUpperCase() }}
              </div>
              <div 
                v-else-if="newApp.iconType === 'local' && !newApp.iconUrl"
                class="preview-placeholder-icon"
                title="请选择本地图标"
              >
                <n-icon :size="24" :component="ImageOutline" />
              </div>
              <!-- 在线图标 -->
              <img 
                v-else-if="newApp.iconType === 'online' && newApp.iconUrl && !previewImageError" 
                :src="newApp.iconUrl" 
                alt=""
                @error="previewImageError = true"
                @load="previewImageError = false"
              >
              <div 
                v-else-if="newApp.iconType === 'online' && newApp.iconUrl && previewImageError"
                class="preview-fallback-icon"
                title="在线图标加载失败，显示文字图标"
              >
                {{ (newApp.title || 'A').charAt(0).toUpperCase() }}
              </div>
              <div 
                v-else-if="newApp.iconType === 'online' && !newApp.iconUrl"
                class="preview-placeholder-icon"
                title="请输入网站地址获取图标"
              >
                <n-icon :size="24" :component="GlobeOutline" />
              </div>
            </div>
            <div class="preview-content">
              <div class="preview-title">{{ newApp.title || '应用标题' }}</div>
              <div class="preview-desc">{{ newApp.description || '应用描述' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 编辑模式表单内容省略，与手动添加相同 -->
      <n-form :model="newApp" label-placement="top">
        <!-- 完整的表单内容，这里简化显示 -->
        <div class="form-row">
          <div class="form-item">
            <n-form-item label="分组" required>
              <div class="category-input-container">
                <n-select 
                  v-model:value="newApp.category" 
                  :options="categoryOptions"
                  placeholder="选择分组"
                  style="flex: 1;"
                />
                <n-button 
                  quaternary
                  type="primary"
                  @click="showAddCategoryModal = true"
                  style="margin-left: 8px;"
                >
                  <template #icon>
                    <n-icon><AddOutline /></n-icon>
                  </template>
                  添加分类
                </n-button>
              </div>
            </n-form-item>
          </div>
          <div class="form-item">
            <n-form-item label="卡片类型">
              <n-select 
                v-model:value="newApp.cardType" 
                :options="cardTypeOptions"
                placeholder="选择卡片类型"
              />
            </n-form-item>
          </div>
          <div class="form-item">
            <n-form-item label="卡片背景色">
              <div class="color-picker-section">
                <n-color-picker 
                  v-model:value="newApp.bgColor" 
                  :show-alpha="false"
                  :modes="['hex']"
                />
              </div>
            </n-form-item>
          </div>
        </div>

        <div class="form-row">
          <div class="form-item">
            <n-form-item label="标题" required>
              <n-input 
                v-model:value="newApp.title" 
                placeholder="请输入标题"
                :maxlength="20"
                show-count
              />
            </n-form-item>
          </div>
          <div class="form-item">
            <n-form-item label="描述信息">
              <n-input 
                v-model:value="newApp.description" 
                placeholder="请输入描述"
                :maxlength="100"
                show-count
              />
            </n-form-item>
          </div>
        </div>

        <n-form-item label="图标风格">
          <n-radio-group v-model:value="newApp.iconType">
            <n-radio value="text">文字</n-radio>
            <n-radio value="local">本地图标</n-radio>
            <n-radio value="online">在线图标</n-radio>
          </n-radio-group>
        </n-form-item>

        <n-form-item v-if="newApp.iconType === 'text'" label="文本内容">
          <div class="icon-input-container">
            <n-input 
              v-model:value="newApp.textContent" 
              placeholder="最多2个字符"
              :maxlength="2"
              show-count
            />
          </div>
        </n-form-item>

        <n-form-item v-if="newApp.iconType === 'local'" label="选择图标">
          <div class="icon-input-container">
            <IconSelector @change="handleIconSelected" />
          </div>
        </n-form-item>

        <n-form-item v-if="newApp.iconType === 'online'" label="网站地址">
          <div class="icon-input-container">
            <n-input 
              v-model:value="newApp.iconUrl" 
              placeholder="输入网站地址或直接输入图标地址"
            />
            <n-button quaternary @click="getWebsiteIcon">获取图标</n-button>
          </div>
        </n-form-item>

        <n-form-item label="外网地址">
          <n-input 
            v-model:value="newApp.url" 
            placeholder="http(s)://"
            @blur="handleUrlBlur('url')"
          />
        </n-form-item>

        <n-form-item label="内网地址">
          <n-input 
            v-model:value="newApp.internalUrl" 
            placeholder="http(s):// (内网环境，会跳转该地址)"
            @blur="handleUrlBlur('internalUrl')"
          />
        </n-form-item>

        <n-form-item label="打开方式">
          <n-radio-group v-model:value="newApp.openType">
            <n-radio value="new">新窗口</n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
    </div>

    <template #action>
      <div class="modal-actions">
        <n-button 
          v-if="editMode || currentTab === 'manual'" 
          type="primary" 
          @click="handleSave"
        >
          {{ editMode ? '更新' : '保存' }}
        </n-button>
        <n-button 
          v-if="currentTab === 'import' && !editMode" 
          type="primary" 
          @click="handleImportBookmarks"
        >
          开始导入
        </n-button>
      </div>
    </template>
  </n-modal>

  <!-- 添加分类弹窗 -->
  <n-modal
    v-model:show="showAddCategoryModal"
    preset="dialog"
    title="添加分类"
    positive-text="确定"
    negative-text="取消"
    @positive-click="handleCreateCategory"
  >
    <n-form ref="categoryFormRef" :model="newCategory" :rules="categoryRules">
      <n-form-item label="分类名称" path="name">
        <n-input 
          v-model:value="newCategory.name" 
          placeholder="请输入分类名称"
          :maxlength="50"
          show-count
          @keyup.enter="handleCreateCategory"
        />
      </n-form-item>
    </n-form>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { useMessage } from 'naive-ui'
import { getFavicon } from '@/api/http/system'
import { importSelectedBookmarks } from '@/api/http/bookmark'
import { getCurrentBackground } from '@/api/http/background'
import { createCategory, getAllCategoriesForManage, type CategoryDTO } from '@/api/http/category'
import { parseBookmarkContent, readFileAsText, type BookmarkParseResult } from '@/utils/bookmarkParser'
import defaultBackgroundImg from '@/assets/background.png'
import {
  ImageOutline,
  GlobeOutline,
  AddOutline,
  ChevronDownOutline,
  ChevronForwardOutline
} from '@vicons/ionicons5'
import IconSelector from '@/components/IconSelector.vue'

// Props
interface Props {
  modelValue?: boolean
  categoryOptions?: Array<{ label: string, value: any }>
  editMode?: boolean
  appData?: any
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false,
  categoryOptions: () => [],
  editMode: false,
  appData: null
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'save': [appData: any]
  'update': [appData: any]
}>()

const message = useMessage()

// 弹窗显示状态
const visible = ref(props.modelValue)

// 当前选项卡
const currentTab = ref('manual')

// 书签导入相关状态
const importStep = ref(1) // 1:上传 2:选择 3:结果
const bookmarkParseResult = ref<BookmarkParseResult | null>(null)
const importResult = ref<any>(null)

// 分组展开状态管理
const expandedGroups = ref<Record<string, boolean>>({})

// 预览区域图片错误状态
const previewImageError = ref(false)

// 当前背景图片
const currentBackgroundImage = ref('')

// 透明度滑块值
const opacityValue = ref(42) // 默认42%

// 添加分类相关状态
const showAddCategoryModal = ref(false)
const newCategory = ref({ name: '' })
const categoryFormRef = ref()

// 分类表单验证规则
const categoryRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 50, message: '分类名称长度应在1-50个字符之间', trigger: 'blur' }
  ]
}

// 新应用数据
const newApp = ref({
  category: '',
  cardType: 'normal',
  bgColor: '#2a2a2a6b',
  title: '',
  description: '',
  iconType: 'local', // 'text', 'image', 'online', 'local' - 默认本地图标
  textContent: '', // 文字图标的文本内容
  iconUrl: '', // 图片图标的地址
  websiteUrl: '', // 在线图标的网站地址
  url: '',
  internalUrl: '',
  openType: 'new' // 默认新窗口
})

// 卡片类型选项
const cardTypeOptions = ref([
  { label: '普通图标', value: 'normal' },
  { label: '文字卡片', value: 'text' }
])

// 监听编辑数据变化，回填表单
watch(() => props.appData, (newData) => {
  if (props.editMode && newData) {
    console.log('🔧 编辑模式回填数据:', newData)
    console.log('🏷️ categoryId:', newData.categoryId, '类型:', typeof newData.categoryId)
    console.log('📂 categoryOptions:', props.categoryOptions)
    
    // 回填编辑数据
    newApp.value = {
      category: newData.categoryId,
      cardType: newData.cardType || 'normal',
      bgColor: newData.bgColor || '#2a2a2a6b',
      title: newData.name || '',
      description: newData.description || '',
      iconType: newData.iconType || 'text',
      textContent: newData.iconType === 'text' ? newData.iconUrl : '',
      iconUrl: (newData.iconType === 'image' || newData.iconType === 'online' || newData.iconType === 'local') ? newData.iconUrl : '',
      websiteUrl: '',
      url: newData.externalUrl || '',
      internalUrl: newData.internalUrl || '',
      openType: newData.openType || 'new'
    }
    
    console.log('✅ 回填后的表单数据:', newApp.value)
  }
}, { immediate: true })

// 处理弹窗显示状态变化
const handleVisibleChange = (value: boolean) => {
  visible.value = value
  emit('update:modelValue', value)
}

// 关闭弹窗
const handleClose = () => {
  visible.value = false
  emit('update:modelValue', false)
  if (!props.editMode) {
    resetForm()
  }
}

// 保存应用
const handleSave = () => {
  if (!newApp.value.title || !newApp.value.category) {
    message.error('请填写必填字段（标题和分组）')
    return
  }

  if (props.editMode) {
    // 编辑模式，发出更新事件，包含原始ID
    emit('update', { 
      id: props.appData?.id,
      ...newApp.value 
    })
  } else {
    // 新增模式，发出保存事件
    emit('save', { ...newApp.value })
  }
  
  handleClose()
}

// 处理书签导入
const handleImportBookmarks = () => {
  if (importStep.value === 1) {
    // 从第一步到第二步，这里应该不会触发，因为文件上传后会自动跳转
    message.warning('请先上传书签文件')
  } else if (importStep.value === 2) {
    // 开始导入选中的书签
    startImportBookmarks()
  }
}

// 处理文件上传
const handleBookmarkUpload = async (options: any) => {
  const { file } = options
  const loadingMessage = message.loading('正在解析书签文件...', { duration: 0 })
  
  try {
    // 读取文件内容
    const htmlContent = await readFileAsText(file.file)
    console.log('📄 文件内容长度:', htmlContent.length)
    
    // 前端解析书签
    const result = await parseBookmarkContent(htmlContent)
    loadingMessage.destroy()
    
    bookmarkParseResult.value = result
    importStep.value = 2
    message.success(`书签文件解析成功！共解析到 ${result.totalCount} 个书签`)
  } catch (error: any) {
    loadingMessage.destroy()
    console.error('解析书签文件失败:', error)
    message.error(error.message || '解析书签文件失败，请检查文件格式')
  }
}

// 开始导入书签
const startImportBookmarks = async () => {
  if (!bookmarkParseResult.value?.groups) {
    message.error('没有可导入的书签数据')
    return
  }
  
  // 收集选中的书签
  const selectedBookmarks = bookmarkParseResult.value.groups
    .flatMap((group: any) => group.items)
    .filter((item: any) => item.selected)
    .map((item: any) => ({
      title: item.title,
      url: item.url,
      groupName: item.groupName
    }))
  
  if (selectedBookmarks.length === 0) {
    message.warning('请选择要导入的书签')
    return
  }
  
  const loadingMessage = message.loading(`正在导入 ${selectedBookmarks.length} 个书签...`, { duration: 0 })
  
  try {
    const result = await importSelectedBookmarks(selectedBookmarks)
    loadingMessage.destroy()
    
    // 响应拦截器已经处理了ApiResponse，直接使用返回的BookmarkImportResult
    importResult.value = result
    importStep.value = 3
    message.success('书签导入完成！')
  } catch (error: any) {
    loadingMessage.destroy()
    console.error('导入书签失败:', error)
    message.error(error.message || '导入书签失败，请稍后重试')
  }
}

// 完成导入
const handleImportComplete = () => {
  handleClose()
  // 刷新页面数据
  emit('save', null) // 通知父组件刷新
}

// 重置导入流程
const resetImport = () => {
  importStep.value = 1
  bookmarkParseResult.value = null
  importResult.value = null
}

// 计算属性：全选状态
const isAllSelected = computed(() => {
  if (!bookmarkParseResult.value?.groups) return false
  return bookmarkParseResult.value.groups.every(group => 
    group.items.every(item => item.selected)
  )
})

// 计算属性：半选状态
const isIndeterminate = computed(() => {
  if (!bookmarkParseResult.value?.groups) return false
  const allItems = bookmarkParseResult.value.groups.flatMap(group => group.items)
  const selectedItems = allItems.filter(item => item.selected)
  return selectedItems.length > 0 && selectedItems.length < allItems.length
})

// 计算属性：已选择数量
const selectedBookmarksCount = computed(() => {
  if (!bookmarkParseResult.value?.groups) return 0
  return bookmarkParseResult.value.groups
    .flatMap(group => group.items)
    .filter(item => item.selected).length
})

// 计算属性：总数量
const totalBookmarksCount = computed(() => {
  if (!bookmarkParseResult.value?.groups) return 0
  return bookmarkParseResult.value.groups
    .flatMap(group => group.items).length
})

// 全选/取消全选
const handleSelectAll = (checked: boolean) => {
  if (!bookmarkParseResult.value?.groups) return
  bookmarkParseResult.value.groups.forEach(group => {
    group.items.forEach(item => {
      item.selected = checked
    })
  })
}

// 分组选择
const handleGroupSelect = (group: any, checked: boolean) => {
  group.items.forEach((item: any) => {
    item.selected = checked
  })
}

// 检查分组是否全选
const isGroupAllSelected = (group: any) => {
  return group.items.every((item: any) => item.selected)
}

// 检查分组是否半选
const isGroupIndeterminate = (group: any) => {
  const selectedItems = group.items.filter((item: any) => item.selected)
  return selectedItems.length > 0 && selectedItems.length < group.items.length
}

// 更新选择状态
const updateSelectionState = () => {
  // 这个方法在checkbox变化时自动触发，用于更新相关状态
}

// 树形结构展开/收起管理
const toggleGroupExpanded = (groupName: string) => {
  expandedGroups.value[groupName] = !expandedGroups.value[groupName]
}

const isGroupExpanded = (groupName: string) => {
  return expandedGroups.value[groupName] !== false // 默认展开
}

// 书签解析完成后，初始化展开状态
watch(() => bookmarkParseResult.value, (newResult) => {
  if (newResult?.groups) {
    // 初始化所有分组为展开状态
    const newExpandedState: Record<string, boolean> = {}
    newResult.groups.forEach(group => {
      newExpandedState[group.name] = true
    })
    expandedGroups.value = newExpandedState
  }
})

// 全部展开/收起功能
const allGroupsExpanded = computed(() => {
  if (!bookmarkParseResult.value?.groups) return true
  return bookmarkParseResult.value.groups.every(group => 
    isGroupExpanded(group.name)
  )
})

const toggleAllGroups = () => {
  if (!bookmarkParseResult.value?.groups) return
  
  const shouldExpand = !allGroupsExpanded.value
  const newExpandedState: Record<string, boolean> = {}
  
  bookmarkParseResult.value.groups.forEach(group => {
    newExpandedState[group.name] = shouldExpand
  })
  
  expandedGroups.value = newExpandedState
}



// 重置表单
const resetForm = () => {
  newApp.value = {
    category: '',
    cardType: 'normal',
    bgColor: '#2a2a2a6b',
    title: '',
    description: '',
    iconType: 'local', // 默认本地图标
    textContent: '',
    iconUrl: '',
    websiteUrl: '',
    url: '',
    internalUrl: '',
    openType: 'new' // 默认新窗口
  }
  previewImageError.value = false
  opacityValue.value = 42 // 重置透明度为42%
}

// 智能补齐协议（与父组件保持一致）
const formatUrl = (url: string): string => {
  if (!url || url.trim() === '') {
    return ''
  }
  
  const trimmedUrl = url.trim()
  
  // 如果已经有协议，直接返回
  if (trimmedUrl.startsWith('http://') || trimmedUrl.startsWith('https://')) {
    return trimmedUrl
  }
  
  // 如果是本地地址（IP或localhost），使用http
  if (trimmedUrl.match(/^(localhost|127\.0\.0\.1|192\.168\.|10\.|172\.(1[6-9]|2[0-9]|3[01])\.|::1)/)) {
    return `http://${trimmedUrl}`
  }
  
  // 其他情况默认使用https
  return `https://${trimmedUrl}`
}

// 获取在线图标
const getWebsiteIcon = async () => {
  if (!newApp.value.iconUrl) {
    message.warning('请先输入网站地址')
    return
  }

  // 智能补齐协议
  const formattedUrl = formatUrl(newApp.value.iconUrl)
  
  // 如果地址被自动补齐，先更新输入框显示
  if (newApp.value.iconUrl !== formattedUrl) {
    console.log(`🔗 地址自动补齐: "${newApp.value.iconUrl}" → "${formattedUrl}"`)
    newApp.value.iconUrl = formattedUrl
    message.info('已自动补齐协议')
  }

  const loadingMessage = message.loading('正在获取网站图标...', { duration: 0 })
  
  try {
    // 使用格式化后的地址获取图标
    const faviconUrl = await getFavicon(formattedUrl)
    
    loadingMessage.destroy() // 关闭加载提示
    
    if (faviconUrl) {
      // 直接覆盖 iconUrl，实现回填效果
      newApp.value.iconUrl = faviconUrl
      previewImageError.value = false // 重置预览错误状态
      message.success('图标获取成功！')
      console.log('获取在线图标成功:', faviconUrl)
    } else {
      message.warning('未能获取到网站图标，请手动输入图片地址')
    }
  } catch (error) {
    loadingMessage.destroy() // 关闭加载提示
    console.error('获取在线图标失败:', error)
    message.error('获取图标失败，请检查网址是否正确')
  }
}

// 处理透明度滑块变化
const handleOpacityChange = (value: number) => {
  const currentColor = newApp.value.bgColor
  const opacity = value / 100 // 转换为0-1范围
  
  if (value === 0) {
    newApp.value.bgColor = 'transparent'
    return
  }
  
  // 如果当前是透明，使用默认灰色
  if (currentColor === 'transparent') {
    const alphaHex = Math.round(opacity * 255).toString(16).padStart(2, '0')
    newApp.value.bgColor = `#2a2a2a${alphaHex}`
    return
  }
  
  // 处理十六进制颜色
  if (currentColor.startsWith('#')) {
    let baseColor = currentColor
    if (currentColor.length === 9) {
      // 移除现有的透明度
      baseColor = currentColor.slice(0, 7)
    }
    const alphaHex = Math.round(opacity * 255).toString(16).padStart(2, '0')
    newApp.value.bgColor = `${baseColor}${alphaHex}`
    return
  }
  
  // 处理rgba格式 - 转换为十六进制
  if (currentColor.startsWith('rgba')) {
    const match = currentColor.match(/rgba\((.*?),(.*?),(.*?),.*?\)/)
    if (match) {
      const r = parseInt(match[1].trim())
      const g = parseInt(match[2].trim())
      const b = parseInt(match[3].trim())
      const baseHex = '#' + [r, g, b].map(x => x.toString(16).padStart(2, '0')).join('')
      const alphaHex = Math.round(opacity * 255).toString(16).padStart(2, '0')
      newApp.value.bgColor = `${baseHex}${alphaHex}`
      return
    }
  }
  
  // 其他情况，使用默认颜色
  const alphaHex = Math.round(opacity * 255).toString(16).padStart(2, '0')
  newApp.value.bgColor = `#2a2a2a${alphaHex}`
}

// 加载背景图片 - 直接使用默认背景
const loadCurrentBackground = () => {
  // 直接使用默认背景图片
  currentBackgroundImage.value = defaultBackgroundImg
  console.log('✅ 应用预览使用默认背景图片:', defaultBackgroundImg)
}

// 监听外部可见状态变化
watch(() => props.modelValue, (newValue) => {
  visible.value = newValue
  // 当弹窗打开时重新加载背景
  if (newValue) {
    loadCurrentBackground()
  }
})

// 监听弹窗visible状态变化
watch(() => visible.value, (newValue) => {
  if (newValue) {
    // 弹窗打开时重新加载背景
    loadCurrentBackground()
  }
})



// 监听颜色变化，同步透明度滑块
watch(() => newApp.value.bgColor, (newColor) => {
  if (newColor === 'transparent') {
    opacityValue.value = 0
  } else if (newColor.startsWith('#') && newColor.length === 9) {
    const alpha = parseInt(newColor.slice(7, 9), 16)
    opacityValue.value = Math.round((alpha / 255) * 100)
  } else if (newColor.startsWith('rgba')) {
    const match = newColor.match(/rgba\(.*?,.*?,.*?,(.*?)\)/)
    if (match) {
      opacityValue.value = Math.round(parseFloat(match[1]) * 100)
    }
  } else {
    // 纯色，设置为100%
    opacityValue.value = 100
  }
})

// 处理本地图标选择
const handleIconSelected = (iconName: string) => {
  console.log('选择的图标:', iconName)
  newApp.value.iconUrl = iconName // 存储选中的图标名称
  previewImageError.value = false // 重置预览错误状态
  message.success(`已选择图标: ${iconName}`)
}

// 处理地址输入框失去焦点，自动补齐协议
const handleUrlBlur = (field: 'url' | 'internalUrl') => {
  const currentValue = field === 'url' ? newApp.value.url : newApp.value.internalUrl
  
  if (!currentValue || currentValue.trim() === '') {
    return
  }
  
  const formattedUrl = formatUrl(currentValue)
  
  // 如果地址被自动补齐，更新输入框并提示用户
  if (currentValue !== formattedUrl) {
    console.log(`🔗 ${field === 'url' ? '外网' : '内网'}地址自动补齐: "${currentValue}" → "${formattedUrl}"`)
    
    if (field === 'url') {
      newApp.value.url = formattedUrl
    } else {
      newApp.value.internalUrl = formattedUrl
    }
    
    message.info('已自动补齐协议')
  }
}

// 创建新分类
const handleCreateCategory = async () => {
  try {
    // 表单验证
    await categoryFormRef.value?.validate()
    
    const loadingMessage = message.loading('正在创建分类...', { duration: 0 })
    
    // 调用API创建分类
    const categoryId = await createCategory({
      name: newCategory.value.name.trim()
    })
    
    // 保存分类名称用于提示
    const categoryName = newCategory.value.name
    
    loadingMessage.destroy()
    
    // 重置表单
    newCategory.value.name = ''
    showAddCategoryModal.value = false
    
    // 通知父组件刷新分类列表
    emit('save', { type: 'categoryCreated', data: { categoryId } })
    
    message.success(`分类"${categoryName}"创建成功！请在下拉框中选择该分类`)
    console.log('✅ 分类创建成功，ID:', categoryId)
    
  } catch (error: any) {
    console.error('❌ 创建分类失败:', error)
    const errorMessage = error.message || '创建分类失败，请稍后重试'
    message.error(errorMessage)
    
    // 阻止弹窗关闭
    return false
  }
}



onMounted(() => {
  // 加载预览背景
  loadCurrentBackground()
})
</script>

<style scoped>
/* 添加应用弹窗样式 */
.add-app-form {
  padding: 20px 0;
}

.preview-section {
  margin-bottom: 24px;
}

.preview-options {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.preview-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  font-style: italic;
}

.preview-area {
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 120px;
  position: relative;
  overflow: hidden;
}

.preview-opacity-slider {
  position: absolute;
  top: 12px;
  right: 16px;
  z-index: 10;
  opacity: 0.9;
  transition: opacity 0.2s ease;
}

.preview-opacity-slider:hover {
  opacity: 1;
}

.preview-area.real-background {
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  /* 添加备用灰色背景，防止背景图片加载失败 */
  background-color: #2a2a2a;
}

.preview-area.real-background::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  pointer-events: none;
  will-change: auto;
}

.preview-card {
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

/* 透明预览卡片样式 */
.preview-card.preview-transparent {
  background: transparent !important;
  backdrop-filter: none !important;
  border: none !important;
  box-shadow: none !important;
}

.preview-card.preview-transparent .preview-title,
.preview-card.preview-transparent .preview-desc {
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.8);
}

.preview-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  font-size: 20px;
  font-weight: 700;
}

/* 透明卡片中的预览图标 */
.preview-card.preview-transparent .preview-icon {
  background: transparent;
}

.preview-icon img {
  width: 36px;
  height: 36px;
  border-radius: 8px;
}

/* 预览区域的降级图标样式 */
.preview-fallback-icon,
.preview-text-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

/* 预览区域的占位符图标样式 */
.preview-placeholder-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  border: 2px dashed rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
}

.preview-content {
  flex: 1;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 2px;
  line-height: 1.3;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.preview-desc {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.3;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

/* 预览文字卡片样式 */
.preview-card.preview-text {
  min-width: 180px;
  padding: 12px 16px;
  border-radius: 8px;
}

.preview-card.preview-text .preview-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 2px;
}

.preview-card.preview-text .preview-desc {
  font-size: 11px;
  opacity: 0.8;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.form-item {
  min-width: 0;
}

.color-picker-section {
  width: 100%;
}

/* 统一的图标输入容器样式 */
.icon-input-container {
  display: flex;
  gap: 8px;
  width: 100%;
}

/* 分类输入容器样式 */
.category-input-container {
  display: flex;
  align-items: center;
  width: 100%;
}

.icon-input-container .n-input {
  flex: 1;
}

.icon-input-container .n-button {
  flex-shrink: 0;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 书签导入样式 */
.bookmark-import {
  min-height: 400px;
}

.step-header {
  text-align: center;
  margin-bottom: 24px;
}

/* 浅色模式 */
.step-header h3 {
  margin: 0 0 8px 0;
  color: #1f2937;
  font-size: 18px;
}

.step-header p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

/* 深色模式 */
[data-theme="dark"] .step-header h3 {
  color: #ffffff;
}

[data-theme="dark"] .step-header p {
  color: rgba(255, 255, 255, 0.7);
}

.upload-step {
  max-width: 500px;
  margin: 0 auto;
}

.upload-tips {
  margin-top: 24px;
}

.selection-step {
  max-width: 800px;
  margin: 0 auto;
}

/* 浅色模式 */
.selection-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  margin-bottom: 16px;
}

.selection-left {
  display: flex;
  align-items: center;
}

/* 深色模式 */
[data-theme="dark"] .selection-header {
  background: rgba(255, 255, 255, 0.05);
}

/* 浅色模式 */
.selection-count {
  color: #6b7280;
  font-size: 14px;
}

/* 深色模式 */
[data-theme="dark"] .selection-count {
  color: rgba(255, 255, 255, 0.7);
}

.bookmark-tree {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

/* 浅色模式 */
.bookmark-group {
  margin-bottom: 16px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.02);
}

/* 深色模式 */
[data-theme="dark"] .bookmark-group {
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.02);
}

/* 浅色模式 */
.group-header {
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.03);
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  font-weight: 600;
  color: #1f2937;
  position: sticky;
  top: 0;
  z-index: 10;
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: background-color 0.2s ease;
  user-select: none;
}

.group-header:hover {
  background: rgba(0, 0, 0, 0.06);
}

.group-header-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expand-icon {
  transition: transform 0.2s ease;
  color: #6b7280;
  flex-shrink: 0;
}

.expand-icon.expanded {
  transform: rotate(0deg);
}

/* 深色模式 */
[data-theme="dark"] .group-header {
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

[data-theme="dark"] .group-header:hover {
  background: rgba(255, 255, 255, 0.08);
}

[data-theme="dark"] .expand-icon {
  color: #9ca3af;
}

.group-items {
  padding: 4px 8px 12px 8px;
  border-left: 2px solid rgba(59, 130, 246, 0.2);
  margin-left: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
}

/* 深色模式下的左边框 */
[data-theme="dark"] .group-items {
  border-left-color: rgba(59, 130, 246, 0.3);
}

.bookmark-item {
  padding: 6px 8px;
  border-radius: 6px;
  margin-bottom: 2px;
  transition: background-color 0.2s ease;
  display: flex;
  align-items: flex-start;
  gap: 4px;
  margin-left: 8px;
  position: relative;
}

/* 浅色模式 */
.bookmark-item:hover {
  background: rgba(0, 0, 0, 0.05);
}

/* 深色模式 */
[data-theme="dark"] .bookmark-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.bookmark-item :deep(.n-checkbox) {
  display: flex;
  align-items: flex-start;
  width: 100%;
}

.bookmark-item :deep(.n-checkbox__label) {
  display: flex;
  flex: 1;
  min-width: 0;
}

.item-indent {
  width: 20px;
  flex-shrink: 0;
  position: relative;
}

.item-indent::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 50%;
  width: 8px;
  height: 1px;
  background: rgba(156, 163, 175, 0.5);
}

/* 深色模式下的连接线 */
[data-theme="dark"] .item-indent::before {
  background: rgba(107, 114, 128, 0.5);
}

.item-content {
  margin-left: 8px;
  flex: 1;
  min-width: 0;
}

/* 浅色模式 - 默认 */
.item-title {
  font-size: 14px;
  color: #1f2937 !important;
  margin-bottom: 2px;
  font-weight: 500;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-url {
  font-size: 12px;
  color: #6b7280 !important;
  word-break: break-all;
  line-height: 1.3;
  max-width: 100%;
}

/* 深色模式 */
[data-theme="dark"] .item-title {
  color: #e2e8f0 !important;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

[data-theme="dark"] .item-url {
  color: #94a3b8 !important;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.result-step {
  text-align: center;
  max-width: 600px;
  margin: 0 auto;
}

.result-stats {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #10b981;
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

/* 弹窗响应式 */
@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .preview-card {
    min-width: 160px;
    padding: 12px;
  }
  
  .preview-icon {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }
  
  .preview-icon img {
    width: 24px;
    height: 24px;
  }
}
</style> 