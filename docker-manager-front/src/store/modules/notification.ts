import { defineStore } from 'pinia';

import type { NotificationItem } from '@/types/interface';

const msgData: NotificationItem[] = [];

type MsgDataType = typeof msgData;

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    msgData,
  }),
  getters: {
    unreadMsg: (state) => state.msgData.filter((item: NotificationItem) => item.status),
    readMsg: (state) => state.msgData.filter((item: NotificationItem) => !item.status),
  },
  actions: {
    setMsgData(data: MsgDataType) {
      this.msgData = data;
    },
    addNotification(notification: NotificationItem) {
      this.msgData.unshift(notification);
    },
    handleWebSocketNotification(data: any) {
      if (data && typeof data === 'object') {
        const notification: NotificationItem = {
          id: data.id || String(Date.now()),
          content: data.content || '',
          type: data.type || '系统通知',
          status: data.status ?? true,
          collected: data.collected ?? false,
          date: data.date || new Date().toLocaleString(),
          quality: data.quality || 'high',
        };
        this.addNotification(notification);
      }
    },
  },
  persist: true,
});
