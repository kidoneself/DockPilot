import { defineStore } from 'pinia';
import { MessagePlugin } from 'tdesign-vue-next';
import { getFavicon } from '@/api/system';
import { useRouter } from 'vue-router';
import { WebServer, CreateWebServerRequest, UpdateWebServerRequest, UpdateSortRequest, BatchUpdateSortRequest } from '@/api/model/webServerModel';
import { getWebServerListApi, createWebServerApi, updateWebServerApi, deleteWebServerApi } from '@/api/webServerApi';

// 定义应用项接口
export interface AppItem {
  id: string;
  name: string;
  icon: string;
  internalUrl: string;
  externalUrl: string;
  description: string;
  category: string;
  itemSort: number;
}

interface HomeState {
  webServers: WebServer[];
  categories: string[];
  newApp: CreateWebServerRequest;
  loading: boolean;
  backgroundBlur: number;
  backgroundMask: number;
  dashboardVisible: boolean;
  webServerListVisible: boolean;
  windowVisible: boolean;
  windowTitle: string;
  windowUrl: string;
  enteringBackend: boolean;
  webServerList: AppItem[];
  addAppVisible: boolean;
  isEdit: boolean;
  confirmVisible: boolean;
  confirmConfig: {
    title: string;
    content: string;
    onConfirm: () => void;
  };
  contextMenuVisible: boolean;
  contextMenuStyle: {
    position: string;
    left: string;
    top: string;
    zIndex: number;
  };
  currentContextItem: AppItem | null;
  isInternalNetwork: boolean;
  currentTab: string;
}

