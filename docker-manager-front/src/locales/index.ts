// 删除所有国际化相关代码，只保留一个简单的t函数
export const t = (key: string) => {
  // 这里可以根据需要添加中文映射
  return key;
};

export default {
  install: (app: any) => {
    // 空实现，保持接口一致
  }
};
