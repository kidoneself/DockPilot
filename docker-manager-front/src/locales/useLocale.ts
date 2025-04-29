// 删除国际化相关代码，只保留必要的结构
export function useLocale() {
  return {
    changeLocale: () => {
      // 空实现
    },
    getComponentsLocale: () => {
      // 返回空对象
      return {};
    }
  };
}