export const useHomeStore = defineStore('home', {
  state: (): HomeState => ({
    webServers: [],
    categories: [],
    newApp: {
      name: '',
      icon: '',
      internalUrl: '',
      externalUrl: '',
      description: '',
      category: '',
      itemSort: 0
    },
    loading: false,
    backgroundBlur: 0.5,
    backgroundMask: 0.3,
    dashboardVisible: false,
    webServerListVisible: true,
    windowVisible: false,
    windowTitle: '',
    windowUrl: '',
    enteringBackend: false,
    webServerList: [],
    addAppVisible: false,
    isEdit: false,
    confirmVisible: false,
    confirmConfig: {
      title: '',
      content: '',
      onConfirm: () => {}
    },
    contextMenuVisible: false,
    contextMenuStyle: {
      position: 'fixed',
      left: '0px',
      top: '0px',
      zIndex: 1000
    },
    currentContextItem: null,
    isInternalNetwork: true,
    currentTab: 'applist',
  }),
  
  actions: {
    // 应用点击处理
    handleAppClick(item: AppItem) {
      const url = this.isInternalNetwork ? item.internalUrl : item.externalUrl;
      if (!url) {
        MessagePlugin.error(`${this.isInternalNetwork ? '内网' : '外网'}地址未设置`);
        return;
      }
      window.open(url, '_blank');
    },
    
    // 编辑应用
    handleEditApp(item: AppItem) {
      this.isEdit = true;
      this.newApp = {
        id: item.id,
        name: item.name,
        icon: item.icon,
        internalUrl: item.internalUrl,
        externalUrl: item.externalUrl,
        description: item.description,
        category: item.category,
        itemSort: item.itemSort
      } as any;
      this.addAppVisible = true;
    },
    
    // 添加应用
    handleAddApp() {
      this.isEdit = false;
      this.newApp = {
        name: '',
        icon: '',
        internalUrl: '',
        externalUrl: '',
        description: '',
        category: '',
        itemSort: 0
      };
      this.addAppVisible = true;
    },
    
    // 删除应用
    handleDeleteApp(item: AppItem) {
      this.confirmConfig = {
        title: '确认删除',
        content: `确定要删除应用 "${item.name}" 吗？`,
        onConfirm: async () => {
          try {
            await deleteWebServerApi(item.id);
            MessagePlugin.success('删除成功');
            this.addAppVisible = false;
            this.confirmVisible = false;
            await this.fetchWebServers();
          } catch (error) {
            MessagePlugin.error('删除失败');
          }
        }
      };
      this.confirmVisible = true;
    },
    
    // 确认对话框取消
    handleConfirmCancel() {
      this.confirmVisible = false;
    },
    
    // 确认对话框确认
    handleConfirmConfirm() {
      this.confirmConfig.onConfirm();
    },
    
    // 提交应用表单
    async onSubmitApp(formData: Partial<CreateWebServerRequest>) {
      try {
        // 确保所有必填字段都有值
        const data: CreateWebServerRequest = {
          name: formData.name || '',
          icon: formData.icon || '',
          internalUrl: formData.internalUrl || '',
          externalUrl: formData.externalUrl || '',
          description: formData.description || '',
          category: formData.category || '默认分类',
          itemSort: formData.itemSort || 0
        };

        // 验证必填字段
        if (!data.name) {
          MessagePlugin.error('请输入应用名称');
          return;
        }
        if (!data.internalUrl && !data.externalUrl) {
          MessagePlugin.error('内网地址和外网地址至少填写一个');
          return;
        }

        const id = (formData as any).id || (this.newApp as any).id;
        if (this.isEdit && id) {
          // 编辑时调用更新接口
          await updateWebServerApi(id, data);
          MessagePlugin.success('更新成功');
        } else {
          // 新增时调用创建接口
          await this.createWebServer(data);
          MessagePlugin.success('添加成功');
        }
        this.addAppVisible = false;
        // 刷新列表
        await this.fetchWebServers();
      } catch (error) {
        MessagePlugin.error('操作失败');
        console.error('应用保存失败:', error);
      }
    },
    
    // 打开设置
    openSettings() {
      // 显示过渡动画
      this.enteringBackend = true;
      
      // 添加延迟，让过渡动画有足够的显示时间
      setTimeout(() => {
        window.location.href = '/docker/containers';
      }, 800);
    },
    
    // 切换网络模式
    toggleNetworkMode() {
      this.isInternalNetwork = !this.isInternalNetwork;
      MessagePlugin.info(`已切换到${this.isInternalNetwork ? '内网' : '外网'}模式`);
    },
    
    // 处理右键菜单
    handleContextMenu(payload: { event: MouseEvent, item: AppItem }) {
      const { event, item } = payload;
      event.preventDefault();
      this.currentContextItem = item;
      
      // 获取视口尺寸
      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;
      
      // 计算菜单位置,确保不会超出视口
      const menuWidth = 120; // 预估菜单宽度
      const menuHeight = 160; // 预估菜单高度
      
      let left = event.clientX;
      let top = event.clientY;
      
      // 如果菜单会超出右边界,则向左偏移
      if (left + menuWidth > viewportWidth) {
        left = viewportWidth - menuWidth;
      }
      
      // 如果菜单会超出下边界,则向上偏移
      if (top + menuHeight > viewportHeight) {
        top = viewportHeight - menuHeight;
      }
      
      this.contextMenuStyle = {
        position: 'fixed',
        left: `${left}px`,
        top: `${top}px`,
        zIndex: 1000
      };
      this.contextMenuVisible = true;
    },
    
    // 处理页面右键菜单
    handlePageContextMenu(event: MouseEvent) {
      // 如果点击的是应用卡片，不处理
      if ((event.target as HTMLElement).closest('.app-item')) {
        return;
      }
      
      this.currentContextItem = null;
      
      // 获取视口尺寸
      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;
      
      // 计算菜单位置
      const menuWidth = 120;
      const menuHeight = 40;
      
      let left = event.clientX;
      let top = event.clientY;
      
      if (left + menuWidth > viewportWidth) {
        left = viewportWidth - menuWidth;
      }
      
      if (top + menuHeight > viewportHeight) {
        top = viewportHeight - menuHeight;
      }
      
      this.contextMenuStyle = {
        position: 'fixed',
        left: `${left}px`,
        top: `${top}px`,
        zIndex: 1000
      };
      this.contextMenuVisible = true;
    },
    
    // 从菜单打开应用
    handleOpenAppFromMenu(payload: { type: 'internal' | 'external', item: AppItem }) {
      if (!payload.item) return;
      
      const url = payload.type === 'internal' 
        ? payload.item.internalUrl 
        : payload.item.externalUrl;
        
      if (!url) {
        MessagePlugin.error(`${payload.type === 'internal' ? '内网' : '外网'}地址未设置`);
        return;
      }
      
      window.open(url, '_blank');
      this.contextMenuVisible = false;
    },
    
    // 刷新页面
    handleRefresh() {
      window.location.reload();
    },
    
    // 处理右键菜单点击
    handleContextMenuClick(payload: { action: string, item: AppItem | null }) {
      const { action, item } = payload;
      
      switch (action) {
        case 'edit':
          if (item) this.handleEditApp(item);
          break;
        case 'refresh':
          this.handleRefresh();
          break;
      }
      
      this.contextMenuVisible = false;
    },
    
    // 切换视图
    toggleView() {
      if (this.currentTab === 'dashboard') {
        // 从仪表盘切换到应用列表
        this.dashboardVisible = false;
        
        // 直接切换视图，由transition组件处理动画效果
        this.currentTab = 'applist';
        
        // 延迟显示应用列表，让其有独立的入场动画
        setTimeout(() => {
          this.webServerListVisible = true;
        }, 300);
      } else {
        // 从应用列表切换到仪表盘
        this.webServerListVisible = false;
        
        // 直接切换视图，由transition组件处理动画效果
        this.currentTab = 'dashboard';
        
        // 延迟显示仪表盘，让其有独立的入场动画
        setTimeout(() => {
          this.dashboardVisible = true;
        }, 300);
      }
    },
    
    // 获取网站图标
    async handleGetFavicon(url: string, formData?: Partial<AppItem>) {
      try {
        const response = await getFavicon(url);
        if (response.code === 0 && response.data) {
          // 如果传入了表单数据，先更新 newApp 以保留用户输入
          if (formData) {
            this.newApp = { 
              name: formData.name || this.newApp.name,
              icon: this.newApp.icon, // 图标会在下一行更新
              internalUrl: formData.internalUrl || this.newApp.internalUrl,
              externalUrl: formData.externalUrl || this.newApp.externalUrl,
              description: formData.description || this.newApp.description,
              category: formData.category || this.newApp.category,
              itemSort: formData.itemSort || this.newApp.itemSort
            };
          }
          // 然后只更新图标
          this.newApp.icon = response.data;
          MessagePlugin.success('获取图标成功');
          return true; // 返回成功状态
        } else {
          MessagePlugin.error('获取图标失败');
          return false; // 返回失败状态
        }
      } catch (e) {
        MessagePlugin.error('获取图标失败');
        return false; // 返回失败状态
      }
    },
    
    // 初始化组件动画
    initAnimations() {
      // 初始化动画状态
      if (this.currentTab === 'dashboard') {
        setTimeout(() => {
          this.dashboardVisible = true;
        }, 300);
      } else {
        setTimeout(() => {
          this.webServerListVisible = true;
        }, 300);
      }
    },

    // 获取所有 Web 服务器
    async fetchWebServers() {
      this.loading = true;
      try {
        const response = await getWebServerListApi();
        if (response.code === 0) {
          this.webServers = response.data;
          // 更新 webServerList
          this.webServerList = this.webServers.map(server => ({
            id: server.id,
            name: server.name,
            icon: server.icon,
            internalUrl: server.internalUrl,
            externalUrl: server.externalUrl,
            description: server.description,
            category: server.category,
            itemSort: server.itemSort
          }));
        }
        this.loading = false;
      } catch (error) {
        this.loading = false;
        throw error;
      }
    },

    // 获取所有分类
    async fetchCategories() {
      try {
        // TODO: 调用 API 获取分类
      } catch (error) {
        throw error;
      }
    },

    // 创建 Web 服务器
    async createWebServer(data: CreateWebServerRequest) {
      try {
        const response = await createWebServerApi(data);
        if (response.code === 0) {
          MessagePlugin.success('创建成功');
          // 重新获取列表
          await this.fetchWebServers();
          return true;
        } else {
          MessagePlugin.error(response.message || '创建失败');
          return false;
        }
      } catch (error) {
        MessagePlugin.error('创建失败');
        throw error;
      }
    },

    // 更新 Web 服务器
    async updateWebServer(data: UpdateWebServerRequest) {
      try {
        // TODO: 调用 API 更新
      } catch (error) {
        throw error;
      }
    },

    // 删除 Web 服务器
    async deleteWebServer(id: string) {
      try {
        // TODO: 调用 API 删除
      } catch (error) {
        throw error;
      }
    },

    // 更新排序
    async updateSort(data: UpdateSortRequest) {
      try {
        // TODO: 调用 API 更新排序
      } catch (error) {
        throw error;
      }
    },

    // 批量更新排序
    async batchUpdateSort(data: BatchUpdateSortRequest) {
      try {
        // TODO: 调用 API 批量更新排序
      } catch (error) {
        throw error;
      }
    },

    // 重置新建表单
    resetNewApp() {
      this.newApp = {
        name: '',
        icon: '',
        internalUrl: '',
        externalUrl: '',
        description: '',
        category: '',
        itemSort: 0
      };
    },

    // 在 handleSaveApp 方法中
    handleSaveApp(formData: Partial<CreateWebServerRequest>) {
      if (this.isEdit) {
        this.newApp = {
          name: formData.name || this.newApp.name,
          icon: formData.icon || this.newApp.icon,
          internalUrl: formData.internalUrl || this.newApp.internalUrl,
          externalUrl: formData.externalUrl || this.newApp.externalUrl,
          description: formData.description || this.newApp.description,
          category: formData.category || this.newApp.category,
          itemSort: formData.itemSort || this.newApp.itemSort
        };
      }
    }
  },
  
  persist: {
    key: 'docker-manager-home',
    paths: ['isInternalNetwork', 'currentTab'],
  },
}); 